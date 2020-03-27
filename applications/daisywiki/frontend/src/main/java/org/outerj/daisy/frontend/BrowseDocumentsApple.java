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
package org.outerj.daisy.frontend;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.environment.Request;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.docbrowser.DocbrowserConfiguration;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerx.daisy.x10.SearchResultDocument;

public class BrowseDocumentsApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Locale locale = frontEndContext.getLocale();
        SiteConf siteConf = frontEndContext.getSiteConf();
        Repository repository = frontEndContext.getRepository();

        String resource = appleRequest.getSitemapParameter("resource");

        String configParam = request.getParameter("config");
        DocbrowserConfiguration docbrowserConf=null;
        if( configParam == null )
        	configParam = "default";
        
    	Configuration config = frontEndContext.getConfigurationManager().getConfiguration(siteConf.getName(), "docbrowser-" + configParam);

        if( config != null){
        	// configuration of documentbrowser GUI: 
        	// which columns to show in result, which tabs to show as search params
        	docbrowserConf = new DocbrowserConfiguration(config);
        }else{
        	docbrowserConf = new DocbrowserConfiguration();
        }
        
        if (resource == null) {
            long branchId = RequestUtil.getBranchId(request, siteConf.getBranchId(), repository);
            long languageId = RequestUtil.getLanguageId(request, siteConf.getLanguageId(), repository);
            String enableFragmentIdParam = request.getParameter("enableFragmentId");
            boolean enableFragmentId = enableFragmentIdParam == null || enableFragmentIdParam.equals("true");

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("branchId", String.valueOf(branchId));
            viewData.put("languageId", String.valueOf(languageId));
            viewData.put("branch", repository.getVariantManager().getBranch(branchId, false).getName());
            viewData.put("language", repository.getVariantManager().getLanguage(languageId, false).getName());
            viewData.put("branches", repository.getVariantManager().getAllBranches(false).getArray());
            viewData.put("languages", repository.getVariantManager().getAllLanguages(false).getArray());
            viewData.put("versionMode", frontEndContext.getVersionMode());
            viewData.put("enableFragmentId", String.valueOf(enableFragmentId));
            viewData.put("siteConf", siteConf);
            viewData.put("mountPoint", getMountPoint());
            viewData.put("pageContext", frontEndContext.getPageContext("dialog"));
            viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/documentbrowser.xml"));
            viewData.put("config", docbrowserConf);
            viewData.put("configParam", configParam);
            appleResponse.sendPage("internal/genericPipe", viewData);
        } else {
            if (resource.equals("documents")) {
                VersionMode versionMode = VersionMode.get(request.getParameter("versionMode"));
                String query = getQuery(request, siteConf, repository, versionMode);
                QueryManager queryManager = repository.getQueryManager();
                SearchResultDocument result = queryManager.performQuery(query, locale);

                Map<String, Object> viewData = new HashMap<String, Object>();
                viewData.put("searchResult", result.getSearchResult());
                viewData.put("versionMode", versionMode);
                viewData.put("siteConf", siteConf);
                viewData.put("mountPoint", getMountPoint());
                viewData.put("pipeConf", GenericPipeConfig.templateOnlyPipe("resources/xml/documentbrowser_docs.xml"));
                appleResponse.sendPage("internal/genericPipe", viewData);
            } else {
                throw new ResourceNotFoundException("Unsupported resource: " + resource);
            }
        }
    }

    private String getQuery(Request request, SiteConf siteConf, Repository repository, VersionMode versionMode) throws Exception {
        String name = request.getParameter("name");
        String fulltextQuery = request.getParameter("fulltextQuery");
        String collections = request.getParameter("collections");
        long branchId = RequestUtil.getLongParameter(request, "branchId");
        long languageId = RequestUtil.getLongParameter(request, "languageId");
        boolean attachmentDocType = request.getParameter("attachmentDocType") != null;
        boolean imageDocType = request.getParameter("imageDocType") != null;

        StringBuilder query = new StringBuilder();
        query.append("select name, branch, language where ");

        if (fulltextQuery != null && fulltextQuery.length() > 0) {
            query.append("FullText(")
                .append(QueryHelper.formatString(fulltextQuery))
                .append(", ")
                .append("true".equals(request.getParameter("searchName"))?1:0)
                .append(", ")
                .append("true".equals(request.getParameter("searchContent"))?1:0)
                .append(", ")
                .append("true".equals(request.getParameter("searchFields"))?1:0)
                .append(", ")
                .append(branchId).append(", ").append(languageId)
                .append(")");
        } else {
            query.append("true");
        }

        boolean nonFullTextConditions = false;

        if (name != null && name.length() > 0) {
            query.append(" and name like ").append(QueryHelper.formatString(name));
            nonFullTextConditions = true;
        }

        if ("current".equals(collections)) {
            String collectionName = repository.getCollectionManager().getCollection(siteConf.getCollectionId(), false).getName();
            query.append(" and InCollection(").append(QueryHelper.formatString(collectionName)).append(")");
            nonFullTextConditions = true;
        }

        if (imageDocType || attachmentDocType) {
            query.append(" and (");
            boolean first = true;
            if (imageDocType) {
                query.append("documentType!='Image'");
                first = false;
            }
            if (attachmentDocType) {
                if (!first)
                    query.append(" and ");
                query.append("documentType!='Attachment'");
            }
            query.append(")");
            nonFullTextConditions = true;
        }

        if (nonFullTextConditions) {
            if (branchId != -1)
                query.append(" and branchId = ").append(branchId);
            if (languageId != -1)
                query.append(" and languageId = ").append(languageId);
        }

        query.append(" order by name");

        if (versionMode != null) { 
            query.append(" option point_in_time = '")
                .append(versionMode.toString())
                .append("'");
        }

        return query.toString();
    }
}
