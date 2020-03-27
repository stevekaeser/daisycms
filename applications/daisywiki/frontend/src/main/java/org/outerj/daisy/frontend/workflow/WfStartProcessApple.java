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

import java.util.HashMap;
import java.util.Map;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.forms.formmodel.Widget;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.workflow.TaskUpdateData;
import org.outerj.daisy.workflow.VariableScope;
import org.outerj.daisy.workflow.WfNodeDefinition;
import org.outerj.daisy.workflow.WfProcessDefinition;
import org.outerj.daisy.workflow.WfProcessInstance;
import org.outerj.daisy.workflow.WfTaskDefinition;
import org.outerj.daisy.workflow.WfValueType;
import org.outerx.daisy.x10Workflowmeta.WorkflowAclInfoDocument;

/**
 * Controller for starting a new workflow process.
 */
public class WfStartProcessApple extends WfAbstractTaskApple {
    private String processDefinitionId;
    private boolean hasStartTask = false;
    private String processDefinitionLabel;

    String getPath() {
        return getMountPoint() + "/" + siteConf.getName() + "/workflow/start/" + getContinuationId();
    }

    String getDefaultDocumentLink(Request request) {
        String documentLink = request.getParameter("documentLink");
        if (documentLink != null && documentLink.trim().length() > 0)
            return documentLink;
        else
            return null;
    }

    String getDefaultDescription(Request request) {
        String documentName = request.getParameter("documentName");
        if (documentName != null && documentName.trim().length() > 0) {
            return processDefinitionLabel + " \"" + documentName + "\"";
        } else {
            return processDefinitionLabel;
        }
    }

    Object[] init(Request request) throws Exception {
        processDefinitionId = RequestUtil.getStringParameter(request, "processDefinitionId");
        WfProcessDefinition processDefinition = workflowManager.getProcessDefinition(processDefinitionId, locale);
        WfTaskDefinition taskDefinition =  processDefinition.getStartTaskDefinition();
        WfNodeDefinition nodeDefinition;
        if (taskDefinition != null) {
            nodeDefinition = taskDefinition.getNode();
            hasStartTask = true;
        } else {
            nodeDefinition = processDefinition.getStartNode();
        }

        viewData.put("isStartWorkflow", "true");

        processDefinitionLabel = processDefinition.getLabel().getText();

        return new Object[] {null, taskDefinition, nodeDefinition, null, null};
    }

    void finish(AppleRequest appleRequest, AppleResponse appleResponse) throws RepositoryException {
        Widget submitWidget = form.getSubmitWidget();

        String transitionName = null;
        if (submitWidget == null) {
            // default transition will be followed
        } else if (submitWidget.getId().equals("cancel")) {
            // TODO better location to send user back to?
            appleResponse.redirectTo(getMountPoint() + "/" + siteConf.getName() + "/");
            return;
        } else {
            transitionName = (String)submitWidget.getAttribute("transitionName");
        }

        TaskUpdateData taskUpdateData = hasStartTask ? getTaskUpdateData() : null;

        // If the start task has a variable daisy_site_hint, set it
        if (hasStartTask && taskDefinition.getVariable("daisy_site_hint", VariableScope.GLOBAL) != null) {
            taskUpdateData.setVariable("daisy_site_hint", VariableScope.GLOBAL, WfValueType.STRING, siteConf.getName());
        }

        WfProcessInstance process = workflowManager.startProcess(processDefinitionId, taskUpdateData, transitionName, locale);

        String returnTo = appleRequest.getCocoonRequest().getParameter("returnTo");
        if (returnTo != null) {
            ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
        } else {
            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("pageContext", FrontEndContext.get(appleRequest.getCocoonRequest()).getPageContext());
            viewData.put("locale", locale);
            viewData.put("wfProcessInstance", process);
            viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/wf_process_started.xml"));
    
            appleResponse.sendPage("internal/genericPipe", viewData);
        }
    }

    @Override
    protected boolean checkAcl() throws RepositoryException {
        WorkflowAclInfoDocument aclDoc = workflowManager.getAclInfo(null, null, processDefinitionId, false);
        return aclDoc.getWorkflowAclInfo().getCanStartProcess();
    }

    boolean loadInitialValues() {
        return true;
    }
}