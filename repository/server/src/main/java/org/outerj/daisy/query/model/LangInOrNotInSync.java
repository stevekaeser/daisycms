/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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

import java.util.Map;
import java.util.HashMap;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Implementation of the LangInSync() and LangNotInSync() conditions.
 *
 * <p>Reminder: when thinking about the behavior of these conditions, one should think
 * what makes sense from the export use case: we need to be able to select the documents
 * which need to be provided to the translation agency.</p>
 */
public class LangInOrNotInSync extends AbstractPredicateExpr {
    private String liveLast;
    private boolean inSync;

    public LangInOrNotInSync(String liveLast, boolean inSync) {
        this.liveLast = liveLast;
        this.inSync = inSync;
    }

    private String getName() {
        return inSync ? "LangInSync" : "LangNotInSync";
    }

    public void prepare(QueryContext context) throws QueryException {
        if (liveLast.equalsIgnoreCase("live")) {
            liveLast = "live";
        } else if (liveLast.equalsIgnoreCase("last")) {
            liveLast = "last";
        } else {
            throw new QueryException("Invalid argument for " + getName() + ": \"" + liveLast + "\".");
        }
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        throw new RuntimeException(getName() + " cannot be dynamically evaluated.");
    }

    private static final ParamString DOCVARIANTS_JOIN_EXPR = new ParamString(
            " left outer join document_variants {alias} on ("
            + "        {document_variants}.doc_id = {alias}.doc_id "
            + "    and {document_variants}.ns_id = {alias}.ns_id "
            + "    and {document_variants}.branch_id = {alias}.branch_id "
            + "    and {alias}.lang_id = {versions}.synced_with_lang_id"
            + " )"
            + " left outer join document_versions {ver_alias} on ("
            + "        {alias}.doc_id = {ver_alias}.doc_id "
            + "    and {alias}.ns_id = {ver_alias}.ns_id "
            + "    and {alias}.branch_id = {ver_alias}.branch_id "
            + "    and {alias}.lang_id = {ver_alias}.lang_id"
            + "    and {alias}.{last-live}version_id = {ver_alias}.id"
            + " )"
    );

    /**
     * The subselect returns '1' if there are major changes between the synced-with version and the actual current last or live reference version.
     * The interval to check for major changes is slightly different depending on the sign of ({synced-withversion} - {reference last or live version})
     */
    private static final ParamString NOT_IN_SYNC_TEST_EXPR = new ParamString(
            "     {versions}.synced_with_version_id is null" /* if synced-with is not set, than not in sync */
            + " or (({versions}.synced_with_version_id <= {alias}.{last-live}version_id and 1 in (select 1 from document_versions {versions_sub}"
            + "          where {versions_sub}.doc_id = {alias}.doc_id"
            + "            and {versions_sub}.ns_id = {alias}.ns_id"
            + "            and {versions_sub}.branch_id = {alias}.branch_id"
            + "            and {versions_sub}.lang_id = {alias}.lang_id"
            + "            and {versions_sub}.change_type='M'"
            + "            and {versions_sub}.id between {versions}.synced_with_version_id + 1 and {alias}.{last-live}version_id ))"
            + "     or ({versions}.synced_with_version_id > {alias}.{last-live}version_id and 1 in (select 1 from document_versions {versions_sub}"
            + "          where {versions_sub}.doc_id = {alias}.doc_id"
            + "            and {versions_sub}.ns_id = {alias}.ns_id"
            + "            and {versions_sub}.branch_id = {alias}.branch_id"
            + "            and {versions_sub}.lang_id = {alias}.lang_id"
            + "            and {versions_sub}.change_type='M'"
            + "            and {versions_sub}.id between {alias}.{last-live}version_id + 1 and {versions}.synced_with_version_id)))");

    private static final ParamString IN_SYNC_TEST_EXPR = new ParamString(
              "     {versions}.synced_with_version_id is not null"
            + " and (({versions}.synced_with_version_id <= {alias}.{last-live}version_id and 1 not in (select 1 from document_versions {versions_sub}"
            + "          where {versions_sub}.doc_id = {alias}.doc_id"
            + "            and {versions_sub}.ns_id = {alias}.ns_id"
            + "            and {versions_sub}.branch_id = {alias}.branch_id"
            + "            and {versions_sub}.lang_id = {alias}.lang_id"
            + "            and {versions_sub}.change_type='M'"
            + "            and {versions_sub}.id between {versions}.synced_with_version_id + 1 and {alias}.{last-live}version_id ))"
            + "     or ({versions}.synced_with_version_id > {alias}.{last-live}version_id and 1 not in (select 1 from document_versions {versions_sub}"
            + "          where {versions_sub}.doc_id = {alias}.doc_id"
            + "            and {versions_sub}.ns_id = {alias}.ns_id"
            + "            and {versions_sub}.branch_id = {alias}.branch_id"
            + "            and {versions_sub}.lang_id = {alias}.lang_id"
            + "            and {versions_sub}.change_type='M'"
            + "            and {versions_sub}.id between {alias}.{last-live}version_id + 1 and {versions}.synced_with_version_id)))");

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        String docvariantsAlias = "docvariants" + context.getNewAliasCounter();
        String versionAlias = "document_versions" + context.getNewAliasCounter();
        String versionSubAlias = "versions_sub" + context.getNewAliasCounter();
        String currentDocVariants = context.getDocumentVariantsTable().getName();
        String versionsTable = context.getVersionsTable().getName();

        final Map<String, String> params = new HashMap<String, String>();
        params.put("alias", docvariantsAlias);
        params.put("ver_alias", versionAlias);
        params.put("versions_sub", versionSubAlias);
        params.put("document_variants", currentDocVariants);
        params.put("last-live", liveLast);
        params.put("versions", versionsTable);

        SqlGenerationContext.Table table = new SqlGenerationContext.Table() {
            public String getName() {
                return "does_not_matter";
            }

            public String getJoinExpression() {
                return DOCVARIANTS_JOIN_EXPR.toString(params);
            }

            public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) {
                return bindPos;
            }
        };
        context.needsJoinWithTable(table);

        sql.append(" (");
        if (inSync)
            sql.append(IN_SYNC_TEST_EXPR.toString(params));
        else
            sql.append(NOT_IN_SYNC_TEST_EXPR.toString(params));
        sql.append(") ");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return new AclConditionViolation(getName() + " is not allowed in ACL conditions");
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) {
        throw new IllegalStateException();
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        // do noting
    }
}
