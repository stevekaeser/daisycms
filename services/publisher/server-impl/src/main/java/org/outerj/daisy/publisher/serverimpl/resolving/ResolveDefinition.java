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
package org.outerj.daisy.publisher.serverimpl.resolving;

import java.util.ArrayList;
import java.util.List;

import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContext;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.query.PredicateExpression;
import org.outerj.daisy.repository.query.QueryException;

public class ResolveDefinition {
    private List<ResolveRule> resolveRules = new ArrayList<ResolveRule>();

    protected void addRule(String expressionString, PredicateExpression expression, String publisherRequestName) {
        resolveRules.add(new ResolveRule(expressionString, expression, publisherRequestName));
    }

    public String resolve(Document document, Version version, PublisherContext publisherContext) throws PublisherException {
        for (ResolveRule rule : resolveRules) {
            try {
                if (rule.expression.evaluate(document, version, publisherContext.getVersionMode(), publisherContext.getRepository()))
                    return rule.publisherRequestName;
            } catch (QueryException e) {
                throw new PublisherException("Error evaluating expression in publisher request mapping: " + rule.expressionString, e);
            }
        }
        return null;
    }

    static class ResolveRule {
        String expressionString;
        PredicateExpression expression;
        String publisherRequestName;

        public ResolveRule(String expressionString, PredicateExpression expression, String publisherRequestName) {
            this.expressionString = expressionString;
            this.expression = expression;
            this.publisherRequestName = publisherRequestName;
        }
    }
}
