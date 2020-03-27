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

public class HasPartWithMimeType extends AbstractPredicateExpr {
    private final String mimeType;

    public HasPartWithMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void prepare(QueryContext context) throws QueryException {
        // no preparation to be done
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return false;
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        String alias = context.getNewPartsTable().getName();

        sql.append(alias);
        sql.append('.');
        sql.append(SqlGenerationContext.PartsTable.MIMETYPE);
        sql.append(" LIKE ?");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
        stmt.setString(bindPos, mimeType);
        bindPos++;
        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return new AclConditionViolation("HasPartWithMimeType is not allowed in ACL conditions");
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) {
        throw new IllegalStateException();
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        // do noting
    }
}
