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
import org.outerj.daisy.query.QueryContext;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

public class MatchesPath extends AbstractPredicateExpr {
    private final ValueExpr pathSpecExpr;
    private ValueExpr testValueExpr;
    private ValueExpr[] exprPerPathPart;
    private ValueExpr extraExpr;
    /**
     * Holds the parsedPathSpec in either of the following cases:
     *  - the pathSpecExpr is a literal, and as an optimalization we only compile it once
     *  - between the generate and bind SQL phases.
     * Note that for evaluation-mode, expression objects should be thread safe, therefore
     * evaluate() must not assign this variable.
     */
    private ParsedPathSpec parsedPathSpec;

    public MatchesPath(ValueExpr pathSpecExpr, ValueExpr testValueExpr) {
        this.pathSpecExpr = pathSpecExpr;
        this.testValueExpr = testValueExpr;
    }

    public void prepare(QueryContext context) throws QueryException {
        testValueExpr.prepare(context);
        pathSpecExpr.prepare(context);

        // Check that the testValueExpr is something we can work with
        Identifier identifier = ValueExprUtil.getIdentifier(testValueExpr);
        if (identifier == null || !identifier.isHierarchical())
            throw new QueryException("The matchesPath condition can only be used with hierarchical identifiers, not with " + testValueExpr.getExpression() + " at " + testValueExpr.getLocation());
        // The only hierarchical identifier is the field identifier, but check it anyway
        if (!(identifier.getDelegate() instanceof Identifier.FieldIdentifier))
            throw new QueryException("Unexpected situation: a hierarhical identifier which isn't a field.");

        if (!ValueExprUtil.isPrimitiveValue(QValueType.STRING, pathSpecExpr))
            throw new QueryException("The argument of matchesPath should evaluate to a string, at " + pathSpecExpr.getLocation());

        if (pathSpecExpr instanceof Literal) {
            this.parsedPathSpec = parsePathSpec((String)((Literal)pathSpecExpr).evaluate(QValueType.STRING, null), context);
        }
    }

    private static class ParsedPathSpec {
        public PathPart[] pathParts;
        public boolean doubleWildcardAtStart;
        public boolean doubleWildcardAtEnd;
    }

    private ParsedPathSpec parsePathSpec(String pathSpec, QueryContext queryContext) throws QueryException {
        // Parse the pathSpec
        List<PathPart> pathParts = new ArrayList<PathPart>();
        String[] stringPathParts = pathSpec.split("/");
        for (String stringPathPart : stringPathParts) {
            stringPathPart = stringPathPart.trim();
            if (stringPathPart.length() > 0) {
                if (stringPathPart.equals("*")) {
                    pathParts.add(new PathPart(PathPartType.WILDCARD, null));
                } else if (stringPathPart.equals("**")) {
                    pathParts.add(new PathPart(PathPartType.DOUBLE_WILDCARD, null));
                } else {
                    Literal literal = new Literal(stringPathPart, stringPathPart);
                    literal.prepare(queryContext);
                    pathParts.add(new PathPart(PathPartType.VALUE, literal));
                }
            }
        }

        final String ERROR_BASE = "Error compiling matchesPath expression, ";

        // Some validation
        if (pathParts.size() == 0) {
            throw new QueryException(ERROR_BASE + "the expression does not contain anything useful: " + pathSpec + " at " + getLocation());
        }

        if (pathParts.size() == 1 && pathParts.get(0).getType() == PathPartType.DOUBLE_WILDCARD) {
            throw new QueryException(ERROR_BASE + "the wildcard ** is only allowed at the start or the end, not by itself: " + pathSpec + " at " + getLocation());
        }

        // check double wildcards don't occur in the middle
        if (pathParts.size() > 2) {
            for (PathPart pathPart : pathParts.subList(1, pathParts.size() - 1)) {
                if (pathPart.getType() == PathPartType.DOUBLE_WILDCARD)
                    throw new QueryException(ERROR_BASE + "the wildcard ** can only be used at the start or the end: " + pathSpec + " at " + getLocation());
            }
        }

        if (pathParts.size() > 1 && pathParts.get(0).getType() == PathPartType.DOUBLE_WILDCARD && pathParts.get(pathParts.size() - 1).getType() == PathPartType.DOUBLE_WILDCARD) {
            throw new QueryException(ERROR_BASE + "the wildcard ** can be used at most one time, either at the beginning or the end: " + pathSpec + " at " + getLocation());
        }

        ParsedPathSpec result = new ParsedPathSpec();
        result.pathParts = pathParts.toArray(new PathPart[0]);
        result.doubleWildcardAtStart = result.pathParts[0].getType() == PathPartType.DOUBLE_WILDCARD;
        result.doubleWildcardAtEnd = result.pathParts[result.pathParts.length - 1].getType() == PathPartType.DOUBLE_WILDCARD;
        return result;
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        ParsedPathSpec parsedPathSpec;
        if (this.parsedPathSpec != null) {
            parsedPathSpec = this.parsedPathSpec;
        } else {
            String pathSpec = (String)pathSpecExpr.evaluate(QValueType.STRING, data, evaluationInfo);
            parsedPathSpec = parsePathSpec(pathSpec, evaluationInfo.getQueryContext());
        }

        QValueType valueType = testValueExpr.getValueType();
        Object value = testValueExpr.evaluate(testValueExpr.getValueType(), data, evaluationInfo);
        if (value instanceof Object[]) {
            Object[] values = (Object[])value;
            for (Object aValue : values) {
                if (matches(parsedPathSpec, (HierarchyPath)aValue, valueType, evaluationInfo))
                    return true;
            }
            return false;
        } else {
            return matches(parsedPathSpec, (HierarchyPath)value, valueType, evaluationInfo);
        }
    }

