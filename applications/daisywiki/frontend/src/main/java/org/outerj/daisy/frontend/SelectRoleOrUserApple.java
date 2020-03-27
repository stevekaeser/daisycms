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
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.user.UserManager;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.xmlbeans.XmlObject;

import java.util.Map;
import java.util.HashMap;

public class SelectRoleOrUserApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        if (repository.isInRole("guest") && repository.getActiveRoleIds().length == 1) {
            throw new Exception("User or role listings not available for users acting in guest role.");
        }
        UserManager userManager = repository.getUserManager();
        String type = appleRequest.getSitemapParameter("type");
        XmlObject data;
        if (type.equals("role")) {
            data = userManager.getRoles().getXml();
        } else if (type.equals("user")) {
            data = userManager.getPublicUserInfos().getXml();
        } else {
            throw new Exception("Invalid value for type parameter: " + type);
        }

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("pageXml", new XmlObjectXMLizable(data));

        GenericPipeConfig pipeConf = new GenericPipeConfig();
        pipeConf.setStylesheet("daisyskin:xslt/select_" + type + ".xsl");
        pipeConf.setApplyLayout(false);
        viewData.put("pipeConf", pipeConf);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }
}
