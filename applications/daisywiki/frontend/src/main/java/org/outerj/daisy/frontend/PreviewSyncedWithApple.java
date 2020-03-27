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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.repository.AvailableVariant;
import org.outerj.daisy.repository.Repository;

public class PreviewSyncedWithApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        SiteConf siteConf = frontEndContext.getSiteConf();
        Repository repository = frontEndContext.getRepository();

        String documentId = RequestUtil.getStringParameter(request, "documentId");
        long branchId = RequestUtil.getBranchId(RequestUtil.getStringParameter(request, "branch"), -1, repository);
        long languageId = RequestUtil.getLanguageId(RequestUtil.getStringParameter(request, "language"), -1, repository);
        long referenceLanguageId = RequestUtil.getLanguageId(RequestUtil.getStringParameter(request, "referenceLanguage"), -1, repository);

        String syncedWithLanguageParam = RequestUtil.getStringParameter(request, "syncedWithLanguage", "-1");
        long syncedWithLanguageId = -1;
        if (syncedWithLanguageParam != null && !syncedWithLanguageParam.equals("-1")) {
            syncedWithLanguageId = RequestUtil.getLanguageId(syncedWithLanguageParam, -1L, repository);
        }
        long syncedWithVersionId = RequestUtil.getLongParameter(request, "syncedWithVersionId", -1L);

        List<AvailableVariant> languageVariants = new ArrayList<AvailableVariant>();
        for (AvailableVariant variant: repository.getAvailableVariants(documentId).getArray()) {
            if (variant.getBranchId() == branchId) {
                languageVariants.add(variant);
            }
        }

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("documentId", documentId);
        viewData.put("branchId", String.valueOf(branchId));
        viewData.put("languageId", String.valueOf(languageId));
        viewData.put("branch", repository.getVariantManager().getBranch(branchId, false).getName());
        viewData.put("language", repository.getVariantManager().getLanguage(languageId, false).getName());
        viewData.put("syncedWithLanguageId", syncedWithLanguageId);
        viewData.put("syncedWithVersionId", syncedWithVersionId);
        viewData.put("referenceLanguageId", referenceLanguageId);
        viewData.put("availableLanguageVariants", languageVariants);
        viewData.put("siteConf", siteConf);
        viewData.put("pageContext", frontEndContext.getPageContext("dialog"));
        viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/preview-synced-with.xml"));
        appleResponse.sendPage("internal/genericPipe", viewData);
    }

}
