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
import java.util.List;

import org.apache.cocoon.xml.SaxBuffer;
import org.outerj.daisy.navigation.LookupAlternative;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.schema.DocumentType;

public interface SiteConf {
    VariantKey getNavigationDoc();

    String getNavigationDocId();

    /**
     *
     * @return null if there is no homepage document ID specified.
     */
    String getHomePageDocId();

    String getHomePage();

    long getCollectionId();

    boolean contextualizedTree();

    int getNavigationDepth();

    String getName();

    String getTitle();

    String getDescription();

    String getSkin();

    String getCocoonSitemapLocation();

    String getGlobalCocoonSitemapLocation();

    VersionState getNewVersionStateDefault();

    boolean getAutomaticLocking();

    LockType getLockType();

    /**
     * Default lock time in milliseconds.
     */
    long getDefaultLockTime();

    boolean getAutoExtendLock();

    /**
     * A SaxBuffer containing XML configuration data intended to customise the skin.
     * The SaxBuffer does not contain startDocument and endDocument events.
     */
    SaxBuffer getSkinConf();

    long getBranchId();
    
    String getBranch();

    long getLanguageId();
    
    String getLanguage();
    
    long getDefaultReferenceLanguageId();

    File getDirectory();

    /**
     * Returns -1 if no default configured.
     */
    long getDefaultDocumentTypeId();

    /*
     * Filters the list of document types to those that should be visible within the current site.
     */
    List<DocumentType> filterDocumentTypes(List<DocumentType> documentTypes);

    String getPublisherRequestSet();

    /**
     * Returns the lookup alternatives for navigation lookups. This list should
     * contain at least one entry, the first entry should always represent the current
     * site.
     */
    LookupAlternative[] getNavigationLookupAlternatives(VersionMode versionMode);
}
