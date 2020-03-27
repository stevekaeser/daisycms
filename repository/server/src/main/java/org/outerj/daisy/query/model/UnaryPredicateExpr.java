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
import java.util.List;
import java.util.ArrayList;

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.query.QueryException;

public abstract class UnaryPredicateExpr extends AbstractPredicateExpr {
    protected final ValueExpr valueExpr1;
    protected final ValueExpr valueExpr2;

    public UnaryPredicateExpr(ValueExpr valueExpr1, ValueExpr valueExpr2) {
        this.valueExpr1 = valueExpr1;
        this.valueExpr2 = valueExpr2;
    }

    public void prepare(QueryContext context) throws QueryException {
        valueExpr1.prepare(context);
        valueExpr2.prepare(context);

        if (valueExpr1.isSymbolicIdentifier() && valueExpr2.isSymbolicIdentifier())
            throw new QueryException("Two symbolic identifiers cannot be compared with each other: \"" + valueExpr1.getExpression() + "\" and \"" + valueExpr2.getExpression() + "\".");

        if ( (valueExpr1.isSymbolicIdentifier() && (valueExpr2.getValueType() == null || valueExpr2.getValueType() == QValueType.STRING))
                || (valueExpr2.isSymbolicIdentifier() && (valueExpr1.getValueType() == null || valueExpr1.getValueType() == QValueType.STRING))) {
            // then it is OK
        } else if (!valueExpr1.getValueType().isCompatible(valueExpr2.getValueType())) {
            throw new QueryException("Cannot use a comparison operator on two different types of data: expression \""  + valueExpr1.getExpression() + "\" is of type " + valueExpr1.getValueType() + " and expression \"" + valueExpr2.getExpression() + "\" is of type " + valueExpr2.getValueType());
        }

        if ( (valueExpr1.getValueType() == QValueType.BOOLEAN || valueExpr2.getValueType() == QValueType.BOOLEAN
              || !ValueExprUtil.isPrimitiveValue(valueExpr1) || !ValueExprUtil.isPrimitiveValue(valueExpr2) )
             && !makesSenseForNonOrderedValues()) {
            throw new QueryException("The operator \"" + getOperatorSqlSymbol() + "\" is used between expressions that have no order-relation: \"" + valueExpr1.getExpression() + "\" and \"" + valueExpr2.getExpression() + "\".");
        }
    }

    protected boolean makesSenseForNonOrderedValues() {
        return false;
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" (");

        String valueExpr1PreCond = valueExpr2.isSymbolicIdentifier() ? null : valueExpr1.getSqlPreConditions(context);
        String valueExpr2PreCond = valueExpr1.isSymbolicIdentifier() ? null : valueExpr2.getSqlPreConditions(context);

        if (valueExpr1PreCond != null)
            sql.append(' ').append(valueExpr1PreCond);
        if (valueExpr2PreCond != null) {
            if (valueExpr1PreCond != null)
                sql.append(" and ");
            sql.append(' ').append(valueExpr2PreCond);
        }

        if (valueExpr1PreCond != null || valueExpr2PreCond != null)
            sql.append(" and ");

        if (valueExpr2.isSymbolicIdentifier())
            sql.append(" ? ");
        else
            valueExpr1.generateSqlValueExpr(sql, context);

        sql.append(getOperatorSqlSymbol());

        if (valueExpr1.isSymbolicIdentifier())
            sql.append(" ? ");
        else
            valueExpr2.generateSqlValueExpr(sql, context);

