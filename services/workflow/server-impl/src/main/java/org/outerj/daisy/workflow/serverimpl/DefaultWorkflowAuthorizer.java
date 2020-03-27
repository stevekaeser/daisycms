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
import org.jbpm.taskmgmt.exe.PooledActor;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.workflow.*;

import java.util.*;

public class DefaultWorkflowAuthorizer implements WorkflowAuthorizer {
    public boolean canDeployProcessDefinition(ProcessDefinition processDefinition, Repository repository) {
        return repository.isInRole(Role.ADMINISTRATOR);
    }

    public boolean canDeleteProcessDefinition(ProcessDefinition processDefinition, Repository repository) {
        return repository.isInRole(Role.ADMINISTRATOR);
    }

    public boolean canReadProcessDefinition(ProcessDefinition processDefinition, Repository repository) {
        return true;
    }

    public boolean canGetProcessInstanceCounts(Repository repository) {
        return repository.isInRole(Role.ADMINISTRATOR);
    }

    public boolean canStartProcess(ProcessDefinition processDefinition, Repository repository) {
        return canReadProcessDefinition(processDefinition, repository);
    }

    public boolean canSignalProcess(ProcessInstance processInstance, Repository repository) {
        return isAdminOrProcessOwner(processInstance, repository);
    }

