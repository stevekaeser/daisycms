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
package org.outerj.daisy.repository.serverimpl.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlObject;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.outerj.daisy.ftindex.FullTextIndex;
import org.outerj.daisy.ftindex.Hits;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.ExtQueryContext;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.model.AccessRestrictions;
import org.outerj.daisy.query.model.ExprDocData;
import org.outerj.daisy.query.model.Expression;
import org.outerj.daisy.query.model.FullTextQuery;
import org.outerj.daisy.query.model.PredicateExpr;
import org.outerj.daisy.query.model.QValueType;
import org.outerj.daisy.query.model.Query;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.query.model.ValueExprList;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentNotFoundException;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.FieldHelper;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.InvalidDocumentIdException;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueComparator;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.commonimpl.DocumentReadAccessWrapper;
import org.outerj.daisy.repository.commonimpl.DocumentStrategy;
import org.outerj.daisy.repository.commonimpl.DocumentWrapper;
import org.outerj.daisy.repository.commonimpl.RepositoryImpl;
import org.outerj.daisy.repository.commonimpl.acl.CommonAccessManager;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.FacetConf;
import org.outerj.daisy.repository.query.PredicateExpression;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.query.RemoteEvaluationContext;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.repository.query.ValueExpression;
import org.outerj.daisy.repository.serverimpl.LocalRepositoryManager;
import org.outerj.daisy.repository.serverimpl.query.facets.AbstractFacetSampler;
import org.outerj.daisy.repository.serverimpl.query.facets.FacetValues;
import org.outerx.daisy.x10.DistinctSearchResultDocument;
import org.outerx.daisy.x10.FacetedQueryResultDocument;
import org.outerx.daisy.x10.HierarchyValueType;
import org.outerx.daisy.x10.LinkDocument;
import org.outerx.daisy.x10.LinkValueType;
import org.outerx.daisy.x10.RepeatedSearchResultValues;
import org.outerx.daisy.x10.SearchResultDocument;

public class LocalQueryManager implements QueryManager {
    private LocalRepositoryManager.Context context;
    private AuthenticatedUser systemUser;
    private AuthenticatedUser user;
    private JdbcHelper jdbcHelper;
    private DocumentStrategy documentStrategy;
    private final Log log = LogFactory.getLog(getClass());


    public LocalQueryManager(LocalRepositoryManager.Context context, DocumentStrategy documentStrategy, AuthenticatedUser user, AuthenticatedUser systemUser, JdbcHelper jdbcHelper) {
        this.context = context;
        this.systemUser = systemUser;
        this.user = user;
        this.jdbcHelper = jdbcHelper;
        this.documentStrategy = documentStrategy;
    }

    public SearchResultDocument performQuery(String queryAsString, Locale locale) throws QueryException {
        return performQuery(queryAsString, null, locale);
    }

    public VariantKey[] performQueryReturnKeys(String queryAsString, Locale locale) throws RepositoryException {
        return performQueryReturnKeys(queryAsString, null, locale);
    }

    public SearchResultDocument performQuery(String query, Locale locale, EvaluationContext evaluationContext) throws QueryException {
        return performQuery(query, null, null, locale, evaluationContext);
    }

    public SearchResultDocument performQuery(String query, String extraCond, Locale locale) throws QueryException {
        return performQuery(query, extraCond, null, locale, new EvaluationContext());
    }

    public SearchResultDocument performQuery(String query, String extraCond, Locale locale, EvaluationContext evaluationContext) throws QueryException {
        return performQuery(query, extraCond, null, locale, evaluationContext);
    }

    public SearchResultDocument performQuery(String query, String extraCond, Map<String, String> queryOptions, Locale locale) throws RepositoryException {
        return performQuery(query, extraCond, queryOptions, locale, new EvaluationContext());
    }

    public SearchResultDocument performQuery(String query, String extraCond, String extraOrderBy, Map<String, String> queryOptions, Locale locale) throws RepositoryException {
        return performQuery(query, extraCond, extraOrderBy, queryOptions, locale, new EvaluationContext());
    }
    
    public SearchResultDocument performQuery(String query, String extraCond, Map<String, String> queryOptions, Locale locale, EvaluationContext evaluationContext) throws QueryException {
    	return performQuery(query, extraCond, null, queryOptions, locale, evaluationContext);
    }
    
    public SearchResultDocument performQuery(String query, String extraCond, String extraOrderBy, Map<String, String> queryOptions, Locale locale,
            RemoteEvaluationContext remoteEvaluationContext) throws RepositoryException {
        EvaluationContext evaluationContext = this.remoteEvaluationContextToEvaluationContext(remoteEvaluationContext);
        return this.performQuery(query, extraCond, extraOrderBy, queryOptions, locale, evaluationContext);
    }

    public SearchResultDocument performQuery(String query, String extraCond, String extraOrderBy, Map<String, String> queryOptions, Locale locale, EvaluationContext evaluationContext) throws QueryException {
        if (query == null)
            throw new IllegalArgumentException("query parameter is null");
        SearchResultDocument xml = null;
        if (evaluationContext == null)
            evaluationContext = new EvaluationContext();
        EvaluationInfo evaluationInfo = new EvaluationInfo(new ExtQueryContext(new RepositoryImpl(context.getCommonRepository(), user)), evaluationContext);

        try {
            SearchData result = performQueryReturnDocuments(query, extraCond, extraOrderBy, queryOptions, locale, evaluationInfo);
            long beforeBuildResult = System.currentTimeMillis();
            Formatter formatter = new ValueFormatter(locale, context, user);
            xml = buildXmlResult(result, formatter, locale, evaluationInfo);
            long afterBuildResult = System.currentTimeMillis();
            QueryExecutionInfo executionInfo = result.executionInfo;
            executionInfo.outputGenerationTime = afterBuildResult - beforeBuildResult;
            xml.getSearchResult().setExecutionInfo(executionInfo.getXml());
        } finally {
            evaluationInfo.dispose();
        }
        return xml;
    }

