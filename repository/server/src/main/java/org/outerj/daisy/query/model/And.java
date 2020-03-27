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

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class And extends AbstractPredicateExpr {
    private final List<PredicateExpr> exprs = new ArrayList<PredicateExpr>(5);

    public And() {
    }

    public void add(PredicateExpr expr) {
        exprs.add(expr);
    }

    public void prepare(QueryContext context) throws QueryException {
        for (PredicateExpr expr : exprs) {
            expr.prepare(context);
        }
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        for (PredicateExpr expr : exprs) {
            boolean result = expr.evaluate(data, evaluationInfo);
            // from the moment one of the AND conditions fails, we don't need to look any further
            if (!result)
                return false;
        }
        return true;
    }

    public AclConditionViolation isAclAllowed() {
        for (PredicateExpr expr : exprs) {
            AclConditionViolation result = expr.isAclAllowed();
            if (result != null)
                return result;
        }
        return null;
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        Tristate highest = Tristate.YES;
        for (PredicateExpr expr : exprs) {
            Tristate result = expr.appliesTo(data, evaluationInfo);
            if (result == Tristate.NO)
                return Tristate.NO;
            else if (result == Tristate.MAYBE)
                highest = Tristate.MAYBE;
        }
        return highest;
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" (");
        Iterator exprsIt = exprs.iterator();
        while (exprsIt.hasNext()) {
            PredicateExpr expr = (PredicateExpr)exprsIt.next();
            expr.generateSql(sql, context);
            if (exprsIt.hasNext())
                sql.append(" AND ");
        }
        sql.append(")");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        for (PredicateExpr expr : exprs) {
            bindPos = expr.bindSql(stmt, bindPos, evaluationInfo);
        }
        return bindPos;
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        for (Expression expr : exprs)
            expr.collectAccessRestrictions(restrictions);
    }
}
