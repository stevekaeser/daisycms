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
package org.outerj.daisy.repository.revisions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.credentialsprovider.CredentialsProvider;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryListener;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.revision.DateRange;
import org.outerj.daisy.repository.revision.DateRangeImpl;
import org.outerj.daisy.repository.revision.RepositoryRevisionManager;
import org.outerj.daisy.repository.spi.ExtensionProvider;

public class CommonRevisionManager implements RepositoryListener {

    private CredentialsProvider credentialsProvider;
    private String repositoryKey;
    private RepositoryManager repositoryManager;
    private Repository repository;
    private JdbcHelper jdbcHelper;
    private DataSource dataSource;
    private PluginRegistry pluginRegistry;
    private ExtensionProvider myExtensionProvider = new MyExtensionProvider();

    private final Log log = LogFactory.getLog(getClass());

    public CommonRevisionManager(Configuration configuration, RepositoryManager repositoryManager,
            PluginRegistry pluginRegistry, CredentialsProvider credentialsProvider, DataSource dataSource) throws RepositoryException {
        this.repositoryManager = repositoryManager;
        this.pluginRegistry = pluginRegistry;
        this.credentialsProvider = credentialsProvider;
        this.dataSource = dataSource;
        configure(configuration);
        initialize();
        
    }
    
    public void configure(Configuration configuration) {
        repositoryKey = configuration.getChild("repositoryKey").getValue("internal");
    }
  
    public void initialize() throws RepositoryException {
        repository = repositoryManager.getRepository(credentialsProvider.getCredentials(repositoryKey));
        repository.addListener(this);

        jdbcHelper = JdbcHelper.getInstance(dataSource, log);   

        pluginRegistry.addPlugin(ExtensionProvider.class, "RevisionManager", myExtensionProvider);
    }
    
    @PreDestroy
    public void destroy() {
        repository.removeListener(this);
        pluginRegistry.removePlugin(ExtensionProvider.class, "RevisionManager", myExtensionProvider);
    }

    public class MyExtensionProvider implements ExtensionProvider {

        public Object createExtension(Repository repository) {
            return new RepositoryRevisionManager() {

                public DateRange getRevisionDateRange(Date date) {
                    Connection conn = null;
                    PreparedStatement stmt = null;
                    try {
                        Date begin = null;
                        Date end = null;

                        conn = dataSource.getConnection();
                        stmt = conn.prepareStatement("select max(revision_date) from repository_revisions where revision_date <= ?");
                        stmt.setTimestamp(1, new Timestamp(date.getTime()));
                        ResultSet rs = stmt.executeQuery();
                        if (rs.next()) {
                            begin = rs.getTimestamp(1);
                        }
                        rs.close();

                        stmt = conn.prepareStatement("select min(revision_date) from repository_revisions where revision_date > ?");
                        stmt.setTimestamp(1, new Timestamp(date.getTime()));
                        rs = stmt.executeQuery();
                        if (rs.next()) {
                            end = rs.getTimestamp(1);
                        }
                        return new DateRangeImpl(begin, end);

                    } catch (SQLException e) {
                        throw new RuntimeException("Failed to normalize date", e);
                    } finally {
                        jdbcHelper.closeStatement(stmt);
                        jdbcHelper.closeConnection(conn);
                    }
                }
                
            };
        }
    }
    
    public void repositoryEvent(RepositoryEventType eventType, Object id,
            long updateCount) {
        if (eventType.isVariantEvent()) {
            DocId docId = DocId.parseDocId(((VariantKey)id).getDocumentId(), repository);
            updateRepositoryRevisionDates(docId);
        } else if (eventType.isDocumentEvent()) {
            DocId docId = DocId.parseDocId((String)id, repository);
            updateRepositoryRevisionDates(docId);
        }
    }
    
