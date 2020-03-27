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

import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.PredicateExpression;
import org.xml.sax.ContentHandler;

public class IfRequest extends AbstractParentPublisherRequest implements Request {
    private final PredicateExpression expression;

    public IfRequest(PredicateExpression expression, LocationInfo locationInfo) {
        super(locationInfo);
        this.expression = expression;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        EvaluationContext evaluationContext = new EvaluationContext();
        publisherContext.pushContextDocuments(evaluationContext);

        boolean result;
        try {
            result = expression.evaluate(publisherContext.getDocument(),
                publisherContext.getVersion(),
                publisherContext.getVersionMode(),
                evaluationContext,
                publisherContext.getRepository());
        } catch (RepositoryException e) {
            throw new PublisherException("Error evaluating expression in publisher request at " + getLocationInfo().getFormattedLocation(), e);
        }

        if (result) {
            super.processInt(contentHandler, publisherContext);
        }
    }
}
