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
package org.outerj.daisy.workflow;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10Workflow.SearchResultDocument;
import org.outerx.daisy.x10Workflowmeta.WorkflowAclInfoDocument;

/**
 * The main interface for accessing Daisy's workflow functionality.
 *
 * <p>This is an optional repository extension component.
 *
 * <p>All workflow operations are performed through this interface. The returned objects are
 * pure value objects (data snapshots), with no active behaviour.
 *
 * <p>The WorkflowManager is obtained from the {@link org.outerj.daisy.repository.Repository Repository} as
 * follows:
 *
 * <pre>
 * WorkflowManager wfManager = (WorkflowManager)repository.getExtension("WorkflowManager");
 * </pre>
 *
 * <p>In the remote repository API, the WorkflowManager extension can be registered as follows:
 *
 * <pre>
 * RemoteRepositoryManager repositoryManager = ...;
 * repositoryManager.registerExtension("WorkflowManager",
 *     new Packages.org.outerj.daisy.workflow.clientimpl.RemoteWorkflowManagerProvider());
 * </pre>
 */
public interface WorkflowManager {

    WfPoolManager getPoolManager();

    /**
     * Defines (deploys) a new workflow definition.
     *
     * <p>It is the responsibility of the caller to close the input stream.
     *
     * @param mimeType Use application/zip for zipped workflow archives, or text/xml for XML-described workflows.
     */
    WfProcessDefinition deployProcessDefinition(InputStream is, String mimeType, Locale locale) throws RepositoryException;

    /**
     * Re-installs the built-in sample workflows. Can be useful after upgrading.
     *
     */
    void loadSampleWorkflows() throws RepositoryException;

    /**
     * Deletes a workflow definition.
     *
     * <p><b>Warning: this removes all process instances that use this workflow definition.</b>
     */
    void deleteProcessDefinition(String processDefinitionId) throws RepositoryException;

    /**
     * @throws ProcessDefinitionNotFoundException in case the workflow definition does not exist.
     */
    WfProcessDefinition getProcessDefinition(String processDefinitionId, Locale locale) throws RepositoryException;

    WfProcessDefinition getLatestProcessDefinition(String workflowName, Locale locale) throws RepositoryException;

    /**
     * Gets a list of the latest versions of all workflow definitions defined in the system.
     */
    List<WfProcessDefinition> getAllLatestProcessDefinitions(Locale locale) throws RepositoryException;

    /**
     * Gets a list of all workflow definitions (in all versions) defined in the system.
     *
     * <p>See {@link #getAllLatestProcessDefinitions(Locale)} to only get the latest versions.
     */
    List<WfProcessDefinition> getAllProcessDefinitions(Locale locale) throws RepositoryException;

    /**
     * Returns the number of process instances of each process definition. The key in the map
     * is the process definition id, the value the instance count.
     */
    Map<String, Integer> getProcessInstanceCounts() throws RepositoryException;

    /**
     * Calculates initial values for start-state task variables.
     */
    List<WfVariable> getInitialVariables(String processDefinitionId, WfVersionKey contextDocument) throws RepositoryException;

    /**
     *
     * @param startTaskData parameters for the start task of the workflow
     * @param initialTransition the transition to take from the start node
     */
    WfProcessInstance startProcess(String processDefinitionId, TaskUpdateData startTaskData, String initialTransition, Locale locale) throws RepositoryException;

    /**
     *
     * @param executionPathFullName fullName property of the execution path
     * @param transitionName allowed to be null
     */
    WfExecutionPath signal(String processInstanceId, String executionPathFullName, String transitionName, Locale locale) throws RepositoryException;

    WfProcessInstance getProcess(String processInstanceId, Locale locale) throws RepositoryException;

    WfTask updateTask(String taskId, TaskUpdateData taskUpdateData, Locale locale) throws RepositoryException;

    WfTask endTask(String taskId, TaskUpdateData taskUpdateData, String transitionName, Locale locale) throws RepositoryException;

    WfTask getTask(String taskId, Locale locale) throws RepositoryException;

    /**
     * Gets the open tasks for the current user.
     */
    List<WfTask> getMyTasks(Locale locale) throws RepositoryException;

    List<WfTask> getPooledTasks(Locale locale) throws RepositoryException;

    WfTask requestPooledTask(String taskId, Locale locale) throws RepositoryException;

    /**
     * Unassigns a task from its current assignee, putting it back in the pool. Unassignment
     * is not allowed if there are no pooled actors to fall back too.
     */
    WfTask unassignTask(String taskId, Locale locale) throws RepositoryException;

    /**
     * Assigns (possibly re-assigns) a task to the given actor (user or pools).
     *
     * @param overwriteSwimlane if the task is associated with a swimlane, should the swimlane be reassigned too? Usually yes.
     */
    WfTask assignTask(String taskId, WfActorKey actor, boolean overwriteSwimlane, Locale locale) throws RepositoryException;

    /**
     *
     * @param chunkOffset specify -1 to ignore
     * @param chunkLength specify -1 to ignore
     */
    List<WfTask> getTasks(QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException;

    SearchResultDocument searchTasks(List<QuerySelectItem> selectItems, QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException;

    List<WfProcessInstance> getProcesses(QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException;

    SearchResultDocument searchProcesses(List<QuerySelectItem> selectItems, QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException;

    void deleteProcess(String processInstanceId) throws RepositoryException;

    WfProcessInstance suspendProcess(String processInstanceId, Locale locale) throws RepositoryException;

    WfProcessInstance resumeProcess(String processInstanceId, Locale locale) throws RepositoryException;

    WfTimer getTimer(String timerId, Locale locale) throws RepositoryException;

    List<WfTimer> getTimers(QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException;

    SearchResultDocument searchTimers(List<QuerySelectItem> selectItems, QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException;

    /**
     * Returns an xmlobject describing which actions the current user can perform with respect to the task instance, process instance, process definition or general workflow information.
     * The id args can be null - leave them blank if you don't need the related access control information.
     * 
     * @param taskId
     * @param processId
     * @param processDefinitionId
     * @param includeGlobalInfo
     * @return
     * @throws RepositoryException
     */
    WorkflowAclInfoDocument getAclInfo(String taskId, String processId, String processDefinitionId, boolean includeGlobalInfo) throws RepositoryException;
}
