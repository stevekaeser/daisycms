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

import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.HierarchyPath;

import java.sql.PreparedStatement;
import java.sql.SQLException;

// Note: this class is used as a delegate inside MultiArgPredicate
public class HierarchicalHasAllAny extends AbstractMultiArgPredicate {
    private boolean exactCount;
    private boolean orMode;
    private ValueExpr valueCountExpr = null;
    private ValueExpr[] exprPerArgument;

    /**
     * Used in case of SQL generation to keep list of argument values between generate and bind phases.
     */
    protected Object[] argumentValues;

    public HierarchicalHasAllAny(MultiArgPredicate.MultiArgPredicateContext multiArgContext, boolean exactCount, boolean orMode) throws QueryException {
        super(multiArgContext);
        Identifier identifier = multiArgContext.getIdentifier();
        // The only hierarchical identifier is the field identifier, but check it anyway
        if (!(identifier.getDelegate() instanceof Identifier.FieldIdentifier))
            throw new QueryException("Unexpected situation: a hierarhical identifier which isn't a field.");

        this.exactCount = exactCount;
        this.orMode = orMode;
        if (orMode && exactCount)
            throw new IllegalArgumentException("exactCount and orMode don't make sense together.");
    }

    public void prepare(QueryContext context) throws QueryException {
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        Object[] values = (Object[])valueExpr.evaluate(valueType, data, evaluationInfo);
        if (values == null)
            return false;

        Object[] argumentValues = getExpandedArgumentList(data, evaluationInfo);

        if (orMode) {
            return EvaluationUtil.hasAny(values, argumentValues);
        } else if (exactCount) {
            return EvaluationUtil.hasExactly(values, argumentValues);
        } else {
            return EvaluationUtil.hasAll(values, argumentValues);
        }
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        argumentValues = getExpandedArgumentList(null, context.getEvaluationInfo());
        int totalValueCount = 0;
        for (Object argumentValue : argumentValues)
            totalValueCount += ((HierarchyPath)argumentValue).length();

        exprPerArgument = new ValueExpr[totalValueCount];
        int exprIndex = -1;
        sql.append(" (");
        for (Object argumentValue : argumentValues) {
            HierarchyPath hierarchyPath = (HierarchyPath)argumentValue;
            Object[] pathElements = hierarchyPath.getElements();

            if (exprIndex != -1) // not the first time
                sql.append(orMode ? " or " : " and ");
            sql.append("(");

            for (int k = 0; k < pathElements.length; k++) {
                exprIndex++;

                if (k > 0)
                    sql.append(" and ");

                exprPerArgument[exprIndex] = valueExpr.clone();
                String preCond = exprPerArgument[exprIndex].getSqlPreConditions(context);
                if (preCond != null)
                    sql.append(preCond).append(" and ");
                exprPerArgument[exprIndex].generateSqlValueExpr(sql, context);
                sql.append(" = ?");

                String alias = ((Identifier.FieldIdentifier) ValueExprUtil.getIdentifier(exprPerArgument[exprIndex]).getDelegate()).getTableAlias();
                sql.append(" and ").append(alias).append(".hier_seq = ").append(k + 1);

                // only need to check once for hier_count
                if (exprIndex == 0)
                    sql.append(" and ").append(alias).append(".hier_count = ").append(pathElements.length);
            }

            sql.append(")");
        }
        sql.append(" ) ");

        if (exactCount) {
            retrieveValueCountIdentifier();
            sql.append(" and ");
            String preCond = valueCountExpr.getSqlPreConditions(context);
            if (preCond != null)
                sql.append(preCond).append(" and ");
            valueCountExpr.generateSqlValueExpr(sql, context);
            sql.append(" = ").append(argumentValues.length).append(" ");
        }
    }

    private void retrieveValueCountIdentifier() {
        // MultiArgPredicate verifies that this operator can only be used on valueExpr with
        // an underlying identifier, which also has a valueCountIdentifier
        Identifier identifier = ValueExprUtil.getIdentifier(valueExpr);
        valueCountExpr = identifier.getValueCountIdentifier();
        // If valueExpr is a dereferenced thing, than the valueCount identifier should go through the same deref chain
        if (valueExpr instanceof Dereference) {
            Dereference deref = (Dereference)valueExpr;
            deref.clone();
            deref.changeDerefValueExpr(valueCountExpr);
            valueCountExpr = deref;
        }
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        int exprIndex = -1;
        for (Object argumentValue : argumentValues) {
            Object[] pathElements = ((HierarchyPath)argumentValue).getElements();
            for (Object pathElement : pathElements) {
                exprIndex++;
                bindPos = exprPerArgument[exprIndex].bindPreConditions(stmt, bindPos, evaluationInfo);
                bindPos = exprPerArgument[exprIndex].bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
                bindPos = Literal.bindLiteral(stmt, bindPos, valueType, pathElement, evaluationInfo.getQueryContext());
            }
        }

        if (valueCountExpr != null) {
            bindPos = valueCountExpr.bindPreConditions(stmt, bindPos, evaluationInfo);
            bindPos = valueCountExpr.bindValueExpr(stmt, bindPos, null, evaluationInfo);
        }

        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return null;
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) {
        return Tristate.MAYBE;
    }
}