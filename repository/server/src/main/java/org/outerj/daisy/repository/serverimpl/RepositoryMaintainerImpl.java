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
package org.outerj.daisy.repository.serverimpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.serverimpl.LocalRepositoryManager.Context;

public class RepositoryMaintainerImpl implements RepositoryMaintainerImplMBean {
    
    private final Log log = LogFactory.getLog(getClass());
    private final DateTimeFormatter iso8601 = ISODateTimeFormat.dateOptionalTimeParser();
    
    private LocalDocumentStrategy documentStrategy;
    private CommonRepository commonRepository;
    private AuthenticatedUser systemUser;
    private DataSource dataSource;
    private JdbcHelper jdbcHelper;
    private ObjectName mbeanName = new ObjectName("Daisy:name=RepositoryMaintainer");
    private MBeanServer mbeanServer;
    
    private ExecutorService executor;
    
    public RepositoryMaintainerImpl() throws Exception {};
    
    public RepositoryMaintainerImpl(LocalDocumentStrategy documentStrategy, AuthenticatedUser systemUser, Context context, DataSource dataSource, JdbcHelper jdbcHelper, MBeanServer mbeanServer) throws Exception {
        this.documentStrategy = documentStrategy;
        this.commonRepository = context.getCommonRepository();
        this.systemUser = systemUser;

        this.executor = Executors.newSingleThreadExecutor();
        this.dataSource = dataSource;
        this.jdbcHelper = JdbcHelper.getInstance(dataSource, log);
        this.mbeanServer = mbeanServer;
        mbeanServer.registerMBean(this, mbeanName);
    }
    
    public void stop() {
        executor.shutdown();
        try {
            mbeanServer.unregisterMBean(mbeanName);
        } catch (Exception e) {
            log.error("Error unregistering MBean", e);
        }
    }
    
    public void reExtractLinks(String query) {
        reExtractLinks(query, null, null);
    }

    public void reExtractLinks(String query, String beginDateIn, String endDateIn) {
        Date beginDate = null;
        Date endDate = null;

        if (beginDateIn != null) {
            beginDate = iso8601.parseDateTime(beginDateIn).toDate();
        }
        if (endDateIn != null) {
            endDate = iso8601.parseDateTime(endDateIn).toDate();
        }

        executor.execute(new LinkReExtractor(query, beginDate, endDate));
    }

    public class LinkReExtractor implements Runnable {
        VariantKey key;
        String query;
        Date beginDate;
        Date endDate;
        
        public LinkReExtractor(String query, Date beginDate, Date endDate) {
            this.query = query;
            this.beginDate = beginDate;
            this.endDate = endDate;
        }

        public LinkReExtractor(VariantKey key, Date beginDate, Date endDate) {
            this.key = key;
            this.beginDate = beginDate;
            this.endDate = endDate;
        }

        public void run() {
            try {
                VariantKey[] keys = commonRepository.getQueryManager(systemUser).performQueryReturnKeys(query, Locale.US);
                for (VariantKey key: keys) {
                    Document document = commonRepository.getDocument(key.getDocumentId(), key.getBranchId(), key.getLanguageId(), true, systemUser);
                    documentStrategy.reExtractLinks(document, beginDate, endDate);
                }
            } catch (Throwable t) {
                log.error("Link re-extraction failed", t);
            }
        }
        
        
    }

    public void rebuildRevisionTable() {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);
            
            stmt = conn.prepareStatement("delete from repository_revisions");
            stmt.executeUpdate();
            
            stmt = conn.prepareStatement("insert into repository_revisions(ns_id, doc_id, revision_date) select ns_id, doc_id, begin_date from live_history");
            stmt.executeUpdate();
            
            stmt = conn.prepareStatement("insert into repository_revisions(ns_id, doc_id, revision_date) select ns_id, doc_id, end_date from live_history where end_date is not null");
            stmt.executeUpdate();
            
            conn.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to rebuild revision table", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.rollback(conn);
            jdbcHelper.closeConnection(conn);
        }
    }

}
