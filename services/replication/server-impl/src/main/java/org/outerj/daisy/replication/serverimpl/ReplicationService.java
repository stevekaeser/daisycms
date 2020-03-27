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
package org.outerj.daisy.replication.serverimpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.credentialsprovider.CredentialsProvider;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.jdbcutil.SqlCounter;
import org.outerj.daisy.jms.JmsClient;
import org.outerj.daisy.repository.AvailableVariant;
import org.outerj.daisy.repository.AvailableVariants;
import org.outerj.daisy.repository.CollectionNotFoundException;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.DocumentNotFoundException;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.Timeline;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.DocumentTypeNotFoundException;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.FieldTypeNotFoundException;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.PartTypeNotFoundException;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.BranchNotFoundException;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;
import org.outerj.daisy.util.ObjectUtils;
import org.outerx.daisy.x10.DocumentDocument;
import org.outerx.daisy.x10.DocumentVariantCreatedDocument;
import org.outerx.daisy.x10.DocumentVariantDeletedDocument;
import org.outerx.daisy.x10.DocumentVariantUpdatedDocument;
import org.outerx.daisy.x10.TimelineUpdatedDocument;

public class ReplicationService implements ReplicationServiceMBean {

    private final Log log = LogFactory.getLog(getClass().getName());
    private static final int DEFAULT_REPLICATION_INTERVAL = 300; // in seconds
    private ObjectName mbeanName = new ObjectName("Daisy:name=ReplicationService");
    private final Runnable replicationTask = new ReplicationRunner();
    
    /**
     * Construction phase
     */
    private JmsClient jmsClient;
    private RepositoryManager sourceRepositoryManager;
    private CredentialsProvider credentialsProvider;
    private DataSource dataSource;
    private JdbcHelper jdbcHelper;
    private MBeanServer mbeanServer;

    /** 
     * Configuration
     */
    private String repositoryKey;
    private String jmsTopic;
    private String jmsSubscriptionName;
    private Map<String, ReplicationTarget> targets = new HashMap<String, ReplicationTarget>();
    private int replicationInterval;
    private List<ReplicationCondition> conditions = new ArrayList<ReplicationCondition>();

    /**
     * Initialization
     */
    private Repository sourceRepository;
    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private SqlCounter replicationCounter;

    public ReplicationService(Configuration configuration, JmsClient jmsClient, RepositoryManager sourceRepositoryManager, CredentialsProvider credentialsProvider, DataSource dataSource, MBeanServer mbeanServer) throws Exception {
        this.jmsClient = jmsClient;
        this.sourceRepositoryManager = sourceRepositoryManager;
        this.credentialsProvider = credentialsProvider;
        this.dataSource = dataSource;
        this.jdbcHelper = JdbcHelper.getInstance(dataSource, log);
        this.mbeanServer = mbeanServer;
        configure(configuration);
        initialize();
    }
    
    public void configure(Configuration configuration) throws ConfigurationException {
        for (Configuration target: configuration.getChild("targets").getChildren("target")) {
            String name = target.getAttribute("name");
            String url = target.getAttribute("url");
            String username = target.getAttribute("username");
            String password = target.getAttribute("password");
            String role = target.getChild("role").getValue(null);
            
            targets.put(name, new ReplicationTarget(name, url, new Credentials(username, password), role));
        }
        
        for (Configuration cond: configuration.getChild("conditions").getChildren("condition")) {
            conditions.add(new ReplicationCondition(cond.getAttribute("namespace", null), cond.getAttribute("branch", null), cond.getAttribute("language", null)));
        }
        
        repositoryKey = configuration.getChild("repositoryKey").getValue("internal");
        
        replicationInterval = configuration.getChild("replicationInterval").getValueAsInteger(DEFAULT_REPLICATION_INTERVAL);
        
        jmsTopic = configuration.getChild("jmsTopic").getValue("daisy");
        jmsSubscriptionName = configuration.getChild("jmsSubscriptionName").getValue("daisy-replication-service");
    }
    
