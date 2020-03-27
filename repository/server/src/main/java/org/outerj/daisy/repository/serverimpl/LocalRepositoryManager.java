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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PreDestroy;
import javax.management.MBeanServer;
import javax.sql.DataSource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.collections.primitives.ArrayLongList;
import org.apache.commons.collections.primitives.LongList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.authentication.UserAuthenticator;
import org.outerj.daisy.blobstore.BlobStore;
import org.outerj.daisy.cache.DocumentCache;
import org.outerj.daisy.ftindex.FullTextIndex;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.jdbcutil.SqlCounter;
import org.outerj.daisy.jdbcutil.SqlDocumentIdCounter;
import org.outerj.daisy.linkextraction.LinkExtractor;
import org.outerj.daisy.linkextraction.LinkExtractorManager;
import org.outerj.daisy.plugin.PluginHandle;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.plugin.PluginUser;
import org.outerj.daisy.query.QueryFactory;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentNotFoundException;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.LinkExtractorInfo;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryListener;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUserImpl;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.commonimpl.DocumentImpl;
import org.outerj.daisy.repository.commonimpl.LinkExtractorInfoImpl;
import org.outerj.daisy.repository.commonimpl.RepositoryImpl;
import org.outerj.daisy.repository.commonimpl.RepositoryStrategy;
import org.outerj.daisy.repository.commonimpl.schema.CommonRepositorySchema;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.query.PredicateExpression;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.serverimpl.acl.LocalAclStrategy;
import org.outerj.daisy.repository.serverimpl.comment.LocalCommentStrategy;
import org.outerj.daisy.repository.serverimpl.model.LocalSchemaStrategy;
import org.outerj.daisy.repository.serverimpl.user.LocalUserManagementStrategy;
import org.outerj.daisy.repository.serverimpl.variant.LocalVariantStrategy;
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerj.daisy.repository.spi.local.PreSaveHook;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.summary.DocumentSummarizer;

public class LocalRepositoryManager implements RepositoryManager {
    private PluginRegistry pluginRegistry;
    private MBeanServer mbeanServer;
    private CommonRepository commonRepository;
    private DataSource dataSource;
    private DocumentCache documentCache;
    private BlobStore blobStore;
    private UserAuthenticator userAuthenticator;
    private QueryFactory queryFactory;
    private AuthenticatedUser systemUser;
    private FullTextIndex fullTextIndex;
    private DocumentSummarizer documentSummarizer;
    private LinkExtractorManager linkExtractorManager;
    private RepositoryMaintainerImpl repositoryMaintainer;
    private JdbcHelper jdbcHelper;
    private String repositoryNamespace;
    private String repositoryNamespaceFingerprint;
    private List<ConditionalNamespace> repositoryNamespaces = new ArrayList<ConditionalNamespace>();
    private Map<String, ExtensionProvider> extensions = new ConcurrentHashMap<String, ExtensionProvider>(16, .75f, 1);
    private PluginUser<ExtensionProvider> extensionPluginUser = new ExtensionPluginUser();
    private List<PluginHandle<PreSaveHook>> preSaveHooks = new CopyOnWriteArrayList<PluginHandle<PreSaveHook>>();
    private PluginUser<PreSaveHook> preSaveHookPluginUser = new PreSaveHookPluginUser();
    private long expiredLockJanitorInterval;
    private Thread expiredLockJanitorThread;
    private long liveVersionJanitorInterval;
    private Thread liveVersionJanitorThread;
    private final Log log = LogFactory.getLog(getClass());
    /**
     * The database schema version number. Not necessarily the same as the Daisy version number.
     * This number only gets augmented on releases where the schema actually changes.
     * If changed here, also needs to be changed in daisy-data.xml and the database
     * upgrade script.
     */
    private final static String DB_SCHEMA_VERSION = "2.4";

    public LocalRepositoryManager(Configuration configuration, DataSource dataSource,
            BlobStore blobStore, DocumentCache documentCache, UserAuthenticator userAuthenticator,
            QueryFactory queryFactory, FullTextIndex fullTextIndex, DocumentSummarizer documentSummarizer,
            LinkExtractorManager linkExtractorManager, PluginRegistry pluginRegistry, MBeanServer mbeanServer) throws Exception {
        this.dataSource = dataSource;
        this.blobStore = blobStore;
        this.documentCache = documentCache;
        this.userAuthenticator = userAuthenticator;
        this.queryFactory = queryFactory;
        this.fullTextIndex = fullTextIndex;
        this.documentSummarizer = documentSummarizer;
        this.linkExtractorManager = linkExtractorManager;
        this.pluginRegistry = pluginRegistry;
        this.mbeanServer = mbeanServer;
        this.configure(configuration);
        this.initialize();
        this.start();
    }

