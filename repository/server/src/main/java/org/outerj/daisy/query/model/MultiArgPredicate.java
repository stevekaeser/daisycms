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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

// Since the exact multi-arg-predicate implementation to use can depend on information about the identifier
// (especially: is it hierarchical or not), which is not yet available during query parsing,
// this class serves as a dispatcher which determines during the prepare() phase to which class
// to delegate for the actual work.
public class MultiArgPredicate extends AbstractPredicateExpr {
    private String name;
    private ValueExpr valueExpr;
    private Identifier identifier;
    private List<ValueExpr> args = new ArrayList<ValueExpr>();
    private QValueType valueType;
    private boolean isHierarchical;
    private Type type;
    private PredicateExpr delegate;

    public static enum Type { HAS_ALL, HAS_EXACTLY, HAS_ANY, HAS_NONE, IN, NOT_IN }
    public static final Set<Type> REQUIRES_IDENTIFIER;
    static {
        REQUIRES_IDENTIFIER = new HashSet<Type>();
        REQUIRES_IDENTIFIER.add(Type.HAS_ALL);
        REQUIRES_IDENTIFIER.add(Type.HAS_EXACTLY);
        REQUIRES_IDENTIFIER.add(Type.HAS_NONE);
    }
    public static final Set<Type> HIERARCHICAL_REQUIRES_IDENTIFIER;
    static {
        HIERARCHICAL_REQUIRES_IDENTIFIER = new HashSet<Type>();
        HIERARCHICAL_REQUIRES_IDENTIFIER.add(Type.HAS_ALL);
        HIERARCHICAL_REQUIRES_IDENTIFIER.add(Type.HAS_EXACTLY);
        HIERARCHICAL_REQUIRES_IDENTIFIER.add(Type.HAS_ANY);
        HIERARCHICAL_REQUIRES_IDENTIFIER.add(Type.HAS_NONE);
    }

    public MultiArgPredicate(Type type, String name, ValueExpr valueExpr) {
        this.type = type;
        this.name = name;
        this.valueExpr = valueExpr;
    }

    public void addParam(ValueExpr valueExpr) {
        args.add(valueExpr);
    }

    public void prepare(QueryContext context) throws QueryException {
        valueExpr.prepare(context);
        valueType = valueExpr.getValueType() != QValueType.UNDEFINED ? valueExpr.getValueType() : QValueType.STRING;

        if (args.size() == 0)
            throw new QueryException("At least one argument required for '" + name + "' at " + getLocation());

        if ((valueExpr.isHierarchical() ? HIERARCHICAL_REQUIRES_IDENTIFIER : REQUIRES_IDENTIFIER).contains(type)) {
            identifier = ValueExprUtil.getIdentifier(valueExpr);
            if (identifier == null)
                throw new QueryException("Expected an identifier for operator '" + name + "' at " + getLocation());
        }

        QValueType checkValueType = valueExpr.isSymbolicIdentifier() ? QValueType.STRING : valueType;
        QValueType currentArgValueType = null;
        Boolean isHierarchical = null;
        for (ValueExpr arg : args) {
            arg.prepare(context);

            QValueType argValueType = arg.getValueType();
            if (argValueType != QValueType.UNDEFINED && argValueType != checkValueType) {
                throw new QueryException("Invalid type of argument for '" + name + "' at " + arg.getLocation() + ". Expected a " + checkValueType + " but got a " + argValueType + ".");
            }

            if (currentArgValueType == null) {
                currentArgValueType = argValueType;
            } else if (currentArgValueType == QValueType.UNDEFINED) {
                currentArgValueType = argValueType;
            } else if (currentArgValueType != argValueType) {
                throw new QueryException("All arguments should be of the same type for '" + name + "' at " + getLocation());
            }

            if (isHierarchical == null) {
                isHierarchical = arg.isHierarchical();
            } else if (arg.isHierarchical() && isHierarchical == Boolean.FALSE) {
                throw new QueryException("If one argument is a hierarchical path, all arguments should be, for '" + name + "' at " + getLocation());
            }
        }

        this.isHierarchical = isHierarchical;

        if (isHierarchical != valueExpr.isHierarchical()) {
            throw new QueryException("A hierarchical identifier should be combined with hierarchical arguments (and vice versa) for '" + name + "' at " + getLocation());
        }

        if (!valueExpr.isMultiValue() && type != Type.IN && type != Type.NOT_IN) {
            throw new QueryException("The " + name + " condition can only be used with multivalue fields, which '" + valueExpr.getExpression() + "' is not.");
        }

        if (isHierarchical) {
            switch (type) {
                case HAS_ALL:
                    delegate = new HierarchicalHasAllAny(new MultiArgPredicateContextImpl(), false, false);
                    break;
                case HAS_EXACTLY:
                    delegate = new HierarchicalHasAllAny(new MultiArgPredicateContextImpl(), true, false);
                    break;
                case HAS_ANY:
                    delegate = new HierarchicalHasAllAny(new MultiArgPredicateContextImpl(), false, true);
                    break;
                case HAS_NONE:
                    delegate = new HierarchicalHasNone(new MultiArgPredicateContextImpl());
                    break;
                case IN:
                case NOT_IN:
                    throw new QueryException("IN or NOT IN cannot be used with the hierarchical identifier " + valueExpr.getExpression() + " at " + valueExpr.getLocation());
            }
        } else {
            switch(type) {
                case HAS_ALL:
                    delegate = new HasAll(new MultiArgPredicateContextImpl(), false);
                    break;
                case HAS_EXACTLY:
                    delegate = new HasAll(new MultiArgPredicateContextImpl(), true);
                    break;
                case HAS_ANY:
                    delegate = new HasAny(new MultiArgPredicateContextImpl());
                    break;
                case HAS_NONE:
                    delegate = new HasNone(new MultiArgPredicateContextImpl());
                    break;
                case IN:
                    delegate = new In(new MultiArgPredicateContextImpl(), false);
                    break;
                case NOT_IN:
                    delegate = new In(new MultiArgPredicateContextImpl(), true);
                    break;
            }
        }

        delegate.setLocation(getLine(), getColumn());
        delegate.prepare(context);
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return delegate.evaluate(data, evaluationInfo);
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        delegate.generateSql(sql, context);
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        return delegate.bindSql(stmt, bindPos, evaluationInfo);
    }

    public AclConditionViolation isAclAllowed() {
        return delegate.isAclAllowed();
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return delegate.appliesTo(data, evaluationInfo);
    }

    /**
     * Convenience interface to provide all needed parameters as one
     * object to the delegate implementations.
     */
    public static interface MultiArgPredicateContext {
        String getName();
        ValueExpr getValueExpr();
        Identifier getIdentifier();
        List<ValueExpr> getArgs();
        QValueType getValueType();
        boolean isHierarcical();
    }

    private class MultiArgPredicateContextImpl implements MultiArgPredicateContext {
        public String getName() {
            return name;
        }

        public ValueExpr getValueExpr() {
            return valueExpr;
        }

        public Identifier getIdentifier() {
            if (identifier != null)
                return identifier;
            else
                throw new RuntimeException("Unexpected situation: cannot request identifier.");
        }

        public List<ValueExpr> getArgs() {
            return args;
        }

        public QValueType getValueType() {
            return valueType;
        }

        public boolean isHierarcical() {
            return isHierarchical;
        }
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        valueExpr.collectAccessRestrictions(restrictions);
        for (Expression expr : args) {
            expr.collectAccessRestrictions(restrictions);
        }
    }
}
