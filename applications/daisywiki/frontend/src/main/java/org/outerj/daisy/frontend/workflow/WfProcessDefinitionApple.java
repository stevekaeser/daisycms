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
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.WfListHelper;
import org.outerj.daisy.workflow.WfProcessDefinition;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.outerx.daisy.x10Workflow.ProcessDefinitionsDocument;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

/**
 * Controller for showing the list of process definitions from
 * which to start a new process instance.
 */
public class WfProcessDefinitionApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Locale locale = frontEndContext.getLocale();
        Repository repository = frontEndContext.getRepository();
        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");

        List<WfProcessDefinition> processDefinitions = workflowManager.getAllLatestProcessDefinitions(locale);
        ProcessDefinitionsDocument processDefsXml = WfListHelper.getProcessDefinitionsAsXml(processDefinitions);

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("wfProcessDefinitions", new XmlObjectXMLizable(processDefsXml));
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("locale", locale);

        String documentLink = request.getParameter("documentLink");
        String documentName = request.getParameter("documentName");
        if (documentLink != null && documentLink.trim().length() > 0 && documentName != null && documentName.trim().length() > 0) {
            viewData.put("documentLink", documentLink);
            viewData.put("documentName", documentName);
        }

        GenericPipeConfig pipeConf = new GenericPipeConfig();
        pipeConf.setTemplate("resources/xml/wf_processdef_selection_page.xml");
        pipeConf.setStylesheet("daisyskin:xslt/workflow/wf_processdef_selection.xsl");
        viewData.put("pipeConf", pipeConf);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }
}