    @PreDestroy
    public void destroy() throws Exception {
        this.stop();
        pluginRegistry.unsetPluginUser(PreSaveHook.class, preSaveHookPluginUser);
        pluginRegistry.unsetPluginUser(ExtensionProvider.class, extensionPluginUser);
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        this.expiredLockJanitorInterval = configuration.getChild("expiredLockJanitorInterval").getValueAsLong(60000); // 1 minute
        this.liveVersionJanitorInterval = configuration.getChild("liveVersionJanitorInterval").getValueAsLong(300000); // 5 minutes
        for (Configuration nsConf : configuration.getChildren("namespace")) {
            if (!nsConf.getAttribute("test", "").trim().equals(""))
                this.repositoryNamespaces.add(new ConditionalNamespace(nsConf.getValue(), nsConf.getAttribute("fingerprint", null), nsConf.getAttribute("test")));
            else
                // this is the default namespace
                this.repositoryNamespace = nsConf.getValue();
                this.repositoryNamespaceFingerprint = nsConf.getAttribute("fingerprint", null);
        }
    }

    private void initialize() throws Exception {
        Context context = new Context();

        // The sytem user is a built-in user which has ID 1
        this.systemUser = new AuthenticatedUserImpl(1, null, new long[] { Role.ADMINISTRATOR }, new long[] { Role.ADMINISTRATOR }, "$system");

        // Get the database-specific JdbcHelper
        jdbcHelper = JdbcHelper.getInstance(dataSource, log);

        RepositoryStrategy repositoryStrategy = new LocalRepositoryStrategy(context, jdbcHelper);
        LocalDocumentStrategy documentStrategy = new LocalDocumentStrategy(context, systemUser, jdbcHelper);
        LocalSchemaStrategy schemaStrategy = new LocalSchemaStrategy(context, jdbcHelper);
        LocalAclStrategy aclStrategy = new LocalAclStrategy(context, systemUser, jdbcHelper);
        LocalUserManagementStrategy userManagementStrategy = new LocalUserManagementStrategy(context, jdbcHelper, systemUser);
        LocalVariantStrategy variantStrategy = new LocalVariantStrategy(context, jdbcHelper);
        LocalCollectionStrategy collectionStrategy = new LocalCollectionStrategy(context, systemUser, jdbcHelper);
        LocalCommentStrategy commentStrategy = new LocalCommentStrategy(context, systemUser, jdbcHelper);
        commonRepository = new LocalCommonRepository(this, repositoryStrategy, documentStrategy, schemaStrategy,
                aclStrategy, userManagementStrategy, variantStrategy, collectionStrategy, commentStrategy, context,
                systemUser, documentCache, extensions, jdbcHelper);
        commonRepository.addListener(new DocumentCacheInvalidator());

        for (ConditionalNamespace namespaceConf : this.repositoryNamespaces) {
            namespaceConf.parseCondition(context);
        }
        
        // supply the user
        userAuthenticator.setUserManager(new RepositoryImpl(commonRepository, systemUser).getUserManager());

        checkDatabaseSchema();  
        
        assureNamespacesRegistered();
        
        this.repositoryMaintainer = new RepositoryMaintainerImpl(documentStrategy, systemUser, context, dataSource, jdbcHelper, mbeanServer);

        pluginRegistry.setPluginUser(PreSaveHook.class, preSaveHookPluginUser);
        pluginRegistry.setPluginUser(ExtensionProvider.class, extensionPluginUser);
    }

    private void start() throws Exception {
        expiredLockJanitorThread = new Thread(new ExpiredLockJanitor(), "Daisy Expired Lock Janitor");
        expiredLockJanitorThread.start();
        liveVersionJanitorThread = new Thread(new LiveVersionJanitor(), "Daisy Live Version Janitor");
        liveVersionJanitorThread.start();
        
    }

