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

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class IsLinked extends AbstractPredicateExpr {
    private final boolean linked;

    public IsLinked(boolean linked) {
        this.linked = linked;
    }

    public void prepare(QueryContext context) throws QueryException {
        // nothing to do
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        throw new RuntimeException("IsLinked/IsNotLinked cannot be dynamically evaluated.");
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" (");
        sql.append(context.getInverseExtractedLinksTable().getName());
        sql.append(".");
        sql.append(SqlGenerationContext.ExtractedLinksTable.SOURCE_DOC_ID);

        if (linked) {
            sql.append(" is not null ");
        } else {
            sql.append(" is null ");
        }

        sql.append(") ");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return new AclConditionViolation("IsLinked/IsNotLinked is not allowed in ACL conditions");
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) {
        throw new IllegalStateException();
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        // do noting
    }
}
