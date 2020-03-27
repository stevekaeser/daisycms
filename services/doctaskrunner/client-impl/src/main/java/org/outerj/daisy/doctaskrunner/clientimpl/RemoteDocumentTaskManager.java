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
package org.outerj.daisy.doctaskrunner.clientimpl;

import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.outerj.daisy.doctaskrunner.DocumentExecutionState;
import org.outerj.daisy.doctaskrunner.DocumentSelection;
import org.outerj.daisy.doctaskrunner.DocumentTaskManager;
import org.outerj.daisy.doctaskrunner.Task;
import org.outerj.daisy.doctaskrunner.TaskDocDetails;
import org.outerj.daisy.doctaskrunner.TaskException;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.doctaskrunner.TaskState;
import org.outerj.daisy.doctaskrunner.Tasks;
import org.outerj.daisy.doctaskrunner.commonimpl.EnumerationDocumentSelection;
import org.outerj.daisy.doctaskrunner.commonimpl.QueryDocumentSelection;
import org.outerj.daisy.doctaskrunner.commonimpl.TaskDocDetailImpl;
import org.outerj.daisy.doctaskrunner.commonimpl.TaskDocDetailsImpl;
import org.outerj.daisy.doctaskrunner.commonimpl.TaskImpl;
import org.outerj.daisy.doctaskrunner.commonimpl.TasksImpl;
import org.outerj.daisy.doctaskrunner.spi.TaskSpecificationImpl;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerx.daisy.x10Doctaskrunner.AllowedTasksDocument;
import org.outerx.daisy.x10Doctaskrunner.TaskCreatedDocument;
import org.outerx.daisy.x10Doctaskrunner.TaskDescriptionDocument;
import org.outerx.daisy.x10Doctaskrunner.TaskDocDetailDocument;
import org.outerx.daisy.x10Doctaskrunner.TaskDocDetailsDocument;
import org.outerx.daisy.x10Doctaskrunner.TaskDocument;
import org.outerx.daisy.x10Doctaskrunner.TasksDocument;

public class RemoteDocumentTaskManager implements DocumentTaskManager {
    private RemoteRepositoryImpl repository;

    public RemoteDocumentTaskManager(RemoteRepositoryImpl repository) {
        this.repository = repository;
    }

    public long runTask(DocumentSelection documentSelection, TaskSpecification taskSpecification) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/doctaskrunner/task");

        TaskDescriptionDocument taskDescriptionDocument = TaskDescriptionDocument.Factory.newInstance();
        TaskDescriptionDocument.TaskDescription taskDescription = taskDescriptionDocument.addNewTaskDescription();
        TaskDescriptionDocument.TaskDescription.DocumentSelection documentSelectionXml = taskDescription.addNewDocumentSelection();

        if (documentSelection instanceof EnumerationDocumentSelection) {
            EnumerationDocumentSelection enumDocumentSelection = (EnumerationDocumentSelection)documentSelection;
            VariantKey[] variantKeys = enumDocumentSelection.getKeys();

            TaskDescriptionDocument.TaskDescription.DocumentSelection.Enumeration enumerationXml = documentSelectionXml.addNewEnumeration();
            TaskDescriptionDocument.TaskDescription.DocumentSelection.Enumeration.Docvariant[] docvariants = new TaskDescriptionDocument.TaskDescription.DocumentSelection.Enumeration.Docvariant[variantKeys.length];

            for (int i = 0; i < variantKeys.length; i++) {
                docvariants[i] = TaskDescriptionDocument.TaskDescription.DocumentSelection.Enumeration.Docvariant.Factory.newInstance();
                docvariants[i].setDocumentId(variantKeys[i].getDocumentId());
                docvariants[i].setBranchId(variantKeys[i].getBranchId());
                docvariants[i].setLanguageId(variantKeys[i].getLanguageId());
            }

            enumerationXml.setDocvariantArray(docvariants);
        } else if (documentSelection instanceof QueryDocumentSelection) {
            QueryDocumentSelection queryDocumentSelection = (QueryDocumentSelection)documentSelection;
            String query = queryDocumentSelection.getQuery();
            documentSelectionXml.setQuery(query);
        } else {
            throw new TaskException("Encountered unsupported DocumentSelection implementation: " + documentSelection.getClass().getName());
        }

        TaskDescriptionDocument.TaskDescription.Specification taskSpecificationXml = taskDescription.addNewSpecification();
        TaskDescriptionDocument.TaskDescription.Specification.Action actionXml = taskSpecificationXml.addNewAction();
        taskSpecificationXml.setStopOnFirstError(taskSpecification.stopOnFirstError());
        taskSpecificationXml.setDescription(taskSpecification.getDescription());
        taskSpecificationXml.setRetryInterval(taskSpecification.getRetryInterval());
        taskSpecificationXml.setMaxTryCount(taskSpecification.getMaxTryCount());
        actionXml.setType(taskSpecification.getActionType());        
        actionXml.setParameters(taskSpecification.getParameters());

