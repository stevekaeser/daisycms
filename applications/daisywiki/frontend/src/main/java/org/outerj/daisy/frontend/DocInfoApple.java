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
import java.util.Map;

import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.cocoon.xml.StringXMLizable;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.MultiXMLizable;

public class DocInfoApple extends AbstractDocumentApple implements StatelessAppleController {

    @Override
    protected boolean needsInitialisation() {
        return true;
    }

    @Override
    protected void processDocumentRequest(AppleRequest appleRequest,
            AppleResponse appleResponse) throws Exception {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("localeAsString", request.getAttribute("localeAsString"));
        viewData.put("documentId", String.valueOf(getDocumentId()));
        viewData.put("branch", String.valueOf(getBranchId()));
        viewData.put("language", String.valueOf(getLanguageId()));
        viewData.put("version", String.valueOf(getRequestedVersion()));
        viewData.put("pageContext", frontEndContext.getPageContext("plain", getRepository()));
        
        SaxBuffer publisherResponse = performPublisherRequest("docinfo", viewData);
        
        viewData.put("variantParams", getVariantParams());
        viewData.put("variantQueryString", getVariantQueryString());

        viewData.put("pipeConf", GenericPipeConfig.stylesheetPipe("daisyskin:xslt/docinfo.xsl"));
        
        MultiXMLizable data = new MultiXMLizable(
                publisherResponse,
                new StringXMLizable("<activePath>" + getRequestedNavigationPath() + "</activePath>")
        );
        viewData.put("pageXml", data);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }

    private SaxBuffer performPublisherRequest(String name, Map params) throws Exception {
        String pipe = "internal/" + name + "_pubreq.xml";
        WikiPublisherHelper wikiPublisherHelper = new WikiPublisherHelper(request, getContext(), getServiceManager());
        return wikiPublisherHelper.performPublisherRequest(pipe, params, "");
    }
}
