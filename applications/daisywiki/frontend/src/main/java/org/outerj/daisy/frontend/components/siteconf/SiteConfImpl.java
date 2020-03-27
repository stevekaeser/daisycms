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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cocoon.xml.SaxBuffer;
import org.outerj.daisy.navigation.LookupAlternative;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.variant.VariantManager;

public class SiteConfImpl implements SiteConf, Comparable {
    private String name;
    private String title;
    private String description;
    private String skin;
    private VariantKey navigationDoc;
    private String homePageDocId;
    private String homePage;
    private long collectionId;
    private boolean contextualizedTree;
    private int navigationDepth;
    private String cocoonSitemap;
    private VersionState newVersionStateDefault;
    private boolean automaticLocking;
    private LockType lockType;
    private long defaultLockTime;
    private boolean autoExtendLock;
    private SaxBuffer skinConf;
    private long branchId;
    private long languageId;
    private long defaultReferenceLanguageId;
    private File directory;
    private long defaultDocumentTypeId;
    private String publisherRequestSet;
    private SiteSwitchingMode siteSwicthingMode;
    private String[] switchSites;
    private WildcardPattern[] docTypeIncludePatterns;
    private WildcardPattern[] docTypeExcludePatterns;
    private SitesManagerImpl sitesManager;
    private VariantManager variantManager;

    public SiteConfImpl(File directory, String name, String title, String description, String skin, String navigationDocId,
            String homePageDocId, String homePage, long collectionId, boolean contextualizedTree, int navigationDepth,
            VersionState newVersionStateDefault, long branchId, long languageId, long referenceLanguageId, long defaultDocumentTypeId,
            String publisherRequestSet, SiteSwitchingMode siteSwitchingMode, String[] switchSites, SitesManagerImpl sitesManager, VariantManager variantManager) {
        this.name = name;
        this.title = title;
        this.description = description;
        this.skin = skin;
        this.navigationDoc = new VariantKey(navigationDocId, branchId, languageId);
        this.homePageDocId = homePageDocId;
        this.homePage = homePage;
        if (this.homePage == null)
            this.homePage = String.valueOf(homePageDocId) + ".html";
        this.collectionId = collectionId;
        this.contextualizedTree = contextualizedTree;
        this.navigationDepth = navigationDepth;
        this.newVersionStateDefault = newVersionStateDefault;
        this.automaticLocking = false;
        this.branchId = branchId;
        this.languageId = languageId;
        this.defaultReferenceLanguageId = referenceLanguageId;
        this.directory = directory;
        this.defaultDocumentTypeId = defaultDocumentTypeId;
        this.sitesManager = sitesManager;
        this.publisherRequestSet = publisherRequestSet;
        this.siteSwicthingMode = siteSwitchingMode;
        this.switchSites = switchSites;
        
        this.variantManager = variantManager;

        cocoonSitemap = new File(new File(directory, "cocoon"), "sitemap.xmap").toURI().toString();
    }

    protected void setAutomaticLocking(boolean automaticLocking) {
        this.automaticLocking = automaticLocking;
    }

    public void setAutoExtendLock(boolean autoExtendLock) {
        this.autoExtendLock = autoExtendLock;
    }

    public void setLockType(LockType lockType) {
        this.lockType = lockType;
    }

    public void setDefaultLockTime(long defaultLockTime) {
        this.defaultLockTime = defaultLockTime;
    }

    public VariantKey getNavigationDoc() {
        return navigationDoc;
    }

    public String getNavigationDocId() {
        return navigationDoc.getDocumentId();
    }

    public String getHomePageDocId() {
        return homePageDocId;
    }

    public String getHomePage() {
        return homePage;
    }

    public long getCollectionId() {
        return collectionId;
    }

    public boolean contextualizedTree() {
        return contextualizedTree;
    }

