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
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.query.ValueExpression;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;

public class PubReqExpr {
    private String expression;
    private CompiledExpression delegate;

    /**
     * Expression syntax:
     *   - only considered an expression if it starts exactly with ${ and ends on }, without
     *     extra blank space before or after, otherwise it is considered as a literal string
     *   - ${ can be escaped using \${, to insert \${ use \\${
     */
    public static PubReqExpr compile(String expression, Repository repository) throws PublisherException {
        CompiledExpression delegate;
        if (expression == null) {
            delegate = new Literal(null);
        } else if (expression.startsWith("${")) {
            if (!expression.endsWith("}")) {
                throw new PublisherException("Missing closing brace in expression: " + expression);
            }
            String dsyExpr = expression.substring(2, expression.length() - 1);
            ValueExpression valExpr;
            try {
                valExpr = repository.getQueryManager().parseValueExpression(dsyExpr);
            } catch (QueryException e) {
                throw new PublisherException("Error compiling expression in publisher request: " + expression, e);
            }
            delegate = new DaisyExpression(valExpr, expression);
        } else {
            if (expression.startsWith("\\${") || expression.startsWith("\\\\${")) {
                expression = expression.substring(1);
            }
            delegate = new Literal(expression);
        }
        return new PubReqExpr(delegate, expression);
    }

    private PubReqExpr(CompiledExpression delegate, String expression) {
        this.delegate = delegate;
        this.expression = expression;
    }

    public String evalAsString(PublisherContext publisherContext, Request request) throws RepositoryException {
        String result = evalAsString(publisherContext, null, request);
        if (result == null || result.equals(""))
            throw new PublisherException(getMissingValueMessage(request));
        return result;
    }

    public String evalAsString(PublisherContext publisherContext, String defaultValue, Request request) throws RepositoryException {
        Object result;
        try {
            result = delegate.evaluate(publisherContext);
        } catch (QueryException e) {
            throw new PublisherException("Error evaluating expression in publisher request: " + expression + " at " + request.getLocationInfo().getFormattedLocation(), e);
        }

        if (result == null)
            return defaultValue;
        else
            return result.toString();
    }

    public long evalAsBranchId(PublisherContext publisherContext, Request request) throws RepositoryException {
        String result = evalAsString(publisherContext, null, request);
        if (result == null || result.equals("")) {
            return publisherContext.hasDocument() ? publisherContext.getBranchId() : Branch.MAIN_BRANCH_ID;
        }

        try {
            return publisherContext.getRepository().getVariantManager().getBranch(result, false).getId();
        } catch (RepositoryException e) {
            throw new PublisherException("Error getting branch " + delegate.getCauseDescription(result) + " at " + request.getLocationInfo().getFormattedLocation(), e);
        }
    }

    public long evalAsLanguageId(PublisherContext publisherContext, Request request) throws RepositoryException {
        String result = evalAsString(publisherContext, null, request);
        if (result == null || result.equals("")) {
            return publisherContext.hasDocument() ? publisherContext.getLanguageId() : Language.DEFAULT_LANGUAGE_ID;
        }

        try {
            return publisherContext.getRepository().getVariantManager().getLanguage(result, false).getId();
        } catch (RepositoryException e) {
            throw new PublisherException("Error getting language " + delegate.getCauseDescription(result) + " at " + request.getLocationInfo().getFormattedLocation(), e);
        }
    }

    public long evalAsLong(PublisherContext publisherContext, Request request) throws RepositoryException {
        String result = evalAsString(publisherContext, null, request);
        if (result == null || result.equals("")) {
            throw new PublisherException(getMissingValueMessage(request));
        }
        return parseLong(result, request);
    }

    public long evalAsLong(PublisherContext publisherContext, long defaultValue, Request request) throws RepositoryException {
        String result = evalAsString(publisherContext, null, request);
        return result != null ? parseLong(result, request) : defaultValue;
    }

    private long parseLong(String text, Request request) throws PublisherException {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new PublisherException("Expected an integer value but got " + delegate.getCauseDescription(text) + " at " + request.getLocationInfo().getFormattedLocation());
        }
    }

    public boolean evalAsBoolean(PublisherContext publisherContext, Request request) throws RepositoryException {
        String result = evalAsString(publisherContext, null, request);
        if (result == null || result.equals("")) {
            throw new PublisherException(getMissingValueMessage(request));
        }
        return result.equalsIgnoreCase("true");
    }

    public boolean evalAsBoolean(PublisherContext publisherContext, boolean defaultValue, Request request) throws RepositoryException {
        String result = evalAsString(publisherContext, null, request);
        return result != null ? result.equalsIgnoreCase("true") : defaultValue;
    }

    private String getMissingValueMessage(Request request) {
        return "Expression in publisher request did not return a result: " + expression + " at " + request.getLocationInfo().getFormattedLocation();
    }

    private static interface CompiledExpression {
        Object evaluate(PublisherContext publisherContext) throws RepositoryException;

        String getCauseDescription(String value);
    }

    private static class Literal implements CompiledExpression {
        private String value;

        public Literal(String value) {
            this.value = value;
        }

        public Object evaluate(PublisherContext publisherContext) {
            return value;
        }

        public String getCauseDescription(String value) {
            return "\"" + value + "\"";
        }
    }

    private static class DaisyExpression implements CompiledExpression {
        private ValueExpression expression;
        private String expressionString;

        public DaisyExpression(ValueExpression expression, String expressionString) {
            this.expression = expression;
            this.expressionString = expressionString;
        }

        public Object evaluate(PublisherContext publisherContext) throws RepositoryException {
            Document document = null;
            Version version = null;
            if (publisherContext.hasDocument()) {
                document = publisherContext.getDocument();
                version = publisherContext.getVersion();
            }

            EvaluationContext evaluationContext = new EvaluationContext();
            publisherContext.pushContextDocuments(evaluationContext);

            return expression.evaluate(document, version, publisherContext.getVersionMode(), evaluationContext, publisherContext.getRepository());
        }

        public String getCauseDescription(String value) {
            return "\"" + value + "\" determined from expression " + expressionString;
        }
    }
}
