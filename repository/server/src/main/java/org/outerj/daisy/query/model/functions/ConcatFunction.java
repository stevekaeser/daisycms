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

import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.model.*;

import java.util.Locale;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ConcatFunction extends AbstractFunction {
    public static final String NAME = "Concat";

    public String getFunctionName() {
        return NAME;
    }

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);
        if (params.size() < 1)
            throw new QueryException(NAME + " requires at least one argument.");
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        for (ValueExpr param : params) {
            if (!(ValueExprUtil.isPrimitiveValue(QValueType.STRING, param) || ValueExprUtil.isPrimitiveValue(QValueType.LONG, param)))
                throw new QueryException(NAME + " can only have string and long arguments (when used for searching).");
        }

        String[] sqlFunction = context.getJdbcHelper().getStringConcatFunction();
        sql.append(' ').append(sqlFunction[0]).append(' ');
        super.generateSqlValueExpr(sql, context);
        sql.append(sqlFunction[1]).append(' ');
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        return super.bindValueExpr(stmt, bindPos, QValueType.STRING, evaluationInfo);
    }

    public Object evaluate(QValueType reqValueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        StringBuilder result = new StringBuilder();
        for (ValueExpr param : params) {
            QValueType valueType = param.getValueType();
            if (valueType == QValueType.UNDEFINED)
                valueType = QValueType.STRING;
            result.append(StringFunction.asString(param.evaluate(QValueType.STRING, data, evaluationInfo), valueType, data));
        }
        return result.toString();
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
        StringBuilder result = new StringBuilder();
        for (ValueExpr param : params) {
            QValueType valueType = param.getOutputValueType();
            result.append(StringFunction.asString(param.getOutputValue(data, evaluationInfo), valueType, null));
        }
        return result.toString();
    }

    public ValueExpr clone() {
        ConcatFunction clone = new ConcatFunction();
        super.fillInClone(clone);
        return clone;
    }
}
