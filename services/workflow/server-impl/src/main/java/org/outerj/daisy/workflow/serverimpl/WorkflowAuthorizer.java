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

import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.WfActorKey;

public interface WorkflowAuthorizer {
    boolean canDeployProcessDefinition(ProcessDefinition processDefinition, Repository repository);

    boolean canDeleteProcessDefinition(ProcessDefinition processDefinition, Repository repository);

    /**
     * Note that access to a process instance automatically assumes access to the process definition
     * to which it belongs, even if this method returns false.
     *
     * This method however can forbid direct retrieval of a process definition and causes filtering
     * of the process definition list.
     */
    boolean canReadProcessDefinition(ProcessDefinition processDefinition, Repository repository);

    boolean canGetProcessInstanceCounts(Repository repository);

    boolean canStartProcess(ProcessDefinition processDefinition, Repository repository);

    boolean canSignalProcess(ProcessInstance processInstance, Repository repository);

    boolean canReadProcess(ProcessInstance processInstance, Repository repository);

    boolean canUpdateTask(TaskInstance taskInstance, Repository repository);

    boolean canRequestPooledTask(TaskInstance taskInstance, Repository repository);

    boolean canAssignTask(TaskInstance taskInstance, WfActorKey newActor, Repository repository);

    boolean canUnassignTask(TaskInstance taskInstance, Repository repository);

    boolean canDeleteProcess(ProcessInstance processInstance, Repository repository);

    boolean canSuspendProcess(ProcessInstance processInstance, Repository repository);

    boolean canResumeProcess(ProcessInstance processInstance, Repository repository);

}
