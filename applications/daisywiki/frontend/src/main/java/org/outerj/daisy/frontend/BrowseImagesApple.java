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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VersionMode;
import org.outerx.daisy.x10.SearchResultDocument;

public class BrowseImagesApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Locale locale = frontEndContext.getLocale();
        SiteConf siteConf = frontEndContext.getSiteConf();
        Repository repository = frontEndContext.getRepository();

        String resource = appleRequest.getSitemapParameter("resource");
        if (resource == null) {
            long branchId = RequestUtil.getBranchId(request, siteConf.getBranchId(), repository);
            long languageId = RequestUtil.getLanguageId(request, siteConf.getLanguageId(), repository);

            DocumentCollection[] collections = repository.getCollectionManager().getCollections(false).getArray();
            Arrays.sort(collections);

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("branchId", String.valueOf(branchId));
            viewData.put("languageId", String.valueOf(languageId));
            viewData.put("branch", repository.getVariantManager().getBranch(branchId, false).getName());
            viewData.put("language", repository.getVariantManager().getLanguage(languageId, false).getName());
            viewData.put("collectionId", String.valueOf(siteConf.getCollectionId()));
            viewData.put("branches", repository.getVariantManager().getAllBranches(false).getArray());
            viewData.put("languages", repository.getVariantManager().getAllLanguages(false).getArray());
            viewData.put("versionMode", frontEndContext.getVersionMode());
            viewData.put("collections", collections);
            viewData.put("siteConf", siteConf);
            viewData.put("mountPoint", getMountPoint());
            viewData.put("pageContext", frontEndContext.getPageContext("dialog"));
            viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/imagebrowser.xml"));
            appleResponse.sendPage("internal/genericPipe", viewData);
        } else if (resource.equals("imagesByCollection")) {
            String collectionIdParam = appleRequest.getSitemapParameter("collectionId");
            long collectionId = -1;
            if (!collectionIdParam.equals("all")) {
                try {
                    collectionId = Long.parseLong(collectionIdParam);
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid collectionId request parameter: " + collectionIdParam);
                }
            }

            long branchId = RequestUtil.getBranchId(request, siteConf.getBranchId(), repository);
            long languageId = RequestUtil.getLanguageId(request, siteConf.getLanguageId(), repository);
            VersionMode versionMode = VersionMode.get(RequestUtil.getStringParameter(request, "versionMode"));

            StringBuilder query = new StringBuilder(400);
            query.append("select name, branch, language where HasPart('ImageData')");
            if (collectionId != -1)
                query.append(" and InCollection(").append(collectionId).append(")");
            query.append(" and branchId = ").append(branchId);
            query.append(" and languageId = ").append(languageId);
            query.append(" order by name");

            if (versionMode != null) {
                query.append(" option point_in_time = '")
                    .append(versionMode.toString())
                    .append("'");
            }
            
            SearchResultDocument queryResult = repository.getQueryManager().performQuery(query.toString(), locale);

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("versionMode", versionMode);
            viewData.put("mountPoint", getMountPoint());
            viewData.put("searchResult", queryResult.getSearchResult());
            viewData.put("pipeConf", GenericPipeConfig.templateOnlyPipe("resources/xml/documentbrowser_docs.xml"));

            appleResponse.sendPage("internal/genericPipe", viewData);
        } else {
            throw new ResourceNotFoundException("Unsupported resource: " + resource);
        }
    }
}