    public int getNavigationDepth() {
        return navigationDepth;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getSkin() {
        return skin;
    }

    public String getCocoonSitemapLocation() {
        return cocoonSitemap;
    }

    public String getGlobalCocoonSitemapLocation() {
        return sitesManager.getGlobalCocoonSitemapLocation();
    }

    public VersionState getNewVersionStateDefault() {
        return newVersionStateDefault;
    }

    public boolean getAutomaticLocking() {
        return automaticLocking;
    }

    public LockType getLockType() {
        return lockType;
    }

    public long getDefaultLockTime() {
        return defaultLockTime;
    }

    public boolean getAutoExtendLock() {
        return autoExtendLock;
    }

    public SaxBuffer getSkinConf() {
        if (skinConf != null)
            return skinConf;
        else
            return sitesManager.getGlobalSkinConf();
    }

    public String getPublisherRequestSet() {
        if (this.publisherRequestSet != null)
            return publisherRequestSet;
        else
            return sitesManager.getGlobalPublisherRequestSet();
    }

    public LookupAlternative[] getNavigationLookupAlternatives(VersionMode versionMode) {
        VersionMode navVersionMode = versionMode;
        if (siteSwicthingMode == SiteSwitchingMode.STAY) {
            return new LookupAlternative[] { new LookupAlternative(getName(), getCollectionId(), getNavigationDoc(), navVersionMode) };
        } else if (siteSwicthingMode == SiteSwitchingMode.ALL) {
            List<SiteConf> siteConfs = sitesManager.getSiteConfs();
            List<LookupAlternative> lookupAlternatives = new ArrayList<LookupAlternative>();
            lookupAlternatives.add(new LookupAlternative(getName(), getCollectionId(), getNavigationDoc(), navVersionMode));

            for (SiteConf siteConf : siteConfs) {
                if (!siteConf.getName().equals(name)) {
                    lookupAlternatives.add(new LookupAlternative(siteConf.getName(), siteConf.getCollectionId(), siteConf.getNavigationDoc(), navVersionMode));
                }
            }

            LookupAlternative[] lookupAlternativesArray = lookupAlternatives.toArray(new LookupAlternative[lookupAlternatives.size()]);

            // To have consistent results, sort the list of sites (excluding the current site, which should always stay at position 0)
            if (lookupAlternativesArray.length > 2)
                Arrays.sort(lookupAlternativesArray, 1, lookupAlternativesArray.length);

            return lookupAlternativesArray;
        } else if (siteSwicthingMode == SiteSwitchingMode.SELECTED) {
            List<LookupAlternative> lookupAlternatives = new ArrayList<LookupAlternative>(switchSites.length + 1);
            // Add current site as first entry
            lookupAlternatives.add(new LookupAlternative(getName(), getCollectionId(), getNavigationDoc(), navVersionMode));
            for (String switchSite : switchSites) {
                SiteConf siteConf = sitesManager.getSiteConfSoftly(switchSite);
                if (siteConf != null)
                    lookupAlternatives.add(new LookupAlternative(siteConf.getName(), siteConf.getCollectionId(),
                            siteConf.getNavigationDoc(), navVersionMode));
            }
            return lookupAlternatives.toArray(new LookupAlternative[lookupAlternatives.size()]);
        }
        throw new RuntimeException("Invalid site switching mode " + siteSwicthingMode);
    }

    /**
     * @param skinConf a SaxBuffer not containing startDocument and endDocument events.
     */
    protected void setSkinConf(SaxBuffer skinConf) {
        this.skinConf = skinConf;
    }

    public long getBranchId() {
        return branchId;
    }
    
    public String getBranch() {
        // getting it from the variant manager since the name can change at runtime
        // the variant manager caches this so this should not be that heavy.
        try {
            return this.variantManager.getBranch(this.branchId, false).getName();
        } catch (RepositoryException e) {
            throw new RuntimeException("Could fetch branch with id " + this.branchId, e);
        }
    }

    public long getLanguageId() {
        return languageId;
    }
    
    public String getLanguage () {
        // getting it from the variant manager since the name can change at runtime
        // the variant manager caches this so this should not be that heavy.
        try {
            return this.variantManager.getLanguage(this.languageId, false).getName();
        } catch (RepositoryException e) {
            throw new RuntimeException("Could fetch language with id " + this.branchId, e);
        }        
    }
    
    public long getDefaultReferenceLanguageId() {
        return defaultReferenceLanguageId;
    }

    public File getDirectory() {
        return directory;
    }

    public long getDefaultDocumentTypeId() {
        return defaultDocumentTypeId;
    }

    public List<DocumentType> filterDocumentTypes(List<DocumentType> documentTypes) {
        if (docTypeIncludePatterns == null && docTypeExcludePatterns == null)
            return documentTypes;

        // The documentTypes that can stay in the list are those that match
        // at least one include pattern and no exclude pattern
        // If there are no include patterns, "include all" is assumed (a "*" pattern) 
        
        List<DocumentType> includeResult = new ArrayList<DocumentType>(documentTypes.size());

        if (docTypeIncludePatterns != null && docTypeIncludePatterns.length > 0) {
            for (DocumentType documentType : documentTypes) {
                for (WildcardPattern pattern : docTypeIncludePatterns) {
                    if (pattern.matches(documentType.getName())) {
                        includeResult.add(documentType);
                        break;
                    }
                }
            }
        } else {
            includeResult = documentTypes;
        }

        List<DocumentType> excludeResult = new ArrayList<DocumentType>(includeResult.size());

        if (docTypeExcludePatterns != null && docTypeExcludePatterns.length > 0) {
            documents: for (DocumentType documentType : includeResult) {
                for (WildcardPattern pattern : docTypeExcludePatterns) {
                    if (pattern.matches(documentType.getName())) {
                        continue documents;
                    }
                }
                excludeResult.add(documentType);
            }
            return excludeResult;
        } else {
            return includeResult;
        }
    }

    public void setDocumentTypePatterns(WildcardPattern[] docTypeIncludePatterns, WildcardPattern[] docTypeExcludePatterns) {
        this.docTypeIncludePatterns = docTypeIncludePatterns;
        this.docTypeExcludePatterns = docTypeExcludePatterns;
    }

    public int compareTo(Object o) {
        SiteConfImpl otherSiteConf = (SiteConfImpl)o;
        return this.name.compareTo(otherSiteConf.name);
    }

}
