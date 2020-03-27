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

import java.util.Locale;

import org.outerj.daisy.navigation.LookupAlternative;
import org.outerj.daisy.navigation.NavigationException;
import org.outerj.daisy.navigation.NavigationLookupResult;
import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Implementation of NavigationManager, instantiated on a per-Repository instance
 * basis, forwards real work to {@link CommonNavigationManager}.
 */
public class NavigationManagerImpl implements NavigationManager {
    private Repository repository;
    private CommonNavigationManager commonNavigationManager;

    public NavigationManagerImpl(Repository repository, CommonNavigationManager commonNavigationManager) {
        this.repository = repository;
        this.commonNavigationManager = commonNavigationManager;
    }

    public void generateNavigationTree(ContentHandler contentHandler, NavigationParams navigationParams,
            VariantKey activeDocument, boolean handleErrors) throws NavigationException, SAXException {
        generateNavigationTree(contentHandler, navigationParams, activeDocument, handleErrors, false);
    }
    
    public void generateNavigationTree(ContentHandler contentHandler, NavigationParams navigationParams,
            VariantKey activeDocument, boolean handleErrors, boolean allowOld) throws NavigationException, SAXException {
        commonNavigationManager.generateNavigationTree(contentHandler, navigationParams, activeDocument,
                handleErrors, repository.getUserId(), repository.getActiveRoleIds(), allowOld);
    }

    public void generatePreviewNavigationTree(ContentHandler contentHandler, String navigationTreeXml,
            long branchId, long languageId, Locale locale) throws RepositoryException, SAXException {
        commonNavigationManager.generatePreviewNavigationTree(contentHandler, navigationTreeXml, branchId, languageId,
                repository.getUserId(), repository.getActiveRoleIds(), locale);
    }

    public void generatePreviewNavigationTree(ContentHandler contentHandler, String navigationTreeXml,
            long branchId, long languageId) throws RepositoryException, SAXException {
        this.generatePreviewNavigationTree(contentHandler, navigationTreeXml, branchId, languageId, Locale.getDefault());
    }

    public NavigationLookupResult lookup(String navigationPath, long requestedBranchId, long requestedLanguageId, LookupAlternative[] lookupAlternatives) throws RepositoryException {
        return lookup(navigationPath, requestedBranchId, requestedLanguageId, lookupAlternatives, false);
    }

    public NavigationLookupResult lookup(String navigationPath, long requestedBranchId, long requestedLanguageId, LookupAlternative[] lookupAlternatives, boolean allowOld) throws RepositoryException {
        return commonNavigationManager.lookup(navigationPath, requestedBranchId, requestedLanguageId, lookupAlternatives, repository.getUserId(), repository.getActiveRoleIds(), allowOld);
    }

    public String reverseLookup(VariantKey document, VariantKey navigationDoc, VersionMode versionMode) throws RepositoryException {
        return reverseLookup(document, navigationDoc, versionMode, false);
    }

    public String reverseLookup(VariantKey document, VariantKey navigationDoc, VersionMode versionMode, boolean allowOld) throws RepositoryException {
        return commonNavigationManager.reverseLookup(document, navigationDoc, versionMode, repository.getUserId(), repository.getActiveRoleIds(), allowOld);
    }

    public String reverseLookup(VariantKey document, VariantKey navigationDoc) throws RepositoryException {
        return reverseLookup(document, navigationDoc, false);
    }

    public String reverseLookup(VariantKey document, VariantKey navigationDoc, boolean allowOld) throws RepositoryException {
        return commonNavigationManager.reverseLookup(document, navigationDoc, VersionMode.LIVE, repository.getUserId(), repository.getActiveRoleIds(), allowOld);
    }

}
