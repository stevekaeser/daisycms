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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.query.SortOrder;

/**
 * Describes a query. Obtained from the {@link org.outerj.daisy.query.QueryFactory QueryFactory}.
 *
 * <p>A query contains the following parts:
 * <ul>
 *   <li>a select part (a list of identifiers). Always present.
 *   <li>a where clause: a query on 'structured' data (metadata etc.). This part is optional
 *       if a fulltext query (see below) is specified, otherwise it is always present.
 *   <li>a fulltext query. Optional. If both a fulltext query and a 'where clause' are specified,
 *       the results of both should be AND'ed together. In other words, the where clause' purpose
 *       is to further limit the results from the fulltext query.
 *   <li>order by clause. Optional.
 * </ul>
 *
 * Once a query has been obtained from the QueryFactory, it should be prepared by calling
 * {@link #prepare(org.outerj.daisy.query.QueryContext)}.
 *
 * <p>This class also has methods to generate SQL for performing the 'where clause'.
 */
public class Query {
    private ValueExprList selectClause;
    private PredicateExpr whereClause;
    private ValueExprList orderByClause;
    private List<SortOrder> sortOrders;
    private int limit;
    private FullTextQuery fullTextQuery;
    private boolean includeRetired = false;
    private VersionMode versionMode = VersionMode.LIVE;
    private boolean isVersionModeSet = false;
    private boolean annotateLinkFields = true;
    private String styleHint;
    private String analyzerName;
    private SqlGenerationContext sqlGenerationContext;
    private int chunkOffset = 1;
    private int chunkLength = 0;

    public Query(ValueExprList selectClause, PredicateExpr whereClause, FullTextQuery fullTextQuery, ValueExprList orderByClause, List<SortOrder> sortOrders, int limit) {
        this.selectClause = selectClause;
        this.whereClause = whereClause;
        this.orderByClause = orderByClause;
        this.sortOrders = sortOrders;
        this.limit = limit;
        this.fullTextQuery = fullTextQuery;

        if (selectClause == null)
            throw new NullPointerException("select clause is a required part of Query.");
        if (whereClause == null && fullTextQuery == null)
            throw new NullPointerException("where clause is a required part of Query.");
    }

    public void prepare(QueryContext context) throws QueryException {
        selectClause.prepare(context);
        if (whereClause != null)
            whereClause.prepare(context);
        if (orderByClause != null) {
            orderByClause.prepare(context);
            ValueExpr[] valueExprs = orderByClause.getArray();
            for (int i = 0; i < valueExprs.length; i++) {
                if (valueExprs[i].isMultiValue() || valueExprs[i].getValueType() == QValueType.UNDEFINED)
                    throw new QueryException("Sorting not possible on \"" + valueExprs[i].getExpression() + "\".");
            }
        }
        if (fullTextQuery != null)
            fullTextQuery.prepare(context);
    }

    /**
     * FIXME: always true? (whereClause == null causes NPE in constructor
     */
    public boolean hasSql() {
        return whereClause != null;
    }

    /**
     * This method should only be called if {@link #hasSql()} return true.
     */
    public String getSql(JdbcHelper jdbcHelper, EvaluationInfo evalutionInfo) throws QueryException {
        if (whereClause == null)
            throw new QueryException("This query has no SQL part.");

        this.sqlGenerationContext = new SqlGenerationContext(jdbcHelper, versionMode, includeRetired, evalutionInfo);

        StringBuilder whereSql = new StringBuilder(1000);
        whereClause.generateSql(whereSql, sqlGenerationContext);

        StringBuilder sql = new StringBuilder(whereSql.length() + 5000);
        sqlGenerationContext.appendDocIdSelectClause(sql);
        sqlGenerationContext.appendFromClause(sql);
        sql.append(" where ");
        sql.append("(");
        sql.append(whereSql);
        sql.append(")");
        sqlGenerationContext.appendExtraWhereConditions(sql);

        return sql.toString();
    }

    public FullTextQuery getFullTextQuery() {
        return fullTextQuery;
    }

    public void bindSql(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        if (whereClause == null)
            throw new IllegalStateException("This query has no SQL part.");

        if (sqlGenerationContext == null)
            throw new IllegalStateException("SQL is not yet generated, can not yet be bound.");

        bindPos = sqlGenerationContext.bindJoins(stmt, bindPos, evaluationInfo);
        bindPos = whereClause.bindSql(stmt, bindPos, evaluationInfo);
        bindPos = sqlGenerationContext.bindExtraWhereConditions(stmt, bindPos);
    }

