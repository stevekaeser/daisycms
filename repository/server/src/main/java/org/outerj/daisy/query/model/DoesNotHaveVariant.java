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
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.AvailableVariant;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DoesNotHaveVariant extends AbstractPredicateExpr {
    private String branch;
    private String language;
    private long branchId;
    private long languageId;

    public DoesNotHaveVariant(String branch, String language) {
        this.branch = branch;
        this.language = language;
    }

    public void prepare(QueryContext context) throws QueryException {
        if (Character.isDigit(branch.charAt(0))) {
            try {
                branchId = Long.parseLong(branch);
            } catch (NumberFormatException e) {
                throw new QueryException("Invalid branch ID in DoesNotHaveVariant: \"" + branch + "\".");
            }
        } else {
            try {
                branchId = context.getBranchByName(branch).getId();
            } catch (RepositoryException e) {
                throw new QueryException("Problem in DoesNotHaveVariant with branch name \"" + branch + "\".");
            }
        }

        if (Character.isDigit(language.charAt(0))) {
            try {
                languageId = Long.parseLong(language);
            } catch (NumberFormatException e) {
                throw new QueryException("Invalid language ID in DoesNotHaveVariant: \"" + language + "\".");
            }
        } else {
            try {
                languageId = context.getLanguageByName(language).getId();
            } catch (RepositoryException e) {
                throw new QueryException("Problem in DoesNotHaveVariant with language name \"" + language + "\".");
            }
        }
    }

    public boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (data == null)
            throw new ExprDocDataMissingException("DoesNotHaveVariant", getLocation());

        AvailableVariant[] availableVariants;
        try {
            availableVariants = data.document.getAvailableVariants().getArray();
        } catch (RepositoryException e) {
            throw new QueryException("DoesNotHaveVariant: problem retrieving variants.", e);
        }
        for (AvailableVariant availableVariant : availableVariants) {
            if (availableVariant.getBranchId() == branchId && availableVariant.getLanguageId() == languageId)
                return false;
        }
        return true;
    }

    private static final ParamString DOCVARIANTS_JOIN_EXPR = new ParamString(" left join document_variants {alias} on ({document_variants}.doc_id = {alias}.doc_id and {document_variants}.ns_id = {alias}.ns_id and {alias}.branch_id = {branchId} and {alias}.lang_id = {languageId})");

    public void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        final String alias = context.getNewAliasCounter();
        final String docvariantsAlias = "docvariants" + alias;
        final String currentDocVariants = context.getDocumentVariantsTable().getName();

        SqlGenerationContext.Table fieldTypesTable = new SqlGenerationContext.Table() {
            public String getName() {
                return "does_not_matter";
            }

            public String getJoinExpression() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("document_variants", currentDocVariants);
                params.put("alias", docvariantsAlias);
                params.put("branchId", String.valueOf(branchId));
                params.put("languageId", String.valueOf(languageId));
                return DOCVARIANTS_JOIN_EXPR.toString(params);
            }

            public int bindJoin(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) {
                return bindPos;
            }
        };
        context.needsJoinWithTable(fieldTypesTable);

        sql.append(" ").append(docvariantsAlias).append(".doc_id is null ");
    }

    public int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException {
        return bindPos;
    }

    public AclConditionViolation isAclAllowed() {
        return new AclConditionViolation("DoesNotHaveVariant is not allowed in ACL conditions");
    }

    public Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) {
        throw new IllegalStateException();
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        // do noting
    }
}
