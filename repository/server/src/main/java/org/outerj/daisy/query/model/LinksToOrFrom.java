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

import org.outerj.daisy.linkextraction.LinkType;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.model.SqlGenerationContext.ExtractedLinksTable;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.query.QueryException;

public class LinksToOrFrom extends AbstractPredicateExpr {
    private final String documentId;
    private DocId docId;
    private long branchId = -1;
    private long languageId = -1;
    private String branch;
    private String language;
    private final LinkType[] linkTypes;
    private final boolean fromMode;

    public LinksToOrFrom(String documentId, LinkType[] linkTypes, boolean fromMode) {
        this.documentId = documentId;
        this.linkTypes = linkTypes;
        this.fromMode = fromMode;
    }

    public LinksToOrFrom(String documentId, String branch, String language, LinkType[] linkTypes, boolean fromMode) {
        this.documentId = documentId;
        this.branch = branch;
        this.language = language;
        this.linkTypes = linkTypes;
        this.fromMode = fromMode;
    }

    public void prepare(QueryContext context) throws QueryException {
        if (branch != null)
            branchId = SqlUtils.parseBranch(branch, context);
        if (language != null)
            languageId = SqlUtils.parseLanguage(language, context);
        docId = context.parseDocId(documentId);
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        throw new RuntimeException("LinksTo/LinksToVariant/LinksFrom/LinksFromVariant cannot be dynamically evaluated.");
    }

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        ExtractedLinksTable linksTable;
        if (fromMode) {
            linksTable = context.getInverseExtractedLinksTable();
        } else {
            linksTable = context.getExtractedLinksTable();
        }

        sql.append(" (");
        if (fromMode) {
            sql.append(linksTable.getName());
            sql.append(".");
            sql.append(SqlGenerationContext.ExtractedLinksTable.SOURCE_DOC_ID);
            sql.append(" = ? and ");
            sql.append(linksTable.getName());
            sql.append(".");
            sql.append(SqlGenerationContext.ExtractedLinksTable.SOURCE_NS_ID);
            sql.append(" = ? ");
        } else {
            sql.append(linksTable.getName());
            sql.append(".");
            sql.append(SqlGenerationContext.ExtractedLinksTable.TARGET_DOC_ID);
            sql.append(" = ? and ");
            sql.append(linksTable.getName());
            sql.append(".");
            sql.append(SqlGenerationContext.ExtractedLinksTable.TARGET_NS_ID);
            sql.append(" = ? ");
        }
        if (branchId != -1) {
            sql.append(" and ");
            sql.append(linksTable.getName());
            sql.append(".");
            if (fromMode)
                sql.append(SqlGenerationContext.ExtractedLinksTable.SOURCE_BRANCH_ID);
            else
                sql.append(SqlGenerationContext.ExtractedLinksTable.TARGET_BRANCH_ID);
            sql.append(" = ? ");
        }
        if (languageId != -1) {
            sql.append(" and ");
            sql.append(linksTable.getName());
            sql.append(".");
            if (fromMode)
                sql.append(SqlGenerationContext.ExtractedLinksTable.SOURCE_LANG_ID);
            else
                sql.append(SqlGenerationContext.ExtractedLinksTable.TARGET_LANG_ID);
            sql.append(" = ? ");
        }
        
        // add version field condition
        VersionMode mode = context.getEvaluationInfo().getVersionMode();
        sql.append(" and ").append(linksTable.getName()).append(".").append(SqlGenerationContext.ExtractedLinksTable.SOURCE_VERSION_ID);
        sql.append(" = ").append(linksTable.getSourceVersionField());

        if (linkTypes != null && linkTypes.length > 0) {
            sql.append(" and ");
            sql.append(linksTable.getName());
            sql.append(".");
            sql.append(SqlGenerationContext.ExtractedLinksTable.LINKTYPE);
            sql.append(" IN ( ");
            for (int i = 0; i < linkTypes.length; i++) {
                if (i > 0)
                    sql.append(", ");
                sql.append("?");
            }
            sql.append(")");
        }
        sql.append(")");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
        stmt.setLong(bindPos++, docId.getSeqId());
        stmt.setLong(bindPos++, docId.getNsId());
        if (branchId != -1)
            stmt.setLong(bindPos++, branchId);
        if (languageId != -1)
            stmt.setLong(bindPos++, languageId);

        if (linkTypes != null) {
            for (int i = 0; i < linkTypes.length; i++)
                stmt.setString(bindPos++, linkTypes[i].getCode());
        }

        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return new AclConditionViolation("LinksTo/LinksToVariant/LinksFrom/LinksFromVariant is not allowed in ACL conditions");
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) {
        throw new IllegalStateException();
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        // do noting
    }
}