        method.setRequestEntity(new InputStreamRequestEntity(taskDescriptionDocument.newInputStream()));

        TaskCreatedDocument taskCreatedDocument = (TaskCreatedDocument)httpClient.executeMethod(method, TaskCreatedDocument.class, true);
        return taskCreatedDocument.getTaskCreated().getTaskId();
    }

    public Task getTask(long id) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/doctaskrunner/task/" + id);

        TaskDocument taskDocument = (TaskDocument)httpClient.executeMethod(method, TaskDocument.class, true);
        return instantiateTaskFromXml(taskDocument.getTask());
    }

    private TaskImpl instantiateTaskFromXml(TaskDocument.Task taskXml) {
        Date finishedAt = taskXml.isSetFinishedAt() ? taskXml.getFinishedAt().getTime() : null;
        return new TaskImpl(taskXml.getId(), taskXml.getDescription(), TaskState.fromString(taskXml.getState().toString()),
                taskXml.getOwnerId(), taskXml.getProgress(), taskXml.getDetails(), taskXml.getAction().getType(),
                taskXml.getAction().getParameters(), taskXml.getStartedAt().getTime(), finishedAt, taskXml.getTryCount(), taskXml.getMaxTries(), taskXml.getRetryInterval());
    }

    public Tasks getTasks() throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/doctaskrunner/task");

        TasksDocument tasksDocument = (TasksDocument)httpClient.executeMethod(method, TasksDocument.class, true);
        List<TaskDocument.Task> tasksXml = tasksDocument.getTasks().getTaskList();

        TaskImpl[] tasks = new TaskImpl[tasksXml.size()];
        for (int i = 0; i < tasksXml.size(); i++) {
            tasks[i] = instantiateTaskFromXml(tasksXml.get(i));
        }

        return new TasksImpl(tasks);
    }

    public void deleteTask(long id) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        DeleteMethod method = new DeleteMethod("/doctaskrunner/task/" + id);
        httpClient.executeMethod(method, null, true);
    }

    public void interruptTask(long id) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/doctaskrunner/task/" + id);

        NameValuePair[] queryString = {new NameValuePair("action", "interrupt")};
        method.setQueryString(queryString);
        httpClient.executeMethod(method, null, true);
    }

    public TaskDocDetails getTaskDocDetails(long taskId) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/doctaskrunner/task/" + taskId + "/docdetails");
        TaskDocDetailsDocument taskDocDetailsDocument = (TaskDocDetailsDocument)httpClient.executeMethod(method, TaskDocDetailsDocument.class, true);
        List<TaskDocDetailDocument.TaskDocDetail> taskDocDetailsXml = taskDocDetailsDocument.getTaskDocDetails().getTaskDocDetailList();

        TaskDocDetailImpl[] taskDocDetails = new TaskDocDetailImpl[taskDocDetailsXml.size()];
        for (int i = 0; i < taskDocDetailsXml.size(); i++) {
            TaskDocDetailDocument.TaskDocDetail taskDocDetailXml = taskDocDetailsXml.get(i);
            VariantKey variantKey = new VariantKey(taskDocDetailXml.getDocumentId(), taskDocDetailXml.getBranchId(), taskDocDetailXml.getLanguageId());
            DocumentExecutionState state = DocumentExecutionState.fromString(taskDocDetailXml.getState().toString());
            taskDocDetails[i] = new TaskDocDetailImpl(variantKey, state, taskDocDetailXml.getDetails(), taskDocDetailXml.getTryCount());
        }

        return new TaskDocDetailsImpl(taskDocDetails);
    }

    public TaskSpecification createTaskSpecification(String description, String script, String scriptLanguage, boolean stopOnFirstError) {
        return new TaskSpecificationImpl(description, scriptLanguage, script, stopOnFirstError);
    }

    public DocumentSelection createQueryDocumentSelection(String query) {
        return new QueryDocumentSelection(query);
    }

    public DocumentSelection createEnumerationDocumentSelection(VariantKey[] variantKeys) {
        return new EnumerationDocumentSelection(variantKeys);
    }

	public String[] getAllowedTasks() throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/doctaskrunner/allowedTasks");
        AllowedTasksDocument allowedTasksDocument = (AllowedTasksDocument)httpClient.executeMethod(method, AllowedTasksDocument.class, true);
        return allowedTasksDocument.getAllowedTasks().getAllowedTaskArray();
	}

}