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
package org.outerj.daisy.frontend.components.siteconf;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.avalon.excalibur.monitor.ActiveMonitor;
import org.apache.avalon.excalibur.monitor.DirectoryResource;
import org.apache.avalon.excalibur.monitor.FileResource;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.source.impl.FileSource;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlSaxHandler;
import org.outerj.daisy.configutil.PropertyResolver;
import org.outerj.daisy.frontend.WikiHelper;
import org.outerj.daisy.frontend.util.WikiPropertiesHelper;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.xmlutil.MergeCharacterEventsHandler;
import org.outerj.daisy.xmlutil.PropertyResolverContentHandler;
import org.outerx.daisy.x10Siteconf.SiteconfDocument;
import org.outerx.daisy.x10Siteconf.SiteconfDocument.Siteconf.DocumentTypeFilter.Exclude;
import org.outerx.daisy.x10Siteconf.SiteconfDocument.Siteconf.DocumentTypeFilter.Include;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Implementation of the sites manager.
 *
 * <p>Sites are defined by subdirectories inside one sites directory, each
 * of these subdirectories should contain a file called siteconf.xml.
 *
 * <p>This component uses the Excalibur ActiveMonitor to asynchronously
 * listen for changes to the siteconf files and the sites directory itself.
 */
public class SitesManagerImpl extends AbstractLogEnabled implements SitesManager, Serviceable, ThreadSafe,
        Disposable, Configurable, Initializable, Contextualizable {
    private ServiceManager serviceManager;
    private Context context;
    private ActiveMonitor monitor;
    /** Map hashed on site name, value is a {@link CachedEntry} instance. */
    private Map<String, CachedEntry> siteConfs = new ConcurrentHashMap<String, CachedEntry>(10, .75f, 1);
    /** Anyone who wants to do updates to the siteConfs must retrieve this lock first (since the map and the repository object are not thread-safe). */
    private Lock updateLock = new ReentrantLock();
    private String siteConfDirName;
    private File sitesDir;
    private DirectoryResource sitesDirResource;
    private boolean running = false;
    private String globalSitemapPath;

    private File globalSiteConfFile;
    private String globalSkinName;
    private String globalPublisherRequestSet;
    private FileResource globalSiteConfResource;

    private File globalSkinConfFile;
    private SaxBuffer globalSkinConf;
    private FileResource globalSkinConfResource;

    private Repository repository;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        monitor = (ActiveMonitor)serviceManager.lookup(ActiveMonitor.ROLE);
    }

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public void configure(Configuration configuration) throws ConfigurationException {
        siteConfDirName = PropertyResolver.resolveProperties(configuration.getChild("directory").getValue(), WikiPropertiesHelper.getResolveProperties(context));
    }

    public void initialize() throws Exception {
        repository = WikiHelper.getGuestRepository(serviceManager);
        SourceResolver sourceResolver = null;
        Source source = null;
        try {
            sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);
            source = sourceResolver.resolveURI(siteConfDirName);
            if (source instanceof FileSource) {
                sitesDir = ((FileSource)source).getFile();
                if (!sitesDir.exists()) {
                    throw new Exception("Specified sites directory does not exist: " + siteConfDirName);
                }
                if (!sitesDir.isDirectory()) {
                    throw new Exception("Specified sites directory is not a directory: " + siteConfDirName);
                }
            } else {
                throw new Exception("Specified sites directory does not point to a filesystem location: " + siteConfDirName);
            }
        } finally {
            if (source != null)
                sourceResolver.release(source);
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
        }
        this.globalSitemapPath = new File(new File(sitesDir, "cocoon"), "sitemap.xmap").toURI().toString();
        initialSitesDirectoryScan();
        sitesDirResource = new DirectoryResource(sitesDir.getAbsolutePath());
        sitesDirResource.addPropertyChangeListener(new SitesDirListener());
        monitor.addResource(sitesDirResource);
        running = true;
    }

    public void dispose() {
        running = false;

        if (sitesDirResource != null) {
            monitor.removeResource(sitesDirResource);
        }

        CachedEntry[] cachedEntries = siteConfs.values().toArray(new CachedEntry[0]);
        for (CachedEntry cachedEntry : cachedEntries) {
            monitor.removeResource(cachedEntry.siteConfResource);
            monitor.removeResource(cachedEntry.skinConfResource);
        }

        monitor.removeResource(globalSkinConfResource);

        serviceManager.release(monitor);
    }

    private void initialSitesDirectoryScan() throws Exception  {
        updateLock.lockInterruptibly();
        try {
            // Scan all subdirs of the sites dir
            File subdirs[] = sitesDir.listFiles();
            for (File subdir : subdirs) {
                if (subdir.isDirectory() && !subdir.isHidden() && !subdir.getName().equals("cocoon")) {
                    File siteConfFile = new File(subdir, "siteconf.xml");
                    SiteConfImpl siteConf = getSiteConf(siteConfFile);

                    // siteConf will be null if reading the siteconf.xml file failed for some reason.
                    // In this case, we still add the necessary entries so that we get notifications
                    // when the file would become available.

                    makeFileListeners(subdir, siteConf);
                }
            }

            this.globalSkinConfFile = new File(sitesDir, "skinconf.xml");
            this.globalSkinConf = loadSkinConf(globalSkinConfFile);
            this.globalSkinConfResource = new FileResource(globalSkinConfFile);
            this.globalSkinConfResource.addPropertyChangeListener(new GlobalSkinConfListener());
            monitor.addResource(globalSkinConfResource);

            this.globalSiteConfFile = new File(sitesDir, "siteconf.xml");
            loadGlobalParameters(globalSiteConfFile);
            this.globalSiteConfResource = new FileResource(globalSiteConfFile);
            this.globalSiteConfResource.addPropertyChangeListener(new GlobalSiteConfListener());
            monitor.addResource(globalSiteConfResource);
        } finally {
            updateLock.unlock();
        }
    }

    private void makeFileListeners(File siteDir, SiteConf siteConf) throws Exception {
        String siteName = siteDir.getName();
        File siteConfFile = new File(siteDir, "siteconf.xml");
        FileResource siteConfResource = new FileResource(siteConfFile);
        FileResource skinConfResource = new FileResource(new File(siteDir, "skinconf.xml"));
        SiteConfListener siteConfListener = new SiteConfListener(siteName, siteConfFile, siteConfResource, skinConfResource);
        siteConfResource.addPropertyChangeListener(siteConfListener);
        skinConfResource.addPropertyChangeListener(siteConfListener); // same listener
        siteConfs.put(siteName, new CachedEntry(siteConf, siteConfResource, skinConfResource));
        monitor.addResource(siteConfResource);
        monitor.addResource(skinConfResource);
    }

    private SiteConfImpl getSiteConf(File siteConfFile) {
        try {
            SiteconfDocument siteConfDocument;
            if (siteConfFile.exists()) {
                InputStream is = null;
                try {
                    is = new FileInputStream(siteConfFile);

                    XMLReader r = LocalSAXParserFactory.newXmlReader();
                    XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(r);
                    XmlSaxHandler xmlSaxHandler = XmlObject.Factory.newXmlSaxHandler(xmlOptions);
                    XmlObject siteConfAsXmlObject;
                    r.setContentHandler(new MergeCharacterEventsHandler(new PropertyResolverContentHandler(xmlSaxHandler.getContentHandler())));
                    r.parse(new InputSource(is));
                    
                    try {
                        siteConfAsXmlObject = xmlSaxHandler.getObject();
                    } catch (XmlException e) {
                        throw new SAXException("Error getting siteConf as XmlObject.", e);
                    }
                    

                    siteConfDocument = (SiteconfDocument)siteConfAsXmlObject.changeType(SiteconfDocument.type);
                    
                } catch (Exception e) {
                    getLogger().error("Could not read or parse the following siteconf.xml file: " + siteConfFile.getAbsolutePath());
                    return null;
                } finally {
                    if (is != null) {
                        try {
                                is.close();
                        } catch (Exception e) {
                            getLogger().error("Error closing inputstream on " + siteConfFile.getAbsolutePath());
                        }
                    }
                }

            if (!siteConfDocument.validate()) {
                getLogger().error("The following siteconf.xml file does not validate against the schema: " + siteConfFile.getAbsolutePath());
                return null;
            }

                SiteconfDocument.Siteconf siteConfXml = siteConfDocument.getSiteconf();

                long branchId = Branch.MAIN_BRANCH_ID;
                if (siteConfXml.isSetBranch()) {
                    String branch = siteConfXml.getBranch();
                    try {
                        branchId = repository.getVariantManager().getBranch(branch, false).getId();
                    } catch (RepositoryException e) {
                        getLogger().error("Problem with branch name \"" + branch + "\" in " + siteConfFile.getAbsolutePath(), e);
                        return null;
                    }
                }

                long languageId = Language.DEFAULT_LANGUAGE_ID;
                if (siteConfXml.isSetLanguage()) {
                    String language = siteConfXml.getLanguage();
                    try {
                        languageId = repository.getVariantManager().getLanguage(language, false).getId();
                    } catch (RepositoryException e) {
                        getLogger().error("Problem with language name \"" + language + "\" in " + siteConfFile.getAbsolutePath(), e);
                        return null;
                    }
                }
                
                long defaultReferenceLanguageId = -1;
                if (siteConfXml.isSetDefaultReferenceLanguage()) {
                    String defaultReferenceLanguage = siteConfXml.getDefaultReferenceLanguage();
                    try {
                        defaultReferenceLanguageId = repository.getVariantManager().getLanguage(defaultReferenceLanguage, false).getId();
                    } catch (RepositoryException e) {
                        getLogger().error("Problem with reference language name \"" + defaultReferenceLanguage + "\" in " + siteConfFile.getAbsolutePath(), e);
                        return null;
                    }
                }
                
                long collectionId = 0;
                if (siteConfXml.isSetCollectionId()) {
                    collectionId = siteConfXml.getCollectionId();
                } else if (siteConfXml.isSetCollectionName()) {
                    String collectionName = siteConfXml.getCollectionName();
                    try {
                        collectionId = repository.getCollectionManager().getCollection(collectionName, false).getId();
                    } catch (RepositoryException e) {
                        getLogger().error("Problem with collection name \"" + collectionName + "\" in " + siteConfFile.getAbsolutePath() );
                        return null;
                    }
                } else {
                    getLogger().error("No collection id or name set in siteconf + " + siteConfFile.getAbsolutePath());
                    return null;
                }

                String defaultDocumentType = siteConfXml.getDefaultDocumentType();
                long defaultDocumentTypeId = -1;
                if (defaultDocumentType != null) {
                    try {
                        defaultDocumentTypeId = repository.getRepositorySchema().getDocumentType(defaultDocumentType, false).getId();
                    } catch (RepositoryException e) {
                        getLogger().error("Problem with default document type \"" + defaultDocumentType + "\" in " + siteConfFile.getAbsolutePath(), e);
                        return null;
                    }
                }

                SiteSwitchingMode siteSwitchingMode = SiteSwitchingMode.ALL;
                String[] switchSites = null;
                if (siteConfXml.isSetSiteSwitching()) {
                    String mode = siteConfXml.getSiteSwitching().getMode();
                    if (mode == null) {
                        // stay in default
                    } else if (mode.equals("stay")) {
                        siteSwitchingMode = SiteSwitchingMode.STAY;
                    } else if (mode.equals("all")) {
                        siteSwitchingMode = SiteSwitchingMode.ALL;
                    } else if (mode.equals("selected")) {
                        siteSwitchingMode = SiteSwitchingMode.SELECTED;
                    }

                    if (siteSwitchingMode == SiteSwitchingMode.SELECTED) {
                        switchSites = siteConfXml.getSiteSwitching().getSiteList().toArray(new String[0]);
                    }
                }

                if (!siteConfXml.isSetHomepageDocId() && !siteConfXml.isSetHomepage()) {
                    getLogger().error("At least one of <homepageDocId> or <homepage> must be present in siteconf.xml at " + siteConfFile.getAbsolutePath());
                    return null;
                }

                boolean contextualizedTree = siteConfXml.getContextualizedTree();
                int navigationDepth;
                if (siteConfXml.isSetNavigationDepth()) {
                    navigationDepth = siteConfXml.getNavigationDepth();
                } else {
                    if (contextualizedTree)
                        navigationDepth = NavigationParams.DEFAULT_CONTEXTUALIZED_DEPTH;
                    else
                        navigationDepth = NavigationParams.DEFAULT_NONCONTEXTUALIZED_DEPTH;
                }


                File confDir = siteConfFile.getParentFile();
                SiteConfImpl siteConf = new SiteConfImpl(confDir, confDir.getName(), siteConfXml.getTitle(),
                        siteConfXml.getDescription(), siteConfXml.getSkin(), siteConfXml.getNavigationDocId(),
                        siteConfXml.getHomepageDocId(), siteConfXml.getHomepage(), collectionId,
                        contextualizedTree, navigationDepth,
                        VersionState.fromString(siteConfXml.getNewVersionStateDefault().toString()),
                        branchId, languageId, defaultReferenceLanguageId,
                        defaultDocumentTypeId, siteConfXml.getPublisherRequestSet(),
                        siteSwitchingMode, switchSites, this, this.repository.getVariantManager());

                if (siteConfXml.getLocking().isSetAutomatic()) {
                    SiteconfDocument.Siteconf.Locking.Automatic automatic = siteConfXml.getLocking().getAutomatic();
                    siteConf.setAutomaticLocking(true);
                    siteConf.setDefaultLockTime(automatic.getDefaultTime() * 60 * 1000); // in the XML, the time is in minutes, in the SiteConf object, it's in millis
                    siteConf.setLockType(LockType.fromString(automatic.getLockType().toString()));
                    siteConf.setAutoExtendLock(automatic.getAutoExtend());
                }

                if (siteConfXml.isSetDocumentTypeFilter()) {
                    WildcardPattern[] docTypeIncludePatterns = null;
                    WildcardPattern[] docTypeExcludePatterns = null;

                    Include[] includes = siteConfXml.getDocumentTypeFilter().getIncludeArray();
                    if (includes.length > 0) {
                        docTypeIncludePatterns = new WildcardPattern[includes.length];
                        for (int i = 0; i < includes.length; i++) {
                            docTypeIncludePatterns[i] = new WildcardPattern(includes[i].getName());
                        }
                    }

                    Exclude[] excludes = siteConfXml.getDocumentTypeFilter().getExcludeArray();
                    if (excludes.length > 0) {
                        docTypeExcludePatterns = new WildcardPattern[excludes.length];
                        for (int i = 0; i < excludes.length; i++) {
                            docTypeExcludePatterns[i] = new WildcardPattern(excludes[i].getName());
                        }
                    }

                    siteConf.setDocumentTypePatterns(docTypeIncludePatterns, docTypeExcludePatterns);
                }

                File skinConfFile = new File(confDir, "skinconf.xml");
                siteConf.setSkinConf(loadSkinConf(skinConfFile));

                return siteConf;
            } else {
                getLogger().error("The following directory does not contain a siteconf.xml file: " + siteConfFile.getParentFile().getAbsolutePath());
                return null;
            }
        } catch (Exception e) {
            getLogger().error("Problem with siteconf definition in " + siteConfFile.getAbsolutePath(), e);
            return null;
        }
    }

    private SaxBuffer loadSkinConf(File file) {
        if (file.exists()) {
            try {
                SAXParserFactory parserFactory = SAXParserFactory.newInstance();
                parserFactory.setNamespaceAware(true);
                parserFactory.setValidating(false);
                SAXParser parser = parserFactory.newSAXParser();
                XMLReader xmlReader = parser.getXMLReader();
                SaxBuffer buffer = new SaxBuffer();
                xmlReader.setContentHandler(new IncludeXMLConsumer(new MergeCharacterEventsHandler(new PropertyResolverContentHandler(buffer))));
                InputSource skinConfInputSource = new InputSource(new FileInputStream(file));
                xmlReader.parse(skinConfInputSource);
                return buffer;
            } catch (Throwable e) {
                getLogger().error("Error parsing skinconf file at " + file.getAbsolutePath(), e);
            }
        }
        return null;
    }

    static class CachedEntry {
        public SiteConf siteConf;
        public FileResource siteConfResource;
        public FileResource skinConfResource;

        public CachedEntry(SiteConf siteConf, FileResource siteConfResource, FileResource skinConfResource) {
            this.siteConf = siteConf;
            this.siteConfResource = siteConfResource;
            this.skinConfResource = skinConfResource;
        }
    }

    /**
     * Handler for when a siteconf.xml changes.
     */
    class SiteConfListener implements PropertyChangeListener {
        private String name;
        private File siteConfFile;
        private FileResource siteConfResource;
        private FileResource skinConfResource;

        public SiteConfListener(String name, File siteConfFile, FileResource siteConfResource, FileResource skinConfResource) {
            this.name = name;
            this.siteConfFile = siteConfFile;
            this.siteConfResource = siteConfResource;
            this.skinConfResource = skinConfResource;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (!running)
                return;

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Received change event for " + ((FileResource)evt.getSource()).getResourceKey());
            }

            try {
                updateLock.lockInterruptibly();
                try {
                    // The change event could be because the file was removed, however in
                    // that case we keep listening for events in case the file becomes
                    // available again. The case where the site's directory is completely
                    // removed is handles by the SitesDirListener

                    SiteConfImpl siteConf = getSiteConf(siteConfFile);
                    if (siteConf == null) {
                        siteConfs.put(name, new CachedEntry(null, siteConfResource, skinConfResource));
                    } else {
                        siteConfs.put(name, new CachedEntry(siteConf, siteConfResource, skinConfResource));
                    }
                } finally {
                    updateLock.unlock();
                }
            } catch (Exception e) {
                getLogger().error("Error processing siteconf.xml change event for " + siteConfFile.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Handler for when changes within the sites directory happen.
     */
    class SitesDirListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (!running)
                return;

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Received a change event on the SitesDirListener of type " + evt.getPropertyName());
            }

            try {
                updateLock.lockInterruptibly();
                try {
                    if (evt.getPropertyName().equals(DirectoryResource.REMOVED)) {
                        for (File file : ((Set<File>)evt.getNewValue())) {
                            String name = file.getName();
                            CachedEntry cachedEntry = siteConfs.get(name);
                            if (cachedEntry != null) {
                                monitor.removeResource(cachedEntry.siteConfResource);
                                monitor.removeResource(cachedEntry.skinConfResource);
                                siteConfs.remove(name);
                            }
                        }
                    } else if (evt.getPropertyName().equals(DirectoryResource.ADDED)) {
                        for (File file: ((Set<File>)evt.getNewValue())) {

                            if (file.isDirectory() && !file.isHidden()) {
                                newSiteDetected(file);
                            }
                        }
                    }
                } finally {
                    updateLock.unlock();
                }
            } catch (Throwable e) {
                getLogger().error("Error handling sites dir change event.", e);
            }
        }
    }

    /**
     * Handler for when the global skinconf.xml changes.
     */
    class GlobalSkinConfListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (!running)
                return;

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Received change event for " + ((FileResource)evt.getSource()).getResourceKey());
            }

            SitesManagerImpl.this.globalSkinConf = loadSkinConf(SitesManagerImpl.this.globalSkinConfFile);
        }
    }

    public String getGlobalSkinName() {
        return globalSkinName;
    }

    class GlobalSiteConfListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (!running)
                return;

            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Received change event for " + ((FileResource)evt.getSource()).getResourceKey());
            }

            loadGlobalParameters(SitesManagerImpl.this.globalSiteConfFile);
        }
    }

    private void loadGlobalParameters(File file) {
        try {
            if (file.exists()) {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                SiteconfDocument siteconfDoc = SiteconfDocument.Factory.parse(file, xmlOptions);

                this.globalSkinName = siteconfDoc.getSiteconf().getSkin();
                if (this.globalSkinName == null || this.globalSkinName.trim().length() == 0)
                    this.globalSkinName = "default";

                this.globalPublisherRequestSet = siteconfDoc.getSiteconf().getPublisherRequestSet();
                if (this.globalPublisherRequestSet == null || this.globalPublisherRequestSet.trim().length() == 0)
                    this.globalPublisherRequestSet = "default";
            }
        } catch (Exception e) {
            getLogger().error("Error reading global siteconf.xml at " + file.getAbsolutePath(), e);
        }
    }

    public SiteConf getSiteConf(String name) throws Exception {
        CachedEntry cachedEntry = siteConfs.get(name);
        SiteConf siteConf = null;

        if (cachedEntry != null)
            siteConf = cachedEntry.siteConf;

        if (siteConf == null)
            throw new ResourceNotFoundException("There is no site called \"" + name + "\".");

        return siteConf;
    }

    public SiteConf getSiteConfSoftly(String name) {
        CachedEntry cachedEntry = siteConfs.get(name);
        SiteConf siteConf = null;

        if (cachedEntry != null)
            siteConf = cachedEntry.siteConf;

        return siteConf;
    }

    public SaxBuffer getGlobalSkinConf() {
        return globalSkinConf;
    }

    public List<SiteConf> getSiteConfs() {
        List<CachedEntry> snapshot = new ArrayList<CachedEntry>(siteConfs.values());
        List<SiteConf> result = new ArrayList<SiteConf>(snapshot.size());

        for (CachedEntry entry : snapshot) {
            if (entry.siteConf != null)
                result.add(entry.siteConf);
        }

        return result;
    }

    public String getGlobalCocoonSitemapLocation() {
        return globalSitemapPath;
    }

    public String getGlobalPublisherRequestSet() {
        return globalPublisherRequestSet;
    }

    public void addNewSite(String siteName) throws Exception {
        // Note to the future:
        //
        // If we would ever need more of this kind of functionality, like hinting to
        // the sitesmanager that we want to reload all files right now, it might be
        // simpler to give up on this ActiveMonitor so that we avoid the complexity
        // of registering all these listeners.

        updateLock.lockInterruptibly();
        try {
            File siteDir = new File(sitesDir, siteName);
            newSiteDetected(siteDir);
        } finally {
            updateLock.unlock();
        }
    }

    private void newSiteDetected(File file) throws Exception {
        String siteName = file.getName();

        // check if we already know it
        CachedEntry cachedEntry = siteConfs.get(siteName);
        if (cachedEntry != null) {
            return;
        }

        // add new entry
        File siteConfFile = new File(file, "siteconf.xml");
        SiteConfImpl siteConf = getSiteConf(siteConfFile);
        makeFileListeners(file, siteConf);
    }
}
