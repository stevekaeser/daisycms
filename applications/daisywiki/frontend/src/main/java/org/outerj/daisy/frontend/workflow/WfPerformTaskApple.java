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

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.forms.formmodel.Widget;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.frontend.components.config.ConfigurationManager;
import org.outerj.daisy.frontend.editor.InlineFormConfig;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldTypeUse;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerj.daisy.workflow.TaskUpdateData;
import org.outerj.daisy.workflow.WfNodeDefinition;
import org.outerj.daisy.workflow.WfProcessDefinition;
import org.outerj.daisy.workflow.WfTask;
import org.outerj.daisy.workflow.WfTaskDefinition;
import org.outerj.daisy.workflow.WfVariable;
import org.outerj.daisy.workflow.WfVersionKey;
import org.outerx.daisy.x10Workflowmeta.WorkflowAclInfoDocument;

public class WfPerformTaskApple extends WfAbstractTaskApple {
    private String taskId;

    String getPath() {
        return getMountPoint() + "/" + siteConf.getName() + "/workflow/performtask/" + getContinuationId() + "/";
    }

    Object[] init(Request request) throws Exception {
        taskId = RequestUtil.getStringParameter(request, "taskId");
        WfTask task = workflowManager.getTask(taskId, locale);
        
        // the task is either not assigned or assigned to a pool
        if (task.getActorId() == -1 && task.hasPools()) { 
            // try to assign the task to the current user should he/she be in the pool
            workflowManager.requestPooledTask(task.getId(), locale);
            // now we must refetch the task or else we would be working with old data
            task = workflowManager.getTask(taskId, locale);
        }

        WfTaskDefinition taskDefinition =  task.getDefinition();
        WfNodeDefinition nodeDefinition = taskDefinition.getNode();

        Document document = null;
        InlineFormConfig formConfig = null;
        
        WfVariable documentVariable = (WfVariable)task.getVariable("daisy_document");
        if (documentVariable != null) {
            WfVersionKey versionKey = (WfVersionKey) documentVariable.getValue();
            if (versionKey != null) {
                if (repository.getAccessManager().getAclInfoOnLive(repository.getUserId(), repository.getActiveRoleIds(), versionKey.getVariantKey()).isAllowed(AclPermission.WRITE)) {
                    document = repository.getDocument(versionKey.getVariantKey(), true);
                    formConfig = getFormConfig(document, taskDefinition, nodeDefinition);
                }
            }
        }

        return new Object[] {task, taskDefinition, nodeDefinition, (formConfig==null?null:document) , formConfig};
    }

    private InlineFormConfig getFormConfig(Document document, WfTaskDefinition taskDefinition, WfNodeDefinition nodeDefinition) throws Exception {
        ConfigurationManager configurationManager = null;
        try {
            configurationManager = (ConfigurationManager)serviceManager.lookup(ConfigurationManager.ROLE);
            Configuration wfmapping = configurationManager.getConfiguration("wfmapping");
            if (wfmapping == null)
                return null;
            
            WfProcessDefinition processDefinition = workflowManager.getProcessDefinition(nodeDefinition.getProcessDefinitionId(), locale);
            
            DocumentType documentType = repository.getRepositorySchema().getDocumentTypeById(document.getDocumentTypeId(), false);
            for (Configuration mapping: wfmapping.getChildren("mapping")) {
                String processDefinitionName = mapping.getAttribute("processDefinitionName", null);
                if (processDefinitionName != null && !processDefinitionName.equals(processDefinition.getName()))
                    continue;
    
                String taskName = mapping.getAttribute("taskName", null);
                if (taskName != null && !taskName.equals(taskDefinition.getName()))
                    continue;
    
                String nodeName = mapping.getAttribute("nodeName", null);
                if (nodeName != null && !nodeName.equals(nodeDefinition.getName()))
                    continue;
                
                String documentTypeAttr = mapping.getAttribute("documentType", null);
                if (documentTypeAttr != null && !documentTypeAttr.equals(documentType.getName()))
                    continue;
    
                return buildInlineFormConfig(mapping, documentType); 
            }
        } finally {
            if (configurationManager != null)
                serviceManager.release(configurationManager);
        }
        
        return null;
        
    }

    private InlineFormConfig buildInlineFormConfig(Configuration mapping, DocumentType documentType) throws ConfigurationException {
        InlineFormConfig formConfig = new InlineFormConfig();
        if (mapping.getChild("parts") != null) {
            for (Configuration part: mapping.getChild("parts").getChildren("part")) {
                PartTypeUse partTypeUse = documentType.getPartTypeUse(part.getValue());
                boolean required = part.getAttributeAsBoolean("required", partTypeUse.isRequired());
                boolean editable = part.getAttributeAsBoolean("editable", partTypeUse.isEditable());
                if ((required ^ partTypeUse.isRequired()) || (editable ^ partTypeUse.isEditable())) {
                    formConfig.addPart(new PartTypeUseWrapper(partTypeUse, required, editable));
                } else {
                    formConfig.addPart(partTypeUse);
                }
            }
        }
        if (mapping.getChild("fields") != null) {
            for (Configuration field: mapping.getChild("fields").getChildren("field")) {
                FieldTypeUse fieldTypeUse = documentType.getFieldTypeUse(field.getValue());
                if (fieldTypeUse == null) {
                    continue;
                }
                boolean required = field.getAttributeAsBoolean("required", fieldTypeUse.isRequired());
                boolean editable = field.getAttributeAsBoolean("editable", fieldTypeUse.isEditable());
                if ((required ^ fieldTypeUse.isRequired()) || (editable ^ fieldTypeUse.isEditable())) {
                    formConfig.addField(new FieldTypeUseWrapper(fieldTypeUse, required, editable));
                } else {
                    formConfig.addField(fieldTypeUse);
                }
            }
        }
        return formConfig;
    }

    void finish(AppleRequest appleRequest, AppleResponse appleResponse) throws RepositoryException {
        Widget submitWidget = form.getSubmitWidget();

        String transitionName = (String)form.lookupWidget("transitionName").getValue();
        if (submitWidget.getId().equals("cancel")) {
            // do nothing
        } else { // save
            TaskUpdateData taskUpdateData = getTaskUpdateData();

            if (transitionName == null)
                workflowManager.updateTask(taskId, taskUpdateData, locale);
            else
                workflowManager.endTask(taskId, taskUpdateData, transitionName, locale);
        }

        String returnTo = appleRequest.getCocoonRequest().getParameter("returnTo");
        if (returnTo != null) {
            ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
        } else {
            ResponseUtil.safeRedirect(appleRequest, appleResponse, getMountPoint() + "/" + siteConf.getName() + "/workflow/tasks");
        }
    }
    
    @Override
    protected boolean checkAcl() throws RepositoryException {
        WorkflowAclInfoDocument aclDoc = workflowManager.getAclInfo(taskId, null, null, false);
        return aclDoc.getWorkflowAclInfo().getCanUpdateTask();
    }

    boolean loadInitialValues() {
        return false;
    }
}
