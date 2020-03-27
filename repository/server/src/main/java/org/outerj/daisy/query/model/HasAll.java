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

import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.query.EvaluationInfo;

import java.sql.PreparedStatement;
import java.sql.SQLException;

// Note: this class is used as a delegate inside MultiArgPredicate
// It implements both "has all" and "has exactly"
public class HasAll extends AbstractMultiArgPredicate {
    private boolean exactCount;
    private ValueExpr valueCountExpr;
    private ValueExpr[] exprPerArgument;

    /**
     * Used in case of SQL generation to keep list of argument values between generate and bind phases.
     */
    protected Object[] argumentValues;

    public HasAll(MultiArgPredicate.MultiArgPredicateContext multiArgContext, boolean exactCount) {
        super(multiArgContext);
        this.exactCount = exactCount;
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        Object[] values = (Object[])valueExpr.evaluate(valueType, data, evaluationInfo);
        if (values == null)
            return false;

        Object[] argumentValues = getExpandedArgumentList(data, evaluationInfo);

        if (exactCount) {
            return EvaluationUtil.hasExactly(values, argumentValues);
        } else {
            return EvaluationUtil.hasAll(values, argumentValues);
        }
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        argumentValues = getExpandedArgumentList(null, context.getEvaluationInfo());
        exprPerArgument = new ValueExpr[argumentValues.length];
        sql.append(" (");
        for (int i = 0; i < argumentValues.length; i++) {
            if (i > 0)
              sql.append(" and ");

            exprPerArgument[i] = valueExpr.clone();
            String preCond = exprPerArgument[i].getSqlPreConditions(context);
            if (preCond != null)
                sql.append(preCond).append(" and ");
            exprPerArgument[i].generateSqlValueExpr(sql, context);
            sql.append(" = ?");
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
        for (int i = 0; i < argumentValues.length; i++) {
            bindPos = exprPerArgument[i].bindPreConditions(stmt, bindPos, evaluationInfo);
            bindPos = exprPerArgument[i].bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
            bindPos = Literal.bindLiteral(stmt, bindPos, valueType, argumentValues[i], evaluationInfo.getQueryContext());
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