    @SuppressWarnings("unchecked")
    public boolean canReadProcess(ProcessInstance processInstance, Repository repository) {
        // Admins can access all process instances
        if (repository.isInRole(Role.ADMINISTRATOR))
            return true;

        // process instance owner can always access it
        WfUserKey owner = getProcessOwner(processInstance);
        if (owner != null && owner.getId() == repository.getUserId())
            return true;

        // if the process is associated with a document and the user has read access on the document,
        // then he can also see the process instance
        WfVersionKey versionKey = getAssociatedDocument(processInstance);
        boolean canReadDocument = canReadAssociatedDocument(processInstance, repository);
        if (versionKey != null && canReadDocument)
            return true;

        Set<String> poolIds = null; // will be loaded when first needed

        // If the user is the actor of a task in the process, or belongs to a pool for as task
        // which is associated with a pooled actor, then the user can access the process too.
        Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getTaskInstances();
        for (TaskInstance taskInstance : taskInstances) {
            if (taskInstance.getActorId() != null) {
                try {
                    long actorId = Long.parseLong(taskInstance.getActorId());
                    if (repository.getUserId() == actorId)
                        return true;
                } catch (NumberFormatException e) {
                    // if the actorId is not a valid long, it will certainly not be equal to the current user's id
                }
            } else {
                Set<PooledActor> pooledActors = taskInstance.getPooledActors();
                if (pooledActors != null) {
                    for (PooledActor pooledActor : pooledActors) {
                        if (poolIds == null)
                            poolIds = getUserPools(repository);
                        // when belonging to a pool, also check the user can read the associated document (if any)
                        if (poolIds.contains(pooledActor.getActorId()) && canReadDocument) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean canUpdateTask(TaskInstance taskInstance, Repository repository) {
        if (repository.isInRole(Role.ADMINISTRATOR))
            return true;

        if (taskInstance.getActorId() != null && taskInstance.getActorId().equals(String.valueOf(repository.getUserId())))
            return true;

        WfUserKey owner = getProcessOwner(taskInstance.getTaskMgmtInstance().getProcessInstance());
        if (owner != null && owner.getId() == repository.getUserId())
            return true;

        return false;
    }

    @SuppressWarnings("unchecked")
    public boolean canRequestPooledTask(TaskInstance taskInstance, Repository repository) {
        boolean isAdminOrProcessOwner = isAdminOrProcessOwner(taskInstance.getTaskMgmtInstance().getProcessInstance(), repository);
        if (isAdminOrProcessOwner)
            return true;

        // Get list of pools the user belongs to
        Set<String> userPools = getUserPools(repository);

        // Check if the user belongs to one of the pools assigned to the task
        boolean belongsToOneOfThePools = false;
        Set<PooledActor> pooledActors = taskInstance.getPooledActors();
        if (pooledActors != null) {
            for (PooledActor pooledActor : pooledActors) {
                if (userPools.contains(pooledActor.getActorId())) {
                    belongsToOneOfThePools = true;
                    break;
                }
            }
        }
        if (!belongsToOneOfThePools)
            return false;

        // To correspond with process/task filtering: if the process instance is associated with a document
        // which the user cannot read, don't allow requesting the task either
        boolean canReadDocument = canReadAssociatedDocument(taskInstance.getTaskMgmtInstance().getProcessInstance(), repository);
        return canReadDocument;
    }

    public boolean canAssignTask(TaskInstance taskInstance, WfActorKey newActor, Repository repository) {
        boolean isAdminOrProcessOwner = isAdminOrProcessOwner(taskInstance.getTaskMgmtInstance().getProcessInstance(), repository);
        if (isAdminOrProcessOwner)
            return true;

        return false;
    }

    public boolean canUnassignTask(TaskInstance taskInstance, Repository repository) {
        boolean isAdminOrProcessOwner = isAdminOrProcessOwner(taskInstance.getTaskMgmtInstance().getProcessInstance(), repository);
        if (isAdminOrProcessOwner)
            return true;

        // current assignee can un-assign the task.
        Set pooledActors = taskInstance.getPooledActors();
        if (pooledActors != null
                && !pooledActors.isEmpty()
                && taskInstance.getActorId() != null
                && taskInstance.getActorId().equals(String.valueOf(repository.getUserId())))
            return true;

        return false;
    }

    public boolean canDeleteProcess(ProcessInstance processInstance, Repository repository) {
        return isAdminOrProcessOwner(processInstance, repository);
    }

    public boolean canSuspendProcess(ProcessInstance processInstance, Repository repository) {
        return isAdminOrProcessOwner(processInstance, repository);
    }

    public boolean canResumeProcess(ProcessInstance processInstance, Repository repository) {
        return isAdminOrProcessOwner(processInstance, repository);
    }

    private boolean isAdminOrProcessOwner(ProcessInstance processInstance, Repository repository) {
        if (repository.isInRole(Role.ADMINISTRATOR))
            return true;

        WfUserKey owner = getProcessOwner(processInstance);
        return owner != null && owner.getId() == repository.getUserId();
    }

    private WfUserKey getProcessOwner(ProcessInstance processInstance) {
        return (WfUserKey)processInstance.getContextInstance().getLocalVariable("daisy_owner", processInstance.getRootToken());
    }

    private WfVersionKey getAssociatedDocument(ProcessInstance processInstance) {
        return (WfVersionKey)processInstance.getContextInstance().getLocalVariable("daisy_document", processInstance.getRootToken());
    }

    /**
     * Returns true if the user either has read access on the document associated via
     * the daisy_document global process variable, or if there is no such property.
     */
    private boolean canReadAssociatedDocument(ProcessInstance processInstance, Repository repository) {
        WfVersionKey versionKey = getAssociatedDocument(processInstance);
        boolean canReadDocument = true; // default to true for case there is no adocument association
        if (versionKey != null) {
            try {
                VariantKey[] results = repository.getAccessManager().filterDocuments(new VariantKey[] { versionKey.getVariantKey() }, AclPermission.READ, true);
                canReadDocument = results.length == 1;
            } catch (RepositoryException e) {
                throw new RuntimeException("Unexpected error checking document read access.", e);
            }
        }
        return canReadDocument;
    }

    private Set<String> getUserPools(Repository repository) {
        WfPoolManager poolManager = ((WorkflowManager)repository.getExtension("WorkflowManager")).getPoolManager();
        List<WfPool> pools;
        try {
            pools = poolManager.getPoolsForUser(repository.getUserId());
        } catch (RepositoryException e) {
            throw new RuntimeException("Unexpected error fetching user pools.", e);
        }
        Set<String> poolIds = new HashSet<String>();
        for (WfPool pool : pools) {
            poolIds.add(String.valueOf(pool.getId()));
            // this is in the case someone chooses to use the poolname instead of the poolid.
            // eg. in the processdefinition the name of a pool could be used instead of an ID
            // for a task/swimlane assignment
            poolIds.add(pool.getName());
        }

        return poolIds;
    }
}