    private void stop() throws Exception {
        log.info("Stopping the repository maintainer.");
        repositoryMaintainer.stop();

        log.info("Waiting for expired lock janitor thread to end.");
        expiredLockJanitorThread.interrupt();
        try {
            expiredLockJanitorThread.join();
        } catch (InterruptedException e) {
            // ignore
        }
        log.info("Waiting for live version janitor thread to end.");
        liveVersionJanitorThread.interrupt();
        try {
            liveVersionJanitorThread.join();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public Repository getRepository(final Credentials credentials) throws RepositoryException {
        AuthenticatedUser user = userAuthenticator.authenticate(credentials);
        return new RepositoryImpl(commonRepository, user);
    }

    public Repository getRepositoryAsUser(final User user) throws RepositoryException {
        commonRepository.getUserManager().checkUser(user);
        long[] allRoles = user.getAllRoleIds();
        Role defaultRole = user.getDefaultRole();
        return new RepositoryImpl(commonRepository, new AuthenticatedUserImpl(user.getId(), null, determineActiveRoles(defaultRole, allRoles), allRoles, user.getLogin()));
    }

    //FIXME: duplicated code (see UserAuthenticatorImpl)
    private long[] determineActiveRoles(Role defaultRole, long[] availableRoleIds) {
        if (defaultRole != null)
            return new long[] {defaultRole.getId()};
        if (availableRoleIds.length == 1)
            return availableRoleIds;

        LongList roleIds = new ArrayLongList(availableRoleIds.length);
        for (long availableRoleId : availableRoleIds) {
            if (availableRoleId != Role.ADMINISTRATOR)
                roleIds.add(availableRoleId);
        }
        return roleIds.toArray();
    }

    private class ExtensionPluginUser implements PluginUser<ExtensionProvider> {

        public void pluginAdded(PluginHandle<ExtensionProvider> pluginHandle) {
            extensions.put(pluginHandle.getName(), pluginHandle.getPlugin());
        }

        public void pluginRemoved(PluginHandle<ExtensionProvider> pluginHandle) {
            extensions.remove(pluginHandle.getName());
        }
    }

    private class PreSaveHookPluginUser implements PluginUser<PreSaveHook> {

        public void pluginAdded(PluginHandle<PreSaveHook> plugin) {
            preSaveHooks.add(plugin);
        }

        public void pluginRemoved(PluginHandle<PreSaveHook> plugin) {
            preSaveHooks.remove(plugin);
        }
    }

    /**
     * Context information for the document implementation
     */
    public class Context {
        private SqlDocumentIdCounter docIdCounter = new SqlDocumentIdCounter(dataSource, log);
        private SqlCounter collectionIdCounter = new SqlCounter("collection_sequence", dataSource, log);
        private SqlCounter docTypeCounter = new SqlCounter("documenttype_sequence", dataSource, log);
        private SqlCounter partTypeCounter = new SqlCounter("parttype_sequence", dataSource, log);
        private SqlCounter fieldTypeCounter = new SqlCounter("fieldtype_sequence", dataSource, log);
        private SqlCounter localizedStringCounter = new SqlCounter("localizedstring_sequence", dataSource, log);
        private SqlCounter commentCounter = new SqlCounter("comment_sequence", dataSource, log);
        private SqlCounter eventCounter = new SqlCounter("event_sequence", dataSource, log);
        private SqlCounter userCounter = new SqlCounter("user_sequence", dataSource, log);
        private SqlCounter roleCounter = new SqlCounter("role_sequence", dataSource, log);
        private SqlCounter namespaceCounter = new SqlCounter("namespace_sequence", dataSource, log);
        private SqlCounter liveHistoryCounter = new SqlCounter("live_history_sequence", dataSource, log);

        private Context() {
            // private constructor to make sure no-one else can create this
        }

        public DataSource getDataSource() {
            return dataSource;
        }

        public QueryFactory getQueryFactory() {
            return queryFactory;
        }

        public BlobStore getBlobStore() {
            return blobStore;
        }

        public FullTextIndex getFullTextIndex() {
            return fullTextIndex;
        }

        public UserAuthenticator getUserAuthenticator() {
            return userAuthenticator;
        }

        public DocumentSummarizer getDocumentSummarizer() {
            return documentSummarizer;
        }

        public CommonRepositorySchema getRepositorySchema() {
            return commonRepository.getRepositorySchema();
        }

        public CommonRepository getCommonRepository() {
            return commonRepository;
        }

        public Log getLogger() {
            return log;
        }

        public long getNextDocumentId(String namespace) throws Exception {
            return docIdCounter.getNextId(namespace);
        }

        public long getNextCollectionId() throws SQLException {
            return collectionIdCounter.getNextId();
        }

        public long getNextDocumentTypeId() throws SQLException {
            return docTypeCounter.getNextId();
        }

        public long getNextPartTypeId() throws SQLException {
            return partTypeCounter.getNextId();
        }

        public long getNextFieldTypeId() throws SQLException {
            return fieldTypeCounter.getNextId();
        }

        public long getNextLocalizedStringId() throws SQLException {
            return localizedStringCounter.getNextId();
        }

        public long getNextCommentId() throws SQLException {
            return commentCounter.getNextId();
        }

        public long getNextEventId() throws SQLException {
            return eventCounter.getNextId();
        }

        public long getNextUserId() throws SQLException {
            return userCounter.getNextId();
        }

        public long getNextRoleId() throws SQLException {
            return roleCounter.getNextId();
        }

        public long getNextNamespaceId() throws SQLException {
            return namespaceCounter.getNextId();
        }

        public long getNextLiveHistoryId() throws SQLException {
            return liveHistoryCounter.getNextId();
        }

        public LinkExtractor getLinkExtractor(String name) {
            return linkExtractorManager.getLinkExtractor(name);
        }

        public LinkExtractorInfo[] getLinkExtractors() {
            LinkExtractor[] extractors = linkExtractorManager.getLinkExtractors();
            LinkExtractorInfo[] extractorInfos = new LinkExtractorInfo[extractors.length];
            for (int i = 0; i < extractorInfos.length; i++) {
                extractorInfos[i] = new LinkExtractorInfoImpl(extractors[i].getName(), extractors[i].getDescription());
            }
            return extractorInfos;
        }

        public List<PluginHandle<PreSaveHook>> getPreSaveHooks() {
            return preSaveHooks;
        }

        public String getRepositoryNamespace() {
            return repositoryNamespace;
        }

        public String getRepositoryNamespace(Document document) throws RepositoryException {
            String ns = null;
            int pos = 0;

            while (ns == null && pos < repositoryNamespaces.size()) {
                ConditionalNamespace nsConf = repositoryNamespaces.get(pos);
                // should we use another user than the system user to go over the document?
                if (nsConf.checkCondition(document, systemUser))
                    ns = nsConf.getNamespace();

                pos++;
            }
            if (ns == null) {
                ns = this.getRepositoryNamespace();
            }

            return ns;
        }

        public String[] getRepositoryNamespaces() {
            String[] namespaces = new String[repositoryNamespaces.size() + 1];
            for (int i = 0; i < repositoryNamespaces.size(); i++) {
                namespaces[i + 1] = repositoryNamespaces.get(i).getNamespace();
            }
            namespaces[0] = repositoryNamespace;

            return namespaces;
        }
    }

    /**
     * Event listener that removes a document from the cache when it has been updated.
     */
    private class DocumentCacheInvalidator implements RepositoryListener {
        public void repositoryEvent(RepositoryEventType eventType, Object id, long updateCount) {
            if (eventType == RepositoryEventType.DOCUMENT_UPDATED
                    || eventType == RepositoryEventType.DOCUMENT_DELETED) {
                documentCache.remove((String)id);
                documentCache.removeAvailableVariants((String)id);
            } else if (eventType == RepositoryEventType.COLLECTION_DELETED
                    || eventType == RepositoryEventType.COLLECTION_UPDATED) {
                // If a collection is deleted (or its name is changed), we would
                // need to remove/update all cached document objects referencing
                // that collection.
                // To avoid this complexity, we simply clear the entire cache.
                documentCache.clear();
            } else if (eventType == RepositoryEventType.VERSION_UPDATED
                    || eventType == RepositoryEventType.DOCUMENT_VARIANT_UPDATED
                    || eventType == RepositoryEventType.DOCUMENT_VARIANT_CREATED
                    || eventType == RepositoryEventType.DOCUMENT_VARIANT_DELETED
                    || eventType == RepositoryEventType.DOCUMENT_VARIANT_TIMELINE_UPDATED) {
                VariantKey variantKey = (VariantKey)id;
                // If a variant is deleted we would need to remove/update all cached
                // objects referencing this variant through document.referenceLanguageId,
                // variant.(live|last)SyncedWith or version.syncedWith.
                // To avoid this complexity, we simply remove the whole document.
                documentCache.remove(variantKey.getDocumentId());
                // removing available variants is needed in all of these event types because
                // availableVariants contains the liveVersionId and the retired flag.
                documentCache.removeAvailableVariants(variantKey.getDocumentId());
            } else if (eventType == RepositoryEventType.LOCK_CHANGE) {
                VariantKey variantKey = (VariantKey)id;
                DocumentImpl document = documentCache.get(variantKey.getDocumentId(), variantKey.getBranchId(), variantKey.getLanguageId());
                if (document != null) {
                    document.clearLockInfo();
                }
            }
        }
    }

    private class ExpiredLockJanitor implements Runnable {
        public void run() {
            try {
                while (true) {
                    Thread.sleep(expiredLockJanitorInterval);
                    Connection conn = null;
                    PreparedStatement stmt = null;
                    try {
                        conn = dataSource.getConnection();
                        stmt = conn.prepareStatement("select doc_id, ns_id, ns.name_ as ns_name, branch_id, lang_id from locks left join daisy_namespaces ns on (locks.ns_id = ns.id) where time_expires is not null and time_expires < ?");
                        stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
                        ResultSet rs = stmt.executeQuery();
                        while (rs.next()) {
                            if (Thread.interrupted()) {
                                return;
                            }
                            String documentId = rs.getLong("doc_id") + "-" + rs.getString("ns_name");
                            long branchId = rs.getLong("branch_id");
                            long languageId = rs.getLong("lang_id");
                            try {
                                commonRepository.getDocument(documentId, branchId, languageId, false, systemUser).getLockInfo(true);
                            } catch (DocumentVariantNotFoundException e) {
                                // ignore
                            } catch (DocumentNotFoundException e) {
                                // ignore
                            } catch (RepositoryException e) {
                                log.error("Error trying to update expired lock info for document " + documentId + ", branch " + branchId + ", language " + languageId);
                            }
                        }
                    } catch (Throwable e) {
                        log.error("Exception occured in ExpiredLockJanitor.", e);
                    } finally {
                        jdbcHelper.closeStatement(stmt);
                        jdbcHelper.closeConnection(conn);
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            } finally {
                log.info("Expired lock janitor thread ended.");
            }
        }
    }

    private class LiveVersionJanitor implements Runnable {
        List<VariantKey> changedDocs = new ArrayList<VariantKey>();

        public void run() {
            try {
                while (true) {
                    Thread.sleep(liveVersionJanitorInterval);
                    Connection conn = null;
                    PreparedStatement stmt = null;
                    PreparedStatement stmt2 = null;
                    try {
                        Timestamp now = new Timestamp(System.currentTimeMillis());
                        conn = dataSource.getConnection();
                        jdbcHelper.startTransaction(conn);
                        
                        // select documents which are not in sync at this time
                        stmt = conn.prepareStatement("select dv.doc_id, dv.ns_id, ns.name_, dv.branch_id, dv.lang_id, dv.liveversion_id, lh.version_id from document_variants dv"
                                + " left join daisy_namespaces ns on dv.ns_id = ns.id"
                                + " left join live_history lh on dv.ns_id = lh.ns_id and dv.doc_id = lh.doc_id and dv.branch_id = lh.branch_id and dv.lang_id = lh.lang_id and ((? between lh.begin_date and lh.end_date) or (? >= lh.begin_date and lh.end_date is null))"
                                + " where not (dv.liveversion_id <=> lh.version_id)");
                        
                        stmt.setTimestamp(1, now);
                        stmt.setTimestamp(2, now);

                        stmt2 = conn.prepareStatement("update document_variants set liveversion_id = ? where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");

                        ResultSet rs = stmt.executeQuery();
                        while (rs.next()) {
                            long seqId = rs.getLong("doc_id");
                            String documentId = seqId + "-" + rs.getString("name_");
                            long namespaceId = rs.getLong("ns_id");
                            long branchId = rs.getLong("branch_id");
                            long languageId = rs.getLong("lang_id");
                            long versionId = jdbcHelper.getNullableIdField(rs, "lh.version_id");
                            
                            jdbcHelper.setNullableIdField(stmt2, 1, versionId);
                            stmt2.setLong(2, seqId);
                            stmt2.setLong(3, namespaceId);
                            stmt2.setLong(4, branchId);
                            stmt2.setLong(5, languageId);

                            stmt2.executeUpdate();
                            
                            documentCache.remove(documentId, branchId, languageId);
                        }
                        
                    } catch (Throwable e) {
                        if (conn != null) {
                            jdbcHelper.rollback(conn);
                        }
                        log.error("Exception occured in LiveVersionJanitor.", e);
                    } finally {
                        jdbcHelper.closeStatement(stmt);
                        jdbcHelper.closeStatement(stmt2);
                        jdbcHelper.closeConnection(conn);
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            } finally {
                log.info("Live Version janitor thread ended.");
            }
        }
    }

    private void checkDatabaseSchema() throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement("select propvalue from daisy_system where propname = 'schema_version'");
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                throw new RepositoryException("No schema_version found in daisy_system table.");
            String version = rs.getString(1);
            if (!DB_SCHEMA_VERSION.equals(version))
                throw new RepositoryException("The repository database schema is not at the correct version: found :\"" + version + "\", expected: \"" + DB_SCHEMA_VERSION + "\".");
        } catch (SQLException e) {
            throw new RepositoryException("Error getting schema version information.", e);
        } catch (RepositoryException e) {
            throw e;
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public String getRepositoryServerVersion() {
        return commonRepository.getServerVersion(systemUser);
    }

    private void assureNamespacesRegistered() throws RepositoryException {
        // check the default repository namespace
        Set<String> usedNamespaces = new HashSet<String>(this.repositoryNamespaces.size() + 1);

        assureNamespaceRegistered(this.repositoryNamespace, this.repositoryNamespaceFingerprint);
        usedNamespaces.add(this.repositoryNamespace);
        
        for (ConditionalNamespace namespaceConf: this.repositoryNamespaces) {
            assureNamespaceRegistered(namespaceConf.getNamespace(), namespaceConf.getFingerprint());
            usedNamespaces.add(namespaceConf.getNamespace());
        }

        for (Namespace ns : commonRepository.getNamespaceManager().getAllNamespaces(systemUser)) {
            if (ns.isManaged() && !usedNamespaces.contains(ns.getName())) {
                log.warn(ns.getName() + " is a repository managed namespace that is not refered to in the configuration." + 
                        " Check the configuration to see if this is correct or unmanage this namespace. ");                
            }
        }
        
    }
    
    private void assureNamespaceRegistered(String nsName, String fingerprint) throws RepositoryException {
        try {
            Namespace ns = commonRepository.getNamespaceManager().getNamespace(nsName);
            if (fingerprint != null && !fingerprint.equals(ns.getFingerprint())) {
                log.error("The repository namespace "+nsName+" exists, but its fingerprint does not match the specified fingerprint." +
                        " You should check your namespaces (either fix the fingerprint or choose another namespace name).");
            }
            if (!ns.isManaged()) {
                log.error("The namespace '" + ns.getName() + "' is known to the repository but it" +
                            " is not managed by the repository. This will most likely lead to errors when wanting to" +
                            " store documents under this namespace. You should either provide a document counter for this" + 
                            " namespace or choose another namespace.");
            }
        } catch (NamespaceNotFoundException e) {
            log.info("The namespace '" + this.repositoryNamespace + "' was not yet registered, doing that now.");
            // register
            Namespace ns;
            if (fingerprint == null) {
                ns = commonRepository.getNamespaceManager().registerNamespace(nsName, systemUser);
            } else {
                ns = commonRepository.getNamespaceManager().registerNamespace(nsName, fingerprint, systemUser);
            }
            ns.setDocumentCount(0);
            ns.setManaged(true);
            // and set as managed, we'll start the count at 1
            commonRepository.getNamespaceManager().updateNamespace(ns, systemUser);
        }

    }

    private class ConditionalNamespace {
        private String namespace;
        private String fingerprint;
        private String test;
        private PredicateExpression expression;

        public ConditionalNamespace(String namespace, String test) {
            this.namespace = namespace;
            this.test = test;
        }

        public ConditionalNamespace(String namespace, String fingerprint, String test) {
            this.namespace = namespace;
            this.fingerprint = fingerprint;
            this.test = test;
        }

        public PredicateExpression parseCondition(Context context) throws QueryException {
            if (test != null)
                this.expression = commonRepository.getQueryManager(systemUser).parsePredicateExpression(this.test);

            return this.expression;
        }

        public boolean checkCondition(Document document, AuthenticatedUser user) throws RepositoryException {
            return this.expression.evaluate(document, document.getLastVersion(), VersionMode.LAST);
        }

        public String getNamespace() {
            return namespace;
        }

        public String getTest() {
            return test;
        }

        public String getFingerprint() {
            return fingerprint;
        }
        
        public PredicateExpression getExpression() {
            return expression;
        }
    }

}
