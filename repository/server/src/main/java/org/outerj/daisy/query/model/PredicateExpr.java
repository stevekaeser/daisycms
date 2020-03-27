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

import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.query.EvaluationInfo;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Interface for predicate expressions, these are expression which
 * evaluate to either "true" or "false". They are used in the "where"
 * part of the query, or can be evaluated stand-alone.
 */
public interface PredicateExpr extends Expression {
    /**
     * Evaluates the expression for the given document and version. The version
     * parameter is allowed to be null, in which case version-dependent information
     * will be retrieved from the document object.
     */
    boolean evaluate(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException;

    /**
     * Generate SQL for this expression by appending to a StringBuilder.
     *
     * <p>Note that the EvaluationInfo is available through the SqlGenerationContext.
     */
    void generateSql(StringBuilder sql, SqlGenerationContext context) throws QueryException;

    /**
     * @param bindPos the binding position on which to bind the next value
     * @return the next binding position
     */
    int bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException;

    /**
     * Checks if this PredicateExpr only uses stuff allowed in ACL object conditions.
     * Returns null if successfull.
     */
    AclConditionViolation isAclAllowed();

    /**
     * Checks if this conditionl expression could evaluate to true
     * for a document, without really knowing everything about the document.
     * The supplied Document object only needs to return the document type
     * and collections, the other methods do not need to be implemented.
     *
     * <p>The result can be:
     * <ul>
     *   <li>yes, if the expression is guaranteed to evaluate
     *     to true for such a document. For example, the simple
     *     expression 'true' will always be true regardless
     *     of the document.</li>
     *   <li>no,if the expression is guaranteed to evaluate to
     *     false for such a document. For example, the expression
     *     test "documentType = 'abc'" but the supplied document
     *     object is of another type.</li>
     *   <li>maybe, if the epxression could apply but the exact
     *     outcome depends on further information. For example,
     *     when the expression depends on the value of a field.
     * </ul>
     */
    Tristate appliesTo(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException;
}
