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
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.HierarchyPath;

import java.util.Locale;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ContextDocFunction extends AbstractFunction {
    public static final String NAME = "ContextDoc";

    public String getFunctionName() {
        return NAME;
    }

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);
        if (params.size() < 1 || params.size() > 2)
            throw new QueryException(NAME + " function expects one or two parameters, not " + params.size());

        if (params.size() > 1) {
            ValueExpr positionParam = getParam(1);
            if (!ValueExprUtil.isPrimitiveValue(QValueType.LONG, positionParam))
                throw new QueryException("Function " + getFunctionName() + " needs an integer number as second parameter.");
        }
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" ? ");
    }

    public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
        // the parameter of this function is evaluated statically, no SQL should be generated.
        return null;
    }

    public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        // the parameter of this function is evaluated statically, no SQL should be generated.
        return bindPos;
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        if (evaluationInfo.getEvaluationContext().getContextDocument() == null)
            throw new QueryException(NAME + " function is used but there is no context document available.");

        valueType = getParam(0).getValueType() != null ? getParam(0).getValueType() : valueType;
        Object value = evaluate(valueType, null, evaluationInfo);

        if (value.getClass().isArray())
            throw new QueryException("ContextDoc() function call returns a multivalue value, which can't be used at this location in the query: " + getLocation());

        if (value instanceof HierarchyPath)
            throw new QueryException("ContextDoc() function call returns a hierarchy path value, which can't be used at this location in the query: " + getLocation());

        Literal.bindLiteral(stmt, bindPos, valueType, value, evaluationInfo.getQueryContext());
        return ++bindPos;
    }

    public boolean isMultiValue() {
        return getParam(0).isMultiValue();
    }

    public boolean isHierarchical() {
        return getParam(0).isHierarchical();
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return getOutputValue(data, evaluationInfo);
    }

    public QValueType getValueType() {
        return getParam(0).getOutputValueType();
    }

    public String getTitle(Locale locale) {
        return getExpression();
    }

    public QValueType getOutputValueType() {
        return getParam(0).getOutputValueType();
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        int position = params.size() < 2 ? 1 : ((Long)((ValueExpr)params.get(1)).evaluate(QValueType.LONG, data, evaluationInfo)).intValue();
        EvaluationContext context = evaluationInfo.getEvaluationContext();
        if (context.getContextDocument(position) == null)
            throw new QueryException(NAME + " function is used but there is no context document available (context document stack position: " + position + ")");
        return getParam(0).getOutputValue(new ExprDocData(context.getContextDocument(position), context.getContextVersion(position)), evaluationInfo);
    }

    public ValueExpr clone() {
        ContextDocFunction clone = new ContextDocFunction();
        super.fillInClone(clone);
        return clone;
    }
}
