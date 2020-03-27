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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import java.text.DateFormat;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;
import org.outerj.daisy.publisher.serverimpl.docpreparation.ContentProcessor;
import org.outerj.daisy.publisher.serverimpl.docpreparation.PreparedDocuments;
import org.outerj.daisy.publisher.serverimpl.variables.Variables;
import org.outerj.daisy.publisher.serverimpl.variables.VariablesHelper;
import org.outerj.daisy.publisher.serverimpl.variables.VariablesManager;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionNotFoundException;
import org.outerj.daisy.repository.query.EvaluationContext;

public class PublisherContextImpl implements PublisherContext {
    private PublisherContext parentPublisherContext;

    private Locale locale = null;
    private DateFormat dateFormat = null;
    private boolean documentInformationSet = false;
    private String documentId = null;
    private long branchId = -1;
    private long languageId = -1;
    private VariantKey variantKey;
    private boolean versionIdSet = false;
    private long versionId = -3;
    private Document document = null;
    private PreparedDocuments preparedDocuments;
    private ContentProcessor contentProcessor;
    private VersionMode versionMode;

    private VariablesConfig variablesConfig;
    private boolean variablesLoaded = false;
    private Variables variables;

    public PublisherContextImpl(PublisherContext parentPublisherContext) {
        if (parentPublisherContext == null)
            throw new IllegalArgumentException("parentPublisherContext argument cannot be null");
        this.parentPublisherContext = parentPublisherContext;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        if (locale == null)
            this.dateFormat =  null;
        else
            this.dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
    }

    public Locale getLocale() {
        if (this.locale != null)
            return locale;
        else
            return parentPublisherContext.getLocale();
    }

    public Repository getRepository() {
        return parentPublisherContext.getRepository();
    }

    public Log getLogger() {
        return parentPublisherContext.getLogger();
    }

    public void setDocumentVariant(String documentId, long branchId, long languageId) {
        this.documentId = documentId;
        this.branchId = branchId;
        this.languageId = languageId;
        this.variantKey = new VariantKey(documentId, branchId, languageId);
        this.documentInformationSet = true;
    }

    public void setVersionId(long versionId) {
        this.versionId = versionId;
        this.versionIdSet = true;
    }

    public void setPreparedDocuments(PreparedDocuments preparedDocuments) {
        this.preparedDocuments = preparedDocuments;
    }

    public void setContentProcessor(ContentProcessor contentProcessor) {
        this.contentProcessor = contentProcessor;
    }

    public String getDocumentId() {
        if (documentInformationSet)
            return documentId;
        else
            return parentPublisherContext.getDocumentId();
    }

    public long getBranchId() {
        if (documentInformationSet)
            return branchId;
        else
            return parentPublisherContext.getBranchId();
    }

    public long getLanguageId() {
        if (documentInformationSet)
            return languageId;
        else
            return parentPublisherContext.getLanguageId();
    }

    public long getVersionId() throws RepositoryException {
        if (documentInformationSet) {
            long versionId = versionIdSet ? this.versionId : parentPublisherContext.getVersionPreference();
            if (versionId == -1) {
                Version version = getDocument().getLiveVersion();
                if (version != null)
                    return version.getId();
                else
                    return NO_VERSION;
            } else if (versionId == -2) {
                return getDocument().getLastVersionId();
            } else {
                return versionId;
            }
        } else {
            return parentPublisherContext.getVersionId();
        }
    }

    public long getVersionPreference() {
        if (versionIdSet && versionId < 0)
            return versionId;
        else
            return parentPublisherContext.getVersionPreference();
    }

    public VariantKey getVariantKey() {
        if (documentInformationSet)
            return variantKey;
        else
            return parentPublisherContext.getVariantKey();
    }

    public DateFormat getTimestampFormat() {
        if (this.dateFormat != null)
            return dateFormat;
        else
            return parentPublisherContext.getTimestampFormat();
    }

