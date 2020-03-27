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
import org.outerj.daisy.publisher.GlobalPublisherException;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.xml.SaxBuffer;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;

import java.util.Map;
import java.util.HashMap;

public class VariablesListApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        PublisherRequestDocument pubReqDoc = PublisherRequestDocument.Factory.newInstance();
        PublisherRequestDocument.PublisherRequest pubReq = pubReqDoc.addNewPublisherRequest();
        pubReq.setLocale(frontEndContext.getLocaleAsString());

        pubReq.setVariablesConfig(PublisherRequestHelper.getVariablesConfig(frontEndContext));

        pubReq.addNewVariablesList();

        Publisher publisher = (Publisher)frontEndContext.getRepository().getExtension("Publisher");

        String result = "ok";
        SaxBuffer pubResponse = new SaxBuffer();
        try {
            publisher.processRequest(pubReqDoc, pubResponse);
        } catch (GlobalPublisherException e) {
            result = "error";
        }

        MultiXMLizable pageXml = new MultiXMLizable();
        pageXml.add(new SimpleElementXMLizable("result", result));
        pageXml.add(pubResponse);

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageXml", pageXml);

        GenericPipeConfig pipeConfig = new GenericPipeConfig();
        pipeConfig.setStylesheet("resources/xslt/variableslist.xsl");
        pipeConfig.setApplyLayout(false);
        pipeConfig.setXmlSerializer();
        viewData.put("pipeConf", pipeConfig);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }
}
