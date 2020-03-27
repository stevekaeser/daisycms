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

import org.outerj.daisy.query.model.AbstractFunction;
import org.outerj.daisy.query.model.QValueType;
import org.outerj.daisy.query.model.SqlGenerationContext;
import org.outerj.daisy.query.model.ExprDocData;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;

import java.util.Locale;
import java.math.BigDecimal;

public abstract class AbstractUnaryMathFunction extends AbstractFunction {
    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);
        if (params.size() != 1)
            throw new QueryException("\"" + getFunctionName() + "\" requires exactly one parameter.");

        if (!AbstractBinaryMathFunction.isNumeric(getParam(0)))
            throw new QueryException("\"" + getFunctionName() + "\" can only be performed on numeric expressions, which this is not: " + getParam(0).getExpression());
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        BigDecimal value = AbstractBinaryMathFunction.toBigDecimal(getParam(0).evaluate(QValueType.DECIMAL, data, evaluationInfo));
        return evaluate(value);
    }

    private Object evaluate(BigDecimal value) {
        return value == null ? null : performCalculation(value);
    }

    protected abstract Object performCalculation(BigDecimal value);
    protected abstract String[] getSqlFunction(SqlGenerationContext context);

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        String[] sqlFunction = getSqlFunction(context);
        sql.append(' ').append(sqlFunction[0]);
        super.generateSqlValueExpr(sql, context);
        sql.append(sqlFunction[1]).append(' ');
    }

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
