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
package org.outerj.daisy.query.model.functions;

import org.outerj.daisy.query.model.*;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;

import java.util.Locale;

/**
 * Base class for upper and lower case functions
 */
public abstract class AbstractCaseFunction extends AbstractFunction {

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);

        if (params.size() != 1)
            throw new QueryException("Function " + getFunctionName() + " expects exactly one parameter.");

        ValueExpr param = getParam(0);
        if (!ValueExprUtil.isPrimitiveValue(QValueType.STRING, param))
            throw new QueryException("Function " + getFunctionName() + " needs a string as parameter.");
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(' ').append(getSqlFunctionName(context)).append("( ");
        super.generateSqlValueExpr(sql, context);
        sql.append(") ");
    }

    protected abstract String getSqlFunctionName(SqlGenerationContext context);

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        String value = (String)(params.get(0)).evaluate(QValueType.STRING, data, evaluationInfo);
        return evaluate(value);
    }

    private Object evaluate(String value) {
        if (value == null)
            return null;
        else
            return applyCase(value);
    }

    protected abstract String applyCase(String value);

    public QValueType getValueType() {
        return QValueType.STRING;
    }

    public String getTitle(Locale locale) {
        return getExpression();
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return evaluate(null, data, evaluationInfo);
    }

    public QValueType getOutputValueType() {
        return QValueType.STRING;
    }
}


