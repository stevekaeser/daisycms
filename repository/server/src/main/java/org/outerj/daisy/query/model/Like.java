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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;

public class Like extends AbstractPredicateExpr {
    private final ValueExpr valueExpr;
    private final Literal literal;
    private final boolean not;
    private Pattern likePattern;

    public Like(boolean not, ValueExpr valueExpr, Literal literal) {
        this.valueExpr = valueExpr;
        this.literal = literal;
        this.not = not;
    }

    public void prepare(QueryContext context) throws QueryException {
        valueExpr.prepare(context);
        if (valueExpr.isSymbolicIdentifier())
            throw new QueryException("A symbolic identifier cannot be used with LIKE");

        if (valueExpr.getValueType() != QValueType.STRING && valueExpr.getValueType() != QValueType.UNDEFINED) {
            throw new QueryException("LIKE can only be used for strings.");
        }
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        Object value = valueExpr.evaluate(QValueType.STRING, data, evaluationInfo);
        if (value instanceof Object[]) {
            // multivalue fields: true if one of the values yields true
            Object[] values = (Object[])value;
            for (int i = 0; i < values.length; i++) {
                if (evaluate(values[i]))
                    return true;
            }
            return false;
        } else {
            return evaluate(value);
        }
    }

    private boolean evaluate(Object value) throws QueryException {
        compileLikePattern();
        Matcher matcher = likePattern.matcher((String)value);
        boolean matches = matcher.matches();
        return not? !matches :matches;
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (valueExpr.canTestAppliesTo()) {
            boolean result = evaluate(valueExpr.evaluate(QValueType.STRING, data, evaluationInfo));
            return result ? Tristate.YES : Tristate.NO;
        } else {
            return Tristate.MAYBE;
        }
    }

    /**
     * Converts the SQL LIKE pattern to a regexp, to avoid having to write
     * our own algorithm.
     */
    private void compileLikePattern() throws QueryException {
        if (likePattern != null)
            return;

        String likeExpr = (String)literal.evaluate(QValueType.STRING, null);
        StringBuilder pattern = new StringBuilder((likeExpr.length() * 4) + 10);

        boolean inEscape = false;
        for (int i = 0; i < likeExpr.length(); i++) {
            char c = likeExpr.charAt(i);
            switch (c) {
                case '%':
                    if (inEscape) {
                        inEscape = false;
                        pattern.append('%');
                    } else {
                        pattern.append(".*");
                    }
                    break;
                case '_':
                    if (inEscape) {
                        inEscape = false;
                        pattern.append('_');
                    } else {
                        pattern.append('.');
                    }
                    break;
                case '\\':
                    // NOTE: currently no way to \ itself in there
                    inEscape = true;
                    break;
                default:
                    char ch1 = Character.forDigit((c >> 4) & 0xF, 16);
                    char ch2 = Character.forDigit(c & 0xF, 16);
                    pattern.append('\\').append('u').append('0').append('0').append(ch1).append(ch2);
                    break;
            }
        }
        likePattern = Pattern.compile(pattern.toString());
    }

    public AclConditionViolation isAclAllowed() {
        return valueExpr.isAclAllowed();
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" (");
        String preCond = valueExpr.getSqlPreConditions(context);
        if (preCond != null)
            sql.append(preCond).append(" AND ");
        valueExpr.generateSqlValueExpr(sql, context);
        if (not)
            sql.append(" NOT LIKE ");
        else
            sql.append(" LIKE ");
        sql.append(" ?) ");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        bindPos = valueExpr.bindPreConditions(stmt, bindPos, evaluationInfo);
        bindPos = valueExpr.bindValueExpr(stmt, bindPos, QValueType.STRING, evaluationInfo);
        bindPos = literal.bindValueExpr(stmt, bindPos, QValueType.STRING, evaluationInfo);
        return bindPos;
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        valueExpr.collectAccessRestrictions(restrictions);
        literal.collectAccessRestrictions(restrictions);
    }
}
