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
package org.outerj.daisy.query.model.functions;

import org.outerj.daisy.query.model.*;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.repository.*;

import java.util.Locale;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Converts its argument to a string (output-only = non-search).
 */
public class StringFunction extends AbstractFunction {
    public static final String NAME = "String";

    public String getFunctionName() {
        return NAME;
    }

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);
        
        if (params.size() != 1) {
            throw new QueryException("Function " + getFunctionName() + " takes exactly one argument.");
        }
    }

    protected static String asString(Object value, QValueType valueType, ExprDocData data) {
        if (value == null) {
            return null;
        } else if (value instanceof Object[]) {
            return multiValueAsString((Object[])value, valueType, data);
        } else if (value instanceof HierarchyPath) {
            return pathAsString((HierarchyPath)value, valueType, data);
        } else {
            return primitiveAsString(value, valueType, data);
        }
    }

    private static String multiValueAsString(Object[] values, QValueType valueType, ExprDocData data) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (Object value : values) {
            if (builder.length() > 1)
                builder.append(",");

            if (value instanceof HierarchyPath) {
                builder.append(pathAsString((HierarchyPath)value, valueType, data));
            } else {
                builder.append(primitiveAsString(value, valueType, data));
            }
        }
        builder.append("]");
        return builder.toString();
    }

    private static String pathAsString(HierarchyPath path, QValueType valueType, ExprDocData data) {
        StringBuilder builder = new StringBuilder();
        for (Object element : path.getElements()) {
            builder.append("/");
            builder.append(primitiveAsString(element, valueType, data));
        }
        return builder.toString();
    }

    private static String primitiveAsString(Object value, QValueType valueType, ExprDocData data) {
        String result;
        switch (valueType) {
            case DATE:
                SimpleDateFormat dateFormat = new SimpleDateFormat(QueryHelper.DATE_PATTERN);
                result = dateFormat.format((Date)value);
                break;
            case DATETIME:
                SimpleDateFormat dateTimeFormat = new SimpleDateFormat(QueryHelper.DATETIME_PATTERN);
                result = dateTimeFormat.format((Date)value);
                break;
            case STRING:
                result = (String)value;
                break;
            case LINK:
                VariantKey variantKey = (VariantKey)value;
                if (data != null && (variantKey.getBranchId() == -1 || variantKey.getLanguageId() == -1)) {
                    variantKey = new VariantKey(variantKey.getDocumentId(), data.document.getBranchId(), data.document.getLanguageId());
                }
                result = "daisy:" + variantKey.getDocumentId() + "@" + variantKey.getBranchId() + ":" + variantKey.getLanguageId();
                break;
            default:
                result = value.toString();
        }
        return result;
    }

    public Object evaluate(QValueType reqValueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        QValueType valueType = getParam(0).getValueType();
        if (valueType == QValueType.UNDEFINED)
            valueType = QValueType.STRING;
        return asString(getParam(0).evaluate(valueType, data, evaluationInfo), valueType, data);
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
        QValueType valueType = getParam(0).getValueType();
        if (valueType == QValueType.UNDEFINED)
            valueType = QValueType.STRING;
        return asString(getParam(0).getOutputValue(data, evaluationInfo), valueType, data);
    }

    public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
        // do nothing, evaluated statically
        return null;
    }

    public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        // do nothing, evaluated statically
        return bindPos;
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" ? ");
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        String value = (String)evaluate(null, null, evaluationInfo);
        stmt.setString(bindPos, value);
        return ++bindPos;
    }

    public ValueExpr clone() {
        StringFunction clone = new StringFunction();
        super.fillInClone(clone);
        return null;
    }
}
