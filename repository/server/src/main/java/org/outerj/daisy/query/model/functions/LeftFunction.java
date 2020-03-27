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

public class LeftFunction extends AbstractFunction {
    public static final String NAME = "Left";

    public String getFunctionName() {
        return NAME;
    }

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);

        if (params.size() != 2)
            throw new QueryException("Function " + getFunctionName() + " expects exactly two parameters.");
        ValueExpr stringParam = getParam(0);
        if (!ValueExprUtil.isPrimitiveValue(QValueType.STRING, stringParam))
            throw new QueryException("Function " + getFunctionName() + " needs a string as first parameter.");

        ValueExpr lengthParam = getParam(1);
        if (!ValueExprUtil.isPrimitiveValue(QValueType.LONG, lengthParam))
            throw new QueryException("Function " + getFunctionName() + " needs an integer number as second parameter.");
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        String sqlFunction = getFunctionName(context);
        sql.append(' ').append(sqlFunction).append('(');
        super.generateSqlValueExpr(sql, context);
        sql.append(") ");
    }

    protected String getFunctionName(SqlGenerationContext context) {
        return context.getJdbcHelper().getStringLeftFunction();
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        String value = (String)(params.get(0)).evaluate(QValueType.STRING, data, evaluationInfo);
        int length = ((Long)(params.get(1)).evaluate(QValueType.LONG, data, evaluationInfo)).intValue();
        return evaluate(value, length);
    }

    protected Object evaluate(String value, int length) throws QueryException {
        if (value == null)
            return null;
        else if (length < 0)
            throw new QueryException("Length parameter of " + getFunctionName() + " function cannot be negative.");
        else if (length > value.length())
            return value;
        else
            return value.substring(0, length);
    }

    public QValueType getValueType() {
        return QValueType.STRING;
    }

    public String getTitle(Locale locale) {
        return getExpression();
    }

    public QValueType getOutputValueType() {
        return QValueType.STRING;
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return evaluate(null, data, evaluationInfo);
    }

    public ValueExpr clone() {
        LeftFunction clone = new LeftFunction();
        super.fillInClone(clone);
        return clone;
    }
}
