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

public class SubstringFunction  extends AbstractFunction {
    public static final String NAME = "Substring";

    public String getFunctionName() {
        return NAME;
    }

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);

        if (params.size() < 2 || params.size() > 3)
            throw new QueryException("Function " + NAME + " expects two or three parameters.");

        ValueExpr stringParam = getParam(0);
        if (!ValueExprUtil.isPrimitiveValue(QValueType.STRING, stringParam))
            throw new QueryException("Function " + NAME + " needs a string as first parameter.");

        ValueExpr lengthParam = getParam(1);
        if (!ValueExprUtil.isPrimitiveValue(QValueType.LONG, lengthParam))
            throw new QueryException("Function " + NAME + " needs an integer number as second parameter.");

        if (params.size() == 3) {
            lengthParam = getParam(2);
            if (!ValueExprUtil.isPrimitiveValue(QValueType.LONG, lengthParam))
                throw new QueryException("Function " + NAME + " needs an integer number as third parameter.");
        }
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        String sqlFunction = context.getJdbcHelper().getSubstringFunction();
        sql.append(' ').append(sqlFunction).append('(');
        super.generateSqlValueExpr(sql, context);
        sql.append(") ");
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        String value = (String)params.get(0).evaluate(QValueType.STRING, data, evaluationInfo);
        int pos = ((Long)params.get(1).evaluate(QValueType.LONG, data, evaluationInfo)).intValue();
        int length = value.length() - pos + 1;
        if (params.size() == 3)
            length = ((Long)params.get(2).evaluate(QValueType.LONG, data, evaluationInfo)).intValue();
        return evaluate(value, pos, length);
    }

    protected Object evaluate(String value, int pos, int length) throws QueryException {
        if (value == null)
            return null;
        else if (pos < 1)
            throw new QueryException("Position parameter of " + NAME + " function cannot be less than one.");
        else if (length < 0)
            throw new QueryException("Length parameter of " + NAME + " function cannot be negative.");
        else if (pos > value.length() + 1)
            return "";
        else {
            int endIndex = pos + length;
            if (endIndex > value.length() + 1)
                endIndex = value.length() + 1;
            // in SQL, indexes shift one
            return value.substring(pos - 1, endIndex - 1);
        }
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
        SubstringFunction clone = new SubstringFunction();
        super.fillInClone(clone);
        return clone;
    }
}

