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
package org.outerj.daisy.query.model;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.util.Constants;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public final class Literal extends AbstractExpression implements ValueExpr {
    private final String stringValue;
    private final String originalToken;

    public Literal(final String value, final String originalToken) {
        this.stringValue = value;
        this.originalToken = originalToken;
    }

    public Object evaluate(QValueType valueType, EvaluationInfo evaluationInfo) throws QueryException {
        if (valueType == QValueType.STRING) {
            return stringValue;
        } else if (valueType == QValueType.LONG) {
            long longValue;
            try {
                longValue = Long.parseLong(stringValue);
            } catch (NumberFormatException e) {
                throw new QueryException("Invalid long value: \"" + stringValue + "\".", e);
            }
            return new Long(longValue);
        } else if (valueType == QValueType.DOCID) {
            return SqlUtils.parseDocId(stringValue, evaluationInfo.getQueryContext());
        } else if (valueType == QValueType.DOUBLE) {
            double doubleValue;
            try {
                doubleValue = Double.parseDouble(stringValue);
            } catch (NumberFormatException e) {
                throw new QueryException("Invalid double value: \"" + stringValue + "\".", e);
            }
            return new Double(doubleValue);
        } else if (valueType == QValueType.DECIMAL) {
            return new BigDecimal(stringValue);
        } else if (valueType == QValueType.DATE) {
            SimpleDateFormat dateFormat = new SimpleDateFormat(QueryHelper.DATE_PATTERN);
            dateFormat.setLenient(false);
            Date dateValue;
            try {
                dateValue = dateFormat.parse(stringValue);
            } catch (ParseException e) {
                throw new QueryException("Invalid date value: \"" + stringValue + "\".", e);
            }
            return dateValue;
        } else if (valueType == QValueType.DATETIME) {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat(QueryHelper.DATETIME_PATTERN);
            dateTimeFormat.setLenient(false);
            Date dateTimeValue;
            try {
                dateTimeValue = dateTimeFormat.parse(stringValue);
            } catch (ParseException e) {
                throw new QueryException("Invalid datetime value: \"" + stringValue + "\".", e);
            }
            return dateTimeValue;
        } else if (valueType == QValueType.BOOLEAN) {
            if (stringValue.equalsIgnoreCase("true"))
                return Boolean.TRUE;
            else if (stringValue.equalsIgnoreCase("false"))
                return Boolean.FALSE;
            else
                throw new QueryException("Invalid boolean value: \"" + stringValue + "\" (should be 'true' or 'false', with the single quotes).");
        } else if (valueType == QValueType.LINK) {
            // Normalize link value to what is stored in the search column in the DB
            //  - no daisy: prefix
            //  - docID normalized (to include namespace)
            //  - branch and lang specified numerically
            Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(stringValue);
            if (!matcher.matches())
                throw new QueryException("Invalid link: \"" + stringValue + "\". Should be of the form daisy:docid@branch:lang");

            String docIdString = matcher.group(1);
            String branch = matcher.group(2);
            String language = matcher.group(3);

            QueryContext queryContext = evaluationInfo.getQueryContext();
            DocId docId = SqlUtils.parseDocId(docIdString, queryContext);

            long branchId;
            long languageId;

            if (branch == null || branch.length() == 0) {
                branchId = 1;
            } else {
                try {
                    branchId = queryContext.getBranch(branch).getId();
                } catch (RepositoryException e) {
                    throw new QueryException("Problem with branch in link \"" + stringValue + "\".", e);
                }
            }

            if (language == null || language.length() == 0) {
                languageId = 1;
            } else {
                try {
                    languageId = queryContext.getLanguage(language).getId();
                } catch (RepositoryException e) {
                    throw new QueryException("Problem with language in link \"" + stringValue + "\".", e);
                }
            }

            return new VariantKey(docId.toString(), branchId, languageId);
        } else {
            throw new QueryException("Unsupported value type: " + valueType);
        }
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return evaluate(valueType, evaluationInfo);
    }

    public boolean isMultiValue() {
        return false;
    }

    public boolean isHierarchical() {
        return false;
    }

    public void prepare(QueryContext context) {
    }

    public boolean isOutputOnly() {
        return false;
    }

    public AclConditionViolation isAclAllowed() {
        return null;
    }

    public String getSqlPreConditions(SqlGenerationContext context) {
        return null;
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) {
        sql.append(" ? ");
    }

    public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) {
        return bindPos;
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType,
            EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        if (valueType == null || valueType == QValueType.UNDEFINED)
            stmt.setString(bindPos, (String)evaluate(valueType, evaluationInfo));
        else if (valueType == QValueType.LONG)
            stmt.setLong(bindPos, ((Long)evaluate(valueType, evaluationInfo)).longValue());
        else if (valueType == QValueType.STRING)
            stmt.setString(bindPos, (String)evaluate(valueType, evaluationInfo));
        else if (valueType == QValueType.DOCID)
            stmt.setString(bindPos, getDocIdBindValue((DocId)evaluate(valueType, evaluationInfo)));
        else if (valueType == QValueType.DATE)
            stmt.setDate(bindPos, new java.sql.Date(((Date)evaluate(valueType, evaluationInfo)).getTime()));
        else if (valueType == QValueType.DATETIME)
            stmt.setTimestamp(bindPos, new Timestamp(((Date)evaluate(valueType, evaluationInfo)).getTime()));
        else if (valueType == QValueType.DECIMAL)
            stmt.setBigDecimal(bindPos, (BigDecimal)evaluate(valueType, evaluationInfo));
        else if (valueType == QValueType.DOUBLE)
            stmt.setDouble(bindPos, ((Double)evaluate(valueType, evaluationInfo)).doubleValue());
        else if (valueType == QValueType.BOOLEAN)
            stmt.setBoolean(bindPos, ((Boolean)evaluate(valueType, evaluationInfo)).booleanValue());
        else if (valueType == QValueType.LINK)
            stmt.setString(bindPos, getLinkBindValue((VariantKey)evaluate(valueType, evaluationInfo), evaluationInfo.getQueryContext()));
        else
            throw new RuntimeException("Unrecognized valuetype: " + valueType);

        return ++bindPos;
    }

    private static String getLinkBindValue(VariantKey variantKey, QueryContext queryContext) {
        // Note: the link_search column in the database contains the numeric namespace id, therefore we need
        // to parse the document ID first
        DocId docId = queryContext.parseDocId(variantKey.getDocumentId());
        return docId.getSeqId() + "-" + docId.getNsId() + "@" + variantKey.getBranchId() + ":" + variantKey.getLanguageId();
    }

    public static String getDocIdBindValue(DocId docId) {
        return docId.getSeqId() + "-" + docId.getNsId();
    }

    /**
     * Utility method for use by other classes.
     */
    public static int bindLiteral(PreparedStatement stmt, int bindPos, QValueType valueType, Object value,
            QueryContext queryContext) throws SQLException {
        if (value == null) {
            stmt.setObject(bindPos, null);
        } else if (valueType == QValueType.LONG)
            stmt.setLong(bindPos, ((Long)value).longValue());
        else if (valueType == QValueType.STRING)
            stmt.setString(bindPos, (String)value);
        else if (valueType == QValueType.DOCID) {
            DocId docId = (DocId)value;
            stmt.setString(bindPos, getDocIdBindValue(docId));
        } else if (valueType == QValueType.DATE)
            stmt.setDate(bindPos, new java.sql.Date(((Date)value).getTime()));
        else if (valueType == QValueType.DATETIME)
            stmt.setTimestamp(bindPos, new Timestamp(((Date)value).getTime()));
        else if (valueType == QValueType.DECIMAL)
            stmt.setBigDecimal(bindPos, (BigDecimal)value);
        else if (valueType == QValueType.DOUBLE)
            stmt.setDouble(bindPos, ((Double)value).doubleValue());
        else if (valueType == QValueType.BOOLEAN)
            stmt.setBoolean(bindPos, ((Boolean)value).booleanValue());
        else if (valueType == QValueType.LINK) {
            VariantKey variantKey = (VariantKey)value;
            stmt.setString(bindPos, getLinkBindValue(variantKey, queryContext));
        } else
            throw new RuntimeException("Unrecognized valuetype: " + valueType);

        return ++bindPos;
    }

    public QValueType getValueType() {
        return QValueType.UNDEFINED;
    }

    public boolean isSymbolicIdentifier() {
        return false;
    }

    public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
        throw new QueryException("translateSymbolic should not be called if isSymbolic returns false");
    }

    public String getTitle(Locale locale) {
        return "(literal)";
    }

    public String getExpression() {
        return originalToken;
    }

    public QValueType getOutputValueType() {
        // this method is not very useful for literals
        return QValueType.STRING;
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) {
        // this method is not very useful for literals
        return stringValue;
    }

    public boolean canTestAppliesTo() {
        return true;
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        // do noting, we have no children
    }

    public void doInContext(SqlGenerationContext context, ContextualizedRunnable runnable) throws QueryException {
        runnable.run(context);
    }

    public ValueExpr clone() {
        Literal clone = new Literal(stringValue, originalToken);
        return clone;
    }
}