    public Document getDocument() throws RepositoryException {
        if (documentInformationSet) {
            if (document == null) {
                document = getRepository().getDocument(getDocumentId(), getBranchId(), getLanguageId(), false);
            }
            return document;
        } else {
            return parentPublisherContext.getDocument();
        }
    }

    public Version getVersion() throws RepositoryException {
        if (documentInformationSet) {
            // Special values for versionId:
            //   -1: live version
            //   -2: last version
            //   -3: follow the versionMode from the publisher context hierarchy

            long versionId = versionIdSet ? this.versionId : parentPublisherContext.getVersionPreference();
            Version version;
            if (versionId == -1) {
                version = getDocument().getLiveVersion();
            } else if (versionId == -2) {
                version = getDocument().getLastVersion();
            } else if (versionId == -3) {
                try {
                    version = document.getVersion(getVersionMode());
                } catch (VersionNotFoundException vfne) {
                    return null;
                }
            } else {
                version = getDocument().getVersion(versionId);
            }
            return version;
        } else {
            return parentPublisherContext.getVersion();
        }
    }

    public boolean hasDocument() {
        if (documentInformationSet)
            return true;
        else
            return parentPublisherContext.hasDocument();
    }

    public PublisherImpl getPublisher() {
        return parentPublisherContext.getPublisher();
    }

    public PreparedDocuments getPreparedDocuments() {
        if (this.preparedDocuments != null)
            return this.preparedDocuments;
        else
            return parentPublisherContext.getPreparedDocuments();
    }

    public ContentProcessor getContentProcessor() {
        if (this.contentProcessor != null)
            return this.contentProcessor;
        else
            return parentPublisherContext.getContentProcessor();
    }

    public boolean searchRecursivePrepDocs(String documentId, long branchId, long languageId, String pubReqSetName) {
        if (this.preparedDocuments != null && this.preparedDocuments.getPubReqSet().equals(pubReqSetName) &&
                documentId.equals(getDocumentId()) && getBranchId() == branchId && getLanguageId() == languageId)
            return true;
        else
            return parentPublisherContext.searchRecursivePrepDocs(documentId, branchId, languageId, pubReqSetName);
    }

    public void setVersionMode(VersionMode versionMode) {
        this.versionMode = versionMode;
    }

    public VersionMode getVersionMode() {
        if (this.versionMode != null)
            return versionMode;
        else
            return parentPublisherContext.getVersionMode();
    }

    public void pushContextDocuments(EvaluationContext evaluationContext) throws RepositoryException {
        // first let parent push it on the stack
        parentPublisherContext.pushContextDocuments(evaluationContext);
        // and then ourselves, if we have a document
        if (documentInformationSet) {
            evaluationContext.pushContextDocument(getDocument(), getVersion());
        }
    }


    public void pushLocation(LocationInfo locationInfo) {
        parentPublisherContext.pushLocation(locationInfo);
    }

    public void popLocation() {
        parentPublisherContext.popLocation();
    }

    public VariablesManager getVariablesManager() {
        return parentPublisherContext.getVariablesManager();
    }

    public String resolveVariables(String text) {
        Variables variables = getVariables();
        if (variables == null)
            return null;
        return VariablesHelper.substituteVariables(text, variables);
    }

    public Variables getVariables() {
        if (variablesConfig == null
                && getVersionMode() == parentPublisherContext.getVersionMode()
                && !(parentPublisherContext instanceof RootPublisherContext)) {
            return parentPublisherContext.getVariables();
        } else {
            if (!variablesLoaded) {
                VariantKey[] variableDocs = getVariablesConfig().getVariableDocs();
                if (variableDocs != null)
                    variables = getVariablesManager().getVariables(variableDocs, getRepository(), getVersionMode());
                variablesLoaded = true;
            }

            return variables;
        }
    }

    public VariablesConfig getVariablesConfig() {
        if (this.variablesConfig != null)
            return variablesConfig;
        else
            return parentPublisherContext.getVariablesConfig();
    }

    public void setVariablesConfig(VariablesConfig variablesConfig) {
        this.variablesConfig = variablesConfig;
    }
}
