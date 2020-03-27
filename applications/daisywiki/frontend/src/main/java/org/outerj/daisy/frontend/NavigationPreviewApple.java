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

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.navigation.NavigationManager;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.xml.SaxBuffer;

import java.util.Map;
import java.util.HashMap;

public class NavigationPreviewApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        String navigationXml = RequestUtil.getStringParameter(request, "navigationXml", null);

        Repository repository = frontEndContext.getRepository();
        SiteConf siteConf = frontEndContext.getSiteConf();

        long branchId = RequestUtil.getBranchId(request, siteConf.getBranchId(), repository);
        long languageId = RequestUtil.getLanguageId(request, siteConf.getLanguageId(), repository);

        // Note: do this via POST since navigation XML can get large
        SaxBuffer result = new SaxBuffer();
        NavigationManager navigationManager = (NavigationManager)repository.getExtension("NavigationManager");
        navigationManager.generatePreviewNavigationTree(result, navigationXml, branchId, languageId, frontEndContext.getLocale());

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("pageXml", result);

        GenericPipeConfig pipeConf = new GenericPipeConfig();
        pipeConf.setStylesheet("daisyskin:xslt/navigation_preview.xsl");
        pipeConf.setApplyLayout(false);
        pipeConf.setApplyI18n(false);
        viewData.put("pipeConf", pipeConf);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }
}