    private boolean matches(ParsedPathSpec pathSpec, HierarchyPath path, QValueType valueType, EvaluationInfo evalutionInfo) throws QueryException {
        PathPart[] pathParts = pathSpec.pathParts;
        if (pathSpec.doubleWildcardAtStart || pathSpec.doubleWildcardAtEnd) {
            if (path.length() < pathParts.length)
                return false;
        } else {
            if (path.length() != pathParts.length)
                return false;
        }

        Object[] elements = path.getElements();
        for (int i = 0; i < pathParts.length; i++) {
            PathPart pathPart = pathParts[i];

            switch(pathPart.getType()) {
                case WILDCARD:
                    break;
                case DOUBLE_WILDCARD:
                    break;
                case VALUE:
                    Object value = pathPart.getLiteral().evaluate(valueType, evalutionInfo);
                    int testIndex = i;
                    if (pathSpec.doubleWildcardAtStart)
                        testIndex = elements.length - (pathParts.length - i);
                    if (!value.equals(elements[testIndex]))
                        return false;
                    break;
            }
        }

        return true;
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        if (parsedPathSpec == null) {
            String pathSpec = (String)pathSpecExpr.evaluate(QValueType.STRING, null, context.getEvaluationInfo());
            parsedPathSpec = parsePathSpec(pathSpec, context.getEvaluationInfo().getQueryContext());
        }
        PathPart[] pathParts = parsedPathSpec.pathParts;

        sql.append(" (");
        exprPerPathPart = new ValueExpr[pathParts.length];
        String alias = null;
        Ander ander = new Ander();

        for (int i = 0; i < pathParts.length; i++) {
            PathPart pathPart = pathParts[i];

            switch (pathPart.getType()) {
                case WILDCARD:
                    break;
                case DOUBLE_WILDCARD:
                    break;
                case VALUE:
                    ander.and(sql);

                    exprPerPathPart[i] = testValueExpr.clone();
                    String preCond = exprPerPathPart[i].getSqlPreConditions(context);
                    if (preCond != null)
                        sql.append(preCond).append(" and ");
                    exprPerPathPart[i].generateSqlValueExpr(sql, context);
                    sql.append(" = ?");

                    alias = ((Identifier.FieldIdentifier)ValueExprUtil.getIdentifier(exprPerPathPart[i]).getDelegate()).getTableAlias();

                    sql.append(" and ").append(alias).append(".hier_seq = ");
                    if (parsedPathSpec.doubleWildcardAtStart) {
                        if (i == pathParts.length) // this case can also be handled by the else clause, this is just to avoid "- 0" in the SQL
                            sql.append(alias).append(".hier_count");
                        else
                            sql.append(alias).append(".hier_count - ").append(pathParts.length - (i + 1));
                    } else {
                        sql.append(i + 1);
                    }

                    break;
            }
        }

        if (alias == null) {
            // alias can only be null at this point if the path contains only wildcards
            extraExpr = testValueExpr.clone();
            String preCond = extraExpr.getSqlPreConditions(context);
            if (preCond != null)
                sql.append(preCond);
            alias = ((Identifier.FieldIdentifier)ValueExprUtil.getIdentifier(extraExpr).getDelegate()).getTableAlias();
        }

        if (parsedPathSpec.doubleWildcardAtStart || parsedPathSpec.doubleWildcardAtEnd) {
            ander.and(sql);
            sql.append(alias).append(".hier_count >= ").append(pathParts.length);
        } else {
            ander.and(sql);
            sql.append(alias).append(".hier_count = ").append(pathParts.length);
        }

        sql.append(") ");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        QValueType valueType = testValueExpr.getValueType();
        PathPart[] pathParts = parsedPathSpec.pathParts;
        for (int i = 0; i < pathParts.length; i++) {
            PathPart pathPart = pathParts[i];
            if (pathPart.getType() == PathPartType.VALUE) {
                bindPos = exprPerPathPart[i].bindPreConditions(stmt, bindPos, evaluationInfo);
                bindPos = exprPerPathPart[i].bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
                bindPos = pathPart.getLiteral().bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
            }
        }

        if (extraExpr != null) {
            bindPos = extraExpr.bindPreConditions(stmt, bindPos, evaluationInfo);
        }

        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return null;
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return Tristate.MAYBE;
    }

    private static enum PathPartType { WILDCARD, DOUBLE_WILDCARD, VALUE }

    private static class PathPart {
        private PathPartType type;
        private Literal literal;

        public PathPart(PathPartType type, Literal literal) {
            this.type = type;
            this.literal = literal;
        }

        public PathPartType getType() {
            return type;
        }

        public Literal getLiteral() {
            return literal;
        }
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        testValueExpr.collectAccessRestrictions(restrictions);
    }

    private static class Ander {
        private boolean first = true;

        public void and(StringBuilder sql) {
            if (first)
                first = false;
            else
                sql.append(" and ");
        }
    }
}