    public ValueExpr[] getSelectValueExprs() {
        return selectClause.getArray();
    }

    public ValueExpr[] getOrderByValueExprs() {
        if (orderByClause == null)
            return null;
        else
            return orderByClause.getArray();
    }

    public SortOrder[] getOrderBySortOrders() {
        if (sortOrders == null)
            return null;
        else
            return sortOrders.toArray(new SortOrder[0]);
    }

    public int getLimit() {
        return limit;
    }

    public void setIncludeRetired(boolean includeRetired) {
        this.includeRetired = includeRetired;
    }

    public void setVersionMode(VersionMode versionMode) {
        if (!isVersionModeSet) {
            isVersionModeSet = true;
            this.versionMode = versionMode;
        }
    }

    public VersionMode getVersionMode() {
        return this.versionMode;
    }

    public String getStyleHint() {
        return styleHint;
    }

    public void setStyleHint(String styleHint) {
        this.styleHint = styleHint;
    }

    public boolean getAnnotateLinkFields() {
        return annotateLinkFields;
    }

    public void setAnnotateLinkFields(boolean annotateLinkFields) {
        this.annotateLinkFields = annotateLinkFields;
    }

    public void setOption(String name, String value) {
        if (name.equalsIgnoreCase("include_retired"))
            setIncludeRetired(value.equalsIgnoreCase("true"));
        else if (name.equalsIgnoreCase("search_last_version")) {
            if (value.equals("true")) {
                setVersionMode(VersionMode.LAST);
            } else if (value.equals("false")){
                setVersionMode(VersionMode.LIVE);
            }
        }
        else if (name.equalsIgnoreCase("point_in_time"))
            setVersionMode(VersionMode.get(value));
        else if (name.equalsIgnoreCase("style_hint"))
            setStyleHint(value);
        else if (name.equalsIgnoreCase("annotate_link_fields"))
            setAnnotateLinkFields(value.equalsIgnoreCase("true"));
        else if (name.equalsIgnoreCase("chunk_offset")) { 
            try {
                setChunkOffset(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new NumberFormatException("The following value could not be used as a chunk offset : " + value + ". Chunk offsets must be numeric.");
            }
        } else if (name.equalsIgnoreCase("chunk_length")) {
            try {
                setChunkLength(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                throw new NumberFormatException("The following value could not be used as a chunk length : " + value + ". Chunk lengths must be numeric.");
            }
        } else if (name.equalsIgnoreCase("ft_query_analyzer")) {
          this.analyzerName = value;  
        } else
            throw new RuntimeException("Unrecognized option: " + name);
    }

    public void setOptions(Map<String, String> options) {
        if (options.size() == 0)
            return;

        for (Map.Entry<String, String> entry : options.entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();
            setOption(name, value);
        }
    }

    /**
     * Merges the where clause of this query with the given condition using the AND operator.
     */
    public void mergeCondition(PredicateExpr predicateExpr) {
        if (this.whereClause == null) {
            this.whereClause = predicateExpr;
        } else {
            And and = new And();
            and.add(whereClause);
            and.add(predicateExpr);
            this.whereClause = and;
        }
    }

    /**
     * Pushes a sort clause to the front of the list of clauses
     * @param order
     */
    public void addSortClause(ValueExpr order, SortOrder sortOrder) {
    	if (sortOrders == null)
    		sortOrders = new ArrayList<SortOrder>();
    	if (orderByClause == null)
    		orderByClause = new ValueExprList();
    	this.sortOrders.add(0,sortOrder);
    	this.orderByClause.add(0, order);
    }

    
    public int getChunkLength() {
        return chunkLength;
    }

    public void setChunkLength(int chunkLength) {
        this.chunkLength = chunkLength;
    }

    public int getChunkOffset() {
        return chunkOffset;
    }

    public void setChunkOffset(int chunkOffset) {
        this.chunkOffset = chunkOffset;
    }
    
    public String getAnalyzerName() {
        return analyzerName;
    }

    /**
     *
     * @return null if nothing applicable.
     */
    public AccessRestrictions collectAccessRestrictions() {
        if (!hasSql())
            return null;

        AccessRestrictions restrictions = new AccessRestrictions();

        if (fullTextQuery != null)
            restrictions.setFullTextQueryPresent(true);

        whereClause.collectAccessRestrictions(restrictions);
        if (restrictions.isEmpty())
            restrictions = null;
        return restrictions;
    }
}
