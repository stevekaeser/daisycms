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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.WidgetState;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.workflow.WfActorKey;
import org.outerj.daisy.workflow.WfTask;
import org.outerj.daisy.workflow.WorkflowManager;

public class WfAssignTaskApple extends AbstractDaisyApple implements Serviceable {
    private ServiceManager serviceManager;
    private boolean init = false;
    private Form form;
    private Locale locale;
    private Repository repository;
    private WorkflowManager workflowManager;
    private String taskId;
    private String returnTo;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            repository = frontEndContext.getRepository();
            workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
            locale = frontEndContext.getLocale();
            SiteConf siteConf = frontEndContext.getSiteConf();

            taskId = appleRequest.getSitemapParameter("taskId");
            form = FormHelper.createForm(serviceManager, "resources/form/wf_assign_definition.xml");
            WfTask task = workflowManager.getTask(taskId, locale);
            Field scopeField = (Field)form.getChild("scope");
            if (task.hasSwimlane()) {
                scopeField.setValue("swimlane");
            } else {
                scopeField.setValue("task");
                scopeField.setState(WidgetState.DISABLED);
            }

            init = true;

            String mountPoint = frontEndContext.getMountPoint();
            String siteName = siteConf.getName();
            appleResponse.redirectTo(mountPoint + "/" + siteName + "/workflow/task/" + taskId + "/assign/" + getContinuationId());

            returnTo = request.getParameter("returnTo");
            if (returnTo == null)
                returnTo = mountPoint + "/" + siteName + "/workflow/tasks";

            return;
        }

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pools", workflowManager.getPoolManager().getPools());
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("CocoonFormsInstance", form);

        if (request.getMethod().equals("GET")) {
            appleResponse.sendPage("Form-wf_assign-Pipe", viewData);
        } else if (request.getMethod().equals("POST")) {
            boolean finished = form.process(new FormContext(request, locale));
            if (finished) {
                Widget submitWidget = form.getSubmitWidget();
                String submitWidgetId = submitWidget != null ? submitWidget.getId() : null;
                if (!"cancel".equals(submitWidgetId)) {
                    boolean overwriteSwimlane = form.getChild("scope").getValue().equals("swimlane");
                    WfActorKey actorKey;
                    String actorCase = (String)form.getChild("actorCase").getValue();
                    if (actorCase.equals("user")) {
                        String userLogin = (String)form.lookupWidget("actor/user/user").getValue();
                        long userId;
                        try {
                            userId = repository.getUserManager().getUserId(userLogin);
                        } catch (RepositoryException e) {
                            throw new RuntimeException("Error trying to retrieve user ID for user login \"" + userLogin + "\".", e);
                        }
                        actorKey = new WfActorKey(userId);
                    } else if (actorCase.equals("pool")) {
                        Object[] pools = (Object[])form.lookupWidget("actor/pool/pool").getValue();
                        List<Long> actorKeys = new ArrayList<Long>(pools.length);
                        for (Object pool : pools) actorKeys.add((Long)pool);
                        actorKey = new WfActorKey(actorKeys);
                    } else {
                        throw new Exception("Unexpected value for actorCase: " + actorCase);
                    }
                    workflowManager.assignTask(taskId, actorKey, overwriteSwimlane, locale);
                }
                ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
            } else {
                appleResponse.sendPage("Form-wf_assign-Pipe", viewData);
            }
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }

    }
}
