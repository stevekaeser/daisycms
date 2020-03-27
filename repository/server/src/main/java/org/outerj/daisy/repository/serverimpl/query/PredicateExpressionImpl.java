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
package org.outerj.daisy.repository.serverimpl.query;

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.ExtQueryContext;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.model.ExprDocData;
import org.outerj.daisy.query.model.PredicateExpr;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.PredicateExpression;
import org.outerj.daisy.repository.query.QueryException;

public class PredicateExpressionImpl implements PredicateExpression {
    private PredicateExpr predicateExpr;
    private QueryContext queryContext;

    protected PredicateExpressionImpl(PredicateExpr predicateExpr, QueryContext queryContext) {
        this.queryContext = queryContext;
        this.predicateExpr = predicateExpr;
    }

    public boolean evaluate(Document document, Version version) throws QueryException {
        return evaluate(document, version, version == null ? VersionMode.LAST : VersionMode.LIVE, queryContext, null);
    }

    public boolean evaluate(Document document, Version version, VersionMode versionMode) throws QueryException {
        return evaluate(document, version, versionMode, queryContext, null);
    }

    public boolean evaluate(Document document, Version version, VersionMode versionMode, Repository repository) throws QueryException {
        return evaluate(document, version, versionMode, new ExtQueryContext(repository), null);
    }

    public boolean evaluate(Document document, Version version, VersionMode versionMode, EvaluationContext evaluationContext) throws QueryException {
        return evaluate(document, version, versionMode, queryContext, evaluationContext);
    }

    public boolean evaluate(Document document, Version version, VersionMode versionMode, EvaluationContext evaluationContext, Repository repository) throws QueryException {
        return evaluate(document, version, versionMode, new ExtQueryContext(repository), evaluationContext);
    }

    private boolean evaluate(Document document, Version version, VersionMode versionMode, QueryContext queryContext, EvaluationContext evaluationContext) throws QueryException {
        if (document == null)
            throw new IllegalArgumentException("document argument cannot be null");

        if (evaluationContext == null)
            evaluationContext = new EvaluationContext();

        EvaluationInfo evaluationInfo = new EvaluationInfo(queryContext, evaluationContext);
        evaluationInfo.setVersionMode(versionMode);

        return predicateExpr.evaluate(new ExprDocData(document, version), evaluationInfo);
    }

}
