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

import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.query.EvaluationInfo;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

// Note: this class is used as a delegate inside MultiArgPredicate
public class HierarchicalHasNone extends AbstractMultiArgPredicate {
    private ValueExpr valueExpr;
    private Identifier identifier; // the identifier behing the valueExpr
    private Object[] argumentValues;

    public HierarchicalHasNone(MultiArgPredicate.MultiArgPredicateContext multiArgContext) throws QueryException {
        super(multiArgContext);
        this.valueExpr = multiArgContext.getValueExpr();
        this.identifier = multiArgContext.getIdentifier();
        if (!(identifier.getDelegate() instanceof Identifier.FieldIdentifier))
            throw new QueryException("Unexpected situation: a hierarhical identifier which isn't a field.");
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        // This is copy and past from the 'normal' HasNone
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

    public void generateSql(final StringBuilder sql, SqlGenerationContext context) throws QueryException {
        // The SQL generation needs to run in the context of the valueExpr (for the case where it is
        // dereferenced) so that we join with the correct document_variants table.
        valueExpr.doInContext(context, new ValueExpr.ContextualizedRunnable() {
            public void run(SqlGenerationContext context) throws QueryException {
                // has none for hierarchical multi-value fields is implemented using "not exists (subquery)"

                // First we'll generate the where-clause of the subquery
                Identifier.FieldIdentifier fieldIdentifier = (Identifier.FieldIdentifier)identifier.getDelegate();
                List<String> fieldTableAliases = new ArrayList<String>();
                StringBuilder fieldClauses = new StringBuilder();
                argumentValues = getExpandedArgumentList(null, context.getEvaluationInfo());
                int valueCounter = -1;

                for (Object argumentValue : argumentValues) {
                    Object[] pathElements = ((HierarchyPath)argumentValue).getElements();

                    if (valueCounter != -1) // not the first time
                        fieldClauses.append(" or ");
                    fieldClauses.append("(");

                    for (int k = 0; k < pathElements.length; k++) {
                        valueCounter++;

                        String alias = "hier_hasnone_thefields" + context.getNewAliasCounter();
                        fieldTableAliases.add(alias);

                        if (k > 0)
                            fieldClauses.append(" and ");

                        fieldClauses.append(alias).append(".fieldtype_id = ").append(fieldIdentifier.getfieldTypeId());
                        String valueColumn = SqlGenerationContext.FieldsTable.getValueColumn(identifier.getValueType());
                        fieldClauses.append(" and ").append(alias).append(".").append(valueColumn).append(" = ?");
                        fieldClauses.append(" and ").append(alias).append(".hier_seq = ").append(k + 1);

                        // only need to check once for hier_count
                        if (k == 0)
                            fieldClauses.append(" and ").append(alias).append(".hier_count = ").append(pathElements.length);
                    }

                    fieldClauses.append(")");
                }


                // Now build the complete SQL
                // The first alias is the one used as "from", the other ones are joined with that
                sql.append(" not exists ( select 1 from thefields ").append(fieldTableAliases.get(0));

                for (String fieldTableAlias : fieldTableAliases.subList(1, fieldTableAliases.size())) {
                    Map<String, String> params = new HashMap<String, String>();
                    params.put("alias", fieldTableAlias);
                    params.put("versionField", "version_id");
                    // Additional thefields tables are joined with the first thefields table, and not with document_variants,
                    // since it is not allowed to reference that table in the join on-clauses (since MySQL 5.0.12 -- see MySQL docs).
                    params.put("refTable", fieldTableAliases.get(0));
                    String joinExpr = FIELDS_ALIAS_JOIN.toString(params);
                    sql.append(joinExpr);
                }

                sql.append(" where ");

                // join conditions for the table selected by "from"
                Map<String, String> params = new HashMap<String, String>();
                params.put("alias", fieldTableAliases.get(0));
                params.put("versionField", context.getVersionField());
                params.put("refTable", context.getVersionFieldTable());
                sql.append(FIELDS_JOIN_CONDITIONS.toString(params));

                sql.append(" and (");
                sql.append(fieldClauses);
                sql.append(") ");

                sql.append(")");
            }
        });
    }

    private static final String FIELDS_JOIN_CONDITIONS_STRING = " {refTable}.doc_id = {alias}.doc_id and {refTable}.ns_id = {alias}.ns_id and {refTable}.branch_id = {alias}.branch_id and {refTable}.lang_id = {alias}.lang_id and {refTable}.{versionField} = {alias}.version_id ";
    private static final ParamString FIELDS_JOIN_CONDITIONS = new ParamString(FIELDS_JOIN_CONDITIONS_STRING);
    private static final ParamString FIELDS_ALIAS_JOIN = new ParamString(" join thefields {alias} on (" + FIELDS_JOIN_CONDITIONS_STRING + ")");

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        for (Object argumentValue : argumentValues) {
            Object[] pathElements = ((HierarchyPath)argumentValue).getElements();
            for (Object pathElement : pathElements) {
                bindPos = Literal.bindLiteral(stmt, bindPos, valueType, pathElement, evaluationInfo.getQueryContext());
            }
        }
        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return null;
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return Tristate.MAYBE;
    }
}
