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
package org.outerj.daisy.httpconnector.handlers;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.query.FacetConf;
import org.outerj.daisy.repository.query.FacetConf.FacetType;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerx.daisy.x10.FacetedQueryRequestDocument;
import org.outerx.daisy.x10.FacetedQueryResultDocument;
import org.outerx.daisy.x10.FacetedQueryRequestDocument.FacetedQueryRequest.FacetConfs.FacetConf.Properties;
import org.outerx.daisy.x10.FacetedQueryRequestDocument.FacetedQueryRequest.FacetConfs.FacetConf.Properties.Property;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class FacetedQueryHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/facetedQuery";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        if (request.getMethod().equals(HttpConstants.POST)) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            FacetedQueryRequestDocument.FacetedQueryRequest facetedRequest = FacetedQueryRequestDocument.Factory.parse(request.getInputStream(), xmlOptions).getFacetedQueryRequest();            
            String query = facetedRequest.getQuery();
            FacetedQueryRequestDocument.FacetedQueryRequest.FacetConfs.FacetConf[] facetConfsXml = facetedRequest.getFacetConfs().getFacetConfArray();
            FacetConf[] facetConfs = new FacetConf[facetConfsXml.length];
            
            Map queryOptions = new HashMap();
            FacetedQueryRequestDocument.FacetedQueryRequest.QueryOptions.QueryOption[] queryOptionArray =  facetedRequest.getQueryOptions().getQueryOptionArray();
            for (int i = 0; i < queryOptionArray.length; i++)
                queryOptions.put(queryOptionArray[i].getName(), queryOptionArray[i].getValue());            
            
            for (int i = 0; i < facetConfs.length; i++) {
                facetConfs[i] = new FacetConf();
                facetConfs[i].setIsFacet(facetConfsXml[i].getIsFacet());
                facetConfs[i].setMaxValues(facetConfsXml[i].getMaxValues());
                facetConfs[i].setSortAscending(facetConfsXml[i].getSortAscending());
                facetConfs[i].setSortOnValue(facetConfsXml[i].getSortOnValue());
                facetConfs[i].setType(FacetType.valueOf(facetConfsXml[i].getType()));
                Properties props = facetConfsXml[i].getProperties();
                if (props != null) {
                    Property[] propArray = props.getPropertyArray();
                    Map<String, String> facetProperties = new HashMap<String, String>();
                    facetConfs[i].setProperties(facetProperties);
                    for (Property prop : propArray) {
                        facetProperties.put(prop.getName(), prop.getValue());
                    }
                }
            }
            Locale locale = LocaleHelper.parseLocale(facetedRequest.getLocale());

            QueryManager queryManager = repository.getQueryManager();
            FacetedQueryResultDocument result = queryManager.performFacetedQuery(query, facetConfs, queryOptions, locale);
            result.save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
