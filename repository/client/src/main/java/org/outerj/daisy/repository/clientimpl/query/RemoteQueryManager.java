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
package org.outerj.daisy.repository.clientimpl.query;

import org.outerj.daisy.repository.clientimpl.infrastructure.AbstractRemoteStrategy;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.query.*;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VariantKeys;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.outerx.daisy.x10.*;
import org.outerx.daisy.x10.FacetedQueryRequestDocument.FacetedQueryRequest.FacetConfs.FacetConf.Properties;
import org.outerx.daisy.x10.FacetedQueryRequestDocument.FacetedQueryRequest.FacetConfs.FacetConf.Properties.Property;

import java.util.*;

public class RemoteQueryManager extends AbstractRemoteStrategy implements QueryManager {
    private AuthenticatedUser user;

    public RemoteQueryManager(RemoteRepositoryManager.Context context, AuthenticatedUser user) {
        super(context);
        this.user = user;
    }

    public SearchResultDocument performQuery(String query, Locale locale) throws RepositoryException {
        return performQuery(query, null, null, locale);
    }

    public SearchResultDocument performQuery(String query, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        throw new RepositoryException("Specifying an evaluationContext is not possible in the remote repository API implementation.");
    }

    public VariantKey[] performQueryReturnKeys(String query, Locale locale) throws RepositoryException {
        return performQueryReturnKeys(query, null, null, locale);
    }

