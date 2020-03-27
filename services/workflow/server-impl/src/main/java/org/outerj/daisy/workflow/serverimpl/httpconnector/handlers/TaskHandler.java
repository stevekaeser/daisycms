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
package org.outerj.daisy.workflow.serverimpl.httpconnector.handlers;

import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.TaskUpdateData;
import org.outerj.daisy.workflow.WfTask;
import org.outerj.daisy.workflow.WfActorKey;
import org.outerj.daisy.workflow.commonimpl.WfXmlHelper;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerx.daisy.x10Workflow.UpdateTaskDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;

public class TaskHandler extends AbstractWorkflowRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        String id = (String)matchMap.get("1");
        Locale locale = WfHttpUtil.getLocale(request);

        WfTask task = null;
        if (request.getMethod().equals(HttpConstants.GET)) {
            task = workflowManager.getTask(id, locale);
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            String action = request.getParameter("action");

            if (action == null) {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                UpdateTaskDocument document = UpdateTaskDocument.Factory.parse(request.getInputStream(), xmlOptions);
                UpdateTaskDocument.UpdateTask xml = document.getUpdateTask();

                boolean end = xml.isSetEndTask() ? xml.getEndTask() : false;

                TaskUpdateData taskUpdateData;
                if (xml.isSetTaskUpdateData()) {
                    taskUpdateData = WfXmlHelper.instantiateTaskUpdateData(xml.getTaskUpdateData());
                } else {
                    taskUpdateData = new TaskUpdateData();
                }

                if (end) {
                    task = workflowManager.endTask(id, taskUpdateData, xml.getTransitionName(), locale);
                } else {
                    task = workflowManager.updateTask(id, taskUpdateData, locale);
                }
            } else if (action.equals("requestPooledTask")) {
                task = workflowManager.requestPooledTask(id, locale);
            } else if (action.equals("unassignTask")) {
                task = workflowManager.unassignTask(id, locale);
            } else if (action.equals("assignTask")) {
                boolean overwriteSwimlane = HttpUtil.getBooleanParam(request, "overwriteSwimlane");
                String actorType = HttpUtil.getStringParam(request, "actorType");
                WfActorKey actor;
                if (actorType.equals("user")) {
                    long actorId = HttpUtil.getLongParam(request, "actor");
                    actor = new WfActorKey(actorId);
                } else if (actorType.equals("pools")) {
                    String[] actorIds = request.getParameterValues("actor");
                    List<Long> parsedActorIds = new ArrayList<Long>();
                    for (String actorId: actorIds) {
                        if (actorId.length() > 0) {
                                parsedActorIds.add(Long.parseLong(actorId));
                        }
                    }
                    actor = new WfActorKey(parsedActorIds);
                } else {
                    HttpUtil.sendCustomError("Invalid value for actorType parameter: " + actorType, HttpConstants._400_Bad_Request, response);
                    return;
                }
                task = workflowManager.assignTask(id, actor, overwriteSwimlane, locale);
            } else {
                HttpUtil.sendCustomError("Invalid value for action parameter: " + action, HttpConstants._400_Bad_Request, response);
            }
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }

        if (task != null)
            task.getXml().save(response.getOutputStream());
    }

    public String getPathPattern() {
        return "/task/*";
    }
}