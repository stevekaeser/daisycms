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
package org.outerj.daisy.frontend;

import org.outerj.daisy.frontend.util.*;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.repository.Repository;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.xml.SaxBuffer;
import org.outerx.daisy.x10Publisher.*;

import java.util.Map;
import java.util.HashMap;

/**
 * Shows a page to the user with the variable documents for the current site.
 */
public class VariablesIndexApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();

        if (repository.isInRole("guest") && repository.getActiveRoleIds().length == 1)
            throw new Exception("This functionality is not available for guest users.");

        MultiXMLizable pageXml = new MultiXMLizable();

        // Get information about the variable documents
        VariablesConfigType variablesConfig = PublisherRequestHelper.getVariablesConfig(frontEndContext);
        if (variablesConfig.isSetVariableSources()) {
            PublisherRequestDocument pubReqDoc = PublisherRequestHelper.createTemplateRequest(frontEndContext);
            PublisherRequestDocument.PublisherRequest pubReq = pubReqDoc.getPublisherRequest();

            GroupDocument.Group group = pubReq.addNewGroup();
            group.setId("variableDocuments");

            for (VariantKeyType varDoc : variablesConfig.getVariableSources().getVariableDocumentList()) {
                DocumentDocument.Document doc = group.addNewDocument();
                doc.setId(varDoc.getId());
                doc.setBranch(varDoc.getBranch());
                doc.setLanguage(varDoc.getLanguage());

                doc.addNewAnnotatedDocument();
                doc.addNewAclInfo();
            }

            pubReq.setNavigationTreeArray(PublisherRequestHelper.getNavigationTreeArray(frontEndContext));

            Publisher publisher = (Publisher)repository.getExtension("Publisher");
            SaxBuffer buffer = new SaxBuffer();
            publisher.processRequest(pubReqDoc, buffer);
            pageXml.add(buffer);
        }


        // Display the page

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageXml", pageXml);
        viewData.put("pageContext", frontEndContext.getPageContext());

        GenericPipeConfig pipeConfig = new GenericPipeConfig();
        pipeConfig.setStylesheet("daisyskin:xslt/variables_index.xsl");
        viewData.put("pipeConf", pipeConfig);

        appleResponse.sendPage("internal/genericPipe", viewData);

    }
}
