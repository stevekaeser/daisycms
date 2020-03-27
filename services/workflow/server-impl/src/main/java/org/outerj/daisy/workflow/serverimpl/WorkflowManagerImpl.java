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
package org.outerj.daisy.workflow.serverimpl;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.workflow.QueryConditions;
import org.outerj.daisy.workflow.QueryOrderByItem;
import org.outerj.daisy.workflow.QuerySelectItem;
import org.outerj.daisy.workflow.TaskUpdateData;
import org.outerj.daisy.workflow.WfActorKey;
import org.outerj.daisy.workflow.WfExecutionPath;
import org.outerj.daisy.workflow.WfPoolManager;
import org.outerj.daisy.workflow.WfProcessDefinition;
import org.outerj.daisy.workflow.WfProcessInstance;
import org.outerj.daisy.workflow.WfTask;
import org.outerj.daisy.workflow.WfTimer;
import org.outerj.daisy.workflow.WfVariable;
import org.outerj.daisy.workflow.WfVersionKey;
import org.outerj.daisy.workflow.WorkflowException;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerx.daisy.x10Workflow.SearchResultDocument;
import org.outerx.daisy.x10Workflowmeta.WorkflowAclInfoDocument;

public class WorkflowManagerImpl implements WorkflowManager {
    private final CommonWorkflowManager delegate;
    private final Repository repository;

    public WorkflowManagerImpl(CommonWorkflowManager delegate, Repository repository) {
        this.delegate = delegate;
        this.repository = repository;
    }

    public WfPoolManager getPoolManager() {
        return delegate.getPoolManager(repository);
    }

    public WfProcessDefinition deployProcessDefinition(InputStream is, String mimeType, Locale locale) throws WorkflowException {
        return delegate.deployProcessDefinition(is, mimeType, locale, repository);
    }

    public void loadSampleWorkflows() throws RepositoryException {
        delegate.loadSampleWorkflows(repository);
    }

    public void deleteProcessDefinition(String processDefinitionId) throws WorkflowException {
        delegate.deleteProcessDefinition(processDefinitionId, repository);
    }

    public WfProcessDefinition getProcessDefinition(String processDefinitionId, Locale locale) throws WorkflowException {
        return delegate.getProcessDefinition(processDefinitionId, locale, repository);
    }

    public WfProcessDefinition getLatestProcessDefinition(String workflowName, Locale locale) throws WorkflowException {
        return delegate.getLatestProcessDefinition(workflowName, locale, repository);
    }

    public List<WfProcessDefinition> getAllLatestProcessDefinitions(Locale locale) throws WorkflowException {
        return delegate.getAllLatestProcessDefinitions(locale, repository);
    }

    public List<WfProcessDefinition> getAllProcessDefinitions(Locale locale) throws WorkflowException {
        return delegate.getAllProcessDefinitions(locale, repository);
    }

    public Map<String, Integer> getProcessInstanceCounts() throws RepositoryException {
        return delegate.getProcessInstanceCounts(repository);
    }

    public List<WfVariable> getInitialVariables(String processDefinitionId, WfVersionKey contextDocument) throws RepositoryException {
        return delegate.getInitialVariables(processDefinitionId, contextDocument, repository);
    }

    public WfProcessInstance startProcess(String processDefinitionId, TaskUpdateData startTaskData, String initialTransition, Locale locale) throws WorkflowException {
        return delegate.startProcess(processDefinitionId, startTaskData, initialTransition, locale, repository);
    }

    public WfExecutionPath signal(String processInstanceId, String executionPathFullName, String transitionName, Locale locale) throws WorkflowException {
        return delegate.signal(processInstanceId, executionPathFullName, transitionName, locale, repository);
    }

    public WfProcessInstance getProcess(String processInstanceId, Locale locale) throws WorkflowException {
        return delegate.getProcess(processInstanceId, locale, repository);
    }

    public WfTask updateTask(String taskId, TaskUpdateData taskUpdateData, Locale locale) throws WorkflowException {
        return delegate.updateTask(taskId, taskUpdateData, locale, repository);
    }

    public WfTask endTask(String taskId, TaskUpdateData taskUpdateData, String transitionName, Locale locale) throws WorkflowException {
        return delegate.endTask(taskId, taskUpdateData, transitionName, locale, repository);
    }

    public WfTask getTask(String taskId, Locale locale) throws RepositoryException {
        return delegate.getTask(taskId, locale, repository);
    }

    public List<WfTask> getMyTasks(Locale locale) throws WorkflowException {
        return delegate.getMyTasks(locale, repository);
    }

    public List<WfTask> getPooledTasks(Locale locale) throws RepositoryException {
        return delegate.getPooledTasks(locale, repository);
    }

    public WfTask requestPooledTask(String taskId, Locale locale) throws RepositoryException {
        return delegate.requestPooledTask(taskId, locale, repository);
    }

    public WfTask unassignTask(String taskId, Locale locale) throws RepositoryException {
        return delegate.unassignTask(taskId, locale, repository);
    }

    public WfTask assignTask(String taskId, WfActorKey actor, boolean overwriteSwimlane, Locale locale) throws RepositoryException {
        return delegate.assignTask(taskId, actor, overwriteSwimlane, locale, repository);
    }

    public List<WfTask> getTasks(QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset,
            int chunkLength, Locale locale) throws WorkflowException {
        return delegate.getTasks(queryConditions, orderByItems, chunkOffset, chunkLength, locale, repository);
    }

    public SearchResultDocument searchTasks(List<QuerySelectItem> selectItems, QueryConditions queryConditions,
            List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws WorkflowException {
        return delegate.searchTasks(selectItems, queryConditions, orderByItems, chunkOffset, chunkLength, locale, repository);
    }

    public List<WfProcessInstance> getProcesses(QueryConditions queryConditions, List<QueryOrderByItem> orderByItems,
            int chunkOffset, int chunkLength, Locale locale) throws WorkflowException {
        return delegate.getProcesses(queryConditions, orderByItems, chunkOffset, chunkLength, locale, repository);
    }

    public SearchResultDocument searchProcesses(List<QuerySelectItem> selectItems, QueryConditions queryConditions,
            List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws WorkflowException {
        return delegate.searchProcesses(selectItems, queryConditions, orderByItems, chunkOffset, chunkLength, locale, repository);
    }

    public void deleteProcess(String processInstanceId) throws RepositoryException {
        delegate.deleteProcess(processInstanceId, repository);
    }

    public WfProcessInstance suspendProcess(String processInstanceId, Locale locale) throws RepositoryException {
        return delegate.suspendProcess(processInstanceId, locale, repository);
    }

    public WfProcessInstance resumeProcess(String processInstanceId, Locale locale) throws RepositoryException {
        return delegate.resumeProcess(processInstanceId, locale, repository);
    }

    public WfTimer getTimer(String timerId, Locale locale) throws RepositoryException {
        return delegate.getTimer(timerId, locale, repository);
    }

    public List<WfTimer> getTimers(QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException {
        return delegate.getTimers(queryConditions, orderByItems, chunkOffset, chunkLength, locale, repository);
    }

    public SearchResultDocument searchTimers(List<QuerySelectItem> selectItems, QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException {
        return delegate.searchTimers(selectItems, queryConditions, orderByItems, chunkOffset, chunkLength, locale, repository);
    }

    public WorkflowAclInfoDocument getAclInfo(String taskId, String processId, String processDefinitionId,
            boolean includeGlobalInfo) throws RepositoryException {
        return delegate.getAclInfo(taskId, processId, processDefinitionId, includeGlobalInfo, repository); 
    }
}
