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
package org.outerj.daisy.navigation.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PreDestroy;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.collections.comparators.ReverseComparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.credentialsprovider.CredentialsProvider;
import org.outerj.daisy.navigation.LookupAlternative;
import org.outerj.daisy.navigation.NavigationException;
import org.outerj.daisy.navigation.NavigationLookupResult;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryListener;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.revision.DateRange;
import org.outerj.daisy.repository.revision.RepositoryRevisionManager;
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Component providing the functionality to generate navigation trees. This component
 * does not provide any service of itself, but registers an extension with the repository.
 *
 */
public class CommonNavigationManager {
    private static final String EXTENSION_NAME = "NavigationManager";
    private static final ReverseComparator REVERSE_NATURAL_COMPARATOR = new ReverseComparator();
    private static final String NAV_TREE_CACHE_NAME = "org.outerj.daisy.navigation.trees";

    private String repositoryKey;
    private CredentialsProvider credentialsProvider;
    private RepositoryManager repositoryManager;
    private Repository repository;
    private RepositoryRevisionManager revisionManager; 
    private VariantManager variantManager;
    private PluginRegistry pluginRegistry;
    private ExtensionProvider extensionProvider = new MyExtensionProvider();

    private int treeBuildingThreads;
    private ExecutorService treeBuildingExecutor;
    private ScheduledExecutorService invalidTreeJanitor = Executors.newSingleThreadScheduledExecutor();
    private URL cacheConfiguration;
    private CacheManager cacheManager;
    private CacheEventListener navTreeCacheEventListener = new EvictionListener();
    
    private long nearestTreeTolerance = -1; 
    
    private EhcacheWrapper<NavigationCacheKey, NavigationTreeHolder> cachedNavigationTrees;
    private final Map<VariantKey, TreeMap<Date, NavigationTreeHolder>> cachedNavigationTreesByDocId = new HashMap<VariantKey, TreeMap<Date, NavigationTreeHolder>>();
    
    private Context context = new Context();
    private CacheInvalidator cacheInvalidator = new CacheInvalidator();
    private final Log log = LogFactory.getLog(getClass());
    
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    public CommonNavigationManager(Configuration configuration, RepositoryManager repositoryManager,
            PluginRegistry pluginRegistry, CredentialsProvider credentialsProvider) throws Exception {
        this.pluginRegistry = pluginRegistry;
        this.repositoryManager = repositoryManager;
        this.credentialsProvider = credentialsProvider;
        configure(configuration);
        initialize();
    }

    @PreDestroy
    public void destroy() {
        stop();
        dispose();
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        repositoryKey = configuration.getChild("repositoryKey").getValue("internal");
        
        treeBuildingThreads = configuration.getChild("treeBuildingThreads").getValueAsInteger(8);
        nearestTreeTolerance = configuration.getChild("nearestTreeTolerance").getValueAsLong(5 * 60); // default to 5 minutes
        
        String cacheConfigURL= configuration.getChild("cacheConfigurationURL").getValue("").trim();
        if (cacheConfigURL != null && cacheConfigURL.trim().length() > 0) {
            try {
                cacheConfiguration = new URL(cacheConfigURL);
            } catch (MalformedURLException murle) {
                log.error("Provided cache configuration url is not a valid, will fall back to built-in configuration");
            }
        }
        if (cacheConfiguration == null) {
            cacheConfiguration = this.getClass().getResource("/daisy-navigation-ehcache.xml");
        }
    }

    private void initialize() throws Exception {
        try {
            repository = repositoryManager.getRepository(credentialsProvider.getCredentials(repositoryKey));
        } catch (Throwable e) {
            throw new Exception("Problem getting repository.", e);
        }

        revisionManager = (RepositoryRevisionManager)repository.getExtension("RevisionManager");
        pluginRegistry.addPlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);

        variantManager = repository.getVariantManager();
        repository.addListener(cacheInvalidator);
        
        cacheManager = new CacheManager(cacheConfiguration);
        cacheManager.addCache(NAV_TREE_CACHE_NAME);
        navTreeCacheEventListener = new EvictionListener();
        cachedNavigationTrees = new EhcacheWrapper<NavigationCacheKey, NavigationTreeHolder>(NAV_TREE_CACHE_NAME, cacheManager);
        cachedNavigationTrees.getCache().getCacheEventNotificationService().registerListener(navTreeCacheEventListener);

