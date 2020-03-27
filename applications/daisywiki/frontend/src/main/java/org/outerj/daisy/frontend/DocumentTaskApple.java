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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.xmlbeans.XmlCursor;
import org.outerj.daisy.doctaskrunner.DocumentTaskManager;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.UserManager;
import org.outerx.daisy.x10Doctaskrunner.TaskDocument;
import org.outerx.daisy.x10Doctaskrunner.TasksDocument;

public class DocumentTaskApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();

        if (repository.isInRole("guest") && repository.getActiveRoleIds().length == 1)
            throw new Exception("Document Task functionality not available for guest users.");

        DocumentTaskManager taskManager = (DocumentTaskManager)repository.getExtension("DocumentTaskManager");
        TasksDocument tasksDocument = taskManager.getTasks().getXml();

        String idParam = appleRequest.getSitemapParameter("id");
        if (idParam != null) {
            long id;
            try {
                id = Long.parseLong(idParam);
            } catch (NumberFormatException e) {
                throw new Exception("Invalid task ID: " + idParam);
            }
            if (request.getMethod().equals("POST")) {
                String action = RequestUtil.getStringParameter(request, "action");
                if (action.equals("interrupt")) {
                    taskManager.interruptTask(id);
                } else if (action.equals("delete")) {
                    taskManager.deleteTask(id);
                } else {
                    throw new Exception("Unsupported action parameter value: " + action);
                }
                appleResponse.redirectTo(getMountPoint() + "/doctask");
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        } else if (request.getMethod().equals("GET")) {
            Locale locale = frontEndContext.getLocale();
            annotateTasks(tasksDocument.getTasks().getTaskList(), repository, locale);

            Map<String, Object> viewData = new HashMap<String, Object>();
            GenericPipeConfig pipeConfig = GenericPipeConfig.templatePipe("resources/xml/doctasklist.xml");
            pipeConfig.setStylesheet("daisyskin:xslt/documenttask_list.xsl");
            viewData.put("documentTasks", new XmlObjectXMLizable(tasksDocument));
            viewData.put("allowedActionTypes", taskManager.getAllowedTasks());
            viewData.put("pageContext", frontEndContext.getPageContext());
            viewData.put("pipeConf", pipeConfig);

            appleResponse.sendPage("internal/genericPipe", viewData);
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }
    }

    private void annotateTasks(List<TaskDocument.Task> tasksXml, Repository repository, Locale locale) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
        UserManager userManager = repository.getUserManager();
        for (TaskDocument.Task taskXml : tasksXml) {
            long ownerId = taskXml.getOwnerId();
            String ownerName;
            try {
                ownerName = userManager.getUserDisplayName(ownerId);
            } catch (RepositoryException e) {
                ownerName = "(error retrieving owner name)";
            }
            Date startedAt = taskXml.getStartedAt().getTime();
            Date finishedAt = taskXml.getFinishedAt() != null ? taskXml.getFinishedAt().getTime() : null;
            XmlCursor cursor = taskXml.newCursor();
            cursor.toNextToken();
            cursor.insertAttributeWithValue("ownerName", ownerName);
            cursor.insertAttributeWithValue("startedAtFormatted", dateFormat.format(startedAt));
            if (finishedAt != null)
                cursor.insertAttributeWithValue("finishedAtFormatted", dateFormat.format(finishedAt));
            cursor.dispose();
        }
    }
}
