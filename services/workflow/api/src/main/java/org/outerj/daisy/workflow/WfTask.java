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

import org.outerx.daisy.x10Workflow.TaskDocument;

import java.util.Date;
import java.util.List;

public interface WfTask {
    String getId();

    Date getCreated();

    /**
     * Returns null as long as the task is not ended.
     */
    Date getEnd();

    /**
     * Time by which the task should be finished, can be null.
     */
    Date getDueDate();

    TaskPriority getPriority();

    /**
     * The ID of the user to who this task is assigned.
     * This is -1 if the task is not assigned to anyone
     * (or assigned to a pool).
     */
    long getActorId();

    /**
     * Returns true if this task is associated with pools.
     * This is useful to know on beforehand if it is possible
     * to unassign a task, so that it is assigned back to the pool(s).
     */
    boolean hasPools();

    /**
     * Returns true if this task is associated with a swimlane.
     * This is useful to know if the 'overwriteSwimlane' flag for
     * the (re-)assignment of tasks will have any influence.
     */
    boolean hasSwimlane();

    /**
     * Returns the ID of the process instance to which this task belongs.
     */
    String getProcessId();

    /**
     * Returns the execution path in the workflow to where
     * this task was created.
     */
    String getExecutionPath();

    WfVariable getVariable(String name);

    WfVariable getVariable(String name, VariableScope scope);

    List<WfVariable> getVariables();

    WfTaskDefinition getDefinition();

    TaskDocument getXml();
}