    public VariantKey[] performQueryReturnKeys(String queryAsString, String extraCond, Locale locale) throws RepositoryException {
        return performQueryReturnKeys(queryAsString, extraCond, locale, new EvaluationContext());
    }

    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Map<String, String> queryOptions, Locale locale) throws RepositoryException {
        return performQueryReturnKeys(query, extraCond, queryOptions, locale, new EvaluationContext());
    }

    public VariantKey[] performQueryReturnKeys(String query, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        return performQueryReturnKeys(query, null, locale, evaluationContext);
    }

    public VariantKey[] performQueryReturnKeys(String queryAsString, String extraCond, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        return performQueryReturnKeys(queryAsString, extraCond, null, locale, evaluationContext);
    }
    
    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Map<String, String> queryOptions, Locale locale,
            RemoteEvaluationContext remoteEvaluationContext) throws RepositoryException {
        EvaluationContext evaluationContext = this.remoteEvaluationContextToEvaluationContext(remoteEvaluationContext);
        return this.performQueryReturnKeys(query, extraCond, queryOptions, locale, evaluationContext);
    }

    public VariantKey[] performQueryReturnKeys(String queryAsString, String extraCond, Map<String, String> queryOptions, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        if (queryAsString == null)
            throw new IllegalArgumentException("query parameter is null");

        if (evaluationContext == null)
            evaluationContext = new EvaluationContext();
        EvaluationInfo evaluationInfo = new EvaluationInfo(new ExtQueryContext(new RepositoryImpl(context.getCommonRepository(), user)), evaluationContext);

        VariantKey[] keys = null;
        try {
            SearchData result = performQueryReturnDocuments(queryAsString, extraCond, queryOptions, locale, evaluationInfo);
            List<DocumentEntry> documents = result.documents;

            keys = new VariantKey[documents.size()];
            int i = 0;
            for (DocumentEntry documentEntry : documents) {
                keys[i] = documentEntry.document.getVariantKey();
                i++;
            }
        } finally {
            evaluationInfo.dispose();
        }

        return keys;
    }

    public org.outerj.daisy.repository.query.ResultSet performQueryReturnResultSet(String query, String extraCond, Map<String, String> queryOptions, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        if (query == null)
            throw new IllegalArgumentException("query parameter is null");
        if (evaluationContext == null)
            evaluationContext = new EvaluationContext();
        EvaluationInfo evaluationInfo = new EvaluationInfo(new ExtQueryContext(new RepositoryImpl(context.getCommonRepository(), user)), evaluationContext);

        try {
            SearchData result = performQueryReturnDocuments(query, extraCond, queryOptions, locale, evaluationInfo);
            return buildResultSet(result, locale, evaluationInfo);
        } finally {
            evaluationInfo.dispose();
        }
    }

    private SearchResultDocument buildXmlResult(SearchData queryResult, Formatter formatter, Locale locale, EvaluationInfo evaluationInfo) throws QueryException {
        Query query = queryResult.query;
        List<DocumentEntry> documents = queryResult.documents;
        ResultInfo queryResultInfo = queryResult.resultInfo;

        // generate result
        ValueExpr[] selectExprs = query.getSelectValueExprs();

        SearchResultDocument searchResultDocument = SearchResultDocument.Factory.newInstance();
        SearchResultDocument.SearchResult searchResult = searchResultDocument.addNewSearchResult();
        if (query.getStyleHint() != null)
            searchResult.setStyleHint(query.getStyleHint());

        SearchResultDocument.SearchResult.Titles titles = searchResult.addNewTitles();
        for (ValueExpr selectExpr : selectExprs) {
            String title = selectExpr.getTitle(locale);
            SearchResultDocument.SearchResult.Titles.Title titleXml = titles.addNewTitle();
            titleXml.setStringValue(title);
            titleXml.setName(selectExpr.getExpression());
        }

        SearchResultDocument.SearchResult.Rows.Row[] rowArray = new SearchResultDocument.SearchResult.Rows.Row[documents.size()];

        if (documents.size() > 0) {
            
            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i).document;
                SearchResultDocument.SearchResult.Rows.Row row = SearchResultDocument.SearchResult.Rows.Row.Factory.newInstance();
                rowArray[i] = row;
                row.setDocumentId(document.getId());
                row.setBranchId(document.getBranchId());
                row.setLanguageId(document.getLanguageId());
                row.setAccess(documents.get(i).aclInfo.getCompactString());

                Version version = null;
                try {
                    version = document.getVersion(query.getVersionMode());
                } catch (RepositoryException e) {
                    throw new QueryException("Problem retrieving document version.", e);
                }

                // Note: version can only be null in border cases, e.g. when the document lost
                // its live version in between the querying on the database and the retrieval
                // of the version object
                if (version != null) {
                    for (ValueExpr selectExpr : selectExprs) {
                        Object value = selectExpr.getOutputValue(new ExprDocData(document, version, documents.get(i).aclInfo), evaluationInfo);
                        outputValue(row, selectExpr, value, formatter, query.getAnnotateLinkFields());
                    }
                }
            }
        }

        SearchResultDocument.SearchResult.ResultInfo resultInfo = searchResult.addNewResultInfo();
        resultInfo.setSize(queryResultInfo.getSize());
        resultInfo.setChunkOffset(queryResultInfo.getChunkOffset());
        resultInfo.setChunkLength(queryResultInfo.getChunkLength());
        resultInfo.setRequestedChunkLength(queryResultInfo.getRequestedChunkLength());
        resultInfo.setRequestedChunkOffset(queryResultInfo.getRequestedChunkOffset());

        SearchResultDocument.SearchResult.Rows rows = searchResult.addNewRows();
        rows.setRowArray(rowArray);

        return searchResultDocument;
    }


    private void outputValue(RepeatedSearchResultValues container, ValueExpr valueExpr, Object value,
             Formatter formatter, boolean annotateLinkFields) {
        if (valueExpr.isMultiValue()) {
            SearchResultDocument.SearchResult.Rows.Row.MultiValue multiValueXml = container.addNewMultiValue();
            if (value != null) {
                Object[] multiValues = (Object[])value;
                for (Object multiValue : multiValues) {
                    outputHierarchicalValue(multiValueXml, valueExpr, multiValue, formatter, annotateLinkFields);
                }
            }
        } else {
            outputHierarchicalValue(container, valueExpr, value, formatter, annotateLinkFields);
        }
    }

    private void outputHierarchicalValue(RepeatedSearchResultValues container, ValueExpr valueExpr, Object value,
            Formatter formatter, boolean annotateLinkFields) {
        if (valueExpr.isHierarchical()) {
            HierarchyValueType hierarchyValue = container.addNewHierarchyValue();
            if (value != null) {
                HierarchyPath hierarchyPath = (HierarchyPath)value;
                Object[] pathElements = hierarchyPath.getElements();
                for (Object pathElement : pathElements) {
                    outputPrimitiveValue(hierarchyValue, valueExpr, pathElement, formatter, annotateLinkFields);
                }
            }
        } else {
            outputPrimitiveValue(container, valueExpr, value, formatter, annotateLinkFields);
        }
    }

    private void outputPrimitiveValue(RepeatedSearchResultValues container, ValueExpr valueExpr, Object value,
            Formatter formatter, boolean annotateLinkFields) {
        QValueType valueType = valueExpr.getOutputValueType();
        if (valueType == QValueType.LINK) {
            LinkValueType linkValue = container.addNewLinkValue();
            if (value != null) {
                VariantKey variantKey = (VariantKey)value;
                setLinkValue(linkValue, variantKey, annotateLinkFields);
            }
        } else if (valueType == QValueType.XML) {
            XmlObject xmlValue = container.addNewXmlValue();
            if (value != null) {
                xmlValue.set((XmlObject)value);
            }
        } else {
            String stringValue = formatter.format(valueExpr, value);
            container.addValue(stringValue);
        }
    }

    private void setLinkValue(LinkValueType linkValue, VariantKey variantKey, boolean annotate) {
        String documentId = variantKey.getDocumentId();
        long branchId = variantKey.getBranchId();
        long languageId = variantKey.getLanguageId();
        linkValue.setDocumentId(documentId);
        linkValue.setBranchId(branchId);
        linkValue.setLanguageId(languageId);
        String label = null;
        if (annotate) {
            try {
                Document linkedDoc = context.getCommonRepository().getDocument(documentId, branchId, languageId, false, user);
                if (linkedDoc.getLiveVersion() != null)
                    label = linkedDoc.getLiveVersion().getDocumentName();
                else
                    label = linkedDoc.getName();
            } catch (InvalidDocumentIdException e) {
                // ignore exception
            } catch (RepositoryException e) {
                // ignore exception
            }
        }
        if (label == null)
            label = "daisy:" + documentId + "@" + branchId + ":" + languageId;
        linkValue.setStringValue(label);
    }

    static String getLocalizedString(String name, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/query/model/messages", locale);
        return bundle.getString(name);
    }

    private org.outerj.daisy.repository.query.ResultSet buildResultSet(SearchData queryResult, Locale locale,
            EvaluationInfo evaluationInfo) throws QueryException {
        Query query = queryResult.query;
        List<DocumentEntry> documents = queryResult.documents;

        // generate result
        ValueExpr[] selectExprs = query.getSelectValueExprs();

        String[] titles = new String[selectExprs.length];
        for (int i = 0; i < selectExprs.length; i++) {
            titles[i] = selectExprs[i].getTitle(locale);
        }

        ValueType[] valueTypes = new ValueType[selectExprs.length];
        boolean[] multiValue = new boolean[selectExprs.length];
        boolean[] hierarchical = new boolean[selectExprs.length];
        for (int i = 0; i < selectExprs.length; i++) {
            ValueExpr selectExpr = selectExprs[i];
            ValueType valueType;
            // map query value types to public value types
            valueType = ValueTypeHelper.queryToFieldValueType(selectExpr.getOutputValueType());
            valueTypes[i] = valueType;
            multiValue[i] = selectExpr.isMultiValue();
            hierarchical[i] = selectExpr.isHierarchical();
        }

        List<ResultSetImpl.Row> rows = new ArrayList<ResultSetImpl.Row>(documents.size());
        if (documents.size() > 0) {
            for (DocumentEntry documentEntry : documents) {
                Document document = documentEntry.document;
                Object[] rowValues = new Object[selectExprs.length];
                VariantKey rowVariantKey = new VariantKey(document.getId(), document.getBranchId(), document.getLanguageId());

                Version version;
                try {
                    version = document.getVersion(query.getVersionMode());
                } catch (RepositoryException e) {
                    throw new QueryException("Problem retrieving document version.", e);
                }

                // Note: version can only be null in border cases, e.g. when the document lost
                // its live version in between the querying on the database and the retrieval
                // of the version object
                if (version != null) {
                    for (int j = 0; j < selectExprs.length; j++) {
                        Object value = selectExprs[j].getOutputValue(new ExprDocData(document, version, documentEntry.aclInfo), evaluationInfo);
                        value = ValueTypeHelper.queryValueToFieldValueType(value, selectExprs[j].getOutputValueType());
                        rowValues[j] = value;
                    }
                }

                rows.add(new ResultSetImpl.Row(rowVariantKey, rowValues));
            }
        }

        return new ResultSetImpl(valueTypes, multiValue, hierarchical, titles, query.getStyleHint(), rows);
    }

    private SearchData performQueryReturnDocuments(String queryAsString, String extraCond, Map<String, String> queryOptions, Locale locale, EvaluationInfo evaluationInfo) throws QueryException {
    	return performQueryReturnDocuments(queryAsString, extraCond, null, queryOptions, locale, evaluationInfo);
    }
    
    /**
     * @return An array containing 3 elements: the Query object, the List of Document objects (= result of the query), and an QueryExecutionInfo object.
     */
    private SearchData performQueryReturnDocuments(String queryAsString, String extraCond, String extraOrderBy, Map<String, String> queryOptions, Locale locale, EvaluationInfo evaluationInfo) throws QueryException {
        try {
            QueryExecutionInfo executionInfo = new QueryExecutionInfo();
            executionInfo.query = queryAsString;
            executionInfo.extraCondition = extraCond;
            executionInfo.locale = locale;

            // Parse and prepare query
            long beforeParseAndPrepare = System.currentTimeMillis();
            Query query = context.getQueryFactory().parseQuery(queryAsString);
            if (extraCond != null) {
                PredicateExpr extraPredicateExpr = context.getQueryFactory().parsePredicateExpression(extraCond);
                query.mergeCondition(extraPredicateExpr);
            }
            if (extraOrderBy != null) {
                Object[] orderByList = context.getQueryFactory().parseOrderBy(extraOrderBy);
                ValueExpr[] identifierList = ((ValueExprList)orderByList[0]).getArray();
                ArrayList<SortOrder> sortList = (ArrayList<SortOrder>)orderByList[1];
                
                for(int i = 0; i<identifierList.length; i++){
                	query.addSortClause(identifierList[i], sortList.get(i));
                }
            }            
            if (queryOptions != null) {
                query.setOptions(queryOptions);
            }
            query.prepare(new ExtQueryContext(new RepositoryImpl(context.getCommonRepository(), user)));
            long afterParseAndPrepare = System.currentTimeMillis();
            executionInfo.parseAndPrepareTime = afterParseAndPrepare - beforeParseAndPrepare;

            // Make searchLastVersion info available to expression implementations
            evaluationInfo.setVersionMode(query.getVersionMode());

            // Perform fulltext query
            Hits fullTextHits = null;
            if (query.getFullTextQuery() != null) {
                long beforeFullTextQuery = System.currentTimeMillis();
                FullTextIndex fullTextIndex = context.getFullTextIndex();
                FullTextQuery ftQuery = query.getFullTextQuery();
                Date pointInTime = null;
                if (query.getVersionMode().isLast() || query.getVersionMode().isLive()) {
                    pointInTime = new Date();
                } else {
                    pointInTime = query.getVersionMode().getDate();
                }
                fullTextHits = fullTextIndex.search(query.getAnalyzerName(), ftQuery.getQuery(), ftQuery.getBranchId(), ftQuery.getLanguageId(), pointInTime, 
                        ftQuery.getSearchName(), ftQuery.getSearchContent(), ftQuery.getSearchFields());
                evaluationInfo.setHits(fullTextHits);
                long afterFullTextQuery = System.currentTimeMillis();
                executionInfo.fullTextQueryTime = afterFullTextQuery - beforeFullTextQuery;
                if (log.isDebugEnabled())
                    log.debug("Resultcount from fulltext search: " + fullTextHits.length());
            }

            // Perform SQL query
            List<VariantKey> sqlResultKeys = null;
            if (query.hasSql()) {
                long beforeRdbmsQuery = System.currentTimeMillis();
                String sql = query.getSql(jdbcHelper, evaluationInfo);

                if (log.isDebugEnabled())
                    log.debug("Generated SQL: " + sql);

                // perform the query on the database
                sqlResultKeys = new ArrayList<VariantKey>();
                Connection conn = null;
                PreparedStatement stmt = null;
                try {
                    conn = context.getDataSource().getConnection();
                    jdbcHelper.startTransaction(conn);
                    stmt = conn.prepareStatement(sql);
                    query.bindSql(stmt, 1, evaluationInfo);
                    ResultSet rs = stmt.executeQuery();
                    while (rs.next())
                        sqlResultKeys.add(new VariantKey(rs.getLong(1) + "-" + rs.getString(2), rs.getLong(3), rs.getLong(4)));
                } finally {
                    jdbcHelper.closeStatement(stmt);
                    jdbcHelper.closeConnection(conn);
                }
                long afterRdbmsQuery = System.currentTimeMillis();
                executionInfo.rdbmsQueryTime = afterRdbmsQuery - beforeRdbmsQuery;
                if (log.isDebugEnabled())
                    log.debug("Resultcount from SQL database search: " + sqlResultKeys.size());
            }

            // Merge fulltext and SQL results
            List<VariantKey> mergedResults;
            if (fullTextHits != null && sqlResultKeys == null) {
                // only full text results, copy hit results over in mergedResults list
                mergedResults = new ArrayList<VariantKey>(fullTextHits.getAllVariantKeys());
            } else if (fullTextHits != null && sqlResultKeys != null) {
                // merge the results
                long beforeMergeTime = System.currentTimeMillis();
                VariantKey[] sqlKeysArray = sqlResultKeys.toArray(new VariantKey[sqlResultKeys.size()]);
                Arrays.sort(sqlKeysArray);
                mergedResults = new ArrayList<VariantKey>(fullTextHits.length());
                int index;
                for (VariantKey variantKey : fullTextHits.getAllVariantKeys()) {
                    index = Arrays.binarySearch(sqlKeysArray, variantKey);
                    if (index >= 0)
                        mergedResults.add(variantKey);
                }
                long afterMergeTime = System.currentTimeMillis();
                executionInfo.mergeTime = afterMergeTime - beforeMergeTime;
            } else if (sqlResultKeys != null) {
                mergedResults = sqlResultKeys;
            } else {
                // This situation can never occur
                throw new RuntimeException("The impossible has happened: both fulltext and sql search results do not exist.");
            }

            if (log.isDebugEnabled())
                log.debug("Resultcount after merge of SQL and Fulltext results: " + mergedResults.size());


            // Retrieve documents and filter acording to ACL
            long beforeAclFiltering = System.currentTimeMillis();
            // Search for additional access restrictions in query (caused by dereferenced links etc.).
            // collectAccessRestrictions() returns null if there are none or if not applicable
            AccessRestrictions restrictions = query.collectAccessRestrictions();
            DocumentRetriever documentRetriever = new DocumentRetriever(user.getId(), user.getActiveRoleIds(), query.getVersionMode());
            List<DocumentEntry> documents = new ArrayList<DocumentEntry>(mergedResults.size());
            for (VariantKey key : mergedResults) {
                Document document = documentRetriever.getDocument(key);
                AclResultInfo aclInfo = documentRetriever.getAclInfo();
                if (document != null && restrictions != null) {
                    if (restrictions.canRead(document, aclInfo, evaluationInfo, documentRetriever))
                        documents.add(new DocumentEntry(document, aclInfo));
                } else if (document != null) {
                    documents.add(new DocumentEntry(document, aclInfo));
                }
            }
            long afterAclFiltering = System.currentTimeMillis();
            executionInfo.aclFilterTime = afterAclFiltering - beforeAclFiltering;

            if (log.isDebugEnabled())
                log.debug("Resultcount after applying ACL filtering: " + documents.size());

            // Perform sorting
            if (query.getOrderByValueExprs() != null) {
                long beforeSortingTime = System.currentTimeMillis();
                ValueExpr[] orderByExprs = query.getOrderByValueExprs();
                SortOrder[] sortOrders = query.getOrderBySortOrders();
                CompareContext compareContext = new CompareContext(orderByExprs, sortOrders, new ValueComparator(true, locale));
                DocumentComparable[] comparables = new DocumentComparable[documents.size()];
                for (int i = 0; i < comparables.length; i++) {
                    comparables[i] = new DocumentComparable(compareContext, documents.get(i), query.getVersionMode(), evaluationInfo);
                }
                Arrays.sort(comparables);

                documents = new ArrayList<DocumentEntry>(comparables.length);
                for (int i = 0; i < comparables.length; i++) {
                    documents.add(i, comparables[i].getDocumentEntry());
                }
                long afterSortingTime = System.currentTimeMillis();
                executionInfo.sortTime = afterSortingTime - beforeSortingTime;
            }

            // take limit clause into account
            int finish;
            if (query.getLimit() != -1)
                finish = documents.size() < query.getLimit() ? documents.size() : query.getLimit();
            else
                finish = documents.size();

            ResultInfo resultInfo = new ResultInfo();
            resultInfo.setSize(finish);
            resultInfo.setChunkOffset(query.getChunkOffset());
            resultInfo.setChunkLength(query.getChunkLength());
            resultInfo.setRequestedChunkOffset(query.getChunkOffset());
            resultInfo.setRequestedChunkLength(query.getChunkLength());

            int start = 1;
            if (query.getChunkLength()> 0 && query.getChunkOffset()> 0) {
                if (finish >= query.getChunkOffset()) {
                    start = query.getChunkOffset();
                    if (finish >= query.getChunkOffset() + query.getChunkLength())
                        finish = query.getChunkOffset() + query.getChunkLength() - 1;
                    else
                        resultInfo.setChunkLength(finish - query.getChunkOffset() + 1);
                } else {
                    finish = 0;
                }
            }

            return new SearchData(query, documents.subList(start - 1, finish), executionInfo, resultInfo, documents);
        } catch (Exception e) {
            throw new QueryException("Error performing query.", e);
        }
    }
    
    private EvaluationContext remoteEvaluationContextToEvaluationContext(RemoteEvaluationContext remoteEvaluationContext) throws RepositoryException {
        EvaluationContext evaluationContext = new EvaluationContext();
        
        while (remoteEvaluationContext.size() > 0) {
            VersionKey key = remoteEvaluationContext.popContextDocument();
            Document document = this.context.getCommonRepository().getDocument(key.getDocumentId(), key.getBranchId(), key.getLanguageId(), false, this.user);
            
            Version version;
            if (key.getVersionId() == -1)
                version = document.getLiveVersion();
            else if (key.getVersionId() == -2)
                version = document.getLastVersion();
            else 
                version = document.getVersion(key.getVersionId());
            
            evaluationContext.pushContextDocument(document, version);            
        }
        
        return evaluationContext;
    }

    /**
     * Helper class for retrieving document objects, keeping care of access control.
     * This class is not thread safe.
     */
    public class DocumentRetriever {
        private final CommonRepository repository;
        private final CommonAccessManager accessManager;
        private final long userId;
        private final long[] activeRoleIds;
        private final VersionMode versionMode;
        private AclResultInfo aclInfo;

        public DocumentRetriever(long userId, long[] activeRoleIds, VersionMode versionMode) {
            this.repository = context.getCommonRepository();
            this.accessManager = repository.getAccessManager();
            this.userId = userId;
            this.activeRoleIds = activeRoleIds;
            this.versionMode = versionMode;
        }

        /**
         * Returns the document if it exists and access is allowed, otherwise returns null.
         */
        public Document getDocument(VariantKey key) throws RepositoryException {
            try {
                // Documents are retrieved by the systemUser to avoid AccessExceptions, exceptions
                // are normally less performant then explicitly checking the ACL.
                Document document = repository.getDocument(key.getDocumentId(), key.getBranchId(), key.getLanguageId(), false, systemUser);
                aclInfo = accessManager.getAclInfoOnLive(systemUser, userId, activeRoleIds, document);
                boolean canRead = false;
                if (aclInfo.isNonLiveAllowed(AclPermission.READ)) {
                    canRead = true;
                } else if (aclInfo.isAllowed(AclPermission.READ)) {
                    if (document.getLiveVersionId() == -1 || document.isRetired()) {
                        // don't include document in query results
                    } else if (document.getVersionId(versionMode) == document.getLiveVersionId()) {
                        canRead = true;
                    }
                }
                if (canRead) {
                    AccessDetails details = aclInfo.getAccessDetails(AclPermission.READ);
                    if (details != null)
                        document = new DocumentReadAccessWrapper(((DocumentWrapper)document).getWrappedDocument(documentStrategy), details, repository, user, documentStrategy);
                    return document;
                }
            } catch (DocumentNotFoundException e) {
                // the document has been deleted since we got the search results, skip it
            } catch (DocumentVariantNotFoundException e) {
                // the document has been deleted since we got the search results, skip it
            }
            return null;
        }

        /**
         * Returns the AclResultInfo for the last retrieved document.
         */
        public AclResultInfo getAclInfo() {
            return aclInfo;
        }
    }

    /**
     * An object combining a document and its acl evaluation info.
     */
    private static class DocumentEntry {
        public Document document;
        public AclResultInfo aclInfo;

        public DocumentEntry(Document document, AclResultInfo info) {
            this.document = document;
            this.aclInfo = info;
        }
    }

    private static class SearchData {
        public List<DocumentEntry> documents;
        public Query query;
        public QueryExecutionInfo executionInfo;
        public ResultInfo resultInfo;
        public List<DocumentEntry> allDocuments;

        public SearchData(Query query, List<DocumentEntry> documents, QueryExecutionInfo executionInfo, ResultInfo resultInfo, List<DocumentEntry> allDocuments) {
            this.query = query;
            this.documents = documents;
            this.executionInfo = executionInfo;
            this.resultInfo = resultInfo;
            this.allDocuments = allDocuments;
        }
    }

    public FacetedQueryResultDocument performFacetedQuery(String queryString, FacetConf[] facetConfs, Map<String, String> queryOptions, Locale locale) throws RepositoryException {
        return this.performFacetedQuery(queryString, facetConfs, queryOptions, locale, new EvaluationContext());
    }

    public FacetedQueryResultDocument performFacetedQuery(String queryString, FacetConf[] facetConfs, Map<String, String> queryOptions, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {        
        EvaluationInfo evaluationInfo = new EvaluationInfo(new ExtQueryContext(new RepositoryImpl(context.getCommonRepository(), user)), evaluationContext);
        FacetedQueryResultDocument resultDocument = null;

        try {
            SearchData result = performQueryReturnDocuments(queryString, null, queryOptions, locale, evaluationInfo);
            Query query = result.query;
            List<DocumentEntry> allDocuments = result.allDocuments;
            QueryExecutionInfo executionInfo = result.executionInfo;

            ValueExpr[] selectExprs = query.getSelectValueExprs();
            int facetCount = Math.min(facetConfs.length, selectExprs.length);
            FacetValues[] facetValues = new FacetValues[facetCount];
            for (int i = 0; i < facetCount; i++)
                facetValues[i] = facetConfs[i].isFacet() ? new FacetValues(AbstractFacetSampler.createFacetSampler(facetConfs[i], locale, log), user, context) : null;

            for (DocumentEntry documentEntry : allDocuments) {
                Document document = documentEntry.document;
                for (int j = 0; j < facetCount; j++) {
                    if (facetValues[j] == null)
                        continue;
                    Version version;
                    try {
                        version = document.getVersion(query.getVersionMode());
                    } catch (RepositoryException e) {
                        throw new QueryException("Problem retrieving document version.", e);
                    }
                    if (version != null) {
                        Object value = selectExprs[j].getOutputValue(new ExprDocData(document, version, documentEntry.aclInfo), evaluationInfo);
                        if (value != null) {
                            if (selectExprs[j].isMultiValue()) {
                                Object[] values = (Object[])value;
                                values:
                                for (int k = 0; k < values.length; k++) {
                                    // a multivalue can contain the same value more then once, but we
                                    // only want it to be counted once for each document
                                    for (int l = 0; l < k; l++) {
                                        if (values[k].equals(values[l]))
                                            continue values;
                                    }
                                    facetValues[j].addValue(values[k]);
                                }
                            } else {
                                facetValues[j].addValue(value);
                            }
                        }
                    }
                }
            }

            resultDocument = FacetedQueryResultDocument.Factory.newInstance();
            FacetedQueryResultDocument.FacetedQueryResult facetedQueryResult = resultDocument.addNewFacetedQueryResult();
            FacetedQueryResultDocument.FacetedQueryResult.Facets facetsXml = facetedQueryResult.addNewFacets();

            for (int i = 0; i < facetValues.length; i++) {
                if (facetValues[i] != null)
                    facetValues[i].addXml(facetsXml, facetConfs[i], selectExprs[i]);
            }

            Formatter formatter = new FacetValueFormatter(locale, context, user);
            SearchResultDocument xmlResult = buildXmlResult(result, formatter, locale, evaluationInfo);
            xmlResult.getSearchResult().setExecutionInfo(executionInfo.getXml());
            facetedQueryResult.setSearchResult(xmlResult.getSearchResult());
        } finally {
            evaluationInfo.dispose();
        }

        return resultDocument;
    }

    public DistinctSearchResultDocument performDistinctQuery(String queryString, String extraCond, SortOrder sortOrder, Locale locale) throws RepositoryException {
        Object[] result = performDistinctQueryReturnValuesInt(queryString, extraCond, sortOrder, locale);
        List<Object> values = (List<Object>)result[0];
        ValueExpr valueExpr = (ValueExpr)result[1];

        DistinctSearchResultDocument.DistinctSearchResult.Values.Value[] valuesXml = new DistinctSearchResultDocument.DistinctSearchResult.Values.Value[values.size()];
        QValueType outputValueType = valueExpr.getOutputValueType();
        FieldHelper.XmlFieldValueSetter resultSetter = null;
        if (outputValueType != QValueType.LINK)
            resultSetter = FieldHelper.getXmlFieldValueSetter(getDistinctValueType(outputValueType));
        ValueFormatter valueFormatter = new ValueFormatter(locale, context, user);
        for (int i = 0; i < values.size(); i++) {
            valuesXml[i] = DistinctSearchResultDocument.DistinctSearchResult.Values.Value.Factory.newInstance();
            if (outputValueType == QValueType.LINK) {
                String formattedValue = valueFormatter.format(valueExpr, values.get(i));
                valuesXml[i].setLabel(formattedValue);
                VariantKey variantKey = (VariantKey)values.get(i);
                LinkDocument.Link link = valuesXml[i].addNewLink();
                link.setDocumentId(variantKey.getDocumentId());
                link.setBranchId(variantKey.getBranchId());
                link.setLanguageId(variantKey.getLanguageId());
            } else {
                resultSetter.addValue(values.get(i), valuesXml[i]);
            }
        }

        DistinctSearchResultDocument resultDoc = DistinctSearchResultDocument.Factory.newInstance();
        DistinctSearchResultDocument.DistinctSearchResult resultXml = resultDoc.addNewDistinctSearchResult();
        DistinctSearchResultDocument.DistinctSearchResult.Values valuesXmlParent = resultXml.addNewValues();
        valuesXmlParent.setValueType(outputValueType.toString());
        valuesXmlParent.setValueArray(valuesXml);

        return resultDoc;
    }

    public DistinctSearchResultDocument performDistinctQuery(String queryString, SortOrder sortOrder, Locale locale) throws RepositoryException {
        return performDistinctQuery(queryString, null, sortOrder, locale);
    }

    public List<Object> performDistinctQueryReturnValues(String query, SortOrder sortOrder, Locale locale) throws RepositoryException {
        return performDistinctQueryReturnValues(query, null, sortOrder, locale);
    }

    public List<Object> performDistinctQueryReturnValues(String queryString, String extraCond, SortOrder sortOrder,
            Locale locale) throws RepositoryException {
        return (List<Object>)performDistinctQueryReturnValuesInt(queryString, extraCond, sortOrder, locale)[0];
    }

    public Object[] performDistinctQueryReturnValuesInt(String queryString, String extraCond, SortOrder sortOrder,
            Locale locale) throws RepositoryException {
        EvaluationContext evaluationContext = new EvaluationContext();
        EvaluationInfo evaluationInfo = new EvaluationInfo(new ExtQueryContext(new RepositoryImpl(context.getCommonRepository(), user)), evaluationContext);

        try {
            SearchData result = performQueryReturnDocuments(queryString, extraCond, null, locale, evaluationInfo);
            Query query = result.query;
            List<DocumentEntry> documents = result.documents;

            ValueExpr valueExpr = query.getSelectValueExprs()[0];
            boolean multiValue = valueExpr.isMultiValue();
            HashSet<Object> distinctValues = new HashSet<Object>();

            for (DocumentEntry documentEntry : documents) {
                Document document = documentEntry.document;
                Version version;
                try {
                    version = document.getVersion(query.getVersionMode());
                } catch (RepositoryException e) {
                    throw new QueryException("Problem retrieving document version.", e);
                }
                if (version != null) {
                    Object value = valueExpr.getOutputValue(new ExprDocData(document, version, documentEntry.aclInfo), evaluationInfo);
                    if (value != null) {
                        if (multiValue) {
                            Object[] values = (Object[])value;
                            for (int k = 0; k < values.length; k++)
                                distinctValues.add(values[k]);
                        } else {
                            distinctValues.add(value);
                        }
                    }
                }
            }

            List<Object> values = new ArrayList<Object>();
            values.addAll(distinctValues);
            if (sortOrder != SortOrder.NONE)
                Collections.sort(values, new ValueComparator<Object>(sortOrder == SortOrder.ASCENDING, locale));

            return new Object[] {values, valueExpr};
        } finally {
          evaluationInfo.dispose();
        }
    }

    private ValueType getDistinctValueType(QValueType valueType) {
        if (valueType == QValueType.STRING)
            return ValueType.STRING;
        else if (valueType == QValueType.LONG)
            return ValueType.LONG;
        else if (valueType == QValueType.DECIMAL)
            return ValueType.DECIMAL;
        else if (valueType == QValueType.DOUBLE)
            return ValueType.DOUBLE;
        else if (valueType == QValueType.BOOLEAN)
            return ValueType.BOOLEAN;
        else if (valueType == QValueType.DATE)
            return ValueType.DATE;
        else if (valueType == QValueType.DATETIME)
            return ValueType.DATETIME;
        else
            throw new RuntimeException("Can't handle this type in distinct query result: " + valueType.toString());
    }

    private static class CompareContext{
        public final ValueExpr[] orderKeys;
        public final SortOrder[] sortOrders;
        public final ValueComparator valueComparator;

        public CompareContext(ValueExpr[] orderKeys, SortOrder[] sortOrders, ValueComparator valueComparator) {
            this.orderKeys = orderKeys;
            this.sortOrders = sortOrders;
            this.valueComparator = valueComparator;
        }
    }

    private static class DocumentComparable implements Comparable {
        private final CompareContext context;
        private final Object[] orderValues;
        private final DocumentEntry documentEntry;
        private final Document document;
        private final VersionMode versionMode;
        private final EvaluationInfo evaluationInfo;

        public DocumentComparable(CompareContext context, DocumentEntry documentEntry, VersionMode versionMode, EvaluationInfo evaluationInfo) {
            this.context = context;
            this.documentEntry = documentEntry;
            this.document = documentEntry.document;
            this.orderValues = new Object[context.orderKeys.length];
            this.versionMode = versionMode;
            this.evaluationInfo = evaluationInfo;
        }

        public Object getValue(int pos) {
            if (orderValues[pos] == null) {
                Version version;
                try {
                    version = document.getVersion(versionMode);
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
                try {
                    orderValues[pos] = context.orderKeys[pos].getOutputValue(new ExprDocData(document, version), evaluationInfo);
                } catch (QueryException e) {
                    throw new RuntimeException(e);
                }
                if (orderValues[pos] == null)
                    orderValues[pos] = NULL_VALUE;
            }
            return orderValues[pos];
        }

        public DocumentEntry getDocumentEntry() {
            return documentEntry;
        }

        public int compareTo(Object o) {
            DocumentComparable theOtherOne = (DocumentComparable)o;
            for (int i = 0; i < context.orderKeys.length; i++) {
                Object myValue = getValue(i);
                Object otherValue = theOtherOne.getValue(i);

                int compareResult;
                if (myValue == NULL_VALUE && otherValue == NULL_VALUE) {
                    compareResult = 0;
                } else if (myValue == NULL_VALUE) {
                    compareResult = 1;
                } else if (otherValue == NULL_VALUE) {
                    compareResult = -1;
                } else {
                    compareResult = context.valueComparator.compare(myValue, otherValue);
                }

                if (compareResult != 0) {
                    if (context.sortOrders[i] == SortOrder.DESCENDING)
                        compareResult = compareResult * -1;
                    return compareResult;
                }
                // and else we move on to the next expression in the order by clause
            }
            return 0;
        }
    }

    private static final NullValue NULL_VALUE = new NullValue();

    private static class NullValue implements Comparable {
        public int compareTo(Object o) {
            if (o instanceof NullValue)
                return 0;
            else
                return 1;
        }
    }

    static class QueryExecutionInfo {
        public String query;
        public String extraCondition;
        public Locale locale;
        public long parseAndPrepareTime = -1;
        public long fullTextQueryTime = -1;
        public long rdbmsQueryTime = -1;
        public long mergeTime = -1;
        public long aclFilterTime = -1;
        public long sortTime = -1;
        public long outputGenerationTime = -1;

        public SearchResultDocument.SearchResult.ExecutionInfo getXml() {
            SearchResultDocument.SearchResult.ExecutionInfo xml = SearchResultDocument.SearchResult.ExecutionInfo.Factory.newInstance();
            xml.setQuery(query);
            if (extraCondition != null)
                xml.setExtraCondition(extraCondition);
            xml.setLocale(locale.toString());
            if (parseAndPrepareTime != -1)
                xml.setParseAndPrepareTime(parseAndPrepareTime);
            if (fullTextQueryTime != -1)
                xml.setFullTextQueryTime(fullTextQueryTime);
            if (rdbmsQueryTime != -1)
                xml.setRdbmsQueryTime(rdbmsQueryTime);
            if (mergeTime != -1)
                xml.setMergeTime(mergeTime);
            if (aclFilterTime != -1)
                xml.setAclFilterTime(aclFilterTime);
            if (sortTime != -1)
                xml.setSortTime(sortTime);
            if (outputGenerationTime != -1)
                xml.setOutputGenerationTime(outputGenerationTime);
            return xml;
        }
    }

    public PredicateExpression parsePredicateExpression(String expression) throws QueryException {
        PredicateExpr predicateExpr = context.getQueryFactory().parsePredicateExpression(expression);
        QueryContext queryContext = new ExtQueryContext(new RepositoryImpl(context.getCommonRepository(), user));
        predicateExpr.prepare(queryContext);
        return new PredicateExpressionImpl(predicateExpr, queryContext);
    }

    public ValueExpression parseValueExpression(String expression) throws QueryException {
        Expression expr = context.getQueryFactory().parseExpression(expression);
        QueryContext queryContext = new ExtQueryContext(new RepositoryImpl(context.getCommonRepository(), user));
        if (expr instanceof ValueExpr) {
            ValueExpr valueExpr = (ValueExpr)expr;
            valueExpr.prepare(queryContext);
            return new ValueExpressionImpl(valueExpr, queryContext);
        } else {
            PredicateExpr predicateExpr = (PredicateExpr)expr;
            predicateExpr.prepare(queryContext);
            return new ValueExpressionImpl(predicateExpr, queryContext);
        }
    }

    private static class ResultInfo  {
        private int size;
        private int chunkOffset;
        private int chunkLength;
        private int requestedChunkOffset;
        private int requestedChunkLength;
        
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
        public int getSize() {
            return size;
        }
        public void setSize(int size) {
            this.size = size;
        }
        public int getRequestedChunkOffset() {
            return requestedChunkOffset;
        }
        public void setRequestedChunkOffset(int requestedChunkOffset) {
            this.requestedChunkOffset = requestedChunkOffset;
        }
        public int getRequestedChunkLength() {
            return requestedChunkLength;
        }
        public void setRequestedChunkLength(int requestedChunkLength) {
            this.requestedChunkLength = requestedChunkLength;
        }
    }
}