        sql.append(")");
    }

    protected abstract String getOperatorSqlSymbol();

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        bindPos = valueExpr2.isSymbolicIdentifier() ? bindPos : valueExpr1.bindPreConditions(stmt, bindPos, evaluationInfo);
        bindPos = valueExpr1.isSymbolicIdentifier() ? bindPos : valueExpr2.bindPreConditions(stmt, bindPos, evaluationInfo);

        QValueType valueType = determineValueType();

        if (valueExpr1.isSymbolicIdentifier() && valueExpr2.isSymbolicIdentifier()) {
            // this should never occur as it is check in the prepare()
            throw new QueryException("Assertion error: cannot compare two symbolic identifiers: \"" + valueExpr1.getExpression() + "\" and \"" + valueExpr2.getExpression() + "\".");
        } else if (valueExpr1.isSymbolicIdentifier()) {
            bindPos = valueExpr1.bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
            bindPos = Literal.bindLiteral(stmt, bindPos, valueType, valueExpr1.translateSymbolic(valueExpr2, evaluationInfo), evaluationInfo.getQueryContext());
        } else if (valueExpr2.isSymbolicIdentifier()) {
            bindPos = Literal.bindLiteral(stmt, bindPos, valueType, valueExpr2.translateSymbolic(valueExpr1, evaluationInfo), evaluationInfo.getQueryContext());
            bindPos = valueExpr2.bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
        } else {
            bindPos = valueExpr1.bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
            bindPos = valueExpr2.bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
        }

        return bindPos;
    }

    private QValueType determineValueType() throws QueryException {
        QValueType valueType;
        if (valueExpr1.getValueType() == QValueType.UNDEFINED && valueExpr2.getValueType() == QValueType.UNDEFINED) {
            // this is normally only the cases when comparing literals, which is little meaningful
            // We could throw an exception, or let it just be handled as strings.
            valueType = QValueType.STRING;
        } else if (valueExpr1.getValueType() == QValueType.UNDEFINED || valueExpr2.isSymbolicIdentifier()) {
            valueType = valueExpr2.getValueType();
        } else if (valueExpr2.getValueType() == QValueType.UNDEFINED || valueExpr1.isSymbolicIdentifier()) {
            valueType = valueExpr1.getValueType();
        } else {
            // value types should be compatible here, since checked in prepare()
            if (!valueExpr1.getValueType().isCompatible(valueExpr2.getValueType()))
                throw new QueryException("Assertion error: value types not compatible.");
            valueType = valueExpr1.getValueType();
        }
        return valueType;
    }

    public AclConditionViolation isAclAllowed() {
        AclConditionViolation violation = valueExpr1.isAclAllowed();
        if (violation != null)
            return violation;
        else
            return valueExpr2.isAclAllowed();
    }

    private boolean evaluateInt(Object value1, Object value2) {
        // behaviour for multivalue and/or hierarchical fields is: if there's one value that satisfies the condition, the result is true
        List values1 = expandMultiValue(value1);
        List values2 = expandMultiValue(value2);
        for (Object aValues1 : values1) {
            for (Object aValues2 : values2) {
                if (evaluate(aValues1, aValues2))
                    return true;
            }
        }
        return false;
    }

    private List<Object> expandMultiValue(Object value) {
        // optimalisation for primitive values
        if (!(value instanceof Object[] || value instanceof HierarchyPath)) {
            List<Object> values = new ArrayList<Object>(1);
            values.add(value);
            return values;
        }

        List<Object> values = new ArrayList<Object>();
        if (value instanceof Object[]) {
            for (Object aValue : (Object[])value)
                expandHierarchyValue(aValue, values);
        } else {
            expandHierarchyValue(value, values);
        }
        return values;
    }

    private void expandHierarchyValue(Object value, List<Object> values) {
        if (value instanceof HierarchyPath) {
            Object[] elements = ((HierarchyPath)value).getElements();
            for (Object element : elements)
                values.add(element);
        } else {
            values.add(value);
        }
    }

    protected abstract boolean evaluate(Object value1, Object value2);

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        QValueType valueType = determineValueType();
        Object value1, value2;
        if (valueExpr1.isSymbolicIdentifier() && valueExpr2.isSymbolicIdentifier()) {
            // this should never occur as it is check in the prepare()
            throw new QueryException("Assertion error: cannot compare two symbolic identifiers: \"" + valueExpr1.getExpression() + "\" and \"" + valueExpr2.getExpression() + "\".");
        } else if (valueExpr1.isSymbolicIdentifier()) {
            value1 = valueExpr1.evaluate(valueType, data, evaluationInfo);
            value2 = valueExpr1.translateSymbolic(valueExpr2, evaluationInfo);
        } else if (valueExpr2.isSymbolicIdentifier()) {
            value1 = valueExpr2.translateSymbolic(valueExpr1, evaluationInfo);
            value2 = valueExpr2.evaluate(valueType, data, evaluationInfo);
        } else {
            value1 = valueExpr1.evaluate(valueType, data, evaluationInfo);
            value2 = valueExpr2.evaluate(valueType, data, evaluationInfo);
        }

        if (value1 == null || value2 == null)
            return false;

        return evaluateInt(value1, value2);
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (valueExpr1.canTestAppliesTo() && valueExpr2.canTestAppliesTo()) {
            return evaluate(data, evaluationInfo) ? Tristate.YES : Tristate.NO;
        } else {
            return Tristate.MAYBE;
        }
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        valueExpr1.collectAccessRestrictions(restrictions);
        valueExpr2.collectAccessRestrictions(restrictions);
    }
}
