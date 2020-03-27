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

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.repository.query.QueryException;

// Note: this class is used as a delegate inside MultiArgPredicate
public class In extends AbstractMultiArgPredicate {
    private final boolean not;
    /**
     * Used in case of SQL generation to keep list of argument values between generate and bind phases.
     */
    protected Object[] argumentValues;

    public In(MultiArgPredicate.MultiArgPredicateContext multiArgContext, boolean not) {
        super(multiArgContext);
        this.not = not;
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        Object value = valueExpr.evaluate(valueType, data, evaluationInfo);
        boolean result =  EvaluationUtil.hasAny(value, getExpandedArgumentList(data, evaluationInfo));
        return not ? !result : result;
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        argumentValues = getExpandedArgumentList(null, context.getEvaluationInfo());
        final int literalCount = argumentValues.length;

        sql.append(" (");
        String preCond = valueExpr.getSqlPreConditions(context);
        if (preCond != null)
            sql.append(preCond).append(" and ");
        valueExpr.generateSqlValueExpr(sql, context);
        if (not)
            sql.append(" NOT");
        sql.append(" IN (");
        for (int i = 0; i < literalCount; i++) {
            if (i < literalCount - 1)
                sql.append("?,");
            else
                sql.append("?");
        }
        sql.append("))");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        bindPos = valueExpr.bindPreConditions(stmt, bindPos, evaluationInfo);
        bindPos = valueExpr.bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
        for (Object argumentValue : argumentValues) {
            bindPos = Literal.bindLiteral(stmt, bindPos, valueType, argumentValue, evaluationInfo.getQueryContext());
        }
        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return valueExpr.isAclAllowed();
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (valueExpr.canTestAppliesTo()) {
            boolean result = evaluate(data, evaluationInfo);
            return result ? Tristate.YES : Tristate.NO;
        } else {
            return Tristate.MAYBE;
        }
    }
}