    /**
     * returns an array with two dates. These dates represent the date-range between which navigation trees must be invalidated.
     * @param documentId
     * @return
     */
    public void updateRepositoryRevisionDates(DocId docId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);
            stmt = conn.prepareStatement("select ns_id from documents where ns_id = ? and doc_id = ? " + jdbcHelper.getSharedLockClause());
            
            Date minDate = getMinLiveHistoryDate(docId, conn);
            if (minDate != null) {
                Date oldMinDate = getMinRepositoryRevisionDate(docId, conn);
                if (oldMinDate == null) minDate = null;
                else minDate = oldMinDate.before(minDate) ? oldMinDate : minDate;
            } else {
                minDate = null;
            }
            
            Date maxDate = getMaxLiveHistoryDate(docId, conn);
            if (maxDate != null) {
                Date oldMaxDate = getMaxRepositoryRevisionDate(docId, conn);
                if (oldMaxDate == null) maxDate = null;
                else maxDate = oldMaxDate.after(maxDate) ? oldMaxDate : maxDate;
            }
            
            // take a lock on the document to make sure the copied data are consistent
            stmt = conn.prepareStatement("select ns_id, doc_id from documents where ns_id = ? and doc_id = ?" + jdbcHelper.getSharedLockClause());
            
            stmt = conn.prepareStatement("delete from repository_revisions where ns_id = ? and doc_id = ?");
            stmt.setLong(1, docId.getNamespaceId());
            stmt.setLong(2, docId.getSeqId());
            stmt.executeUpdate();
            
            stmt = conn.prepareStatement("insert into repository_revisions(ns_id, doc_id, revision_date) select ns_id, doc_id, begin_date from live_history where ns_id = ? and doc_id = ?");
            stmt.setLong(1, docId.getNamespaceId());
            stmt.setLong(2, docId.getSeqId());
            stmt.executeUpdate();

            stmt = conn.prepareStatement("insert into repository_revisions(ns_id, doc_id, revision_date) select ns_id, doc_id, end_date from live_history where end_date is not null and ns_id = ? and doc_id = ?");
            stmt.setLong(1, docId.getNamespaceId());
            stmt.setLong(2, docId.getSeqId());
            stmt.executeUpdate();

            conn.commit();
        } catch (Exception e) {
            jdbcHelper.rollback(conn);
            throw new RuntimeException("Failed to update repository revision dates", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    private Date getMinRepositoryRevisionDate(DocId docId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select min(revision_date) from repository_revisions where ns_id = ? and doc_id = ?");
            stmt.setLong(1, docId.getNamespaceId());
            stmt.setLong(2, docId.getSeqId());
            ResultSet rs = stmt.executeQuery();
            rs.next();
            return rs.getDate(1);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private Date getMaxRepositoryRevisionDate(DocId docId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select max(revision_date) from repository_revisions where ns_id = ? and doc_id = ?");
            stmt.setLong(1, docId.getNamespaceId());
            stmt.setLong(2, docId.getSeqId());
            ResultSet rs = stmt.executeQuery();
            rs.next();
            
            return rs.getDate(1);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private Date getMinLiveHistoryDate(DocId docId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select min(begin_date) from live_history where ns_id = ? and doc_id = ?");
            stmt.setLong(1, docId.getNamespaceId());
            stmt.setLong(2, docId.getSeqId());
            ResultSet rs = stmt.executeQuery();
            rs.next();
            
            return rs.getDate(1);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }
    
    private Date getMaxLiveHistoryDate(DocId docId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("select 1 from dual where exists (select end_date from live_history where end_date is null and ns_id = ? and doc_id = ?)");
            stmt.setLong(1, docId.getNamespaceId());
            stmt.setLong(2, docId.getSeqId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next())
                return null;
            
            stmt = conn.prepareStatement("select max(end_date) from live_history where ns_id = ? and doc_id = ?");
            stmt.setLong(1, docId.getNamespaceId());
            stmt.setLong(2, docId.getSeqId());
            rs = stmt.executeQuery();
            rs.next();
            return rs.getDate(1);
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }
    
}
