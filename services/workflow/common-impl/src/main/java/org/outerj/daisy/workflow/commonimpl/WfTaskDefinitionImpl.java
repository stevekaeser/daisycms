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

import org.outerj.daisy.workflow.WfTaskDefinition;
import org.outerj.daisy.workflow.WfVariableDefinition;
import org.outerj.daisy.workflow.WfNodeDefinition;
import org.outerj.daisy.workflow.VariableScope;
import org.outerj.daisy.i18n.I18nMessage;
import org.outerj.daisy.i18n.impl.StringI18nMessage;
import org.outerx.daisy.x10Workflow.TaskDefinitionDocument;
import org.outerx.daisy.x10Workflow.VariableDefinitionDocument;

import java.util.List;
import java.util.Collections;

public class WfTaskDefinitionImpl implements WfTaskDefinition {
    private String name;
    private I18nMessage label;
    private I18nMessage description;
    private List<WfVariableDefinition> variables;
    private WfNodeDefinition node;

    public WfTaskDefinitionImpl(String name, I18nMessage label, I18nMessage description,
            List<WfVariableDefinition> variables, WfNodeDefinition node) {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        if (variables == null)
            throw new IllegalArgumentException("Null argument: variables");
        if (node == null)
            throw new IllegalArgumentException("Null argument: node");

        this.name = name;
        this.label = label;
        this.description = description;
        this.variables = Collections.unmodifiableList(variables);
        this.node = node;
    }

    public String getName() {
        return name;
    }

    public I18nMessage getLabel() {
        return label != null ? label : new StringI18nMessage(getName());
    }

    public I18nMessage getDescription() {
        return description;
    }

    public WfVariableDefinition getVariable(String name, VariableScope scope) {
        for (WfVariableDefinition variable : variables) {
            if (variable.getName().equals(name) && variable.getScope() == scope) {
                return variable;
            }
        }
        return null;
    }

    public List<WfVariableDefinition> getVariables() {
        return variables;
    }

    public WfNodeDefinition getNode() {
        return node;
    }

    public TaskDefinitionDocument getXml() {
        TaskDefinitionDocument doc = TaskDefinitionDocument.Factory.newInstance();
        TaskDefinitionDocument.TaskDefinition xml = doc.addNewTaskDefinition();
        xml.setName(name);

        xml.addNewLabel().set(label != null ? WfXmlHelper.i18nMessageToXml(label) : WfXmlHelper.stringToXml(name));
        if (description != null)
            xml.addNewDescription().set(WfXmlHelper.i18nMessageToXml(description));

        VariableDefinitionDocument.VariableDefinition[] varsXml = new VariableDefinitionDocument.VariableDefinition[variables.size()];
        for (int i = 0; i < variables.size(); i++)  {
            varsXml[i] = variables.get(i).getXml().getVariableDefinition();
        }
        xml.addNewVariableDefinitions().setVariableDefinitionArray(varsXml);

        xml.setNodeDefinition(node.getXml().getNodeDefinition());

        return doc;
    }
}
