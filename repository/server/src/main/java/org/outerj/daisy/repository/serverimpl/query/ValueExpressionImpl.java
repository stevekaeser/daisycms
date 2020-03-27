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
package org.outerj.daisy.repository.serverimpl.query;

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.ExtQueryContext;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.model.ExprDocData;
import org.outerj.daisy.query.model.PredicateExpr;
import org.outerj.daisy.query.model.QValueType;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.query.ValueExpression;

public class ValueExpressionImpl implements ValueExpression {
    private ValueExpr valueExpr;
    private PredicateExpr predicateExpr;
    private QValueType qValueType;
    private ValueType valueType;
    private QueryContext queryContext;

    protected ValueExpressionImpl(ValueExpr expr, QueryContext queryContext) {
        this.queryContext = queryContext;
        this.valueExpr = expr;
        qValueType = valueExpr.getValueType();
        if (qValueType == QValueType.UNDEFINED) {
            qValueType = QValueType.STRING;
        }
        valueType = ValueTypeHelper.queryToFieldValueType(qValueType);
    }

    protected ValueExpressionImpl(PredicateExpr expr, QueryContext queryContext) {
        this.queryContext = queryContext;
        this.predicateExpr = expr;
        this.valueType = ValueType.BOOLEAN;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public Object evaluate(Document document, Version version) throws QueryException {
        return evaluate(document, version, version == null ? VersionMode.LAST : VersionMode.LIVE, queryContext, null);
    }

    public Object evaluate(Document document, Version version, VersionMode versionMode) throws QueryException {
        return evaluate(document, version, versionMode, queryContext, null);
    }

    public Object evaluate(Document document, Version version, VersionMode versionMode, Repository repository) throws QueryException {
        return evaluate(document, version, versionMode, new ExtQueryContext(repository), null);
    }

    public Object evaluate(Document document, Version version, VersionMode versionMode, EvaluationContext evaluationContext) throws QueryException {
        return evaluate(document, version, versionMode, queryContext, evaluationContext);
    }

    public Object evaluate(Document document, Version version, VersionMode versionMode, EvaluationContext evaluationContext, Repository repository) throws QueryException {
        return evaluate(document, version, versionMode, new ExtQueryContext(repository), evaluationContext);
    }

    private Object evaluate(Document document, Version version, VersionMode versionMode, QueryContext queryContext, EvaluationContext evaluationContext) throws QueryException {
        if (evaluationContext == null)
            evaluationContext = new EvaluationContext();
        EvaluationInfo evaluationInfo = new EvaluationInfo(queryContext, evaluationContext);
        evaluationInfo.setVersionMode(versionMode);

        ExprDocData data = document != null ? new ExprDocData(document, version) : null;
        if (valueExpr != null) {
            Object result = valueExpr.evaluate(qValueType, data, evaluationInfo);
            result = ValueTypeHelper.queryValueToFieldValueType(result, qValueType);
            return result;
        } else if (predicateExpr != null) {
            return predicateExpr.evaluate(data, evaluationInfo);
        } else {
            throw new RuntimeException("Impossible situation.");
        }
    }
}
