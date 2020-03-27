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

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.model.*;
import org.outerj.daisy.repository.query.QueryException;

import java.math.BigDecimal;
import java.util.Locale;

public abstract class AbstractBinaryMathFunction extends AbstractFunction {
    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);
        if (params.size() != 2)
            throw new QueryException("\"" + getFunctionName() + "\" requires exactly two parameters.");

        for (ValueExpr param : params) {
            if (!isNumeric(param))
                throw new QueryException("\"" + getFunctionName() + "\" can only be performed on numeric expressions, which this is not: " + param.getExpression());
        }
    }

    static boolean isNumeric(ValueExpr valueExpr) {
        if (valueExpr.isSymbolicIdentifier())
            return false;

        QValueType valueType = valueExpr.getValueType();
        return valueType == QValueType.UNDEFINED || valueType == QValueType.LONG || valueType == QValueType.DOUBLE || valueType == QValueType.DECIMAL;
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        BigDecimal value1 = toBigDecimal(getParam(0).evaluate(QValueType.DECIMAL, data, evaluationInfo));
        BigDecimal value2 = toBigDecimal(getParam(1).evaluate(QValueType.DECIMAL, data, evaluationInfo));
        return evaluate(value1, value2);
    }

    private Object evaluate(BigDecimal value1, BigDecimal value2) {
        if (value1 == null || value2 == null)
            return null;
        return performCalculation(value1, value2);
    }

    protected abstract Object performCalculation(BigDecimal value1, BigDecimal value2);

    static BigDecimal toBigDecimal(Object value) {
        if (value == null)
            return null;
        else if (value instanceof BigDecimal)
            return (BigDecimal)value;
        else if (value instanceof Long)
            return new BigDecimal(((Long)value).longValue());
        else if (value instanceof Double)
            return new BigDecimal(((Double)value).doubleValue());
        else
            throw new RuntimeException("Unexpected type: " + value.getClass().getName());
    }

    public String buildExpression() {
        StringBuilder expression = new StringBuilder();
        expression.append(" (");
        expression.append(getParam(0).getExpression());
        expression.append(getMathSymbol());
        expression.append(getParam(1).getExpression());
        expression.append(") ");
        return expression.toString();
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" (");
        getParam(0).generateSqlValueExpr(sql, context);
        sql.append(getMathSymbol());
        getParam(1).generateSqlValueExpr(sql, context);
        sql.append(") ");
    }

    protected abstract String getMathSymbol();

    public QValueType getValueType() {
        return QValueType.DECIMAL;
    }

    public String getTitle(Locale locale) {
        return getExpression();
    }

    public QValueType getOutputValueType() {
        return QValueType.DECIMAL;
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return evaluate(null, data, evaluationInfo);
    }
}
