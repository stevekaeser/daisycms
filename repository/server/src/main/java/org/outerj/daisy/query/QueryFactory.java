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
package org.outerj.daisy.query;

import org.outerj.daisy.query.model.PredicateExpr;
import org.outerj.daisy.query.model.Query;
import org.outerj.daisy.query.model.Expression;
import org.outerj.daisy.repository.query.QueryException;

public interface QueryFactory {
    /**
     * Parses a predicate expression, i.e. the where part of query statement.
     *
     * <p>The returned condition will not yet have been "prepared", so you need
     * to call its prepare method yourself.
     */
    public PredicateExpr parsePredicateExpression(String expression) throws QueryException;

    /**
     * Parses an expression, the returned expression is either a PredicateExpr
     * or a ValueExpr.
     */
    public Expression parseExpression(String expression) throws QueryException;

    public Object[] parseOrderBy(String queryString) throws QueryException;
    
    public Query parseQuery(String query) throws QueryException;
}
