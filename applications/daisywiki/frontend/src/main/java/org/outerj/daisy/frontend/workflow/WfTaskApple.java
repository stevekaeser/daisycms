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

import java.util.Locale;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.WorkflowManager;

public class WfTaskApple  extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        Locale locale = frontEndContext.getLocale();
        SiteConf siteConf = frontEndContext.getSiteConf();
        String taskId = appleRequest.getSitemapParameter("taskId");

        if (request.getMethod().equals("GET")) {
            // later on we could implement a "view task info" here
            throw new HttpMethodNotAllowedException(request.getMethod());
        } else if (request.getMethod().equals("POST")) {
            String action = RequestUtil.getStringParameter(request, "action");
            if (action.equals("requestPooledTask")) {
                workflowManager.requestPooledTask(taskId, locale);
            } else if (action.equals("assignBackToPools")) {
                workflowManager.unassignTask(taskId, locale);
            } else {
                throw new Exception("Invalid value for action parameter: " + action);
            }
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }

        String returnTo = request.getParameter("returnTo");
        if (returnTo != null)
            ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
        else
            ResponseUtil.safeRedirect(appleRequest, appleResponse, getMountPoint() + "/" + siteConf.getName() + "/workflow/tasks");
    }
}
