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

public class IsNull extends AbstractPredicateExpr {
    private final ValueExpr valueExpr;

    public IsNull(ValueExpr valueExpr) {
        this.valueExpr = valueExpr;
    }

    public void prepare(QueryContext context) throws QueryException {
        valueExpr.prepare(context);

        // Only allow 'is null' on fields or custom fields.
        // For most other things, it's probably of not much use, and still need to think about how this
        // would work with certain identifiers, such as the part.* and collections identifiers.
        Identifier identifier = ValueExprUtil.getIdentifier(valueExpr);
        Identifier.DelegateIdentifier delegateIdentifier = identifier != null ? identifier.getDelegate() : null;
        if (delegateIdentifier == null
                || !(delegateIdentifier instanceof Identifier.FieldIdentifier
                      || delegateIdentifier instanceof Identifier.CustomFieldIdentifier
                      || delegateIdentifier instanceof Identifier.LiveMajorChangeVersionIdIdentifier
                      || delegateIdentifier instanceof Identifier.LastMajorChangeVersionIdIdentifier
                      || delegateIdentifier instanceof Identifier.ReferenceLanguageIdIdentifier
                      || delegateIdentifier instanceof Identifier.ReferenceLanguageNameIdentifier
                      || delegateIdentifier instanceof Identifier.VersionCommentIdentifier
                      || delegateIdentifier instanceof Identifier.SyncedWithIdentifier)) {
            throw new QueryException("Operator IS NULL is only allowed on fields, custom fields and certain identifiers.");
        }
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        Object value = valueExpr.evaluate(null, data, evaluationInfo);
        return evaluate(value);
    }

    public boolean evaluate(Object value) {
        return value == null;
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        String preCond = valueExpr.getSqlPreConditions(context);
        if (preCond != null)
            sql.append(preCond).append(" and ");
        valueExpr.generateSqlValueExpr(sql, context);
        sql.append(" is null ");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        bindPos = valueExpr.bindPreConditions(stmt, bindPos, evaluationInfo);
        bindPos = valueExpr.bindValueExpr(stmt, bindPos, null, evaluationInfo);
        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return valueExpr.isAclAllowed();
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (valueExpr.canTestAppliesTo()) {
            boolean result = evaluate(valueExpr.evaluate(null, data, evaluationInfo));
            return result ? Tristate.YES : Tristate.NO;
        } else {
            return Tristate.MAYBE;
        }
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        valueExpr.collectAccessRestrictions(restrictions);
    }
}