    @PreDestroy
    public void dispose() {
        try {
            log.info("Waiting for replication thread to be terminated.");
            executorService.shutdownNow(); // stop running threads
            // Wait a while for existing threads to terminate
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Executor service did not terminate.");
            }
        } catch (InterruptedException ie) {
            // (Re-)attempt shutdown if current thread is terminated.
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            mbeanServer.unregisterMBean(mbeanName);
        } catch (Exception e) {
            log.error("Error unregistering MBean", e);
        }
    }

    public void initialize() throws Exception {
        replicationCounter = new SqlCounter("replication_sequence", dataSource, log);
        jmsClient.registerDurableTopicListener(jmsTopic, jmsSubscriptionName, new ReplicationMessageListener());

        sourceRepository = sourceRepositoryManager.getRepository(credentialsProvider.getCredentials(repositoryKey));
        
        mbeanServer.registerMBean(this, mbeanName);

        if (targets.isEmpty()) {
            log.info("No targets defined - will not start replication thread");
            return;
        }
        
        if (replicationInterval >= 0) {
            executorService.scheduleWithFixedDelay(replicationTask, replicationInterval, replicationInterval, TimeUnit.SECONDS);
        }
    }
    
    private Repository getTargetRepository(ReplicationTarget replicationTarget) throws Exception {
        RemoteRepositoryManager repoManager = new RemoteRepositoryManager(replicationTarget.getUrl(), replicationTarget.getCredentials());
        Repository result = repoManager.getRepository(replicationTarget.getCredentials());
        if (!result.getServerVersion().equals(result.getClientVersion())) {
            throw new ReplicationTargetException("The target repository is running another version of Daisy.", replicationTarget.getName());
        }
        result.setActiveRoleIds(new long[] { Role.ADMINISTRATOR });
        return result;
    }
    
    public void scheduleReplication(String query) throws RepositoryException {
        VariantKey[] keys = sourceRepository.getQueryManager().performQueryReturnKeys(query, Locale.getDefault());
        scheduleReplication(keys);
    }
    
    private void scheduleReplication(VariantKey key) throws RepositoryException {
        scheduleReplication(new VariantKey[] { key });
    }

    private void scheduleReplication(VariantKey[] keys) throws RepositoryException {
        Connection conn = null;
        PreparedStatement select = null;
        PreparedStatement insert = null;
        PreparedStatement update = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);
            for (VariantKey key: keys) {
                if  (acceptDocument(key)) {
                    for (String targetName: targets.keySet()) {
                        if (select == null) 
                            select = conn.prepareStatement("select id from replication where document_id = ? and branch_id = ? and lang_id = ? and target = ?" + jdbcHelper.getSharedLockClause());
                        select.setString(1, key.getDocumentId());
                        select.setLong(2, key.getBranchId());
                        select.setLong(3, key.getLanguageId());
                        select.setString(4, targetName);
                        select.execute();
                        try {
                            rs = select.getResultSet();
                            if (!rs.next()) {
                                if (insert == null)
                                    insert = conn.prepareStatement("insert into replication(id, document_id, branch_id, lang_id, target, state) values (?,?,?,?,?,?)");
                                insert.setLong(1, replicationCounter.getNextId());
                                insert.setString(2, key.getDocumentId());
                                insert.setLong(3, key.getBranchId());
                                insert.setLong(4, key.getLanguageId());
                                insert.setString(5, targetName);
                                insert.setString(6, ReplicationState.NEW.getCode());
                                insert.execute();
                            } else {
                                if (update == null)
                                    update = conn.prepareStatement("update replication set state = ? where id = ?");
                                update.setString(1, ReplicationState.NEW.getCode());
                                update.setLong(2, rs.getLong(1));
                                update.execute();
                            }
                        } finally {
                            if (rs != null) rs.close();
                        }
                    }
                }
            }       
            conn.commit();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to schedule replication", e);
        } finally {
            jdbcHelper.closeStatement(select);
            jdbcHelper.closeStatement(insert);
            jdbcHelper.closeStatement(update);
            jdbcHelper.closeConnection(conn);
        }
    }
    
    private class ReplicationMessageListener implements MessageListener {
        public void onMessage(Message aMessage) {
            try {
                TextMessage message = (TextMessage)aMessage;
                String type = message.getStringProperty("type");
                
                String documentId = null;
                long branchId = -1;
                long languageId = -1;

                if (type.equals(RepositoryEventType.DOCUMENT_VARIANT_CREATED.toString())) {
                    DocumentVariantCreatedDocument eventXml = DocumentVariantCreatedDocument.Factory.parse(message.getText());
                    DocumentDocument.Document documentXml = eventXml.getDocumentVariantCreated().getNewDocumentVariant().getDocument();
                    documentId = documentXml.getId();
                    branchId = documentXml.getBranchId();
                    languageId = documentXml.getLanguageId();
                } else if (type.equals(RepositoryEventType.DOCUMENT_VARIANT_UPDATED.toString())) {
                    DocumentVariantUpdatedDocument eventXml = DocumentVariantUpdatedDocument.Factory.parse(message.getText());
                    DocumentDocument.Document documentXml = eventXml.getDocumentVariantUpdated().getNewDocumentVariant().getDocument();
                    documentId = documentXml.getId();
                    branchId = documentXml.getBranchId();
                    languageId = documentXml.getLanguageId();
                } else if (type.equals(RepositoryEventType.DOCUMENT_VARIANT_DELETED.toString())) {
                    DocumentVariantDeletedDocument eventXml = DocumentVariantDeletedDocument.Factory.parse(message.getText());
                    DocumentDocument.Document documentXml = eventXml.getDocumentVariantDeleted().getDeletedDocumentVariant().getDocument();
                    documentId = documentXml.getId();
                    branchId = documentXml.getBranchId();
                    languageId = documentXml.getLanguageId();
                } else if (type.equals(RepositoryEventType.DOCUMENT_VARIANT_TIMELINE_UPDATED.toString())) {
                    TimelineUpdatedDocument eventXml = TimelineUpdatedDocument.Factory.parse(message.getText());
                    documentId = eventXml.getTimelineUpdated().getDocumentId();
                    branchId = eventXml.getTimelineUpdated().getBranchId();
                    languageId = eventXml.getTimelineUpdated().getLanguageId();                  
                }
                
                if (documentId != null) {
                    VariantKey key = new VariantKey(documentId, branchId, languageId);
                    scheduleReplication(key);
                }
            } catch (Throwable e) {
                log.error("Error processing JMS message.", e);
            }
        }

    }

    private void replicateDocument(Repository sourceRepository, DocumentType sourceDocumentType, Document sourceDocument, 
            String targetName, Repository targetRepository) throws Exception {

        VariantKey variantKey = sourceDocument.getVariantKey();

        String branch = sourceRepository.getVariantManager().getBranch(variantKey.getBranchId(), false).getName();
        String language = sourceRepository.getVariantManager().getLanguage(variantKey.getLanguageId(), false).getName();

        Document targetDocument = null;
        
        try {
            targetDocument = targetRepository.getDocument(variantKey, true);
        } catch (DocumentNotFoundException dnfe) {
            targetDocument = targetRepository.createDocument(sourceDocument.getName(), sourceDocumentType.getName(), branch, language);
            targetDocument.setRequestedId(variantKey.getDocumentId());
        } catch (DocumentVariantNotFoundException dvnfe) {
            if (sourceDocument.getVariantCreatedFromBranchId() != -1) {
                String srcFromBranch = sourceRepository.getVariantManager().getBranch(sourceDocument.getVariantCreatedFromBranchId(), false).getName();
                String srcFromLanguage = sourceRepository.getVariantManager().getLanguage(sourceDocument.getVariantCreatedFromLanguageId(), false).getName();
                targetDocument = targetRepository.createVariant(variantKey.getDocumentId(), srcFromBranch, srcFromLanguage, 1, branch, language, false);   
            } else {
                // as a last resort pick the first available variant
                AvailableVariants variants = targetRepository.getAvailableVariants(variantKey.getDocumentId());
                AvailableVariant variant = variants.getArray()[0];
                targetDocument = targetRepository.createVariant(variantKey.getDocumentId(), variant.getBranch().getName(), variant.getLanguage().getName(), 1, branch, language, false);   
            }
        }
        
        if (targetDocument.hasCustomField("dsy-ReplicationError")) {
            log.error("Document is excluded from replication due to earlier errors. Manual intervention needed");
        }

        // copy all missing versions from source to target
        long startVersion = targetDocument.getLastVersionId() == -1 ? 1 : targetDocument.getLastVersionId() + 1;
        for (long i = startVersion; i <= sourceDocument.getLastVersionId(); i++) {
            targetDocument.setDocumentTypeChecksEnabled(false);
            Version sourceVersion = sourceDocument.getVersion(i);
            
            ReplicationHelper.copyVersionData(sourceVersion, targetDocument);
            targetDocument.save(false);
        }
        
        if (targetDocument.getLastVersionId() != sourceDocument.getLastVersionId()) {
            targetDocument.setCustomField("dsy-ReplicationError", String.format("Local and source repository have diverged. (local has %d versions, origin has %d versions)", targetDocument.getLastVersionId(), sourceDocument.getLastVersionId()));
            throw new ReplicationException(String.format("Got %d versions, but expected %d", targetDocument.getLastVersionId(), sourceDocument.getLastVersionId()), variantKey, targetName);
        }
        
        // now copy the non-versioned data and then update the timeline.
        ReplicationHelper.copyNonVersionedData(sourceDocument, targetDocument);
        targetDocument.save(false);

        // find the first difference in the timeline, replace everything from there on with the versions from the target repository
        Timeline sourceTimeline = sourceDocument.getTimeline();
        Timeline targetTimeline = targetDocument.getTimeline();
        LiveHistoryEntry[] sourceHistory = sourceTimeline.getLiveHistory();
        LiveHistoryEntry[] targetHistory = targetTimeline.getLiveHistory();

        int startDiverging = 0;
        while (startDiverging < sourceHistory.length) {
            if (startDiverging >= targetHistory.length)
                break;
            LiveHistoryEntry srcEntry = sourceHistory[startDiverging];
            LiveHistoryEntry targetEntry = targetHistory[startDiverging];
            if (!ObjectUtils.safeEquals(srcEntry.getBeginDate(), targetEntry.getBeginDate())
                    || !ObjectUtils.safeEquals(srcEntry.getEndDate(), targetEntry.getEndDate())
                    || srcEntry.getVersionId() != targetEntry.getVersionId()) {
                break;
            }
            startDiverging++;
        }
        for (int i = startDiverging; i < targetHistory.length; i++) {
            targetTimeline.deleteLiveHistoryEntry(targetHistory[i]);
        }
        for (int i = startDiverging; i < sourceHistory.length; i++) {
            targetTimeline.addLiveHistoryEntry(sourceHistory[i].getBeginDate(), sourceHistory[i].getEndDate(), sourceHistory[i].getVersionId());
        }

        targetTimeline.save();
        
    }

    public boolean acceptDocument(VariantKey variantKey) throws RepositoryException {
        if (conditions.isEmpty())
            return true;

        for (ReplicationCondition condition: conditions) {
            if (condition.matches(sourceRepository, variantKey))
                return true;
        }
        
        return false;
    }

    private Document getSourceOrDeleteTarget(VariantKey variantKey,
            Repository targetRepository) throws RepositoryException {
        try {
            return sourceRepository.getDocument(variantKey, false);
        } catch (DocumentNotFoundException dnfe) {
            try {
                targetRepository.deleteDocument(variantKey.getDocumentId());
            } catch (DocumentNotFoundException dnfe2) {
                log.info("No document found while trying to delete " + variantKey.getDocumentId());
            }
        } catch (DocumentVariantNotFoundException dvnfe) {
            try {
                targetRepository.deleteVariant(variantKey);
            } catch (DocumentNotFoundException dnfe) {
                log.info("No document found while trying to delete " + variantKey);
            } catch (DocumentVariantNotFoundException dvnfe2) {
                log.info("No document found while trying to delete " + variantKey);
            }
        }
        return null;
    }
    
    private class ReplicationRunner implements Runnable {
        public void run() {
            // query for document / target combinations to replicate,
            // for each one obtain a repository client to the target
            // and synchronise the document.
            
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = dataSource.getConnection();
                jdbcHelper.startTransaction(conn);
                stmt = conn.prepareStatement("select id, document_id, branch_id, lang_id, target from replication where state=?");
                stmt.setString(1, ReplicationState.NEW.getCode());
                stmt.execute();
                ResultSet rs = stmt.getResultSet();
                
                Map<String, Repository> repositories = new HashMap<String, Repository>();
                while (rs.next()) {
                    long replicationId = rs.getLong("id");
                    String documentId = rs.getString("document_id");
                    Long branchId = rs.getLong("branch_id");
                    Long languageId = rs.getLong("lang_id");
                    String targetName = rs.getString("target");
                    
                    VariantKey variantKey = new VariantKey(documentId, branchId, languageId);

                    ReplicationTarget replicationTarget = targets.get(targetName);
                    if (replicationTarget == null) {
                        throw new ReplicationTargetException("Unknown replication target: ", targetName);
                    }
                    try {
                        if (replicationTarget.isSuspended()) {
                            log.info("Replication target " + targetName + " is suspended - setting error state for " + variantKey);
                            setReplicationState(conn, replicationId, ReplicationState.ERROR);
                        } else {
                            Repository targetRepository = repositories.get(replicationTarget.getName());
                            if (targetRepository == null) {
                                try {
                                    targetRepository = getTargetRepository(replicationTarget);
                                    repositories.put(targetName, targetRepository);
                                    checkTargetRepository(targetName, targetRepository);
                                } catch  (RepositoryException re) {
                                    log.error("There is a problem with the target repository", re);
                                }
                            }
                            
                            Document sourceDocument = getSourceOrDeleteTarget(variantKey, targetRepository);
                            DocumentType sourceDocumentType = sourceRepository.getRepositorySchema().getDocumentTypeById(sourceDocument.getDocumentTypeId(), false);
                            
                            if (sourceDocument != null) {
                                replicateDocument(sourceRepository, sourceDocumentType, sourceDocument, targetName, targetRepository);
                            }
    
                            finishedReplication(conn, replicationId);
                            
                        }
                    } catch (ReplicationTargetException rte) {
                        log.error("Problem with replication target", rte);
                        setReplicationState(conn, replicationId, ReplicationState.ERROR);
                        if (replicationTarget != null) {
                            replicationTarget.setSuspended(true);
                        }
                    } catch (ReplicationException re) {
                        log.error("Failed to replicate document", re);
                        setReplicationState(conn, replicationId, ReplicationState.ERROR);
                    } catch (Exception e) {
                        log.error("Unhandled error during replication: " , e);
                        setReplicationState(conn, replicationId, ReplicationState.ERROR);
                    } 
                    
                    conn.commit();
                }
            } catch (SQLException e) {
                log.error("Replication error", e);
            } catch (Throwable t) {
                log.error("Unhandled error", t);
            } finally {
                jdbcHelper.closeStatement(stmt);
                jdbcHelper.closeConnection(conn);
            }
        }

    }

    ///////////////////////////////////////// repository namespace, variant-space and schema synchronization
    private void checkTargetRepository(String targetName, Repository targetRepository) throws RepositoryException, ReplicationTargetException {
        checkTargetNamespaces(targetName, targetRepository);
        syncBranches(targetName, targetRepository);
        syncLanguages(targetName, targetRepository);
        syncCollections(targetName, targetRepository);
        syncRepositorySchema(targetRepository);
    }
    private void checkTargetNamespaces(String targetName, Repository targetRepository) throws RepositoryException, ReplicationTargetException {
        for (Namespace srcNamespace: sourceRepository.getNamespaceManager().getAllNamespaces().getArray()) {
            try {
                Namespace targetNamespace = targetRepository.getNamespaceManager().getNamespace(srcNamespace.getName());
                if (!srcNamespace.getFingerprint().equals(targetNamespace.getFingerprint())) {
                    throw new ReplicationTargetException(String.format("Non-matching namespace fingerprint for namespace %s: expected %s, but was %s", srcNamespace.getName(), srcNamespace.getFingerprint(), targetNamespace.getFingerprint()), targetName);
                }
            } catch (NamespaceNotFoundException nnfe) {
                targetRepository.getNamespaceManager().registerNamespace(srcNamespace.getName(), srcNamespace.getFingerprint());
            }
        }
    }
    private void syncCollections(String targetName, Repository targetRepository) throws RepositoryException, ReplicationTargetException {
        // get collections sorted by name.
        DocumentCollection[] srcCollections = sourceRepository.getCollectionManager().getCollections(false).getArray();
        Arrays.sort(srcCollections, 0, srcCollections.length, new Comparator<DocumentCollection>() {
            public int compare(DocumentCollection a, DocumentCollection b) {
                if (a.getId() < b.getId()) return -1;
                if (a.getId() > b.getId()) return 1;
                return 0;
            }
        });
        // check branches and create them if necessary.
        for (DocumentCollection srcCollection: srcCollections) {
            DocumentCollection targetCollection = null;
            try {
                targetCollection = targetRepository.getCollectionManager().getCollection(srcCollection.getId(), true);
                if (!targetCollection.getName().equals(srcCollection.getName())) {
                    targetCollection.setName(srcCollection.getName());
                    targetCollection.save();
                }
            } catch (CollectionNotFoundException e) {
                while (true) {
                    targetCollection = targetRepository.getCollectionManager().createCollection(srcCollection.getName());
                    targetCollection.setName(srcCollection.getName());
                    targetCollection.save();
                    if (targetCollection.getId() > srcCollection.getId()) {
                        throw new ReplicationTargetException("Collection id counters are out of sync. You should manually sunc them", targetName);
                    } else if (targetCollection.getId() < srcCollection.getId()) {
                        targetRepository.getCollectionManager().deleteCollection(targetCollection.getId());
                    } else break;
                }
            }
        }
    }
    private void syncBranches(String targetName, Repository targetRepository) throws RepositoryException, ReplicationTargetException {
        // get branches sorted by name.
        Branch[] srcBranches = sourceRepository.getVariantManager().getAllBranches(false).getArray();
        Arrays.sort(srcBranches, 0, srcBranches.length, new Comparator<Branch>() {
            public int compare(Branch a, Branch b) {
                if (a.getId() < b.getId()) return -1;
                if (a.getId() > b.getId()) return 1;
                return 0;
            }
        });
        // check branches and create them if necessary.
        for (Branch srcBranch: srcBranches) {
            Branch targetBranch = null;
            try {
                targetBranch = targetRepository.getVariantManager().getBranch(srcBranch.getId(), true);
                if (!ObjectUtils.safeEquals(targetBranch.getName(), srcBranch.getName())
                        || !ObjectUtils.safeEquals(targetBranch.getDescription(), srcBranch.getDescription())) {
                    targetBranch.setName(srcBranch.getName());
                    targetBranch.setDescription(srcBranch.getDescription());
                    targetBranch.save();
                }
            } catch (BranchNotFoundException e) {
                while (true) {
                    targetBranch = targetRepository.getVariantManager().createBranch(srcBranch.getName());
                    targetBranch.setAllFromXml(targetBranch.getXml().getBranch());
                    targetBranch.save();
                    if (targetBranch.getId() > srcBranch.getId()) {
                        throw new ReplicationTargetException("Branch id counters are out of sync. You should manually sunc them", targetName);
                    } else if (targetBranch.getId() < srcBranch.getId()) {
                        targetRepository.getVariantManager().deleteBranch(targetBranch.getId());
                    } else break;
                }
            }
        }
    }
    private void syncLanguages(String targetName, Repository targetRepository) throws RepositoryException, ReplicationTargetException {
        // get languaes sorted by name.
        Language[] srcLanguages = sourceRepository.getVariantManager().getAllLanguages(false).getArray();
        Arrays.sort(srcLanguages, 0, srcLanguages.length, new Comparator<Language>() {
            public int compare(Language a, Language b) {
                if (a.getId() < b.getId()) return -1;
                if (a.getId() > b.getId()) return 1;
                return 0;
            }
        });
        // check languages and create them if necessary.
        for (Language srcLanguage: srcLanguages) {
            Language targetLanguage = null;
            try {
                targetLanguage = targetRepository.getVariantManager().getLanguage(srcLanguage.getId(), true);
                if (!ObjectUtils.safeEquals(targetLanguage.getName(), srcLanguage.getName())
                        || !ObjectUtils.safeEquals(targetLanguage.getDescription(), srcLanguage.getDescription())) {
                    targetLanguage.setName(srcLanguage.getName());
                    targetLanguage.setDescription(srcLanguage.getDescription());
                    targetLanguage.save();
                }
            } catch (LanguageNotFoundException e) {
                while (true) {
                    targetLanguage = targetRepository.getVariantManager().createLanguage(srcLanguage.getName());
                    targetLanguage.setAllFromXml(srcLanguage.getXml().getLanguage());
                    targetLanguage.save();
                    if (targetLanguage.getId() > srcLanguage.getId()) {
                        throw new ReplicationTargetException("Language id counters are out of sync. You should manually sync them.", targetName);
                    } else if (targetLanguage.getId() < srcLanguage.getId()) {
                        targetRepository.getVariantManager().deleteLanguage(targetLanguage.getId());
                    } else break;
                }
            }
        }
    }
    private void syncRepositorySchema(Repository targetRepository) throws RepositoryException, ReplicationTargetException {
        for (PartType srcPartType: sourceRepository.getRepositorySchema().getAllPartTypes(false).getArray()) {
            PartType targetPartType = null;
            try {
                targetPartType = targetRepository.getRepositorySchema().getPartType(srcPartType.getName(), true);
            } catch (PartTypeNotFoundException e) {
                targetPartType = targetRepository.getRepositorySchema().createPartType(srcPartType.getName(), srcPartType.getMimeTypes());
                targetPartType.setAllFromXml(srcPartType.getXml().getPartType());
                targetPartType.save();
            }
        }
        for (FieldType srcFieldType: sourceRepository.getRepositorySchema().getAllFieldTypes(false).getArray()) {
            FieldType targetFieldType = null;
            try {
                targetFieldType = targetRepository.getRepositorySchema().getFieldType(srcFieldType.getName(), true);
            } catch (FieldTypeNotFoundException e) {
                targetFieldType = targetRepository.getRepositorySchema().createFieldType(srcFieldType.getName(), srcFieldType.getValueType());
                targetFieldType.setAllFromXml(srcFieldType.getXml().getFieldType());
                targetFieldType.save();
            }
        }
        for (DocumentType srcDocumentType: sourceRepository.getRepositorySchema().getAllDocumentTypes(false).getArray()) {
            DocumentType targetDocumentType = null;
            try {
                targetDocumentType = targetRepository.getRepositorySchema().getDocumentType(srcDocumentType.getName(), true);
            } catch (DocumentTypeNotFoundException e) {
                targetDocumentType = targetRepository.getRepositorySchema().createDocumentType(srcDocumentType.getName());
                targetDocumentType.setAllFromXml(srcDocumentType.getXml().getDocumentType());
                targetDocumentType.save();
            }
        }
    }

    private void setReplicationState(Connection conn,
            long replicationId, ReplicationState replicationState) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("update replication set state = ? where id = ?");
        stmt.setString(1, replicationState.getCode());
        stmt.setLong(2, replicationId);
        stmt.execute();
        conn.commit();
    }

    private void finishedReplication(Connection conn, long replicationId) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement("delete from replication where id = ?");
        stmt.setLong(1, replicationId);
        stmt.execute();
        conn.commit();
    }

    public boolean isSuspended(String targetName) {
        ReplicationTarget target = targets.get(targetName);
        if (target == null)
            throw new IllegalArgumentException("Target '" + targetName + "' does not exist");
        
        return target.isSuspended();
    }

    public void resumeTarget(String targetName) {
        ReplicationTarget target = targets.get(targetName);
        if (target == null)
            throw new IllegalArgumentException("Target '" + targetName + "' does not exist");
        
        target.setSuspended(false);
    }
    
    public void suspendTarget(String targetName) {
        ReplicationTarget target = targets.get(targetName);
        if (target == null)
            throw new IllegalArgumentException("Target '" + targetName + "' does not exist");
        
        target.setSuspended(true);
    }
    
    public void restartFailedReplications() throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);
            stmt = conn.prepareStatement("update replication set state = ? where state = ?");
            stmt.setString(1, ReplicationState.NEW.getCode());
            stmt.setString(2, ReplicationState.ERROR.getCode());
            stmt.execute();
            conn.commit();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to reschedule replication", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void clearFailedReplications() throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);
            stmt = conn.prepareStatement("delete from replication where state = ?");
            stmt.setString(1, ReplicationState.ERROR.getCode());
            stmt.execute();
            conn.commit();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to clear failed replications", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void triggerReplication() {
        executorService.submit(replicationTask);
    }
    
    public int getReplicationsByState(ReplicationState state) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();

            stmt = conn.prepareStatement("select count(*) from replication where state = ?");
            stmt.setString(1, state.getCode());
            stmt.execute();
            rs = stmt.getResultSet();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Error obtaining replication state", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }
    
    public int getReplicationQueueSize() {
        return getReplicationsByState(ReplicationState.NEW);
    }
    
    public int getReplicationErrors() {
        return getReplicationsByState(ReplicationState.ERROR);
    }

    public String[] getTargetNames() {
        Set<String> names = targets.keySet();
        return (String[]) names.toArray(new String[names.size()]);
    }
    
    public String[] getFailedTargetNames() {
        Set<String> names = new HashSet<String>();
        for (String targetName: targets.keySet()) {
            names.add(targetName);
        }
        return (String[]) names.toArray(new String[names.size()]);
    }

}
