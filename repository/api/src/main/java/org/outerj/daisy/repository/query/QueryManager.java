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
package org.outerj.daisy.repository.query;

import org.outerx.daisy.x10.SearchResultDocument;
import org.outerx.daisy.x10.FacetedQueryResultDocument;
import org.outerx.daisy.x10.DistinctSearchResultDocument;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;

import java.util.Locale;
import java.util.Map;
import java.util.List;

/**
 * The QueryManager allows to perform queries on the repository.
 *
 * <p>The QueryManager can be retrieved via {@link org.outerj.daisy.repository.Repository#getQueryManager()}.
 *
 */
public interface QueryManager {
    /**
     * Executes a query and returns the results as XML.
     *
     * @param query a query written in the Daisy Query Language
     * @param locale influences the sort behaviour and formatting of non-string fields
     */
    public SearchResultDocument performQuery(String query, Locale locale) throws RepositoryException;

    public SearchResultDocument performQuery(String query, Locale locale, EvaluationContext evaluationContext) throws RepositoryException;

    /**
     * Executes a query and returns the keys of the matching document variants.
     *
     * <p>In this case, the select part of the query is ignored, but
     * should still be specified to have a valid query. Use eg "select id where ...".
     *
     * @param query a query written in the Daisy Query Language
     * @param locale influences the sort behaviour
     */
    public VariantKey[] performQueryReturnKeys(String query, Locale locale) throws RepositoryException;

    public VariantKey[] performQueryReturnKeys(String query, Locale locale, EvaluationContext evaluationContext) throws RepositoryException;

    /**
     * Executes a query and returns the keys of the matching document variants.
     *
     * <p>In this case, the select part of the query is ignored, but
     * should still be specified to have a valid query. Use eg "select id where ...".
     *
     * @param query a query written in the Daisy Query Language
     * @param extraCond extra conditions that will be and-ed to the conditions of the query.
     *                  This allows to force certain conditions, eg only returning
     *                  documents part of a certain collection.
     * @param locale influences the sort behaviour
     */
    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Locale locale) throws RepositoryException;

    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Map<String, String> queryOptions, Locale locale) throws RepositoryException;

    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Locale locale,
                                               EvaluationContext evaluationContext) throws RepositoryException;

    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Map<String, String> queryOptions, Locale locale,
                                               EvaluationContext evaluationContext) throws RepositoryException;
    
    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Map<String, String> queryOptions, Locale locale,
            RemoteEvaluationContext evaluationContext) throws RepositoryException;

    public ResultSet performQueryReturnResultSet(String query, String extraCond, Map<String, String> queryOptions, Locale locale,
                                               EvaluationContext evaluationContext) throws RepositoryException;

    /**
     * Same as {@link #performQueryReturnKeys(java.lang.String, java.lang.String, java.util.Locale)} but
     * returns the results as XML.
     */
    public SearchResultDocument performQuery(String query, String extraCond, Locale locale) throws RepositoryException;

    public SearchResultDocument performQuery(String query, String extraCond, Map<String, String> queryOptions, Locale locale) throws RepositoryException;
    
    public SearchResultDocument performQuery(String query, String extraCond, String extraOrderBy, Map<String, String> queryOptions, Locale locale) throws RepositoryException;

    public SearchResultDocument performQuery(String query, String extraCond, Locale locale,
                                             EvaluationContext evaluationContext) throws RepositoryException;

    /**
     *
     * @param queryOptions a map specifying forced values for query options (the options which
     *                     one can otherwise specify in the query itself, such as 'search_last_version'.
     *                     Keys and values in this map should be String objects.
     */
    public SearchResultDocument performQuery(String query, String extraCond, Map<String, String> queryOptions, Locale locale,
                                             EvaluationContext evaluationContext) throws RepositoryException;
    
    public SearchResultDocument performQuery(String query, String extraCond, String extraOrderBy, Map<String, String> queryOptions, Locale locale,
            RemoteEvaluationContext evaluationContext) throws RepositoryException;

    /**
     * Performs a query and includes for each selected value the set of distinct values, if the
     * isFacet property of the corresponding entry in the given facetConfs array is true.
     *
     * <p>If the length of the facetConf array does not correspond to the number of selected
     * values, this will not give an error.
     */
    public FacetedQueryResultDocument performFacetedQuery(String query, FacetConf[] facetConfs, Map<String, String> queryOptions, Locale locale) throws RepositoryException;
    
    public FacetedQueryResultDocument performFacetedQuery(String query, FacetConf[] facetConfs, Map<String, String> queryOptions, Locale locale, EvaluationContext evaluationContext) throws RepositoryException;

    /**
     * Performs a query, of which the distinct set of the first selected value will be returned.
     */
    public DistinctSearchResultDocument performDistinctQuery(String query, SortOrder sortOrder, Locale locale) throws RepositoryException;

    public DistinctSearchResultDocument performDistinctQuery(String query, String extraCond, SortOrder sortOrder, Locale locale) throws RepositoryException;

    public List<Object> performDistinctQueryReturnValues(String query, SortOrder sortOrder, Locale locale) throws RepositoryException;

    public List<Object> performDistinctQueryReturnValues(String query, String extraCond, SortOrder sortOrder, Locale locale) throws RepositoryException;

    /**
     * Parses a predicate expression (= the where-part of a Daisy query, an expression which
     * evaluates to either true or false) for future evaluation on a document.
     */
    public PredicateExpression parsePredicateExpression(String expression) throws QueryException;

    /**
     * Parses a query-language expression (not necessarily a predicate expression)
     * for future evaluation on a document.
     */
    public ValueExpression parseValueExpression(String expression) throws QueryException;
}
