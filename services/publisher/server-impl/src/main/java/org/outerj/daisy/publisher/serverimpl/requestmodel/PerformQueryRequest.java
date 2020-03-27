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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerj.daisy.publisher.serverimpl.variables.QueryVarResolverHandler;
import org.outerj.daisy.publisher.serverimpl.variables.Variables;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerx.daisy.x10.SearchResultDocument;
import org.xml.sax.ContentHandler;

public class PerformQueryRequest extends AbstractRequest implements Request {
    private final String query;
    private final String extraConditions;
    private final DocumentRequest documentRequest;

    public PerformQueryRequest(String query, String extraConditions, DocumentRequest documentRequest, LocationInfo locationInfo) {
        super(locationInfo);
        this.query = query;
        this.extraConditions = extraConditions;
        this.documentRequest = documentRequest;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        QueryManager queryManager = publisherContext.getRepository().getQueryManager();
        Locale locale = publisherContext.getLocale();

        EvaluationContext evaluationContext = new EvaluationContext();
        publisherContext.pushContextDocuments(evaluationContext);

        Map<String, String> queryOptions = null;
        if (publisherContext.getVersionMode() != null) {
            queryOptions = new HashMap<String, String>(3);
            queryOptions.put("point_in_time", publisherContext.getVersionMode().toString());
        }

        Variables variables = publisherContext.getVariables();
        if (variables != null) {
            contentHandler = new QueryVarResolverHandler(contentHandler, variables);
        }

        if (documentRequest != null) {
            contentHandler = new AnnotateDocumentHandler(documentRequest, "row", Constants.DAISY_NAMESPACE, contentHandler, publisherContext);
        }

        SearchResultDocument searchResultDocument;
        try {
            searchResultDocument = queryManager.performQuery(query, extraConditions, queryOptions, locale, evaluationContext);
        } catch (RepositoryException e) {
            throw new PublisherException("Error performing query in publisher request at " + getLocationInfo().getFormattedLocation(), e);
        }

        searchResultDocument.save(new StripDocumentHandler(contentHandler), new DummyLexicalHandler());
    }
}
