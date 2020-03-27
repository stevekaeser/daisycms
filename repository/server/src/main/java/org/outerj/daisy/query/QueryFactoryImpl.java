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
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.query.model.Expression;
import org.outerj.daisy.repository.query.QueryException;

import java.io.StringReader;

public class QueryFactoryImpl implements QueryFactory {
    public PredicateExpr parsePredicateExpression(String expression) throws QueryException {
        QueryParser qp = new QueryParser(new StringReader(expression));
        PredicateExpr predicateExpr;
        try {
            predicateExpr = qp.standAloneWhereClause();
        } catch (TokenMgrError e) {
            throw new QueryException("Error parsing expression.", e);
        } catch (ParseException e) {
            throw new QueryException("Error parsing expression.", e);
        }
        return predicateExpr;
    }

    public Expression parseExpression(String expression) throws QueryException {
        QueryParser qp = new QueryParser(new StringReader(expression));
        Expression expr;
        try {
            expr = qp.valueExpr();
        } catch (TokenMgrError e) {
            throw new QueryException("Error parsing expression.", e);
        } catch (ParseException e) {
            throw new QueryException("Error parsing expression.", e);
        }

        if (!(expr instanceof ValueExpr || expr instanceof PredicateExpr)) {
            throw new QueryException("Not a valid expression: " + expression);
        }

        return expr;
    }

    public Query parseQuery(String queryString) throws QueryException {
        QueryParser qp = new QueryParser(new StringReader(queryString));
        Query query;
        try {
            query = qp.query();
        } catch (TokenMgrError e) {
            throw new QueryException("Error parsing expression.", e);
        } catch (ParseException e) {
            throw new QueryException("Error parsing expression.", e);
        }
        return query;
    }
    public Object[] parseOrderBy(String orderBy) throws QueryException {
        QueryParser qp = new QueryParser(new StringReader(orderBy));
        Object[] orderByList;
        try {
        	orderByList = qp.orderByClause();
        } catch (TokenMgrError e) {
            throw new QueryException("Error parsing expression.", e);
        } catch (ParseException e) {
            throw new QueryException("Error parsing expression.", e);
        }
        return orderByList;
    }



}
