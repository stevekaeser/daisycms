/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.query.ValueExpression;
import org.xml.sax.ContentHandler;

public class ExpressionForEachRequest extends AbstractRequest {
    private final String expression;
    private final ValueExpression compiledExpression;
    private final DocumentRequest documentRequest;
    /**
     * For hierarchical values, specifies which element to take from a hierarchical path.
     * 0 = all, means running over all elements in the path
     * the rest note the level starting from the first element, negative levels give the elements starting from the end
     */
    private final int hierarchyElement;

    public ExpressionForEachRequest(String expression, int hierarchyElement, DocumentRequest documentRequest, LocationInfo locationInfo) {
        super(locationInfo);
        this.expression = expression;
        this.compiledExpression = null;
        this.hierarchyElement = hierarchyElement;
        this.documentRequest = documentRequest;
    }

    public ExpressionForEachRequest(ValueExpression compiledExpression, int hierarchyElement, DocumentRequest documentRequest, LocationInfo locationInfo) {
        super(locationInfo);
        this.expression = null;
        this.compiledExpression = compiledExpression;
        this.hierarchyElement = hierarchyElement;
        this.documentRequest = documentRequest;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        Document document = publisherContext.getDocument();
        Version version = publisherContext.getVersion();
        if (document != null && version != null) {
            ValueExpression expr;
            if (compiledExpression != null) {
                expr = compiledExpression;
            } else {
                // if no compiledExpression is available, it means the expression should be dynamically
                // parsed on each execution
                try {
                    expr = publisherContext.getRepository().getQueryManager().parseValueExpression(expression);
                } catch (QueryException e) {
                    throw new PublisherException("Error parsing expression: " + expression + " at " + getLocationInfo().getFormattedLocation(), e);
                }
                if (expr.getValueType() != ValueType.LINK) {
                    throw new PublisherException("forEach expression should evaluate to a link: " + expression + " at " + getLocationInfo().getFormattedLocation());
                }
            }
            Object value;
            try {
                value = expr.evaluate(document, version, publisherContext.getVersionMode(), publisherContext.getRepository());
            } catch (QueryException e) {
                throw new PublisherException("Error evaluating expression in publisher request at " + getLocationInfo().getFormattedLocation(), e);
            }
            if (value != null) {
                processLinkValue(value, contentHandler, publisherContext);
            }
        }
    }

    private void processLinkValue(Object value, ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        if (value instanceof Object[]) {
            Object[] values = (Object[])value;
            for (int i = 0; i < values.length; i++) {
                processHierLinkValue(values[i], contentHandler, publisherContext);
            }
        } else {
            processHierLinkValue(value, contentHandler, publisherContext);
        }
    }

    private void processHierLinkValue(Object value, ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        if (value instanceof HierarchyPath) {
            Object[] elements = ((HierarchyPath)value).getElements();
            if (this.hierarchyElement > 0) {
                if (elements.length >= this.hierarchyElement)
                    emitDocumentForVariantKey((VariantKey)elements[this.hierarchyElement - 1], contentHandler, publisherContext);
            } else if (this.hierarchyElement < 0) {
                if (elements.length >= Math.abs(this.hierarchyElement))
                    emitDocumentForVariantKey((VariantKey)elements[elements.length + this.hierarchyElement], contentHandler, publisherContext);
            } else {
                for (Object element : elements)
                    emitDocumentForVariantKey((VariantKey)element, contentHandler, publisherContext);
            }
        } else {
            emitDocumentForVariantKey((VariantKey)value, contentHandler, publisherContext);
        }
    }

    private void emitDocumentForVariantKey(VariantKey variantKey, ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        PublisherContextImpl childPublisherContext = new PublisherContextImpl(publisherContext);
        Document document = publisherContext.getDocument();
        long branchId = variantKey.getBranchId() != -1 ? variantKey.getBranchId() : document.getBranchId();
        long languageId = variantKey.getLanguageId() != -1 ? variantKey.getLanguageId() : document.getLanguageId();
        childPublisherContext.setDocumentVariant(variantKey.getDocumentId(), branchId, languageId);
        documentRequest.process(contentHandler, childPublisherContext);
    }
}
