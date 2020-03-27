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

import org.outerj.daisy.workflow.*;
import org.outerj.daisy.i18n.I18nMessage;
import org.outerj.daisy.i18n.impl.StringI18nMessage;
import org.outerx.daisy.x10Workflow.ProcessDefinitionDocument;
import org.outerx.daisy.x10Workflow.TaskDefinitionDocument;
import org.outerx.daisy.x10Workflow.VariableDefinitionDocument;

import java.util.*;

public class WfProcessDefinitionImpl implements WfProcessDefinition {
    private final String id;
    private final String name;
    private final String version;
    private final List<String> problems;
    private final Map<String, WfTaskDefinition> taskDefinitions;
    private final WfTaskDefinition startTaskDefinition;
    private final WfNodeDefinition startNodeDefinition;
    private final I18nMessage label;
    private final I18nMessage description;
    private final List<WfVariableDefinition> globalVariableDefinitions;

    public WfProcessDefinitionImpl(String id, String name, String version, List<String> problems,
            Map<String, WfTaskDefinition> taskDefinitions, WfTaskDefinition startTaskDefinition,
            WfNodeDefinition startNodeDefinition, I18nMessage label, I18nMessage description,
            List<WfVariableDefinition> globalVariableDefinitions) {
        if (id == null)
            throw new IllegalArgumentException("Null argument: id");
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        if (version == null)
            throw new IllegalArgumentException("Null argument: version");
        if (taskDefinitions == null)
            throw new IllegalArgumentException("Null argument: taskDefinitions");
        if (startNodeDefinition == null)
            throw new IllegalArgumentException("Null argument: startNodeDefinition");
        if (globalVariableDefinitions == null)
            throw new IllegalArgumentException("Null argument: globalVariableDefinitions");
        // problems and startTaskDefinition can be null

        this.id = id;
        this.name = name;
        this.version = version;
        this.problems = problems != null ? Collections.unmodifiableList(problems) : null;
        this.taskDefinitions = Collections.unmodifiableMap(taskDefinitions);
        this.startTaskDefinition = startTaskDefinition;
        this.startNodeDefinition = startNodeDefinition;
        this.label = label;
        this.description = description;
        this.globalVariableDefinitions = globalVariableDefinitions;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public I18nMessage getLabel() {
        return label != null ? label : new StringI18nMessage(getName());
    }

    public I18nMessage getDescription() {
        return description;
    }

    public List<String> getProblems() {
        return problems;
    }

    public WfTaskDefinition getStartTaskDefinition() {
        return startTaskDefinition;
    }

    public WfNodeDefinition getStartNode() {
        return startNodeDefinition;
    }

    public Collection<WfTaskDefinition> getTasks() {
        return Collections.unmodifiableCollection(taskDefinitions.values());
    }

    public WfTaskDefinition getTask(String name) {
        return taskDefinitions.get(name);
    }

    public List<WfVariableDefinition> getGlobalVariableDefinitions() {
        return globalVariableDefinitions;
    }

    public WfVariableDefinition getGlobalVariable(String name) {
        for (WfVariableDefinition variable : globalVariableDefinitions) {
            if (variable.getName().equals(name)) {
                return variable;
            }
        }
        return null;
    }

    public ProcessDefinitionDocument getXml() {
        ProcessDefinitionDocument doc = ProcessDefinitionDocument.Factory.newInstance();
        ProcessDefinitionDocument.ProcessDefinition xml = doc.addNewProcessDefinition();
        xml.setId(id);
        xml.setName(name);
        xml.setVersion(version);
        xml.addNewLabel().set(label != null ? WfXmlHelper.i18nMessageToXml(label) : WfXmlHelper.stringToXml(name));
        if (description != null)
            xml.addNewDescription().set(WfXmlHelper.i18nMessageToXml(description));
        xml.addNewStartNodeDefinition().setNodeDefinition(startNodeDefinition.getXml().getNodeDefinition());

        if (startTaskDefinition != null) {
            xml.addNewStartTask().setTaskDefinition(startTaskDefinition.getXml().getTaskDefinition());
        }

        Collection<WfTaskDefinition> taskDefs = taskDefinitions.values();
        TaskDefinitionDocument.TaskDefinition taskDefsXml[] = new TaskDefinitionDocument.TaskDefinition[taskDefs.size()];
        int i = 0;
        for (WfTaskDefinition taskDef : taskDefs) {
            taskDefsXml[i] = taskDef.getXml().getTaskDefinition();
            i++;
        }
        xml.addNewTasks().setTaskDefinitionArray(taskDefsXml);

        VariableDefinitionDocument.VariableDefinition varDefsXml[] = new VariableDefinitionDocument.VariableDefinition[globalVariableDefinitions.size()];
        for (i = 0; i < globalVariableDefinitions.size(); i++) {
            varDefsXml[i] = globalVariableDefinitions.get(i).getXml().getVariableDefinition();
        }
        xml.addNewVariableDefinitions().setVariableDefinitionArray(varDefsXml);

        if (problems != null) {
            ProcessDefinitionDocument.ProcessDefinition.Problems problemsXml = xml.addNewProblems();
            for (String problem : problems) {
                problemsXml.addProblem(problem);
            }
        }

        return doc;
    }
}
