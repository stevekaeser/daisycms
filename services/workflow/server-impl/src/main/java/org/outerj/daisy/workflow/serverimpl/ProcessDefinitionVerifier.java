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
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.NodeCollection;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.def.Task;
import org.outerj.daisy.workflow.WorkflowException;

import java.util.List;
import java.util.Collection;
import java.util.Locale;

public class ProcessDefinitionVerifier {
    /**
     * Performs some stricter checks on a process definition than jBPM does
     * to be sure Daisy can handle it without problems.
     */
    @SuppressWarnings("unchecked")
    public static void verify(ProcessDefinition definition, WorkflowMetaManager wfMetaManager, WfObjectBuilder wfBuilder) throws WorkflowException {
        if (!isGoodName(definition.getName()))
            throw new WorkflowException("The workflow definition does not have a name.");

        if (definition.getStartState() == null)
            throw new WorkflowException("Invalid process definition: it does not have a start state.");

        checkNodes(definition.getNodes());

        Task startTask = definition.getTaskMgmtDefinition().getStartTask();
        if (startTask != null)
            checkTask(startTask, definition.getStartState().getName());

        // Try retrieving the workflow metadata, this will throw an error in case it's invalid
        //wfMetaManager.getWorkflowMeta(definition);

        // Try building the process definition, this will verify meta data loading works correctly
        // and is correct (e.g. the extend-chaining of all variables resolves correctly)
        wfBuilder.buildProcessDefinition(definition, null, Locale.getDefault());
    }

    @SuppressWarnings("unchecked")
    private static void checkNodes(List<Node> nodes) throws WorkflowException {
        for (Node node : nodes) {
            if (!isGoodName(node.getName()))
                throw new WorkflowException("The workflow definition contains a node without name. It is a node of type " + node.getClass().getName());

            // Check tasks
            if (node instanceof TaskNode) {
                Collection<Task> tasks = ((TaskNode)node).getTasks();
                if (tasks != null) {
                    for (Task task : tasks)
                        checkTask(task, node.getName());
                }
            }

            // Check transitions
            List<Transition> transitions = node.getLeavingTransitions();
            if (transitions != null) {
                for (Transition transition : transitions) {
                    if (!isGoodName(transition.getName()))
                        throw new WorkflowException("The workflow definition contains a transition without a name in the node " + node.getName());
                }
            }

            // Check child nodes
            if (node instanceof NodeCollection) {
                checkNodes(((NodeCollection)node).getNodes());
            }
        }
    }

    private static void checkTask(Task task, String context) throws WorkflowException {
        if (!isGoodName(task.getName()))
            throw new WorkflowException("The workflow definition contains a task without a name, as part of the node " + context);
    }

    private static boolean isGoodName(String name) {
        return name != null && name.trim().length() > 0;
    }
}