        treeBuildingExecutor = Executors.newFixedThreadPool(treeBuildingThreads);
    }

    private class MyExtensionProvider implements ExtensionProvider {
        public Object createExtension(Repository repository) {
            return new NavigationManagerImpl(repository, CommonNavigationManager.this);
        }
    }

    public void generateNavigationTree(ContentHandler contentHandler, NavigationParams navigationParams,
            VariantKey activeDocument, boolean handleErrors, long userId, long[] roleIds, boolean allowOld)
            throws NavigationException, SAXException {
        SaxBuffer buffer = new SaxBuffer();

        try {
            if (activeDocument != null)
                activeDocument = new VariantKey(repository.normalizeDocumentId(activeDocument.getDocumentId()), activeDocument.getBranchId(), activeDocument.getLanguageId());

            NavigationTree navigationTree = getNavigation(navigationParams.getNavigationDoc(), navigationParams.getVersionMode(), userId, roleIds, allowOld);
            Node rootNode = navigationTree.getRootNode();

            if (log.isDebugEnabled()) {
                log.debug("Generating navigation tree with following params: navigationDoc = "
                        + navigationParams.getNavigationDoc() + ", activePath = "+ navigationParams.getActivePath()
                        + ", activeDocument = " + activeDocument + ", contextualized = "
                        + navigationParams.getContextualized() + ", userId = " + userId + ", roleIds = " + arrayToString(roleIds));
            }

            List<Node> activeNodePath = null;
            if (navigationParams.getActivePath() != null) {
                String[] path = splitPath(navigationParams.getActivePath());
                if (path.length > 0) {
                    Node[] foundPath = new Node[path.length];
                    long searchBranchId = activeDocument != null ? activeDocument.getBranchId() : navigationParams.getNavigationDoc().getBranchId();
                    long searchLanguageId = activeDocument != null ? activeDocument.getLanguageId() : navigationParams.getNavigationDoc().getLanguageId();
                    rootNode.searchPath(path, 0, searchBranchId, searchLanguageId, foundPath);

                    // check if a full path has been found
                    for (int i = 0; i < foundPath.length; i++) {
                        if (foundPath[i] == null) {
                            foundPath = null;
                            break;
                        }
                    }

                    // check that the last element in the foundPath is the activeDocument (if there is an activeDocument)
                    if (foundPath != null && activeDocument != null) {
                        if (foundPath[foundPath.length - 1] instanceof DocumentNode) {
                            DocumentNode node = (DocumentNode)foundPath[foundPath.length - 1];
                            if (!node.getVariantKey().equals(activeDocument)) {
                                foundPath = null;
                            }
                        } else {
                            foundPath = null;
                        }
                    }

                    if (foundPath != null)
                        activeNodePath = Arrays.asList(foundPath);
                }
            }

            if (activeNodePath == null && activeDocument != null) {
                activeNodePath = rootNode.searchDocument(activeDocument);
                if (activeNodePath != null)
                    Collections.reverse(activeNodePath);
            }

            // check if the user has read permissions to all the document nodes on the activeNodePath
            // if not, set the activeNodePath to null
            if (activeNodePath != null) {
                try {
                    for (Object node : activeNodePath) {
                        if (node instanceof DocumentNode) {
                            Document document = ((DocumentNode)node).getDocument();
                            AclResultInfo aclResultInfo = repository.getAccessManager().getAclInfoOnLive(userId, roleIds, document);
                            if (!aclResultInfo.isAllowed(AclPermission.READ)) {
                                activeNodePath = null;
                                break;
                            }
                        }
                    }
                } catch (Throwable e) {
                    throw new NavigationException("Error checking document permissions.", e);
                }
            }

            NavigationValueFormatter valueFormatter = new NavigationValueFormatter(navigationParams.getLocale());

            // if there is an activeNodePath, produce a tree with active nodes highlighted, otherwise generate a default tree
            Node[] activeNodePathArray = activeNodePath == null ? null : activeNodePath.toArray(new Node[activeNodePath.size()]);
            rootNode.generateXml(buffer != null ? buffer : contentHandler, activeNodePathArray, 0, navigationParams.getDepth(), "", userId, roleIds, valueFormatter, navigationParams.getAddChildCounts());
        } catch (Exception e) {
            buffer = null;
            if (handleErrors) {
                streamError(contentHandler, e);
                log.error("Error generating navigation tree", e);
            } else {
                throw new NavigationException("Error generating navigation tree.", e);
            }
        }
        if (buffer != null) {
            buffer.toSAX(contentHandler);
        }
    }

    private String arrayToString(long[] values) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0)
                buffer.append(",");
            buffer.append(values[i]);
        }
        return buffer.toString();
    }

    public void generatePreviewNavigationTree(ContentHandler contentHandler, String navigationTreeXml,
            long branchId, long languageId, long userId, long[] roleIds, Locale locale) throws NavigationException, SAXException {
        try {
            RootNode rootNode = new RootNode();
            BuildContext buildContext = new BuildContext(rootNode, context, VersionMode.LIVE);
            NavigationFactory.build(rootNode, navigationTreeXml, branchId, languageId, buildContext);
            rootNode.generateXml(contentHandler, null, 0, Integer.MAX_VALUE, "", userId, roleIds, new NavigationValueFormatter(locale), false);
        } catch (Exception e) {
            throw new NavigationException("Error generating navigation tree.", e);
        }
    }

    public NavigationLookupResult lookup(String navigationPath, long requestedBranchId, long requestedLanguageId,
            LookupAlternative[] lookupAlternatives, long userId, long[] activeRoleIds, boolean allowOld) throws NavigationException {
        if (lookupAlternatives.length < 1)
            throw new IllegalArgumentException("lookupAlternatives array should contain at least one entry.");

        try {
            NavigationTree navigationTree = getNavigation(lookupAlternatives[0].getNavigationDoc(), lookupAlternatives[0].getVersionMode(), userId, activeRoleIds, allowOld);
            NavigationLookupResult navigationLookupResult;

            String[] path = splitPath(navigationPath);
            if (path.length > 0) {
                // Note: the special handling with the navException is to try to recover in case of navigation tree
                // errors, so that lookups (and thus document access) continues to work.
                Exception navException = null;
                Node[] foundPath = new Node[path.length];
                try {
                    long searchBranchId = requestedBranchId != -1 ? requestedBranchId : lookupAlternatives[0].getNavigationDoc().getBranchId();
                    long searchLanguageId = requestedLanguageId != -1 ? requestedLanguageId : lookupAlternatives[0].getNavigationDoc().getLanguageId();
                    navigationTree.getRootNode().searchPath(path, 0, searchBranchId, searchLanguageId, foundPath);
                } catch (Exception e) {
                    navException = e;
                }

                // check if a full path has been found
                for (int i = 0; i < foundPath.length; i++) {
                    if (foundPath[i] == null) {
                        foundPath = null;
                        break;
                    }
                }

                // navigationPath is in some cases used as part of the returned result, and for
                // consistency we want that to always start with a slash
                if (navigationPath.charAt(0) != '/')
                    navigationPath = "/" + navigationPath;

                if (foundPath == null) {
                    String lastPathPart = path[path.length - 1];
                    if (couldBeDocumentId(lastPathPart)) {
                        String documentId = repository.normalizeDocumentId(lastPathPart);
                        navigationLookupResult = searchLookupAlternatives(documentId, requestedBranchId, requestedLanguageId, lookupAlternatives, userId, activeRoleIds, allowOld);
                    } else {
                        if (navException != null)
                            throw navException;
                        else
                            navigationLookupResult = NavigationLookupResult.createNotFoundResult();
                    }
                } else {
                    Object node = foundPath[foundPath.length - 1];
                    if (node instanceof GroupNode) {
                        String docPath = ((GroupNode)node).findFirstDocumentNode(navigationPath, userId, activeRoleIds);
                        if (docPath != null) {
                            navigationLookupResult = NavigationLookupResult.createRedirectResult(lookupAlternatives[0].getName(), docPath, null);
                        } else {
                            navigationLookupResult = NavigationLookupResult.createNotFoundResult();
                        }
                    } else if (node instanceof DocumentNode) {
                        VariantKey variantKey = ((DocumentNode)node).getVariantKey();
                        if ((requestedBranchId != -1 && variantKey.getBranchId() != requestedBranchId)
                                || (requestedLanguageId != -1 && variantKey.getLanguageId() != requestedLanguageId)) {
                            navigationLookupResult = searchLookupAlternatives(variantKey.getDocumentId(), requestedBranchId, requestedLanguageId, lookupAlternatives, userId, activeRoleIds, allowOld);
                        } else {
                            navigationLookupResult = NavigationLookupResult.createMatchResult(((DocumentNode)node).getVariantKey(), navigationPath);
                        }
                    } else {
                        navigationLookupResult = NavigationLookupResult.createNotFoundResult();
                    }
                }
            } else {
                navigationLookupResult = NavigationLookupResult.createNotFoundResult();
            }

            return navigationLookupResult;
        } catch (Exception e) {
            throw new NavigationException("Error in NavigationManager.", e);
        }
    }

    private NavigationLookupResult searchLookupAlternatives(String docId, long requestedBranchId, long requestedLanguageId,
            LookupAlternative[] lookupAlternatives, long userId, long[] roleIds, boolean allowOld) {
        // Determine branch and language
        long branchId = requestedBranchId != -1 ? requestedBranchId : lookupAlternatives[0].getNavigationDoc().getBranchId();
        long languageId = requestedLanguageId != -1 ? requestedLanguageId : lookupAlternatives[0].getNavigationDoc().getLanguageId();
        VariantKey variantKey  = new VariantKey(docId, branchId, languageId);

        // Get the collections of the document
        long[] collectionIds = null;
        try {
            DocumentCollection[] collections = repository.getDocument(variantKey, false).getCollections().getArray();
            collectionIds = new long[collections.length];
            for (int i = 0; i < collections.length; i++)
                collectionIds[i] = collections[i].getId();
            Arrays.sort(collectionIds);
        } catch (Throwable e) {
            // ignore
        }

        NavigationLookupResult navigationLookupResult = null;
        if (collectionIds == null) {
            // If getting the collections failed.
            String redirectPath = null;
            try {
                NavigationTree navigationTree = getNavigation(lookupAlternatives[0].getNavigationDoc(), lookupAlternatives[0].getVersionMode(), userId, roleIds, allowOld);
                redirectPath = navigationTree.lookupNode(variantKey);
            } catch (Throwable e) {
                // error in navtree, ignore
            }
            if (redirectPath != null)
                navigationLookupResult = NavigationLookupResult.createRedirectResult(lookupAlternatives[0].getName(), redirectPath, variantKey);
            else
                navigationLookupResult = NavigationLookupResult.createMatchResult(variantKey, "");
        } else {
            LookupAlternative firstMatch = null;
            for (LookupAlternative lookupAlternative : lookupAlternatives) {
                VariantKey altNavDoc = lookupAlternative.getNavigationDoc();

                String redirectPath = null;
                try {
                    NavigationTree altNavigationTree = getNavigation(lookupAlternative.getNavigationDoc(), lookupAlternative.getVersionMode(), userId, roleIds, allowOld);
                    redirectPath = altNavigationTree.lookupNode(variantKey);
                } catch (Throwable e) {
                    // error in navtree, ignore
                }
                if (redirectPath != null) {
                    navigationLookupResult = NavigationLookupResult.createRedirectResult(lookupAlternative.getName(), redirectPath, variantKey);
                    break;
                }

                if (branchId == altNavDoc.getBranchId() && languageId == altNavDoc.getLanguageId()
                        && Arrays.binarySearch(collectionIds, lookupAlternative.getCollectionId()) >= 0) {
                    if (firstMatch == null)
                        firstMatch = lookupAlternative;
                }
            }
            if (navigationLookupResult == null) {
                if (firstMatch == null)
                    firstMatch = lookupAlternatives[0];
                if (firstMatch.getName().equals(lookupAlternatives[0].getName())) {
                    // we're already in the right lookup alternative
                    navigationLookupResult = NavigationLookupResult.createMatchResult(variantKey, "");
                } else {
                    navigationLookupResult = NavigationLookupResult.createRedirectResult(firstMatch.getName(), "/" + variantKey.getDocumentId(), variantKey);
                }
            }
        }
        return navigationLookupResult;
    }

    public String reverseLookup(VariantKey document, VariantKey navigationDoc, VersionMode versionMode, long userId, long[] roleIds, boolean allowOld) throws RepositoryException {
        NavigationTree navigationTree = getNavigation(navigationDoc, versionMode, userId, roleIds, allowOld);
        return navigationTree.lookupNode(document);
    }

    private boolean couldBeDocumentId(String text) {
        return Constants.DAISY_COMPAT_DOCID_PATTERN.matcher(text).matches();
    }

    private void streamError(ContentHandler contentHandler, Exception e) throws SAXException {
        contentHandler.startDocument();
        contentHandler.startPrefixMapping("", Node.NAVIGATION_NS);
        contentHandler.startElement(Node.NAVIGATION_NS, "navigationTree", "navigationTree", new AttributesImpl());
        contentHandler.startElement(Node.NAVIGATION_NS, "navigationTreeError", "navigationTreeError", new AttributesImpl());
        contentHandler.endElement(Node.NAVIGATION_NS, "navigationTreeError", "navigationTreeError");
        contentHandler.endElement(Node.NAVIGATION_NS, "navigationTree", "navigationTree");
        contentHandler.endPrefixMapping("");
        contentHandler.endDocument();
    }

    private String[] splitPath(String path) {
        StringTokenizer tokenizer = new StringTokenizer(path, "/");
        String[] parts = new String[tokenizer.countTokens()];

        for (int i = 0; i < parts.length; i++) {
            parts[i] = tokenizer.nextToken();
        }

        return parts;
    }

    private NavigationTree getNavigation(VariantKey navigationDoc, VersionMode versionMode, long userId, long[] roleIds, boolean allowOutdated) throws RepositoryException {
        final VariantKey navDoc = new VariantKey(repository.normalizeDocumentId(navigationDoc.getDocumentId()), navigationDoc.getBranchId(), navigationDoc.getLanguageId());
        
        DateRange range;
        if (versionMode.isLast()) {
            range = null;
        } else if (versionMode.isLive()) {
            range = revisionManager.getRevisionDateRange(new Date());
        } else {
            range = revisionManager.getRevisionDateRange(versionMode.getDate());
        }
        VersionMode normalizedMode = range == null ? VersionMode.LAST : VersionMode.get(range.getStart());
        final NavigationCacheKey cacheKey = new NavigationCacheKey(navDoc, normalizedMode);
        
        NavigationTreeHolder treeHolder = null;
        lock.writeLock().lock();
        try {
            treeHolder = cachedNavigationTrees.get(cacheKey);
            if (treeHolder == null) {
                treeHolder = new NavigationTreeHolder();
                cachedNavigationTrees.put(cacheKey, treeHolder);
            }
        } finally { lock.writeLock().unlock(); }
        
        NavigationTree result = treeHolder.getTree(context, navDoc, normalizedMode, range, allowOutdated);
        
        if (versionMode.isLast()) {
            for (VariantKey dependency : result.getRootNode().getDependencies()) {
                if (!context.canReadNonLive(dependency, userId, roleIds)) {
                    RootNode rootNode = new RootNode();
                    rootNode.add(new ErrorNode("Access to non-live versions of navigation trees is only allowed when you have read permission to them. Tree is: " + navDoc));
                    return new NavigationTree(rootNode);
                }
            }
        } else if (!versionMode.isLive()) {
            for (VariantKey dependency : result.getRootNode().getDependencies()) {
                if (!context.canReadLiveHistory(dependency, userId, roleIds)) {
                    RootNode rootNode = new RootNode();
                    rootNode.add(new ErrorNode("Access to versions of navigation trees other than the current live one is only allowed when you have read permission to them. Tree is: " + navDoc));
                    return new NavigationTree(rootNode);
                }
            }
        }
        return result;
    }

    private static class NavigationCacheKey {
        private final VariantKey navigationDoc;
        private final VersionMode versionMode;
        private final String asString;

        public NavigationCacheKey(VariantKey navigationDoc, VersionMode versionMode) {
            this.navigationDoc = navigationDoc;
            this.versionMode = versionMode;
            this.asString = navigationDoc.toString() + versionMode.toString();
        }

        public String toString() {
            return asString;
        }

        public int hashCode() {
            return asString.hashCode();
        }

        public boolean equals(Object obj) {
            NavigationCacheKey other = (NavigationCacheKey)obj;
            return this.versionMode.equals(other.versionMode) && this.navigationDoc.equals(other.navigationDoc);
        }
    }

    /**
     *  first marks the trees as invalidated, then schedules a job to remove them when they become too stale.
     */
    private void invalidateCachedTrees() {
        lock.writeLock().lock();
        try {
            // Mark all navigation tree holders as invalid and schedule a job to remove them
            final List<NavigationCacheKey> keys = new ArrayList<NavigationCacheKey>();
            for (NavigationCacheKey key: (List<NavigationCacheKey>)cachedNavigationTrees.getCache().getKeys()) {
                NavigationTreeHolder treeHolder = cachedNavigationTrees.get(key);
                treeHolder.invalidate();
                if  (key.versionMode.isLast()) {
                    keys.add(key);
                }
            }

            // Schedule the cleanup task to run after the configured interval ('nearestTreeTolerance')
            invalidTreeJanitor.schedule(new Runnable() {
                public void run() {
                    for (NavigationCacheKey key: keys) {
                        lock.writeLock().lock();
                        try {
                            NavigationTreeHolder treeHolder = cachedNavigationTrees.get(key);
                            if (treeHolder != null) {
                                try {
                                    treeHolder.treeLock.readLock().lock();
                                    if (!treeHolder.valid && treeHolder.future == null){
                                        // the tree is not being rebuilt, so get rid of it.
                                        cachedNavigationTrees.remove(key);
                                        // Since versionMode.isLast() == true, the tree can't be in the 'byId' hashmap.
                                    }
                                } finally {
                                    if (treeHolder != null) {
                                        treeHolder.treeLock.readLock().unlock();
                                    }
                                }
                            }
                        } finally { lock.writeLock().unlock(); }
                    }
                }
                
            }, nearestTreeTolerance, TimeUnit.SECONDS);
        } finally { lock.writeLock().unlock(); }
    }
    
    private void stop() {
        try {
            // Wait a while for existing tasks to terminate
            log.info("Waiting for invalid tree janitor to be terminated.");
            invalidTreeJanitor.shutdownNow(); // Cancel currently executing
                                              // invalidations.
            if (!invalidTreeJanitor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Invalid tree janitor did not terminate.");
            }
        } catch (InterruptedException ie) {
            // (Re-)attempt shutdown if current thread is terminated.
            invalidTreeJanitor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }

        try {
            // Wait a while for existing tasks to terminate
            log.info("Waiting for tree-building executor to be terminated.");
            treeBuildingExecutor.shutdownNow(); // Cancel currently executing
                                              // builds.
            if (!treeBuildingExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.err.println("Tree-building executor did not terminate.");
            }
        } catch (InterruptedException ie) {
            // (Re-)attempt shutdown if current thread is terminated.
            treeBuildingExecutor.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        
    }

    public void dispose() {
        repository.removeListener(cacheInvalidator);
        cachedNavigationTrees.getCache().getCacheEventNotificationService().unregisterListener(navTreeCacheEventListener);
        pluginRegistry.removePlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);
    }

    private class CacheInvalidator implements RepositoryListener {

        public void repositoryEvent(RepositoryEventType eventType, Object id,
                long updateCount) {
            // LOCK change events don't invalidate the navigation trees. All other events do.
            if (eventType.isBranchEvent() ||
                    eventType.isLanguageEvent() ||
                    eventType.isNamespaceEvent() || 
                    eventType.isCollectionEvent() ||
                    eventType.isDocumentEvent() ||
                    (eventType.isVariantEvent() && eventType != RepositoryEventType.LOCK_CHANGE)) {
                invalidateCachedTrees();
            }
        }
        
    }

    public class Context {
        private Context() {
            // private constructor to prevent public construction
        }

        public Repository getRepository() {
            return repository;
        }

        public AclResultInfo getAclInfo(VariantKey document, long userId, long[] roleIds) throws RepositoryException {
            return repository.getAccessManager().getAclInfoOnLive(userId, roleIds, document);
        }

        public boolean canRead(VariantKey document, long userId, long[] roleIds) throws RepositoryException {
            return getAclInfo(document, userId, roleIds).isAllowed(AclPermission.READ);
        }

        public boolean canReadNonLive(VariantKey document, long userId, long[] roleIds) throws RepositoryException {
            return getAclInfo(document, userId, roleIds).isNonLiveAllowed(AclPermission.READ);
        }
        
        public boolean canReadLiveHistory(VariantKey document, long userId, long[] roleIds) throws RepositoryException {
            return getAclInfo(document, userId, roleIds).isLiveHistoryAllowed(AclPermission.READ);
        }

        public String getBranchName(long branchId) throws RepositoryException {
            return variantManager.getBranch(branchId, false).getName();
        }

        public String getLanguageName(long languageId) throws RepositoryException {
            return variantManager.getLanguage(languageId, false).getName();
        }

        public long getBranchId(String name) throws RepositoryException {
            return variantManager.getBranch(name, false).getId();
        }

        public long getLanguageId(String name) throws RepositoryException {
            return variantManager.getLanguage(name, false).getId();
        }

        public Log getLogger() {
            return log;
        }
    }
    
    private class NavigationTreeHolder {
        private ReentrantReadWriteLock treeLock = new ReentrantReadWriteLock();
        private NavigationTree tree;
        private DateRange range;
        private boolean valid = false;
        private FutureTask<FutureTree> future;
        private long currentVersion = 0;
        private long lastFutureVersion = 0;
        
        public NavigationTreeHolder() {
        }
        
        public void invalidate() {
            treeLock.writeLock().lock();
            try {
                if (this.tree != null) {
                    // mark the xml in the current tree as invalid.
                    this.tree.getRootNode().invalidate();
                }
                this.valid = false;
                this.future = null;
            } finally { treeLock.writeLock().unlock(); }
        }

        public NavigationTree getTree(final Context context,
                final VariantKey navDoc, final VersionMode normalizedMode, final DateRange range, boolean allowOld) throws NavigationException {
            try {
                treeLock.writeLock().lock();
                FutureTask<FutureTree> localFuture = future; // (a)
                try {
                    if (!valid && future == null) {
                        final FutureTree result = new FutureTree(++lastFutureVersion);
                        localFuture = future = createFutureTask(context, navDoc, normalizedMode, range, result); // (b)
                        treeBuildingExecutor.execute(future);
                    }
                    
                    if (tree != null && (valid || allowOld))
                        return tree;  // c
                    
                    if (allowOld && !normalizedMode.isLive() && !normalizedMode.isLast() && nearestTreeTolerance > 0) { // 'live' should be translated to @date, so the check for isLive() is not really needed
                        // look for an existing tree that isn't too outdated.
                        Date maxDate = normalizedMode.getDate();
                        Date minDate = new Date(maxDate.getTime() - (1000 * nearestTreeTolerance));

                        TreeMap<Date, NavigationTreeHolder> treeHolders = cachedNavigationTreesByDocId.get(navDoc);
                        if (treeHolders != null) {
                            SortedMap<Date, NavigationTreeHolder> treesInRange = treeHolders.subMap(maxDate, minDate); // dates are reversed because the maps are are using ReverseComparators
                            for (Date key: treesInRange.keySet()) {
                                NavigationTreeHolder treeHolder = treesInRange.get(key);
                                if (treeHolder != null && treeHolder.range != null) {
                                    Date endDate = treeHolder.range.getEnd();
                                    // we don't want trees that are not valid for the normalized date. endDate.after(maxDate) ensures this.
                                    if (endDate == null || endDate.after(maxDate)) { 
                                        return treeHolder.tree;
                                    }
                                }
                            }
                        }
                    }

                } finally { treeLock.writeLock().unlock(); }
                
                // localFuture can not be null here:
                // future != null -> localFuture is set at (a)
                // future == null && !valid -> localFuture is set at (b)
                // future == null && valid -> this implies that tree != null, in which case we never get here (return at (c)).
                return localFuture.get().tree;

            } catch (Throwable t) {
                throw new NavigationException("Failed to build navigation tree", t);
            }
            
        }

        private FutureTask<FutureTree> createFutureTask(final Context context,
                final VariantKey navDoc, final VersionMode normalizedMode,
                final DateRange range, final FutureTree result) {
            return new FutureTask<FutureTree>(new Runnable() {
   
                public void run() {
                    
                    RootNode root;
                    try {
                        root = new RootNode();
                        BuildContext buildContext = new BuildContext(root, context, normalizedMode);
                        NavigationFactory.build(root, navDoc, buildContext);
                    } catch (NavigationException e) {
                        root = new RootNode();
                        root.add(new ErrorNode("Error building navigation tree"));
                        log.error("Error building navigation tree " + navDoc, e);
                    }

                    CommonNavigationManager.this.lock.writeLock().lock();
                    try {
                        treeLock.writeLock().lock();
                        try {
                            result.tree = new NavigationTree(root);
                            if (result.treeNumber  > currentVersion) {
                                // this tree is newer than the current tree - so update it
                                NavigationTreeHolder.this.tree = result.tree;
                                NavigationTreeHolder.this.range = range;
                                // is the new tree valid or not? 
                                if (result.treeNumber == lastFutureVersion) {
                                    NavigationTreeHolder.this.valid = true;
                                    NavigationTreeHolder.this.future = null;
                                    NavigationTreeHolder.this.currentVersion = result.treeNumber;
                                } else {
                                    if (NavigationTreeHolder.this.tree != null) {
                                        NavigationTreeHolder.this.tree.getRootNode().invalidate();
                                    }
                                }
                            }
                        } finally { treeLock.writeLock().unlock(); }
                    } finally { CommonNavigationManager.this.lock.writeLock().unlock(); }
                }
                
            }, result);
        }
    }
    
    private class FutureTree {
        private NavigationTree tree = null;
        private long treeNumber;
        public FutureTree(long treeNumber) {
            this.treeNumber = treeNumber;
        }
    }

    public void notifyRemoveAll(Ehcache cache) {
        // TODO Auto-generated method stub
    }
    
    private class EvictionListener implements CacheEventListener {

        public void notifyElementRemoved(Ehcache cache, Element element)
        throws CacheException {
            removeFromOtherCache(cache, element);
        }
        
        public void notifyElementPut(Ehcache cache, Element element)
                throws CacheException {
            addToOtherCache(cache, element);
        }
        
        public void notifyElementUpdated(Ehcache cache, Element element)
                throws CacheException {
            addToOtherCache(cache, element);
        }
        
        public void notifyElementExpired(Ehcache cache, Element element) {
            try {
                lock.writeLock().lock();
                removeFromOtherCache(cache, element);
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        public void notifyElementEvicted(Ehcache cache, Element element) {
            try {
                lock.writeLock().lock();
                removeFromOtherCache(cache, element);
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void dispose() {
            // not used
            
        }

        public void notifyRemoveAll(Ehcache cache) {
            cachedNavigationTreesByDocId.clear();
        }
        
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
        
        public void removeFromOtherCache(Ehcache cache, Element element) {
            if (cache.getName().equals(NAV_TREE_CACHE_NAME)) {
                NavigationCacheKey key = (NavigationCacheKey) element.getObjectKey();
                
                if (!key.versionMode.isLast() && !key.versionMode.isLive()) {
                    TreeMap<Date, NavigationTreeHolder> treeMap = cachedNavigationTreesByDocId.get(key.navigationDoc);
                    if (treeMap != null) {
                        treeMap.remove(key.versionMode.getDate());
                        if (treeMap.size() == 0) {
                            cachedNavigationTreesByDocId.remove(key.navigationDoc);
                        }
                    }
                }
            }
        }

        public void addToOtherCache(Ehcache cache, Element element) {
            if (cache.getName().equals(NAV_TREE_CACHE_NAME)) {
                NavigationCacheKey key = (NavigationCacheKey) element.getObjectKey();
                
                if (!key.versionMode.isLast() && !key.versionMode.isLive()) {
                    TreeMap<Date, NavigationTreeHolder> treeMap = cachedNavigationTreesByDocId.get(key.navigationDoc);
                    if (treeMap == null) {
                        treeMap = new TreeMap<Date, NavigationTreeHolder>(REVERSE_NATURAL_COMPARATOR);
                        cachedNavigationTreesByDocId.put(key.navigationDoc, treeMap);
                    }
                    treeMap.put(key.versionMode.getDate(), (NavigationTreeHolder)element.getObjectValue());
                }
            }
        }

    }
    
}
