/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.outerj.daisy.workflow.serverimpl;

import org.outerj.daisy.workflow.commonimpl.WfPoolStrategy;
import org.outerj.daisy.workflow.commonimpl.WfPoolImpl;
import org.outerj.daisy.workflow.WfPool;
import org.outerj.daisy.workflow.WorkflowException;
import org.outerj.daisy.workflow.WfPoolNotFoundException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ConcurrentUpdateException;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.jdbcutil.SqlCounter;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.util.List;
import java.util.ArrayList;
import java.sql.*;

public class LocalWfPoolStrategy implements WfPoolStrategy {
    private SqlCounter poolCounter;
    private DataSource dataSource;
    private JdbcHelper jdbcHelper;
    private static final String SELECT_POOL = "select id, name_, description, last_modified, last_modifier, updatecount from wf_pools";
    private final Log log = LogFactory.getLog(getClass());

    public LocalWfPoolStrategy(DataSource dataSource) {
        this.dataSource = dataSource;
        this.poolCounter = new SqlCounter("wfpool_sequence", dataSource, log);
        this.jdbcHelper = JdbcHelper.getInstance(dataSource, log);
    }

    public void store(WfPoolImpl pool, Repository repository) throws WorkflowException {
        WfPoolImpl.IntimateAccess poolInt = pool.getIntimateAccess(this);

        if (!poolInt.getRepository().isInRole(Role.ADMINISTRATOR))
            throw new WorkflowException("Only Administrators can create or update pools.");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);

