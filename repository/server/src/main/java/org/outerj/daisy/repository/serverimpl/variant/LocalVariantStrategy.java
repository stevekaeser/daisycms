/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository.serverimpl.variant;

import org.outerj.daisy.repository.commonimpl.variant.VariantStrategy;
import org.outerj.daisy.repository.commonimpl.variant.BranchImpl;
import org.outerj.daisy.repository.commonimpl.variant.LanguageImpl;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.serverimpl.LocalRepositoryManager;
import org.outerj.daisy.jdbcutil.SqlCounter;
import org.outerj.daisy.repository.serverimpl.EventHelper;
import org.outerj.daisy.repository.variant.BranchNotFoundException;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.ConcurrentUpdateException;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.apache.xmlbeans.XmlObject;
import org.outerx.daisy.x10.*;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class LocalVariantStrategy implements VariantStrategy {
    private LocalRepositoryManager.Context context;
    private JdbcHelper jdbcHelper;
    private static final String SELECT_BRANCH = "select id, name, description, last_modified, last_modifier, updatecount from branches";
    private static final String SELECT_LANGUAGE = "select id, name, description, last_modified, last_modifier, updatecount from languages";
    private SqlCounter branchCounter;
    private SqlCounter languageCounter;
    private EventHelper eventHelper;

    public LocalVariantStrategy(LocalRepositoryManager.Context context, JdbcHelper jdbcHelper) {
        this.context = context;
        this.jdbcHelper = jdbcHelper;
        this.eventHelper = new EventHelper(context, jdbcHelper);
        branchCounter = new SqlCounter("branch_sequence", context.getDataSource(), context.getLogger());
        languageCounter = new SqlCounter("language_sequence", context.getDataSource(), context.getLogger());
    }

    public BranchImpl getBranch(long id, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            return loadBranchUsingConnection(id, user, conn);
        } catch (Throwable e) {
            if (e instanceof RepositoryException)
                throw (RepositoryException)e;
            throw new RepositoryException("Error loading branch.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public BranchImpl loadBranchUsingConnection(long id, AuthenticatedUser user, Connection conn) throws SQLException, BranchNotFoundException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(SELECT_BRANCH + " where id = ?");
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new BranchNotFoundException(id);

            return getBranchFromResultSet(rs, user);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }

    }

    public BranchImpl getBranchByName(String name, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement(SELECT_BRANCH + " where name = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new BranchNotFoundException(name);

            return getBranchFromResultSet(rs, user);
        } catch (Throwable e) {
            if (e instanceof BranchNotFoundException)
                throw (BranchNotFoundException)e;
            throw new RepositoryException("Error loading branch.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public BranchImpl[] getAllBranches(AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement(SELECT_BRANCH + " order by name");
            ResultSet rs = stmt.executeQuery();

            List<BranchImpl> branches = new ArrayList<BranchImpl>();

            while (rs.next()) {
                branches.add(getBranchFromResultSet(rs, user));
            }

            return branches.toArray(new BranchImpl[branches.size()]);
        } catch (Throwable e) {
            throw new RepositoryException("Error loading branches.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    private BranchImpl getBranchFromResultSet(ResultSet rs, AuthenticatedUser user) throws SQLException {
        BranchImpl branch = new BranchImpl(this, rs.getString("name"), user);
        BranchImpl.IntimateAccess branchInt = branch.getIntimateAccess(this);
        branchInt.setId(rs.getLong("id"));
        branch.setDescription(rs.getString("description"));
        branchInt.setLastModified(rs.getTimestamp("last_modified"));
        branchInt.setLastModifier(rs.getLong("last_modifier"));
        branchInt.setUpdateCount(rs.getLong("updatecount"));
        return branch;
    }

    public void storeBranch(BranchImpl branch) throws RepositoryException {
        BranchImpl.IntimateAccess branchInt = branch.getIntimateAccess(this);

        if (!branchInt.getCurrentUser().isInAdministratorRole())
            throw new RepositoryException("Only Administrators can create or update branches.");

        if (branch.getId() == 1)
            throw new RepositoryException("The system branch with ID 1 cannot be modified.");

        boolean isNew = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // check the name isn't already in use
            stmt = conn.prepareStatement("select id from branches where name = ?");
            stmt.setString(1, branch.getName());
            rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                if (id != branch.getId())
                    throw new RepositoryException("There is already a branch with the following name: \"" + branch.getName() + "\", the branch with id " + id);
            }
            rs.close();
            stmt.close();

            // start creating the new entry
            long id = branch.getId();
            java.util.Date lastModified = new java.util.Date();
            long lastModifier = branchInt.getCurrentUser().getId();
            XmlObject eventDescription;

            if (id == -1) {
                isNew = true;
                // insert new record
                id = getNextBranchId();

                stmt = conn.prepareStatement("insert into branches(id, name, description, last_modified, last_modifier, updatecount) values(?,?,?,?,?,?)");
                stmt.setLong(1, id);
                stmt.setString(2, branch.getName());
                stmt.setString(3, branch.getDescription());
                stmt.setTimestamp(4, new Timestamp(lastModified.getTime()));
                stmt.setLong(5, lastModifier);
                stmt.setLong(6, 1L);
                stmt.execute();
                stmt.close();

                eventDescription = createBranchCreatedEvent(branch, id, lastModified);
            } else {
                // update existing record

                // check if nobody else changed it in the meantime
                stmt = conn.prepareStatement("select updatecount from branches where id = ? " + jdbcHelper.getSharedLockClause());
                stmt.setLong(1, id);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new BranchNotFoundException(id);
                } else {
                    long dbUpdateCount = rs.getLong(1);
                    if (dbUpdateCount != branch.getUpdateCount())
                        throw new ConcurrentUpdateException(Branch.class.getName(), String.valueOf(branch.getId()));
                }
                stmt.close(); // closes resultset too

                BranchImpl oldBranch = getBranch(branch.getId(), branchInt.getCurrentUser());
                long newUpdateCount = branch.getUpdateCount() + 1;

                // update the record
                stmt = conn.prepareStatement("update branches set name=?, description=?, last_modified=?, last_modifier=?, updatecount=? where id = ?");
                stmt.setString(1, branch.getName());
                stmt.setString(2, branch.getDescription());
                stmt.setTimestamp(3, new Timestamp(lastModified.getTime()));
                stmt.setLong(4, lastModifier);
                stmt.setLong(5, branch.getUpdateCount() + 1);
                stmt.setLong(6, id);
                stmt.execute();
                stmt.close();

                eventDescription = createBranchUpdatedEvent(oldBranch, branch, lastModified, newUpdateCount);
            }

            eventHelper.createEvent(eventDescription, isNew ? "BranchCreated" : "BranchUpdated", conn);

            conn.commit();

            branchInt.setId(id);
            branchInt.setLastModified(lastModified);
            branchInt.setLastModifier(lastModifier);
            branchInt.setUpdateCount(branch.getUpdateCount() + 1);
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Problem storing branch", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        if (isNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.BRANCH_CREATED, new Long(branch.getId()), branch.getUpdateCount());
        else
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.BRANCH_UPDATED, new Long(branch.getId()), branch.getUpdateCount());
    }

    private BranchCreatedDocument createBranchCreatedEvent(BranchImpl branch, long id, java.util.Date lastModified) {
        BranchCreatedDocument branchCreatedDocument = BranchCreatedDocument.Factory.newInstance();
        BranchCreatedDocument.BranchCreated branchCreated = branchCreatedDocument.addNewBranchCreated();

        BranchDocument.Branch branchXml = branch.getXml().getBranch();
        branchXml.setLastModified(getCalendar(lastModified));
        branchXml.setLastModifier(branch.getIntimateAccess(this).getCurrentUser().getId());
        branchXml.setUpdateCount(1);
        branchXml.setId(id);
        branchCreated.addNewNewBranch().setBranch(branchXml);

        return branchCreatedDocument;
    }

    private BranchUpdatedDocument createBranchUpdatedEvent(BranchImpl oldBranch, BranchImpl branch, java.util.Date lastModified, long newUpdateCount) {
        BranchUpdatedDocument branchUpdatedDocument = BranchUpdatedDocument.Factory.newInstance();
        BranchUpdatedDocument.BranchUpdated branchUpdated = branchUpdatedDocument.addNewBranchUpdated();

        branchUpdated.addNewOldBranch().setBranch(oldBranch.getXml().getBranch());

        BranchDocument.Branch branchXml = branch.getXml().getBranch();
        branchXml.setLastModified(getCalendar(lastModified));
        branchXml.setLastModifier(branch.getIntimateAccess(this).getCurrentUser().getId());
        branchXml.setUpdateCount(newUpdateCount);
        branchUpdated.addNewNewBranch().setBranch(branchXml);

        return branchUpdatedDocument;
    }

    public void deleteBranch(long id, AuthenticatedUser user) throws RepositoryException {
        if (!user.isInAdministratorRole())
            throw new RepositoryException("Only Administrators can delete branches.");

        if (id == 1)
            throw new RepositoryException("The system branch with ID 1 cannot be deleted.");

        // Note: foreign key constraints on the database will prevent deletion of
        // branches that are still in use.
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            BranchImpl deletedBranch = loadBranchUsingConnection(id, user, conn);

            stmt = conn.prepareStatement("select count(*) from document_variants where branch_id = ?");
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            if (rs.getLong(1) > 0)
                throw new RepositoryException("Branch " + id + " is still in use by " + rs.getLong(1) + " document variants.");
            stmt.close();

            stmt = conn.prepareStatement("delete from branches where id = ?");
            stmt.setLong(1, id);
            stmt.execute();

            XmlObject eventDescription = createBranchDeletedEvent(deletedBranch, user);
            eventHelper.createEvent(eventDescription, "BranchDeleted", conn);

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            if (e instanceof BranchNotFoundException)
                throw (BranchNotFoundException)e;
            throw new RepositoryException("Error deleting branch " + id, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.BRANCH_DELETED, new Long(id), -1);
    }

    private BranchDeletedDocument createBranchDeletedEvent(BranchImpl branch, AuthenticatedUser user) {
        BranchDeletedDocument branchDeletedDocument = BranchDeletedDocument.Factory.newInstance();
        BranchDeletedDocument.BranchDeleted branchDeletedXml = branchDeletedDocument.addNewBranchDeleted();

        branchDeletedXml.setDeleterId(user.getId());
        branchDeletedXml.setDeletedTime(getCalendar(new Date()));
        branchDeletedXml.addNewDeletedBranch().setBranch(branch.getXml().getBranch());

        return branchDeletedDocument;
    }

    private Calendar getCalendar(java.util.Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }

    private long getNextBranchId() throws SQLException {
        return branchCounter.getNextId();
    }

    private long getNextLanguageId() throws SQLException {
        return languageCounter.getNextId();
    }

    public LanguageImpl getLanguage(long id, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            return loadLanguageUsingConnection(id, user, conn);
        } catch (Throwable e) {
            if (e instanceof RepositoryException)
                throw (RepositoryException)e;
            throw new RepositoryException("Error loading language.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }


    private LanguageImpl loadLanguageUsingConnection(long id, AuthenticatedUser user, Connection conn) throws RepositoryException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(SELECT_LANGUAGE + " where id = ?");
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new LanguageNotFoundException(id);

            return getLanguageFromResultSet(rs, user);
        } catch (Throwable e) {
            if (e instanceof LanguageNotFoundException)
                throw (LanguageNotFoundException)e;
            throw new RepositoryException("Error loading language.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    public LanguageImpl getLanguageByName(String name, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement(SELECT_LANGUAGE + " where name = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new LanguageNotFoundException(name);

            return getLanguageFromResultSet(rs, user);
        } catch (Throwable e) {
            if (e instanceof LanguageNotFoundException)
                throw (LanguageNotFoundException)e;
            throw new RepositoryException("Error loading language.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public LanguageImpl[] getAllLanguages(AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement(SELECT_LANGUAGE + " order by name");
            ResultSet rs = stmt.executeQuery();

            List<LanguageImpl> languages = new ArrayList<LanguageImpl>();

            while (rs.next()) {
                languages.add(getLanguageFromResultSet(rs, user));
            }

            return languages.toArray(new LanguageImpl[languages.size()]);
        } catch (Throwable e) {
            throw new RepositoryException("Error loading languages.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    private LanguageImpl getLanguageFromResultSet(ResultSet rs, AuthenticatedUser user) throws SQLException {
        LanguageImpl language = new LanguageImpl(this, rs.getString("name"), user);
        LanguageImpl.IntimateAccess languageInt = language.getIntimateAccess(this);
        languageInt.setId(rs.getLong("id"));
        language.setDescription(rs.getString("description"));
        languageInt.setLastModified(rs.getTimestamp("last_modified"));
        languageInt.setLastModifier(rs.getLong("last_modifier"));
        languageInt.setUpdateCount(rs.getLong("updatecount"));
        return language;
    }

    public void storeLanguage(LanguageImpl language) throws RepositoryException {
        LanguageImpl.IntimateAccess languageInt = language.getIntimateAccess(this);

        if (!languageInt.getCurrentUser().isInAdministratorRole())
            throw new RepositoryException("Only Administrators can create or update languages.");

        if (language.getId() == 1)
            throw new RepositoryException("The system language with ID 1 cannot be modified.");

        boolean isNew = false;
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // check the name isn't already in use
            stmt = conn.prepareStatement("select id from languages where name = ?");
            stmt.setString(1, language.getName());
            rs = stmt.executeQuery();
            if (rs.next()) {
                long id = rs.getLong(1);
                if (id != language.getId())
                    throw new RepositoryException("There is already a language with the following name: \"" + language.getName() + "\", the language with id " + id);
            }
            rs.close();
            stmt.close();

            // start creating the new entry
            long id = language.getId();
            java.util.Date lastModified = new java.util.Date();
            long lastModifier = languageInt.getCurrentUser().getId();
            XmlObject eventDescription;

            if (id == -1) {
                isNew = true;
                // insert new record
                id = getNextLanguageId();

                stmt = conn.prepareStatement("insert into languages(id, name, description, last_modified, last_modifier, updatecount) values(?,?,?,?,?,?)");
                stmt.setLong(1, id);
                stmt.setString(2, language.getName());
                stmt.setString(3, language.getDescription());
                stmt.setTimestamp(4, new Timestamp(lastModified.getTime()));
                stmt.setLong(5, lastModifier);
                stmt.setLong(6, 1L);
                stmt.execute();
                stmt.close();

                eventDescription = createLanguageCreatedEvent(language, id, lastModified);
            } else {
                // update existing record

                // check if nobody else changed it in the meantime
                stmt = conn.prepareStatement("select updatecount from languages where id = ? " + jdbcHelper.getSharedLockClause());
                stmt.setLong(1, id);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new LanguageNotFoundException(id);
                } else {
                    long dbUpdateCount = rs.getLong(1);
                    if (dbUpdateCount != language.getUpdateCount())
                        throw new ConcurrentUpdateException(Language.class.getName(), String.valueOf(language.getId()));
                }
                stmt.close(); // closes resultset too

                LanguageImpl oldLanguage = getLanguage(language.getId(), languageInt.getCurrentUser());
                long newUpdateCount = language.getUpdateCount() + 1;

                // update the record
                stmt = conn.prepareStatement("update languages set name=?, description=?, last_modified=?, last_modifier=?, updatecount=? where id = ?");
                stmt.setString(1, language.getName());
                stmt.setString(2, language.getDescription());
                stmt.setTimestamp(3, new Timestamp(lastModified.getTime()));
                stmt.setLong(4, lastModifier);
                stmt.setLong(5, language.getUpdateCount() + 1);
                stmt.setLong(6, id);
                stmt.execute();
                stmt.close();

                eventDescription = createLanguageUpdatedEvent(oldLanguage, language, lastModified, newUpdateCount);
            }

            eventHelper.createEvent(eventDescription, isNew ? "LanguageCreated" : "LanguageUpdated", conn);

            conn.commit();

            languageInt.setId(id);
            languageInt.setLastModified(lastModified);
            languageInt.setLastModifier(lastModifier);
            languageInt.setUpdateCount(language.getUpdateCount() + 1);
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Problem storing language", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        if (isNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.LANGUAGE_CREATED, new Long(language.getId()), language.getUpdateCount());
        else
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.LANGUAGE_UPDATED, new Long(language.getId()), language.getUpdateCount());
    }

    private LanguageCreatedDocument createLanguageCreatedEvent(LanguageImpl language, long id, java.util.Date lastModified) {
        LanguageCreatedDocument languageCreatedDocument = LanguageCreatedDocument.Factory.newInstance();
        LanguageCreatedDocument.LanguageCreated languageCreated = languageCreatedDocument.addNewLanguageCreated();

        LanguageDocument.Language languageXml = language.getXml().getLanguage();
        languageXml.setLastModified(getCalendar(lastModified));
        languageXml.setLastModifier(language.getIntimateAccess(this).getCurrentUser().getId());
        languageXml.setUpdateCount(1);
        languageXml.setId(id);
        languageCreated.addNewNewLanguage().setLanguage(languageXml);

        return languageCreatedDocument;
    }

    private LanguageUpdatedDocument createLanguageUpdatedEvent(LanguageImpl oldLanguage, LanguageImpl language, java.util.Date lastModified, long newUpdateCount) {
        LanguageUpdatedDocument languageUpdatedDocument = LanguageUpdatedDocument.Factory.newInstance();
        LanguageUpdatedDocument.LanguageUpdated languageUpdated = languageUpdatedDocument.addNewLanguageUpdated();

        languageUpdated.addNewOldLanguage().setLanguage(oldLanguage.getXml().getLanguage());

        LanguageDocument.Language languageXml = language.getXml().getLanguage();
        languageXml.setLastModified(getCalendar(lastModified));
        languageXml.setLastModifier(language.getIntimateAccess(this).getCurrentUser().getId());
        languageXml.setUpdateCount(newUpdateCount);
        languageUpdated.addNewNewLanguage().setLanguage(languageXml);

        return languageUpdatedDocument;
    }

    public void deleteLanguage(long id, AuthenticatedUser user) throws RepositoryException {
        if (!user.isInAdministratorRole())
            throw new RepositoryException("Only Administrators can delete languages.");

        if (id == 1)
            throw new RepositoryException("The system language with ID 1 cannot be deleted.");

        // Note: foreign key constraints on the database will prevent deletion of
        // languages that are still in use.
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            LanguageImpl deletedLanguage = loadLanguageUsingConnection(id, user, conn);

            stmt = conn.prepareStatement("select count(*) from document_variants where lang_id = ?");
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            rs.next();
            if (rs.getLong(1) > 0)
                throw new RepositoryException("Language " + id + " is still in use by " + rs.getLong(1) + " document variants.");
            stmt.close();

            stmt = conn.prepareStatement("delete from languages where id = ?");
            stmt.setLong(1, id);
            stmt.execute();

            XmlObject eventDescription = createLanguageDeletedEvent(deletedLanguage, user);
            eventHelper.createEvent(eventDescription, "LanguageDeleted", conn);

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            if (e instanceof BranchNotFoundException)
                throw (BranchNotFoundException)e;
            throw new RepositoryException("Error deleting language " + id, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.LANGUAGE_DELETED, new Long(id), -1);
    }

    private LanguageDeletedDocument createLanguageDeletedEvent(LanguageImpl language, AuthenticatedUser user) {
        LanguageDeletedDocument languageDeletedDocument = LanguageDeletedDocument.Factory.newInstance();
        LanguageDeletedDocument.LanguageDeleted languageDeletedXml = languageDeletedDocument.addNewLanguageDeleted();

        languageDeletedXml.setDeleterId(user.getId());
        languageDeletedXml.setDeletedTime(getCalendar(new Date()));
        languageDeletedXml.addNewDeletedLanguage().setLanguage(language.getXml().getLanguage());

        return languageDeletedDocument;
    }
}
