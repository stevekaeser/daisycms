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
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;

public class Between extends AbstractPredicateExpr {
    private final ValueExpr testValueExpr;
    private final ValueExpr valueExpr1;
    private final ValueExpr valueExpr2;
    private final boolean not;
    private QValueType valueType;

    public Between(boolean not, ValueExpr testValueExpr, ValueExpr valueExpr1, ValueExpr valueExpr2) {
        this.testValueExpr = testValueExpr;
        this.valueExpr1 = valueExpr1;
        this.valueExpr2 = valueExpr2;
        this.not = not;
    }

    public void prepare(QueryContext context) throws QueryException {
        testValueExpr.prepare(context);
        if (!ValueExprUtil.isComparable(testValueExpr))
            throw new QueryException("A non-comparable identifier cannot be used with BETWEEN.");

        valueExpr1.prepare(context);
        valueExpr2.prepare(context);

        if (!isExprOkForComparison(valueExpr1, testValueExpr.getValueType()))
            throw new QueryException("BETWEEN used with an expression whose type does not correspond to the value being tested: " + valueExpr1.getExpression());
        if (!isExprOkForComparison(valueExpr2, testValueExpr.getValueType()))
            throw new QueryException("BETWEEN used with an expression whose type does not correspond to the value being tested: " + valueExpr2.getExpression());

        this.valueType = testValueExpr.getValueType();
    }

    private boolean isExprOkForComparison(ValueExpr expr, QValueType testExprType) {
        return ValueExprUtil.isPrimitiveValue(expr) && (testExprType.isCompatible(expr.getValueType()));
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        Comparable testValue = (Comparable)testValueExpr.evaluate(valueType, data, evaluationInfo);

        if (testValue == null)
            return false;

        Object value1 = valueExpr1.evaluate(valueType, data, evaluationInfo);
        Object value2 = valueExpr2.evaluate(valueType, data, evaluationInfo);

        return evaluate(testValue, value1, value2);
    }

    private boolean evaluate(Comparable testValue, Object value1, Object value2) throws QueryException {
        boolean result = testValue.compareTo(value1) >= 0 && testValue.compareTo(value2) <= 0;
        return not? !result: result;
    }

    public AclConditionViolation isAclAllowed() {
        return testValueExpr.isAclAllowed();
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return Tristate.MAYBE;
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" (");

        String preCond = testValueExpr.getSqlPreConditions(context);
        if (preCond != null)
            sql.append(preCond).append(" and ");

        String valueExpr1PreCond = valueExpr1.getSqlPreConditions(context);
        if (valueExpr1PreCond != null)
            sql.append(valueExpr1PreCond).append(" and ");

        String valueExpr2PreCond = valueExpr2.getSqlPreConditions(context);
        if (valueExpr2PreCond != null)
            sql.append(valueExpr2PreCond).append(" and ");

        testValueExpr.generateSqlValueExpr(sql, context);
        sql.append(" BETWEEN ");
        valueExpr1.generateSqlValueExpr(sql, context);
        sql.append(" AND ");
        valueExpr2.generateSqlValueExpr(sql, context);
        sql.append(" ) ");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        bindPos = testValueExpr.bindPreConditions(stmt, bindPos, evaluationInfo);
        bindPos = valueExpr1.bindPreConditions(stmt, bindPos, evaluationInfo);
        bindPos = valueExpr2.bindPreConditions(stmt, bindPos, evaluationInfo);
        bindPos = testValueExpr.bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
        bindPos = valueExpr1.bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
        bindPos = valueExpr2.bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
        return bindPos;
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        testValueExpr.collectAccessRestrictions(restrictions);
        valueExpr1.collectAccessRestrictions(restrictions);
        valueExpr2.collectAccessRestrictions(restrictions);
    }
}
