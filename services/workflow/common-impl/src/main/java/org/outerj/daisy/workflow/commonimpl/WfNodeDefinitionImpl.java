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
package org.outerj.daisy.workflow.commonimpl;

import org.outerj.daisy.workflow.WfNodeDefinition;
import org.outerj.daisy.workflow.WfTransitionDefinition;
import org.outerx.daisy.x10Workflow.NodeDefinitionDocument;
import org.outerx.daisy.x10Workflow.TransitionDefinitionDocument;

import java.util.List;
import java.util.Collections;

public class WfNodeDefinitionImpl implements WfNodeDefinition {
    private String name;
    private String path;
    private String nodeType;
    private List<WfTransitionDefinition> leavingTransitions;
    private String processDefinitionId;
    private String processDefinitionName;

    public WfNodeDefinitionImpl(String name, String path, String nodeType,
            List<WfTransitionDefinition> leavingTransitions, String processDefinitionId) {
        this(name, path, nodeType, leavingTransitions, processDefinitionId, null);
    }

    public WfNodeDefinitionImpl(String name, String path, String nodeType,
            List<WfTransitionDefinition> leavingTransitions, String processDefinitionId, 
            String processDefinitionName) {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        if (path == null)
            throw new IllegalArgumentException("Null argument: path");
        if (nodeType == null)
            throw new IllegalArgumentException("Null argument: nodeType");
        if (leavingTransitions == null)
            throw new IllegalArgumentException("Null argument: leavingTransitions");
        if (processDefinitionId == null)
            throw new IllegalArgumentException("Null argument: processDefinitionId");

        this.name = name;
        this.path = path;
        this.nodeType = nodeType;
        this.leavingTransitions = Collections.unmodifiableList(leavingTransitions);
        this.processDefinitionId = processDefinitionId;
        this.processDefinitionName = processDefinitionName;
    }

    public String getName() {
        return name;
    }

    public String getFullyQualifiedName() {
        return path;
    }

    public String getNodeType() {
        return nodeType;
    }

    public List<WfTransitionDefinition> getLeavingTransitions() {
        return leavingTransitions;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public NodeDefinitionDocument getXml() {
        NodeDefinitionDocument doc = NodeDefinitionDocument.Factory.newInstance();
        NodeDefinitionDocument.NodeDefinition xml = doc.addNewNodeDefinition();
        xml.setName(name);
        xml.setFullyQualifiedName(path);
        xml.setNodeType(nodeType);
        xml.setProcessDefinitionId(processDefinitionId);
        if (processDefinitionName != null && processDefinitionName.length() >0)
            xml.setProcessDefinitionName(processDefinitionName);

        TransitionDefinitionDocument.TransitionDefinition[] transDefsXml = new TransitionDefinitionDocument.TransitionDefinition[leavingTransitions.size()];
        for (int i = 0; i < leavingTransitions.size(); i++) {
            transDefsXml[i] = leavingTransitions.get(i).getXml().getTransitionDefinition();
        }
        xml.addNewLeavingTransitions().setTransitionDefinitionArray(transDefsXml);

        return doc;
    }

}
