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
package org.outerj.daisy.doctaskrunner.serverimpl.httphandlers;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.doctaskrunner.DocumentSelection;
import org.outerj.daisy.doctaskrunner.DocumentTaskManager;
import org.outerj.daisy.doctaskrunner.TaskException;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.doctaskrunner.spi.TaskSpecificationImpl;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Doctaskrunner.TaskCreatedDocument;
import org.outerx.daisy.x10Doctaskrunner.TaskDescriptionDocument;
import org.outerx.daisy.x10Doctaskrunner.TaskDescriptionDocument.TaskDescription.DocumentSelection.Enumeration.Docvariant;

public class TasksHandler extends AbstractDocTaskRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        DocumentTaskManager taskManager = (DocumentTaskManager)repository.getExtension("DocumentTaskManager");
        if (request.getMethod().equals(HttpConstants.GET)) {
            taskManager.getTasks().getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            TaskDescriptionDocument taskDescriptionDocument = TaskDescriptionDocument.Factory.parse(request.getInputStream(), xmlOptions);
            TaskDescriptionDocument.TaskDescription taskDescription = taskDescriptionDocument.getTaskDescription();
            TaskDescriptionDocument.TaskDescription.Specification specXml = taskDescription.getSpecification();
            TaskDescriptionDocument.TaskDescription.Specification.Action actionXml = specXml.getAction();

            boolean stopOnFirstError = taskDescription.getSpecification().getStopOnFirstError();
            String description = taskDescription.getSpecification().getDescription();
            int retryCount = taskDescription.getSpecification().getMaxTryCount();
            int retryInterval = taskDescription.getSpecification().getRetryInterval();
        
            String actionType = actionXml.getType();
            String parameters = actionXml.getParameters();

            TaskSpecification taskSpecification = new TaskSpecificationImpl(description, actionType, parameters, stopOnFirstError, retryCount, retryInterval);

            DocumentSelection documentSelection;
            if (taskDescription.getDocumentSelection().isSetQuery()) {
                String query = taskDescription.getDocumentSelection().getQuery();
                documentSelection = taskManager.createQueryDocumentSelection(query);
            } else if (taskDescription.getDocumentSelection().isSetEnumeration()) {
                List<Docvariant> variants =  taskDescription.getDocumentSelection().getEnumeration().getDocvariantList();
                VariantKey[] keys = new VariantKey[variants.size()]; 
                for (int i = 0; i < keys.length; i++) {
                    Docvariant variant = variants.get(i);
                    keys[i] = new VariantKey(variant.getDocumentId(), variant.getBranchId(), variant.getLanguageId());
                }
                documentSelection = taskManager.createEnumerationDocumentSelection(keys);
            } else {
                throw new TaskException("Missing document selection in posted XML.");
            }

            long taskId = taskManager.runTask(documentSelection, taskSpecification);

            TaskCreatedDocument taskCreatedDocument = TaskCreatedDocument.Factory.newInstance();
            taskCreatedDocument.addNewTaskCreated().setTaskId(taskId);
            taskCreatedDocument.save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/task";
    }
}
