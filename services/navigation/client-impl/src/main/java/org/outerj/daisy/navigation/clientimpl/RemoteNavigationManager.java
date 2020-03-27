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
package org.outerj.daisy.navigation.clientimpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.outerj.daisy.navigation.LookupAlternative;
import org.outerj.daisy.navigation.NavigationLookupResult;
import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerx.daisy.x10Navigationspec.NavigationLookupDocument;
import org.outerx.daisy.x10Navigationspec.NavigationLookupResultDocument;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class RemoteNavigationManager implements NavigationManager {
    private RemoteRepositoryImpl repository;

    public RemoteNavigationManager(RemoteRepositoryImpl repository) {
        this.repository = repository;
    }

    public void generateNavigationTree(ContentHandler contentHandler, NavigationParams navigationParams,
            VariantKey activeDocument, boolean handleErrors, boolean allowOld) throws RepositoryException, SAXException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        HttpMethod method = new GetMethod("/navigation/tree");
        List<NameValuePair> params = new ArrayList<NameValuePair>();
        VariantKey navigationDoc = navigationParams.getNavigationDoc();
        params.add(new NameValuePair("navigationDocId", navigationDoc.getDocumentId()));
        params.add(new NameValuePair("navigationDocBranch", String.valueOf(navigationDoc.getBranchId())));
        params.add(new NameValuePair("navigationDocLanguage", String.valueOf(navigationDoc.getLanguageId())));
        params.add(new NameValuePair("navigationVersionMode", navigationParams.getVersionMode().toString()));
        if (navigationParams.getActivePath() != null)
            params.add(new NameValuePair("activePath", navigationParams.getActivePath()));
        if (activeDocument != null) {
            params.add(new NameValuePair("activeDocumentId", activeDocument.getDocumentId()));
            params.add(new NameValuePair("activeDocumentBranch", String.valueOf(activeDocument.getBranchId())));
            params.add(new NameValuePair("activeDocumentLanguage", String.valueOf(activeDocument.getLanguageId())));
        }
        params.add(new NameValuePair("contextualized", String.valueOf(navigationParams.getContextualized())));
        params.add(new NameValuePair("depth", String.valueOf(navigationParams.getDepth())));            
        params.add(new NameValuePair("handleErrors", String.valueOf(handleErrors)));
        if (navigationParams.getLocale() != null)
            params.add(new NameValuePair("locale", LocaleHelper.getString(navigationParams.getLocale())));
        params.add(new NameValuePair("addChildCounts", String.valueOf(navigationParams.getAddChildCounts())));
        params.add(new NameValuePair("allowOld", String.valueOf(allowOld)));

        NameValuePair[] queryString = params.toArray(new NameValuePair[0]);
        method.setQueryString(queryString);

        httpClient.executeMethod(method, null, false);
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);
            XMLReader xmlReader = parserFactory.newSAXParser().getXMLReader();
            xmlReader.setContentHandler(contentHandler);
            xmlReader.parse(new InputSource(method.getResponseBodyAsStream()));
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        } finally {
            method.releaseConnection();
        }
    }

    public void generateNavigationTree(ContentHandler contentHandler, NavigationParams navigationParams, 
            VariantKey activeDocument, boolean handleErrors) throws RepositoryException, SAXException {
        generateNavigationTree(contentHandler, navigationParams, activeDocument, handleErrors, false);
    }

    public void generatePreviewNavigationTree(ContentHandler contentHandler, String navigationTreeXml,
            long branchId, long languageId, Locale locale) throws RepositoryException, SAXException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/navigation/preview");
        method.addParameter("navigationXml", navigationTreeXml);
        method.addParameter("branch", String.valueOf(branchId));
        method.addParameter("language", String.valueOf(languageId));
        if (locale != null)
            method.addParameter("locale", LocaleHelper.getString(locale));

        httpClient.executeMethod(method, null, false);
        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);
            XMLReader xmlReader = parserFactory.newSAXParser().getXMLReader();
            xmlReader.setContentHandler(contentHandler);
            xmlReader.parse(new InputSource(method.getResponseBodyAsStream()));
        } catch (IOException e) {
            throw new RepositoryException(e);
        } catch (ParserConfigurationException e) {
            throw new RepositoryException(e);
        } finally {
            method.releaseConnection();
        }
    }

    public void generatePreviewNavigationTree(ContentHandler contentHandler, String navigationTreeXml,
            long branchId, long languageId) throws RepositoryException, SAXException {
        this.generatePreviewNavigationTree(contentHandler, navigationTreeXml, branchId, languageId, null);
    }

    public NavigationLookupResult lookup(String navigationPath, long requestedBranchId, long requestedLanguageId, LookupAlternative[] lookupAlternatives, boolean allowOld) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/navigation/lookup");

        NavigationLookupDocument navigationLookupDocument = NavigationLookupDocument.Factory.newInstance();
        NavigationLookupDocument.NavigationLookup navigationLookupXml = navigationLookupDocument.addNewNavigationLookup();
        navigationLookupXml.setNavigationPath(navigationPath);
        if (requestedBranchId != -1)
            navigationLookupXml.setRequestedBranchId(requestedBranchId);
        if (requestedLanguageId != -1)
            navigationLookupXml.setRequestedLanguageId(requestedLanguageId);
        

        NavigationLookupDocument.NavigationLookup.LookupAlternative[] lookupAlternativesXml
                = new NavigationLookupDocument.NavigationLookup.LookupAlternative[lookupAlternatives.length];

        for (int i = 0; i < lookupAlternatives.length; i++) {
            LookupAlternative lookupAlternative = lookupAlternatives[i];
            lookupAlternativesXml[i] = NavigationLookupDocument.NavigationLookup.LookupAlternative.Factory.newInstance();
            lookupAlternativesXml[i].setName(lookupAlternative.getName());
            lookupAlternativesXml[i].setCollectionId(lookupAlternative.getCollectionId());
            VariantKey navDocKey = lookupAlternative.getNavigationDoc();
            lookupAlternativesXml[i].setNavDocId(navDocKey.getDocumentId());
            lookupAlternativesXml[i].setNavBranchId(navDocKey.getBranchId());
            lookupAlternativesXml[i].setNavLangId(navDocKey.getLanguageId());
            lookupAlternativesXml[i].setNavVersionMode(lookupAlternative.getVersionMode().toString());
        }

        navigationLookupXml.setLookupAlternativeArray(lookupAlternativesXml);
        
        navigationLookupXml.setAllowOld(allowOld);

        method.setRequestEntity(new InputStreamRequestEntity(navigationLookupDocument.newInputStream()));
        NavigationLookupResultDocument resultDocument = (NavigationLookupResultDocument)httpClient.executeMethod(method, NavigationLookupResultDocument.class, true);
        return NavigationLookupResult.createFromXml(resultDocument.getNavigationLookupResult());
    }

    public NavigationLookupResult lookup(String navigationPath, long requestedBranchId, long requestedLanguageId, LookupAlternative[] lookupAlternatives) throws RepositoryException {
        return lookup(navigationPath, requestedBranchId, requestedLanguageId, lookupAlternatives, false);
    }

    public String reverseLookup(VariantKey document, VariantKey navigationDoc, VersionMode versionMode, boolean allowOld) throws RepositoryException {
        throw new RuntimeException("Method not yet implemented in remote implementation.");
    }

    public String reverseLookup(VariantKey document, VariantKey navigationDoc, VersionMode versionMode) throws RepositoryException {
        throw new RuntimeException("Method not yet implemented in remote implementation.");
    }

    public String reverseLookup(VariantKey document, VariantKey navigationDoc, boolean allowOld) throws RepositoryException {
        throw new RuntimeException("Method not yet implemented in remote implementation.");
    }

    public String reverseLookup(VariantKey document, VariantKey navigationDoc) throws RepositoryException {
        throw new RuntimeException("Method not yet implemented in remote implementation.");
    }

}
