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
import org.outerj.daisy.repository.RepositoryException;

import java.util.Map;
import java.util.HashMap;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Implementation of the ReverseLangInSync() and ReverseLangNotInSync() conditions.
 */
public class ReverseLangInOrNotInSync  extends AbstractPredicateExpr {
    private String liveLastSyncedWith;
    private boolean inSync;
    private String language;
    private long languageId;

    public ReverseLangInOrNotInSync(String language, String liveLastSyncedWith, boolean inSync) {
        this.language = language;
        this.liveLastSyncedWith = liveLastSyncedWith;
        this.inSync = inSync;
    }

    private String getName() {
        return inSync ? "ReverseLangInSync" : "ReverseLangNotInSync";
    }

    public void prepare(QueryContext context) throws QueryException {
        try {
            languageId = context.getLanguage(language).getId();
        } catch (RepositoryException e) {
            throw new QueryException("Problem with language argument for " + getName(), e);
        }

        if (liveLastSyncedWith.equalsIgnoreCase("live")) {
            liveLastSyncedWith = "live";
        } else if (liveLastSyncedWith.equalsIgnoreCase("last")) {
            liveLastSyncedWith = "last";
        } else {
            throw new QueryException("Invalid synced-with version argument for " + getName() + ": \"" + liveLastSyncedWith + "\".");
        }
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        throw new RuntimeException(getName() + " cannot be dynamically evaluated.");
    }

    private static final ParamString DOCVARIANTS_JOIN_EXPR = new ParamString(
                      " left outer join document_variants {document_variants2} on "
                    + " ("
                    + "       {document_variants2}.doc_id = {document_variants}.doc_id "
                    + "   and {document_variants2}.ns_id = {document_variants}.ns_id "
                    + "   and {document_variants2}.branch_id = {document_variants}.branch_id "
                    + "   and {document_variants2}.lang_id = ?"
                    + " )");

    private static final ParamString VERSIONS_JOIN_EXPR = new ParamString(
                      " left outer join document_versions {t_version} on "
                    + " ("
                    + "        {t_version}.doc_id = {document_variants2}.doc_id "
                    + "    and {t_version}.ns_id = {document_variants2}.ns_id "
                    + "    and {t_version}.branch_id = {document_variants2}.branch_id "
                    + "    and {t_version}.lang_id = {document_variants2}.lang_id "
                    + "    and {t_version}.id = {document_variants2}.{lastLiveSyncedWith}version_id"
                    + " )");

    private static final ParamString NOT_IN_SYNC_TEST_EXPR = new ParamString(
                      " {document_variants2}.doc_id is not null" /* the variant needs to exist */
                    + " and "
                    + " ( {t_version}.id is null" /* if the (last/live) version does not exist, consider it as not-in-sync */
                    + "   or {t_version}.synced_with_version_id is null " /* synced with not consider as not-in-sync */
                    + "   or ( {t_version}.synced_with_version_id is not null "
                    + "        and 1 in (select 1 from document_versions {r_versions} where {r_versions}.doc_id={document_variants}.doc_id and {r_versions}.ns_id={document_variants}.ns_id and {r_versions}.branch_id = {document_variants}.branch_id and {r_versions}.lang_id = {document_variants}.lang_id"
                    + "        and {r_versions}.change_type='M' and (({t_version}.synced_with_version_id <= {document_versions}.id and {r_versions}.id between {t_version}.synced_with_version_id+1 and {document_versions}.id)" +
                    		"                                     or ({t_version}.synced_with_version_id > {document_versions}.id and {r_versions}.id between {document_versions}.id+1 and {t_version}.synced_with_version_id)))" 
                    + "      )"
                    + " )");

    private static final ParamString IN_SYNC_TEST_EXPR = new ParamString(
                " {document_variants2}.doc_id is not null" /* the variant needs to exist */
                + " and "
                + " ( {t_version}.id is not null" /* can only be in sync if if the (last/live) version exists */
                + "   and {t_version}.synced_with_lang_id = {document_variants}.lang_id" /* can only be in sync if synced-with is pointing to the reference variant */
                + "   and {t_version}.synced_with_version_id is not null "
                + "   and 1 not in (select 1 from document_versions {r_versions} where {r_versions}.doc_id={document_variants}.doc_id and {r_versions}.ns_id={document_variants}.ns_id and {r_versions}.branch_id = {document_variants}.branch_id and {r_versions}.lang_id = {document_variants}.lang_id"
                + "        and {r_versions}.change_type='M' and (({t_version}.synced_with_version_id <= {document_versions}.id and {r_versions}.id between {t_version}.synced_with_version_id+1 and {document_versions}.id)"
                + "                                           or ({t_version}.synced_with_version_id > {document_versions}.id and {r_versions}.id between {document_versions}.id+1 and {t_version}.synced_with_version_id)))" 
                + " )");
    
    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        String docvariantsAlias = "docvariants" + context.getNewAliasCounter();
        String currentDocVariants = context.getDocumentVariantsTable().getName();
        String currentDocVersions = context.getVersionsTable().getName();
        String translatedVersionsTableAlias = "versions" + context.getNewAliasCounter();
        String referenceVersionsTableAlias = "versions" + context.getNewAliasCounter();

        final Map<String, String> params = new HashMap<String, String>();
        params.put("document_variants", currentDocVariants);
        params.put("document_versions", currentDocVersions);
        params.put("document_variants2", docvariantsAlias);
        params.put("lastLiveSyncedWith", liveLastSyncedWith);
        params.put("t_version", translatedVersionsTableAlias); // used for obtaining the syncedWith value of the last/live version
        params.put("r_versions", referenceVersionsTableAlias); // used for checking if a version with major change exists between the reference last/live/pit version and the synced with version

        SqlGenerationContext.Table docVariants2Table = new SqlGenerationContext.Table() {
            public String getName() {
                return "does_not_matter";
            }

            public String getJoinExpression() {
                return DOCVARIANTS_JOIN_EXPR.toString(params);
            }

            public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
                stmt.setLong(bindPos, languageId);
                return bindPos + 1;
            }
        };
        context.needsJoinWithTable(docVariants2Table);

        SqlGenerationContext.Table versionsTable = new SqlGenerationContext.Table() {
            public String getName() {
                return "does_not_matter";
            }

            public String getJoinExpression() {
                return VERSIONS_JOIN_EXPR.toString(params);
            }

            public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
                return bindPos;
            }
        };
        context.needsJoinWithTable(versionsTable);

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

