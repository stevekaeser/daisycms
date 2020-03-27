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

import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VariantKeys;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.query.RemoteEvaluationContext;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10.SearchResultDocument;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;

public class QueryHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/query";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support)
            throws Exception {
        // Support also POST to be able to handle very long queries
        if (request.getMethod().equals(HttpConstants.GET) || request.getMethod().equals(HttpConstants.POST)) {
            String query = HttpUtil.getStringParam(request, "q");
            String localeString = HttpUtil.getStringParam(request, "locale");
            String returnKeysParam = request.getParameter("returnKeys");
            boolean returnKeys = returnKeysParam != null && returnKeysParam.equals("true");
            String extraCond = request.getParameter("extraCondition");
            String extraOrderBy = request.getParameter("extraOrderBy");

            QueryManager queryManager = repository.getQueryManager();

            Map<String, String> queryOptions = null;
            String[] queryOptionsList = request.getParameterValues("queryOption");
            if (queryOptionsList != null && queryOptionsList.length > 0) {
                queryOptions = new HashMap<String, String>();
                for (String option : queryOptionsList) {
                    int eqPos = option.indexOf('=');
                    if (eqPos == -1)
                        throw new RuntimeException("Missing equal (=) sign in query option: " + option);
                    queryOptions.put(option.substring(0, eqPos), option.substring(eqPos + 1));
                }
            }
            RemoteEvaluationContext remoteEvaluationContext = extractRemoteEvaluationContext(request);
            Locale locale = LocaleHelper.parseLocale(localeString);
            if (returnKeys) {
                VariantKey[] keys = queryManager.performQueryReturnKeys(query, extraCond, queryOptions, locale, remoteEvaluationContext);
                new VariantKeys(keys).getXml().save(response.getOutputStream());
            } else {
                SearchResultDocument searchResultDocument = queryManager.performQuery(query, extraCond, extraOrderBy, queryOptions, locale, remoteEvaluationContext);
                searchResultDocument.save(response.getOutputStream());
            }
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    private RemoteEvaluationContext extractRemoteEvaluationContext(HttpServletRequest request) throws Exception {        
        RemoteEvaluationContext remoteEvaluationContext = new RemoteEvaluationContext();        
        
        String[] contextDocList = request.getParameterValues("contextDocument");
        if (contextDocList != null) {
            for (String contextDocKey : contextDocList) {
                remoteEvaluationContext.pushContextDocument(QueryHandler.parseVersionKey(contextDocKey));
            }
        }
        
        return remoteEvaluationContext;

    }
    
    private static VersionKey parseVersionKey(String link) {
        Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(link);
        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid link: " + link);

        String documentId = matcher.group(1);
        String branchInput = matcher.group(2);
        String languageInput = matcher.group(3);
        String versionInput = matcher.group(4);
        
        long branchId = -1, languageId = -1, versionId = -1;
        
        if (branchInput != null && branchInput.length() > 0) {
            branchId = Long.parseLong(branchInput);
        }

        if (languageInput != null && languageInput.length() > 0) {
            languageId = Long.parseLong(languageInput);
        }
        
        if (versionInput != null && versionInput.length() > 0) {
            versionId = Long.parseLong(versionInput);
        }

        return new VersionKey(documentId, branchId, languageId, versionId);
    }
}
