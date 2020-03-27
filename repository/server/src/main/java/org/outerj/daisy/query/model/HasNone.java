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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.repository.query.QueryException;

// Note: this class is used as a delegate inside MultiArgPredicate
public class HasNone extends AbstractMultiArgPredicate {
    private ValueExpr valueExpr;
    private Identifier identifier; // the identifier behing the valueExpr

    /**
     * Used in case of SQL generation to keep list of argument values between generate and bind phases.
     */
    protected Object[] argumentValues;

    public HasNone(MultiArgPredicate.MultiArgPredicateContext multiArgContext) throws QueryException {
        super(multiArgContext);
        valueExpr = multiArgContext.getValueExpr();
        identifier = multiArgContext.getIdentifier();
        if (!(identifier.getDelegate() instanceof Identifier.FieldIdentifier
                || identifier.getDelegate() instanceof Identifier.CollectionsIdentifier
                || identifier.getDelegate() instanceof Identifier.VariantsIdentifier))
            throw new QueryException("HAS NONE can only be used with fields or the collections and variants identifiers.");
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        Object[] values = (Object[])valueExpr.evaluate(valueType, data, evaluationInfo);
        if (values == null)
            return true;
        Object[] argumentValues = getExpandedArgumentList(data, evaluationInfo);
        for (Object argumentValue : argumentValues) {
            for (Object value : values) {
                if (value.equals(argumentValue)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static final ParamString FIELD_JOIN_EXPR = new ParamString(" left outer join thefields {thefieldsAlias} on ({versionFieldTable}.doc_id = {thefieldsAlias}.doc_id and {versionFieldTable}.ns_id = {thefieldsAlias}.ns_id and {versionFieldTable}.branch_id = {thefieldsAlias}.branch_id and {versionFieldTable}.lang_id = {thefieldsAlias}.lang_id and {versionFieldTable}.{versionColumn} = {thefieldsAlias}.version_id and {thefieldsAlias}.fieldtype_id = {fieldTypeId} and {thefieldsAlias}.{valueColumn} IN ({inParameters}) )");
    private static final ParamString COLLECTIONS_JOIN_EXPR = new ParamString(" left outer join document_collections {collectionsAlias} on ({versionFieldTable}.doc_id = {collectionsAlias}.document_id and {versionFieldTable}.ns_id = {collectionsAlias}.ns_id and {versionFieldTable}.branch_id = {collectionsAlias}.branch_id and {versionFieldTable}.lang_id = {collectionsAlias}.lang_id and {collectionsAlias}.collection_id IN ({inParameters}) )");
    private static final ParamString VARIANTS_JOIN_EXPR = new ParamString(" left outer join document_variants {variantsAlias} on ({versionFieldTable}.doc_id = {variantsAlias}.doc_id and {versionFieldTable}.ns_id = {variantsAlias}.ns_id and {variantsAlias}.variant_search IN ({inParameters}) )");

    public void generateSql(final StringBuilder sql, SqlGenerationContext context) throws QueryException {
        argumentValues = getExpandedArgumentList(null, context.getEvaluationInfo());

        // We need to join with the correct versionFieldTable or versions table in case the valueExpr is a dereferenced one,
        // therefore do this using 'doInContext'.
        valueExpr.doInContext(context, new ValueExpr.ContextualizedRunnable() {
            public void run(final SqlGenerationContext context) {
                final String versionFieldTable = context.getVersionFieldTable();

                if (identifier.getDelegate() instanceof Identifier.FieldIdentifier) {
                    final String alias = context.getNewAliasCounter();
                    final String thefieldsAlias = "fields" + alias;

                    SqlGenerationContext.Table fieldTypesTable = new SqlGenerationContext.Table() {
                        public String getName() {
                            return "does_not_matter";
                        }

                        public String getJoinExpression() {
                            StringBuilder inParameters = new StringBuilder();
                            for (int i = 0; i < args.size(); i++) {
                                if (i > 0)
                                    inParameters.append(",");
                                inParameters.append("?");
                            }

                            Map<String, String> params = new HashMap<String, String>();
                            params.put("thefieldsAlias", thefieldsAlias);
                            params.put("fieldTypeId", String.valueOf(((Identifier.FieldIdentifier)identifier.getDelegate()).getfieldTypeId()));
                            params.put("inParameters", inParameters.toString());
                            params.put("valueColumn", SqlGenerationContext.FieldsTable.getValueColumn(identifier.getValueType()));
                            params.put("versionColumn", context.getVersionField());
                            params.put("versionFieldTable", versionFieldTable);
                            return FIELD_JOIN_EXPR.toString(params);
                        }

                        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
                            for (Object argumentValue : argumentValues) {
                                bindPos = Literal.bindLiteral(stmt, bindPos, valueType, argumentValue, evaluationInfo.getQueryContext());
                            }
                            return bindPos;
                        }
                    };
                    context.needsJoinWithTable(fieldTypesTable);

                    sql.append(" ").append(thefieldsAlias).append(".fieldtype_id is null ");
                } else if (identifier.getDelegate() instanceof Identifier.CollectionsIdentifier) {
                    final String alias = context.getNewAliasCounter();
                    final String collectionsAlias = "collections" + alias;

                    SqlGenerationContext.Table collectionsTable = new SqlGenerationContext.Table() {
                        public String getName() {
                            return "does_not_matter";
                        }

                        public String getJoinExpression() {
                            StringBuilder inParameters = new StringBuilder();
                            for (int i = 0; i < args.size(); i++) {
                                if (i > 0)
                                    inParameters.append(",");
                                inParameters.append("?");
                            }

                            Map<String, String> params = new HashMap<String, String>();
                            params.put("collectionsAlias", collectionsAlias);
                            params.put("inParameters", inParameters.toString());
                            params.put("versionFieldTable", versionFieldTable);
                            return COLLECTIONS_JOIN_EXPR.toString(params);
                        }

                        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
                            for (Object argumentValue : argumentValues) {
                                bindPos = Literal.bindLiteral(stmt, bindPos, valueType, argumentValue, evaluationInfo.getQueryContext());
                            }
                            return bindPos;
                        }
                    };
                    context.needsJoinWithTable(collectionsTable);

                    sql.append(" ").append(collectionsAlias).append(".collection_id is null ");
                } else if (identifier.getDelegate() instanceof Identifier.VariantsIdentifier) {
                    final String alias = context.getNewAliasCounter();
                    final String variantsAlias = "variants" + alias;

                    SqlGenerationContext.Table collectionsTable = new SqlGenerationContext.Table() {
                        public String getName() {
                            return "does_not_matter";
                        }

                        public String getJoinExpression() {
                            StringBuilder inParameters = new StringBuilder();
                            for (int i = 0; i < args.size(); i++) {
                                if (i > 0)
                                    inParameters.append(",");
                                inParameters.append("?");
                            }

                            Map<String, String> params = new HashMap<String, String>();
                            params.put("variantsAlias", variantsAlias);
                            params.put("inParameters", inParameters.toString());
                            params.put("versionFieldTable", versionFieldTable);
                            return VARIANTS_JOIN_EXPR.toString(params);
                        }

                        public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
                            for (Object argumentValue : argumentValues) {
                                bindPos = Literal.bindLiteral(stmt, bindPos, valueType, argumentValue, evaluationInfo.getQueryContext());
                            }
                            return bindPos;
                        }
                    };
                    context.needsJoinWithTable(collectionsTable);

                    sql.append(" ").append(variantsAlias).append(".doc_id is null ");
                } else {
                    throw new RuntimeException("Unsupported identifier type in HAS NONE: " + identifier.getExpression());
                }
            }
        });
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return null;
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) {
        return Tristate.MAYBE;
    }

}