            // check the name isn't already in use
            stmt = conn.prepareStatement("select id from wf_pools where name_ = ?");
            stmt.setString(1, pool.getName());
            rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                if (id != pool.getId())
                    throw new WorkflowException("There is already a workflow pool with the following name: \"" + pool.getName() + "\", the pool with id " + id);
            }
            rs.close();
            stmt.close();

            // start creating the new entry
            long id = pool.getId();
            java.util.Date lastModified = new java.util.Date();
            long lastModifier = poolInt.getRepository().getUserId();

            if (id == -1) {
                // insert new record
                id = poolCounter.getNextId();

                stmt = conn.prepareStatement("insert into wf_pools(id, name_, description, last_modified, last_modifier, updatecount) values(?,?,?,?,?,?)");
                stmt.setLong(1, id);
                stmt.setString(2, pool.getName());
                stmt.setString(3, pool.getDescription());
                stmt.setTimestamp(4, new Timestamp(lastModified.getTime()));
                stmt.setLong(5, lastModifier);
                stmt.setLong(6, 1L);
                stmt.execute();
                stmt.close();
            } else {
                // update existing record

                // check if nobody else changed it in the meantime
                stmt = conn.prepareStatement("select updatecount from wf_pools where id = ? " + jdbcHelper.getSharedLockClause());
                stmt.setLong(1, id);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new WfPoolNotFoundException(id);
                } else {
                    long dbUpdateCount = rs.getLong(1);
                    if (dbUpdateCount != pool.getUpdateCount())
                        throw new ConcurrentUpdateException(WfPool.class.getName(), String.valueOf(id));
                }
                stmt.close(); // closes resultset too

                // update the record
                stmt = conn.prepareStatement("update wf_pools set name_=?, description=?, last_modified=?, last_modifier=?, updatecount=? where id = ?");
                stmt.setString(1, pool.getName());
                stmt.setString(2, pool.getDescription());
                stmt.setTimestamp(3, new Timestamp(lastModified.getTime()));
                stmt.setLong(4, lastModifier);
                stmt.setLong(5, pool.getUpdateCount() + 1);
                stmt.setLong(6, id);
                stmt.execute();
                stmt.close();
            }

            conn.commit();

            poolInt.setId(id);
            poolInt.setLastModified(lastModified);
            poolInt.setLastModifier(lastModifier);
            poolInt.setUpdateCount(pool.getUpdateCount() + 1);
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new WorkflowException("Problem storing workflow pool.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public WfPoolImpl loadPoolUsingConnection(long id, Repository repository, Connection conn) throws SQLException, WfPoolNotFoundException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(SELECT_POOL + " where id = ?");
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new WfPoolNotFoundException(id);

            return getPoolFromResultSet(rs, repository);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }

    }

    private WfPoolImpl getPoolFromResultSet(ResultSet rs, Repository repository) throws SQLException {
        WfPoolImpl pool = new WfPoolImpl(rs.getString("name_"), this, repository);
        WfPoolImpl.IntimateAccess poolInt = pool.getIntimateAccess(this);
        poolInt.setId(rs.getLong("id"));
        pool.setDescription(rs.getString("description"));
        poolInt.setLastModified(rs.getTimestamp("last_modified"));
        poolInt.setLastModifier(rs.getLong("last_modifier"));
        poolInt.setUpdateCount(rs.getLong("updatecount"));
        return pool;
    }

    public WfPool getPool(long id, Repository repository) throws WorkflowException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            return loadPoolUsingConnection(id, repository, conn);
        } catch (Throwable e) {
            if (e instanceof WorkflowException)
                throw (WorkflowException)e;
            throw new WorkflowException("Error loading workflow pool.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public WfPool getPoolByName(String name, Repository repository) throws WorkflowException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement(SELECT_POOL + " where name_ = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new WfPoolNotFoundException(name);

            return getPoolFromResultSet(rs, repository);
        } catch (Throwable e) {
            if (e instanceof WfPoolNotFoundException)
                throw (WfPoolNotFoundException)e;
            throw new WorkflowException("Error loading workflow pool.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public List<WfPool> getPools(Repository repository) throws WorkflowException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement(SELECT_POOL + " order by name_");
            ResultSet rs = stmt.executeQuery();

            List<WfPool> pools = new ArrayList<WfPool>();

            while (rs.next()) {
                pools.add(getPoolFromResultSet(rs, repository));
            }

            return pools;
        } catch (Throwable e) {
            throw new WorkflowException("Error loading workflow pools.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public List<WfPool> getPoolsForUser(long userId, Repository repository) throws WorkflowException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement(SELECT_POOL + ", wf_pool_members where wf_pool_members.pool_id = wf_pools.id and wf_pool_members.user_id = ? order by name_");
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            List<WfPool> pools = new ArrayList<WfPool>();

            while (rs.next()) {
                pools.add(getPoolFromResultSet(rs, repository));
            }

            return pools;
        } catch (Throwable e) {
            throw new WorkflowException("Error loading workflow pools for user " + userId, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void addUsersToPool(long poolId, List<Long> userIds, Repository repository) throws WorkflowException {
        if (!repository.isInRole(Role.ADMINISTRATOR))
            throw new WorkflowException("Only Administrators can change workflow pools.");

        Connection conn = null;
        PreparedStatement poolCheckStmt = null;
        PreparedStatement poolMemberCheckStmt = null;
        PreparedStatement poolMemberAddStmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);

            poolCheckStmt = conn.prepareStatement(SELECT_POOL + " where id = ?");
            poolCheckStmt.setLong(1, poolId);
            ResultSet rs = poolCheckStmt.executeQuery();
            if (!rs.next())
                throw new WfPoolNotFoundException(poolId);

            poolMemberCheckStmt = conn.prepareStatement("select 1 from wf_pool_members where pool_id = ? and user_id = ?");
            poolMemberAddStmt = conn.prepareStatement("insert into wf_pool_members (pool_id, user_id, added, adder) values(?,?,?,?)");
            for (long userId : userIds) {
                poolMemberCheckStmt.setLong(1, poolId);
                poolMemberCheckStmt.setLong(2, userId);
                rs = poolMemberCheckStmt.executeQuery();
                if (!rs.next()) {
                    poolMemberAddStmt.setLong(1, poolId);
                    poolMemberAddStmt.setLong(2, userId);
                    poolMemberAddStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    poolMemberAddStmt.setLong(4, repository.getUserId());
                    poolMemberAddStmt.execute();
                }
            }
            conn.commit();
        } catch (Throwable e) {
            throw new WorkflowException("Error adding users to pool.", e);
        } finally {
            jdbcHelper.closeStatement(poolCheckStmt);
            jdbcHelper.closeStatement(poolMemberCheckStmt);
            jdbcHelper.closeStatement(poolMemberAddStmt);
            jdbcHelper.closeConnection(conn);
        }

    }

    public void removeUsersFromPool(long poolId, List<Long> userIds, Repository repository) throws WorkflowException {
        if (!repository.isInRole(Role.ADMINISTRATOR))
            throw new WorkflowException("Only Administrators can change workflow pools.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement(SELECT_POOL + " where id = ?");
            stmt.setLong(1, poolId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                throw new WfPoolNotFoundException(poolId);
            stmt.close();

            stmt = conn.prepareStatement("delete from wf_pool_members where pool_id = ? and user_id = ?");
            stmt.setLong(1, poolId);
            for (long userId : userIds) {
                stmt.setLong(2, userId);
                stmt.execute();
            }
            conn.commit();
        } catch (Throwable e) {
            throw new WorkflowException("Error deleting users from pool.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void clearPool(long poolId, Repository repository) throws WorkflowException {
        if (!repository.isInRole(Role.ADMINISTRATOR))
            throw new WorkflowException("Only Administrators can change workflow pools.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement(SELECT_POOL + " where id = ?");
            stmt.setLong(1, poolId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                throw new WfPoolNotFoundException(poolId);
            stmt.close();

            stmt = conn.prepareStatement("delete from wf_pool_members where pool_id = ?");
            stmt.setLong(1, poolId);
            stmt.execute();
            conn.commit();
        } catch (Throwable e) {
            throw new WorkflowException("Error clearing pool " + poolId, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public List<Long> getUsersForPool(long poolId, Repository repository) throws WorkflowException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement(SELECT_POOL + " where id = ?");
            stmt.setLong(1, poolId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                throw new WfPoolNotFoundException(poolId);
            stmt.close();

            stmt = conn.prepareStatement("select user_id from wf_pool_members where pool_id = ?");
            stmt.setLong(1, poolId);
            rs = stmt.executeQuery();

            List<Long> userIdsList = new ArrayList<Long>();
            while (rs.next()) {
                userIdsList.add(rs.getLong(1));
            }

            return userIdsList;
        } catch (Throwable e) {
            throw new WorkflowException("Error loading users for workflow pool " + poolId, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void deletePool(long poolId, Repository repository) throws RepositoryException {
        if (!repository.isInRole(Role.ADMINISTRATOR))
            throw new WorkflowException("Only Administrators can delete workflow pools.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement("delete from wf_pool_members where pool_id = ?");
            stmt.setLong(1, poolId);
            stmt.execute();
            stmt.close();

            stmt = conn.prepareStatement("delete from wf_pools where id = ?");
            stmt.setLong(1, poolId);
            stmt.execute();

            conn.commit();
        } catch (Throwable e) {
            throw new WorkflowException("Error deleting pool " + poolId, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }
}
