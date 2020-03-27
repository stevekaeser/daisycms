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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;
import org.outerj.daisy.publisher.serverimpl.docpreparation.ContentProcessor;
import org.outerj.daisy.publisher.serverimpl.docpreparation.PreparedDocuments;
import org.outerj.daisy.publisher.serverimpl.variables.Variables;
import org.outerj.daisy.publisher.serverimpl.variables.VariablesManager;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.EvaluationContext;

public class RootPublisherContext implements PublisherContext {
    private Repository repository;
    private PublisherImpl publisher;
    private VariablesManager variablesManager;
    private Locale locale = Locale.getDefault();
    private Log log;
    private DateFormat dateFormat;
    private List<LocationInfo> locationStack = new ArrayList<LocationInfo>();
    private static final String NO_DOC_AVAILABLE = "A document-related request was used in a context where no document is available.";

    public RootPublisherContext(Repository repository, PublisherImpl publisher, VariablesManager variablesManager, Log log) {
        if (repository == null)
            throw new IllegalArgumentException("repository argument cannot be null");
        if (log == null)
            throw new IllegalArgumentException("logger argument cannot be null");
        this.repository = repository;
        this.publisher = publisher;
        this.variablesManager = variablesManager;
        this.log = log;
        this.dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
    }

    public Locale getLocale() {
        return locale;
    }

    public Repository getRepository() {
        return repository;
    }

    public DateFormat getTimestampFormat() {
        return dateFormat;
    }

    public String getDocumentId() {
        throw new AssertionError(NO_DOC_AVAILABLE);
    }

    public long getBranchId() {
        throw new AssertionError(NO_DOC_AVAILABLE);
    }

    public long getLanguageId() {
        throw new AssertionError(NO_DOC_AVAILABLE);
    }

    public VariantKey getVariantKey() {
        throw new AssertionError(NO_DOC_AVAILABLE);
    }

    public long getVersionId() {
        throw new AssertionError(NO_DOC_AVAILABLE);
    }

    public long getVersionPreference() {
        return -3;
    }

    public Document getDocument() {
        throw new AssertionError(NO_DOC_AVAILABLE);
    }

    public Version getVersion() throws RepositoryException {
        throw new AssertionError(NO_DOC_AVAILABLE);
    }

    public boolean hasDocument() {
        return false;
    }

    public PublisherImpl getPublisher() {
        return publisher;
    }

    public Log getLogger() {
        return log;
    }

    public PreparedDocuments getPreparedDocuments() {
        return null;
    }

    public ContentProcessor getContentProcessor() {
        return null;
    }

    public boolean searchRecursivePrepDocs(String documentId, long branchId, long languageId, String pubReqSetName) {
        return false;
    }

    public VersionMode getVersionMode() {
        return VersionMode.LIVE;
    }

    public void pushContextDocuments(EvaluationContext evaluationContext) throws RepositoryException {
        // do nothing
    }

    public void pushLocation(LocationInfo locationInfo) {
        locationStack.add(locationInfo);
    }

    public void popLocation() {
        locationStack.remove(locationStack.size() - 1);
    }

    public List<LocationInfo> getLocationStack() {
        return locationStack; 
    }

    public VariablesManager getVariablesManager() {
        return variablesManager;
    }

    public VariablesConfig getVariablesConfig() {
        return VariablesConfig.DEFAULT_INSTANCE;
    }

    public Variables getVariables() {
        throw new AssertionError("No variables config available.");
    }

    public String resolveVariables(String text) {
        throw new AssertionError("No variables config available.");
    }
}
