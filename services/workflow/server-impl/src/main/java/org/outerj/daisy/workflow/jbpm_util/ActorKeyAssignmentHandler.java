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
package org.outerj.daisy.workflow.jbpm_util;

import org.jbpm.taskmgmt.def.AssignmentHandler;
import org.jbpm.taskmgmt.exe.Assignable;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.context.exe.ContextInstance;
import org.outerj.daisy.workflow.WfActorKey;

import java.util.List;

/**
 * JBPM assignment handler which performs assignment based on a
 * {@link WfActorKey} object retrieved from a process variable.
 *
 * <p>This assignment handler is configured by setting the fields
 * variableName and variableScope (to 'local' for variables of the
 * current token or 'global' for values of the root token).
 */
public class ActorKeyAssignmentHandler implements AssignmentHandler {
    public String variableName;
    public String variableScope;

    public void assign(Assignable assignable, ExecutionContext executionContext) throws Exception {
        if (variableName == null)
            throw new Exception(this.getClass().getName() + ": variableName property not set.");

        ContextInstance contextInstance = executionContext.getContextInstance();
        Object value;
        if (variableScope == null || variableScope.equals("global")) {
            value = contextInstance.getLocalVariable(variableName, executionContext.getProcessInstance().getRootToken());
        } else if (variableScope.equals("task")) {
            TaskInstance taskInstance = executionContext.getTaskInstance();
            value = taskInstance != null ? taskInstance.getVariableLocally(variableName) : null;
        } else {
            throw new Exception(this.getClass().getName() + ": unsupported variable scope: " + variableScope);
        }

        if (value == null)
            throw new Exception(this.getClass().getName() + ": variable " + variableName + " does not have a value.");

        if (!(value instanceof WfActorKey))
            throw new Exception(this.getClass().getName() + ": variable " + variableName + " does not contain a WfActorKey but a " + value.getClass().getName());

        WfActorKey actorKey = (WfActorKey)value;
        if (actorKey.isUser()) {
            assignable.setActorId(String.valueOf(actorKey.getUserId()));
        } else {
            List<Long> poolIds = actorKey.getPoolIds();
            String[] poolIdStrings = new String[poolIds.size()];
            for (int i = 0; i < poolIdStrings.length; i++) {
                poolIdStrings[i] = String.valueOf(poolIds.get(i));
            }
            assignable.setPooledActors(poolIdStrings);
        }
    }
}
