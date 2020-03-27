/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.frontend.workflow;

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.WfTimer;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class WfTimerApple  extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        Locale locale = frontEndContext.getLocale();
        String timerId = appleRequest.getSitemapParameter("timerId");

        if (request.getMethod().equals("GET")) {
            // Show information about a timer
            WfTimer timer = workflowManager.getTimer(timerId, locale);

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("locale", locale);
            viewData.put("pageContext", frontEndContext.getPageContext());
            viewData.put("pageXml", new XmlObjectXMLizable(timer.getXml()));
            viewData.put("pipeConf", GenericPipeConfig.stylesheetPipe("daisyskin:xslt/workflow/wf_timer.xsl"));

            appleResponse.sendPage("internal/genericPipe", viewData);
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }
    }
}

