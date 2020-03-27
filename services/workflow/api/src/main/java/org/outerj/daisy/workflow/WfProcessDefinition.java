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

import org.outerx.daisy.x10Workflow.ProcessDefinitionDocument;
import org.outerj.daisy.i18n.I18nMessage;

import java.util.List;
import java.util.Collection;

public interface WfProcessDefinition {
    /**
     * A unique ID for this specific version of the workflow process.
     */
    String getId();

    String getName();

    String getVersion();

    I18nMessage getLabel();

    I18nMessage getDescription();

    /**
     * A workflow process can have a "start task" to collect initial parameters
     * for the workflow (possibly none) and to select the initial
     * path to follow in the workflow (possibly only one option).
     *
     * <p>The start task is a task which is automatically completed
     * upon starting the workflow, so it normally doesn't appear
     * in anyones task list.
     *
     * <p>Returns null if there is no start task.
     */
    WfTaskDefinition getStartTaskDefinition();

    WfNodeDefinition getStartNode();

    /**
     * Retrieves a task by name. Returns null if no task by the given name.
     */
    WfTaskDefinition getTask(String name);

    List<WfVariableDefinition> getGlobalVariableDefinitions();
    
    WfVariableDefinition getGlobalVariable(String name);

    /**
     * Returns a list of all (named) tasks in the workflow
     * definition. This includes the start task. Returns
     * an empty collection if there are no tasks.
     */
    Collection<WfTaskDefinition> getTasks();

    /**
     * Returns a list of problems encountered during parsing the
     * workflow definition. It is possible that this information
     * is only available from the WorkflowDefinition returned by
     * {@link WorkflowManager#deployProcessDefinition}, i.o.w. right
     * after defining it.
     */
    List<String> getProblems();

    ProcessDefinitionDocument getXml();
}
