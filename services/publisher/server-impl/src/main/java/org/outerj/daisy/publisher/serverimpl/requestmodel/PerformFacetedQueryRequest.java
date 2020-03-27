/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import java.util.Map;

import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.FacetConf;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerx.daisy.x10.FacetedQueryResultDocument;
import org.xml.sax.ContentHandler;

public class PerformFacetedQueryRequest extends AbstractRequest implements Request {
    private String query;
    private Map<String,String> queryOptions;
    private FacetConf[] facetConfs;
    
    public PerformFacetedQueryRequest(FacetDefinition[] facetDefinitions, String defaultConditions, String defaultOrder, 
            Map<String,String> queryOptions, LocationInfo locationInfo) {
        super(locationInfo);
        
        facetConfs = new FacetConf[facetDefinitions.length];
        
        // build query
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append("select");
        for (int i = 0; i < facetDefinitions.length; i++) {
            FacetDefinition facetDefinition = facetDefinitions[i];
            queryBuffer.append(" ").append(facetDefinition.getExpression()).append(",");
            facetConfs[i] = facetDefinition.getFacetConf();
        }
        queryBuffer.deleteCharAt(queryBuffer.length()-1);
        queryBuffer.append(" where ").append(defaultConditions);
        
        if (defaultOrder != null)
            queryBuffer.append(" order by ").append(defaultOrder);
        
        this.query = queryBuffer.toString();        
        this.queryOptions = queryOptions;
    }

    @Override
    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        Repository repository = publisherContext.getRepository();        
        FacetedQueryResultDocument facetResultDocument;
        
        EvaluationContext evaluationContext = new EvaluationContext();
        publisherContext.pushContextDocuments(evaluationContext);
        
        try {
            facetResultDocument  = repository.getQueryManager().performFacetedQuery(query, facetConfs, queryOptions, publisherContext.getLocale(), evaluationContext);
        } catch (RepositoryException e) {
            throw new PublisherException("Error performing faceted query in publisher request at " + getLocationInfo().getFormattedLocation(), e);
        }

        facetResultDocument.save(new StripDocumentHandler(contentHandler), new DummyLexicalHandler());
    }
    
    public static class FacetDefinition {
        private FacetConf delegate;
        private String expression;
        
        public FacetDefinition(String expression, boolean isFacet, int maxValues, boolean sortOnValue, boolean sortAscending, String type) {
            delegate = new FacetConf(isFacet, maxValues, sortOnValue, sortAscending, type.toUpperCase());
            this.expression = expression;
        }

        public String getExpression() {
            return expression;
        }
        
        public FacetConf getFacetConf(){
            return delegate;
        }
        
        public Map<String,String> getProperties() {
            return delegate.getProperties();
        }
        
        public void setProperties(Map<String,String> properties) {
            this.delegate.setProperties(properties);
        }
    }
}
