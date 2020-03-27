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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Locale;

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.model.AbstractFunction;
import org.outerj.daisy.query.model.ExprDocData;
import org.outerj.daisy.query.model.ExprDocDataMissingException;
import org.outerj.daisy.query.model.Literal;
import org.outerj.daisy.query.model.QValueType;
import org.outerj.daisy.query.model.SqlGenerationContext;
import org.outerj.daisy.query.model.SqlUtils;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.QueryException;

public class LinkFunction extends AbstractFunction {
    public static final String NAME = "link";

    public String getFunctionName() {
        return NAME;
    }

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);
        if (params.size() < 1 || params.size() > 3)
            throw new QueryException(NAME
                    + " function expects 1 to 3 parameters, not " + params.size());
    }

    public void generateSqlValueExpr(StringBuilder sql,
            SqlGenerationContext context) throws QueryException {
        sql.append(" ? ");
    }

    public String getSqlPreConditions(SqlGenerationContext context)
            throws QueryException {
        // the parameter of this function is evaluated statically, no SQL should
        // be generated.
        return null;
    }

    public int bindPreConditions(PreparedStatement stmt, int bindPos,
            EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        // the parameter of this function is evaluated statically, no SQL should
        // be generated.
        return bindPos;
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos,
            QValueType valueType, EvaluationInfo evaluationInfo)
            throws SQLException, QueryException {
        valueType = valueType.LINK;
        Object value = evaluate(valueType, null, evaluationInfo);

        Literal.bindLiteral(stmt, bindPos, valueType, value, evaluationInfo
                .getQueryContext());
        return ++bindPos;
    }

    public boolean isMultiValue() {
        return getParam(0).isMultiValue();
    }

    public boolean isHierarchical() {
        return getParam(0).isHierarchical();
    }

    public Object evaluate(QValueType valueType, ExprDocData data,
            EvaluationInfo evaluationInfo) throws QueryException {
        return getOutputValue(data, evaluationInfo);
    }

    public QValueType getValueType() {
        return QValueType.LINK;
    }

    public String getTitle(Locale locale) {
        return getExpression();
    }

    public QValueType getOutputValueType() {
        return QValueType.LINK;
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        EvaluationContext context = evaluationInfo.getEvaluationContext();
        
        String documentId = (String)getParam(0).getOutputValue(data, evaluationInfo);
        DocId docId = SqlUtils.parseDocId(documentId, evaluationInfo.getQueryContext());

        long branchId = -1;
        if (params.size() > 1)
            branchId = SqlUtils.parseBranch(getParam(1).getOutputValue(data, evaluationInfo).toString(), evaluationInfo.getQueryContext());
        
        long languageId = -1;
        if (params.size() > 2)
            languageId = SqlUtils.parseLanguage(getParam(2).getOutputValue(data, evaluationInfo).toString(), evaluationInfo.getQueryContext());

        if (branchId == -1 || languageId == -1) {
            if (data == null) throw new ExprDocDataMissingException(getExpression(), getLocation());
        }
        if  (branchId == -1) {
            branchId = data.document.getBranchId();
        }
        if (languageId == -1) {
            languageId = data.document.getLanguageId();
        }
        
        return new VariantKey(docId.toString(), branchId, languageId);
    }

    public ValueExpr clone() {
        LinkFunction clone = new LinkFunction();
        super.fillInClone(clone);
        return clone;
    }
}
