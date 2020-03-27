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
package org.outerj.daisy.query.model.functions;

import org.outerj.daisy.query.model.AbstractFunction;
import org.outerj.daisy.query.model.SqlGenerationContext;
import org.outerj.daisy.query.model.QValueType;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.repository.query.QueryException;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractNonBindableFunction extends AbstractFunction {

    public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
        throw new QueryException("Function " + getFunctionName() + " is not supported for searching.");
    }

    public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        throw new QueryException("Function " + getFunctionName() + " is not supported for searching.");
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        throw new QueryException("Function " + getFunctionName() + " is not supported for searching.");
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        throw new QueryException("Function " + getFunctionName() + " is not supported for searching.");
    }
}
