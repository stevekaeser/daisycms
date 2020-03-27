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
import java.util.Map;

import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.QueryManager;
import org.xml.sax.ContentHandler;

public class QueryForEachRequest extends AbstractRequest implements Request {
    private final String query;
    private final boolean useLastVersion;
    private final DocumentRequest documentRequest;

    public QueryForEachRequest(String query, boolean useLastVersion, DocumentRequest documentRequest, LocationInfo locationInfo) {
        super(locationInfo);
        this.query = query;
        this.useLastVersion = useLastVersion;
        this.documentRequest = documentRequest;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        EvaluationContext evaluationContext = new EvaluationContext();
        publisherContext.pushContextDocuments(evaluationContext);
        QueryManager queryManager = publisherContext.getRepository().getQueryManager();

        Map<String, String> queryOptions = null;
        if (publisherContext.getVersionMode() != null) {
            queryOptions = new HashMap<String, String>(3);
            queryOptions.put("point_in_time", publisherContext.getVersionMode().toString());
        }

        VariantKey[] keys;
        try {
            keys = queryManager.performQueryReturnKeys(query, null, queryOptions, publisherContext.getLocale(), evaluationContext);
        } catch (RepositoryException e) {
            throw new PublisherException("Error performing query in publisher request at " + getLocationInfo().getFormattedLocation(), e);
        }

        for (VariantKey key : keys) {
            PublisherContextImpl childPublisherContext = new PublisherContextImpl(publisherContext);
            childPublisherContext.setDocumentVariant(key.getDocumentId(), key.getBranchId(), key.getLanguageId());
            childPublisherContext.setVersionId(useLastVersion ? -2 : -3);
            documentRequest.process(contentHandler, childPublisherContext);
        }
    }
}
