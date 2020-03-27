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
import org.outerj.daisy.publisher.serverimpl.variables.VariablesManager;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.EvaluationContext;

public interface PublisherContext {
    public static long NO_VERSION = -1000;

    Locale getLocale();

    String getDocumentId();

    long getBranchId();

    long getLanguageId();

    /**
     * If a live version was requested but the document doesn't have a live version,
     * this method returns {@link #NO_VERSION}. Otherwise, the actual version number
     * is returned.
     */
    long getVersionId() throws RepositoryException;

    /**
     * Returns a version preference ID (-1 for live, -2 for last, -3 for live or last depending
     * on the publisher version mode)
     */
    long getVersionPreference();

    VariantKey getVariantKey();

    Document getDocument() throws RepositoryException;

    /**
     * Returns true if the getDocument and getVersion methods can safely be called.
     * (though getVersion might return null)
     */
    boolean hasDocument();

    /**
     * If a live version was requested but the document doesn't have a live version,
     * this method returns null.
     */
    Version getVersion() throws RepositoryException;

    DateFormat getTimestampFormat();

    Repository getRepository();

    Log getLogger();

    PublisherImpl getPublisher();

    /**
     * Returns null if not available.
     */
    PreparedDocuments getPreparedDocuments();

    /**
     * Returns null if not available.
     */
    ContentProcessor getContentProcessor();

    boolean searchRecursivePrepDocs(String documentId, long branchId, long languageId, String pubReqSetName);

    VersionMode getVersionMode();

    void pushContextDocuments(EvaluationContext evaluationContext) throws RepositoryException;

    void pushLocation(LocationInfo locationInfo);

    void popLocation();

    /**
     * Will never return null.
     */
    VariablesConfig getVariablesConfig();

    /**
     * Can return null.
     */
    Variables getVariables();

    /**
     * Returns null if there are no variables resolved in the given text.
     */
    String resolveVariables(String text);

    VariablesManager getVariablesManager();
}
