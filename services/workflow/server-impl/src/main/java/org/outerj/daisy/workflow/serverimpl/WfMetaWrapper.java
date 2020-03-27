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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.Transition;
import org.outerj.daisy.i18n.DResourceBundles;
import org.outerj.daisy.i18n.I18nMessage;
import org.outerj.daisy.i18n.impl.AggregateResourceBundle;
import org.outerj.daisy.i18n.impl.StringI18nMessage;
import org.outerx.daisy.x10Workflowmeta.I18NType;
import org.outerx.daisy.x10Workflowmeta.NodeDocument;
import org.outerx.daisy.x10Workflowmeta.TaskDocument;
import org.outerx.daisy.x10Workflowmeta.TransitionDocument;
import org.outerx.daisy.x10Workflowmeta.VariableDocument;
import org.outerx.daisy.x10Workflowmeta.WorkflowMetaDocument;

/**
 * Helper class for retrieving relevant parts of the workflow meta document.
 */
public class WfMetaWrapper {
    private WorkflowMetaDocument workflowMetaDocument;
    private WorkflowMetaDocument.WorkflowMeta workflowMeta;
    private AggregateResourceBundle i18nBundle;

    public WfMetaWrapper(WorkflowMetaDocument workflowMetaDocument, AggregateResourceBundle i18nBundle) {
        this.workflowMetaDocument = workflowMetaDocument;
        this.workflowMeta = workflowMetaDocument.getWorkflowMeta();
        this.i18nBundle = i18nBundle;
    }

    public WorkflowMetaDocument.WorkflowMeta getRoot() {
        return workflowMeta;
    }

    /**
     * Looks up the metadata for a node based on the node path.
     * A string node path (separated by slashes) can be parsed
     * using {@link #parseNodePath}.
     */
    public NodeDocument.Node getNodeMeta(String[] nodePath) {
        if (workflowMeta.isSetNodes()) {
            for (NodeDocument.Node nodeMeta : workflowMeta.getNodes().getNodeList()) {
                if (Arrays.equals(parseNodePath(nodeMeta.getPath()), nodePath)) {
                    return nodeMeta;
                }
            }
        }
        return null;
    }

    public  TransitionDocument.Transition getTransitionMeta(Transition transition) {
        // Get fully qualified node name in the form of an array of names
        List<String> parentNamesList = new ArrayList<String>(3);
        Node current = transition.getFrom();
        while (current != null)  {
            parentNamesList.add(current.getName());
            current = current.getSuperState();
        }
        String[] parentNames = parentNamesList.toArray(new String[0]);

        NodeDocument.Node node = getNodeMeta(parentNames);
        if (node == null)
            return null;

        for (TransitionDocument.Transition transitionMeta: node.getTransitionList()) {
            if (transitionMeta.getName().equals(transition.getName())) {
                return transitionMeta;
            }
        }

        return null;
    }

    public TaskDocument.Task getTaskMeta(String name) {
        if (workflowMeta.isSetTasks()) {
            for (TaskDocument.Task task : workflowMeta.getTasks().getTaskList()) {
                if (task.getName().equals(name)) {
                    return task;
                }
            }
        }
        return null;
    }

    public VariableDocument.Variable getGlobalVariable(String name) {
        if (!workflowMeta.isSetVariables())
            return null;

        for (VariableDocument.Variable variableXml : workflowMeta.getVariables().getVariableList()) {
            if (name.equals(variableXml.getName())) {
                return variableXml;
            }
        }
        return null;
    }

    private String[] parseNodePath(String nodePath) {
        String[] parts = nodePath.split("/");
        List<String> goodParts = new ArrayList<String>();
        for (String part : parts) {
            if (part.trim().length() > 0)
                goodParts.add(part);
        }
        return goodParts.toArray(new String[0]);
    }

    public I18nMessage getI18nMessage(I18NType i18nXml, Locale locale) {
        boolean i18nKey = i18nXml.isSetI18N() ? i18nXml.getI18N() : false;
        String value = i18nXml.getStringValue();
        if (!i18nKey)
            return new StringI18nMessage(value);

        return i18nBundle.get(locale, i18nXml.getStringValue());
    }

    public DResourceBundles getResourceBundles() {
        return i18nBundle;
    }
}