    public VariantKey[] performQueryReturnKeys(String query, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        throw new RepositoryException("Specifying an evaluationContext is not possible in the remote repository API implementation.");
    }

    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Locale locale) throws RepositoryException {
        return performQueryReturnKeys(query, extraCond, null, locale);
    }

    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Map<String, String> queryOptions, Locale locale) throws RepositoryException {
        return performQueryReturnKeys(query, extraCond, queryOptions, locale, new RemoteEvaluationContext());
    }
    
    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Map<String, String> queryOptions, Locale locale, RemoteEvaluationContext remoteEvaluationContext) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        PostMethod method = new PostMethod("/repository/query");

        List<NameValuePair> queryString = new ArrayList<NameValuePair>(4);
        queryString.add(new NameValuePair("q", query));
        queryString.add(new NameValuePair("locale", LocaleHelper.getString(locale)));
        queryString.add(new NameValuePair("returnKeys", "true"));
        if (extraCond != null)
            queryString.add(new NameValuePair("extraCondition", extraCond));
        addQueryOptions(queryString, queryOptions);
        
        if (remoteEvaluationContext != null) {
            for (int i = 0; i < remoteEvaluationContext.size(); i++ ) {
                org.outerj.daisy.repository.VersionKey versionKey = remoteEvaluationContext.getContextDocument(i);
                StringBuilder keyBuilder = new StringBuilder(20);
                keyBuilder.append("daisy:").append(versionKey.getDocumentId()).append("@").append(versionKey.getBranchId()).append(":")
                    .append(versionKey.getLanguageId()).append(":").append(versionKey.getVersionId());
                queryString.add(new NameValuePair("contextDocument", keyBuilder.toString()));
            }
        }

        method.addParameters(queryString.toArray(new NameValuePair[queryString.size()]));

        VariantKeysDocument variantKeysDocument = (VariantKeysDocument)httpClient.executeMethod(method, VariantKeysDocument.class, true);
        return VariantKeys.fromXml(variantKeysDocument).getArray();
    }

    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Map<String, String> queryOptions, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        throw new RepositoryException("Specifying an evaluationContext is not possible in the remote repository API implementation.");
    }

    public VariantKey[] performQueryReturnKeys(String query, String extraCond, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        throw new RepositoryException("Specifying an evaluationContext is not possible in the remote repository API implementation.");
    }

    public ResultSet performQueryReturnResultSet(String query, String extraCond, Map<String, String> queryOptions, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        throw new RepositoryException("This method is not supported in the remote repository API implementation.");
    }


    public SearchResultDocument performQuery(String query, String extraCond, Locale locale) throws RepositoryException {
        return performQuery(query, extraCond, null, locale);
    }
    
    public SearchResultDocument performQuery(String query, String extraCond, Map<String, String> queryOptions, Locale locale) throws RepositoryException {
    	return performQuery(query, extraCond, null, queryOptions, locale);
    }
    
    public SearchResultDocument performQuery(String query, String extraCond, String extraOrderBy, Map<String, String> queryOptions, Locale locale) throws RepositoryException {
        return performQuery(query, extraCond, extraOrderBy, queryOptions, locale, null);        
    }
    
    public SearchResultDocument performQuery(String query, String extraCond, String extraOrderBy, Map<String, String> queryOptions, Locale locale, RemoteEvaluationContext remoteEvaluationContext) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        PostMethod method = new PostMethod("/repository/query");

        List<NameValuePair> queryString = new ArrayList<NameValuePair>(4);
        queryString.add(new NameValuePair("q", query));
        queryString.add(new NameValuePair("locale", LocaleHelper.getString(locale)));
        if (extraCond != null)
            queryString.add(new NameValuePair("extraCondition", extraCond));
        if(extraOrderBy != null)
            queryString.add(new NameValuePair("extraOrderBy", extraOrderBy));
        
        addQueryOptions(queryString, queryOptions);
        
        if (remoteEvaluationContext != null) {
            for (int i = 0; i < remoteEvaluationContext.size(); i++ ) {
                org.outerj.daisy.repository.VersionKey versionKey = remoteEvaluationContext.getContextDocument(i);
                StringBuilder keyBuilder = new StringBuilder(20);
                keyBuilder.append("daisy:").append(versionKey.getDocumentId()).append("@").append(versionKey.getBranchId()).append(":")
                    .append(versionKey.getLanguageId()).append(":").append(versionKey.getVersionId());
                queryString.add(new NameValuePair("contextDocument", keyBuilder.toString()));
            }
        }

        method.addParameters(queryString.toArray(new NameValuePair[queryString.size()]));

        SearchResultDocument searchResultDocument = (SearchResultDocument)httpClient.executeMethod(method, SearchResultDocument.class, true);
        return searchResultDocument;
    }

    private void addQueryOptions(List<NameValuePair> queryString, Map<String, String> queryOptions) {
        if (queryOptions != null) {
            for (Map.Entry<String, String> entry : queryOptions.entrySet()) {
                queryString.add(new NameValuePair("queryOption", entry.getKey() + "=" + entry.getValue()));
            }
        }
    }
    
    public SearchResultDocument performQuery(String query, String extraCond, Map<String, String> queryOptions, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        throw new RepositoryException("Specifying an evaluationContext is not possible in the remote repository API implementation.");
    }

    public SearchResultDocument performQuery(String query, String extraCond, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        throw new RepositoryException("Specifying an evaluationContext is not possible in the remote repository API implementation.");
    }
    
    public FacetedQueryResultDocument performFacetedQuery(String query, FacetConf[] facetConfs, Map<String, String> queryOptions, Locale locale, EvaluationContext evaluationContext) throws RepositoryException {
        throw new RepositoryException("Specifying an evaluationContext is not possible in the remote repository API implementation.");
    }

    public FacetedQueryResultDocument performFacetedQuery(String query, FacetConf[] facetConfs, Map<String, String> queryOptions, Locale locale) throws RepositoryException {
        FacetedQueryRequestDocument facetedQueryRequestDocument = FacetedQueryRequestDocument.Factory.newInstance();
        FacetedQueryRequestDocument.FacetedQueryRequest facetedQueryRequest = facetedQueryRequestDocument.addNewFacetedQueryRequest();
        facetedQueryRequest.setQuery(query);
        FacetedQueryRequestDocument.FacetedQueryRequest.FacetConfs facetConfsXml = facetedQueryRequest.addNewFacetConfs();
        for (FacetConf facetConf : facetConfs) {
            FacetedQueryRequestDocument.FacetedQueryRequest.FacetConfs.FacetConf facetConfXml = facetConfsXml.addNewFacetConf();
            facetConfXml.setIsFacet(facetConf.isFacet());
            facetConfXml.setMaxValues(facetConf.getMaxValues());
            facetConfXml.setSortAscending(facetConf.getSortAscending());
            facetConfXml.setSortOnValue(facetConf.getSortOnValue());
            facetConfXml.setType(facetConf.getType().name());
            Map<String, String> facetProperties = facetConf.getProperties();
            if (facetProperties != null) {
                Properties propertiesXml = facetConfXml.addNewProperties();
                for (String propName : facetProperties.keySet()) {
                    Property property = propertiesXml.addNewProperty();
                    property.setName(propName);
                    property.setValue(facetProperties.get(propName));
                }
            }
        }
        
        FacetedQueryRequestDocument.FacetedQueryRequest.QueryOptions requestQueryOptions = facetedQueryRequest.addNewQueryOptions();
        for (Map.Entry<String, String> entry : queryOptions.entrySet()) {
            FacetedQueryRequestDocument.FacetedQueryRequest.QueryOptions.QueryOption requestQueryOption = requestQueryOptions.addNewQueryOption();
            requestQueryOption.setName(entry.getKey());
            requestQueryOption.setValue(entry.getValue());
        }
        
        facetedQueryRequest.setLocale(LocaleHelper.getString(locale));

        DaisyHttpClient httpClient = getClient(user);
        PostMethod method = new PostMethod("/repository/facetedQuery");
        method.setRequestEntity(new InputStreamRequestEntity(facetedQueryRequestDocument.newInputStream()));

        FacetedQueryResultDocument resultDocument = (FacetedQueryResultDocument)httpClient.executeMethod(method, FacetedQueryResultDocument.class, true);
        return resultDocument;
    }

    public DistinctSearchResultDocument performDistinctQuery(String query, SortOrder sortOrder, Locale locale) throws RepositoryException {
        return performDistinctQuery(query, null, sortOrder, locale);
    }

    public DistinctSearchResultDocument performDistinctQuery(String query, String extraCond, SortOrder sortOrder, Locale locale) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        GetMethod method = new GetMethod("/repository/distinctquery");

        NameValuePair[] queryString = new NameValuePair[extraCond == null ? 3 : 4];
        queryString[0] = new NameValuePair("q", query);
        queryString[1] = new NameValuePair("locale", LocaleHelper.getString(locale));
        queryString[2] = new NameValuePair("sortOrder", sortOrder.toString());
        if (extraCond != null)
            queryString[3] = new NameValuePair("extraCondition", extraCond);

        method.setQueryString(queryString);

        DistinctSearchResultDocument resultDocument = (DistinctSearchResultDocument)httpClient.executeMethod(method, DistinctSearchResultDocument.class, true);
        return resultDocument;
    }

    public List<Object> performDistinctQueryReturnValues(String query, SortOrder sortOrder, Locale locale) throws RepositoryException {
        throw new QueryException("This method is not available in the remote repository API implementation.");
    }

    public List<Object> performDistinctQueryReturnValues(String query, String extraCond, SortOrder sortOrder, Locale locale) throws RepositoryException {
        throw new QueryException("This method is not available in the remote repository API implementation.");
    }

    public PredicateExpression parsePredicateExpression(String expression) throws QueryException {
        throw new QueryException("This method is not available in the remote repository API implementation.");
    }

    public ValueExpression parseValueExpression(String expression) throws QueryException {
        throw new QueryException("This method is not available in the remote repository API implementation.");
    }

}
