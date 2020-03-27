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
import java.util.Locale;

/**
 * An expression which evaluates to some value.
 */
public interface ValueExpr extends Expression {
    /**
     * Evaluates this expression for the given document and/or version.
     *
     * <p>The ExprDocData argument is optional. If an expressions needs access
     * to document data, it should check ExprDocData is available and if not,
     * throw a {@link ExprDocDataMissingException}.
     *
     * @param valueType Indicates the type of object to return. This parameter only matters
     *                  when {@link #getValueType()} return QValueType.UNDEFINED. In other
     *                  cases null can be supplied.
     */
    Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException;

    /**
     * Returns the ValueType of this expression, or null if undetermined.
     */
    QValueType getValueType();

    /**
     * Returns true if this ValueExpr represents a symbolic identifier.
     *
     * A symbolic identifier is an identifier that identifies an object by name
     * in the query, but uses an ID to search on the database.
     *
     * <p>If this method returns true, then {@link #translateSymbolic} can
     * be called to convert the symbolic value to the test value.
     */
    boolean isSymbolicIdentifier();

    /**
     * See {@link #isSymbolicIdentifier()}. This method should only be called
     * if {@link #isSymbolicIdentifier()} returns true.
     */
    Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException;

    boolean isMultiValue();

    boolean isHierarchical();

    /**
     * Returns true for ValueExpr's which cannot be searched on (i.e. no
     * SQL can be generated) and which cannot be evaluated. Thus valueExpr's
     * which can be used in the select and order by parts of a query, but
     * not in the where part.
     */
    boolean isOutputOnly();

    /**
     * Returns non-null if this ValueExpr is not fitted for use in ACL
     * document selection expressions.
     */
    AclConditionViolation isAclAllowed();

    /**
     * Generates any SQL conditions needed outside of the main value expression (which is
     * generated using {@link #generateSqlValueExpr(StringBuilder, SqlGenerationContext)}).
     *
     * <p>Should throw an exception if this is an output-only ValueExpr.
     *
     * @return null if not applicable
     */
    String getSqlPreConditions(SqlGenerationContext context) throws QueryException;

    /**
     * Generates the SQL for this expression.
     *
     * <p>Should throw an exception if this is an output-only ValueExpr.
     */
    void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException;

    int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException;

    int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType,
                      EvaluationInfo evaluationInfo) throws SQLException, QueryException;

    String getTitle(Locale locale);

    /**
     * Get a textual representation of this expression, i.e. something that could be parsed
     * again and give the same ValueExpr again.
     *
     * <p>If this requires effort to to build up (rather than returning a fixed string), it
     * is recommended that implementations of this method cache the result for future fast
     * retrievals.
     */
    String getExpression();

    /**
     * Identifies the type of data returned from the
     * {@link #getOutputValue(ExprDocData, EvaluationInfo)} method.
     */
    QValueType getOutputValueType();

    /**
     * Returns the output value of this identifier for the document
     * and version supplied via the ExprDocData object.
     * For symbolic identifiers this will be different from the value returned by
     * {@link #evaluate(QValueType, ExprDocData, EvaluationInfo)}.
     * The version is allowed to be null.
     */
    Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException;

    boolean canTestAppliesTo();

    /**
     * Perform something in the context of this value expression.
     */
    void doInContext(SqlGenerationContext context, ContextualizedRunnable runnable) throws QueryException;

    public static interface ContextualizedRunnable {
        void run(SqlGenerationContext context) throws QueryException;
    }

    ValueExpr clone();
}
