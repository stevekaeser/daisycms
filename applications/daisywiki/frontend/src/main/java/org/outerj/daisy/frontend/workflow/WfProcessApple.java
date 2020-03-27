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

import org.outerj.daisy.frontend.util.*;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.workflow.*;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.xmlbeans.XmlObject;

import java.util.*;

public class WfProcessApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        Locale locale = frontEndContext.getLocale();
        String localeAsString = frontEndContext.getLocaleAsString();
        String processId = appleRequest.getSitemapParameter("processId");

        if (request.getMethod().equals("GET")) {
            // Show information about a process
            WfProcessInstance processInstance = workflowManager.getProcess(processId, locale);
            XmlObject timersXml = WfListHelper.getTimersAsXml(getTimersForProcess(processId, workflowManager, locale));

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("locale", locale);
            viewData.put("localeAsString", localeAsString);
            viewData.put("pageContext", frontEndContext.getPageContext());
            viewData.put("pageXml", new MultiXMLizable(new XmlObjectXMLizable(processInstance.getXml()), new XmlObjectXMLizable(timersXml)));
            viewData.put("template", "resources/xml/page.xml");

            GenericPipeConfig pipeConfig = new GenericPipeConfig();
            pipeConfig.setStylesheet("daisyskin:xslt/workflow/wf_process.xsl");
            viewData.put("pipeConf", pipeConfig);

            appleResponse.sendPage("internal/genericPipe", viewData);
        } else if (request.getMethod().equals("POST")) {
            // suspend, resume, delete a process
            String action = RequestUtil.getStringParameter(request, "action");
            String returnTo = RequestUtil.getStringParameter(request, "returnTo");
            String returnToLabel = RequestUtil.getStringParameter(request, "returnToLabel");

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("returnTo", returnTo);
            viewData.put("returnToLabel", returnToLabel);
            viewData.put("locale", locale);
            viewData.put("localeAsString", localeAsString);
            viewData.put("pageContext", frontEndContext.getPageContext());

            String confirmationTemplate;
            if (action.equals("suspend")) {
                workflowManager.suspendProcess(processId, locale);
                confirmationTemplate = "wf_process_suspend_success";
            } else if (action.equals("resume")) {
                workflowManager.resumeProcess(processId, locale);
                confirmationTemplate = "wf_process_resume_success";
            } else if (action.equals("delete")) {
                workflowManager.deleteProcess(processId);
                confirmationTemplate = "wf_process_delete_success";
            } else {
                throw new Exception("Invalid value for action parameter: " + action);
            }

            viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/" + confirmationTemplate + ".xml"));
            appleResponse.sendPage("internal/genericPipe", viewData);
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }
    }

    private List<WfTimer> getTimersForProcess(String processId, WorkflowManager workflowManager, Locale locale) throws RepositoryException {
        QueryConditions queryConditions = new QueryConditions();
        queryConditions.addCondition("process.id", WfValueType.ID, "eq", processId);

        List<QueryOrderByItem> orderByItems = new ArrayList<QueryOrderByItem>();
        orderByItems.add(new QueryOrderByItem("timer.id", QueryValueSource.PROPERTY, SortOrder.ASCENDING));

        return workflowManager.getTimers(queryConditions, orderByItems, -1, -1, locale);
    }
}
