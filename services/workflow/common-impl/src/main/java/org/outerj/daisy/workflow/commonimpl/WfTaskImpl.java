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
import org.outerx.daisy.x10Workflow.TaskDocument;
import org.outerx.daisy.x10Workflow.PriorityType;
import org.outerx.daisy.x10Workflow.VariableDocument;
import org.apache.xmlbeans.XmlCursor;

import java.util.*;
import java.text.DateFormat;

public class WfTaskImpl implements WfTask {
    private String id;
    private Date created;
    private Date end;
    private Date dueDate;
    private TaskPriority priority;
    private long actorId;
    private boolean hasPools;
    private boolean hasSwimlane;
    private String processId;
    private String executionPath;
    private List<WfVariable> variables;
    private WfTaskDefinition taskDefinition;
    private Locale locale;

    public WfTaskImpl(String id, Date created, Date end, TaskPriority priority, Date dueDate,
            long actorId, boolean hasPools, boolean hasSwimlane, String processId, String executionPath,
            List<WfVariable> variables, WfTaskDefinition taskDefinition, Locale locale) {
        if (id == null)
            throw new IllegalArgumentException("Null argument: id");
        if (created == null)
            throw new IllegalArgumentException("Null argument: created");
        if (priority == null)
            throw new IllegalArgumentException("Null argument: priority");
        if (processId == null)
            throw new IllegalArgumentException("Null argument: processId");
        if (executionPath == null)
            throw new IllegalArgumentException("Null argument: executionPath");
        if (variables == null)
            throw new IllegalArgumentException("Null argument: variables");
        if (taskDefinition == null)
            throw new IllegalArgumentException("Null argument: taskDefinition");
        if (locale == null)
            throw new IllegalArgumentException("Null argument: locale");

        this.id = id;
        this.created = created;
        this.end = end;
        this.dueDate = dueDate;
        this.priority = priority;
        this.actorId = actorId;
        this.hasPools = hasPools;
        this.hasSwimlane = hasSwimlane;
        this.processId = processId;
        this.executionPath = executionPath;
        this.variables = Collections.unmodifiableList(variables);
        this.taskDefinition = taskDefinition;
        this.locale = locale;
    }

    public String getId() {
        return id;
    }

    public Date getCreated() {
        return (Date)created.clone();
    }

    public Date getEnd() {
        return end != null ? (Date)end.clone() : null;
    }

    public Date getDueDate() {
        return dueDate != null ? (Date)dueDate.clone() : null;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public long getActorId() {
        return actorId;
    }

    public boolean hasPools() {
        return hasPools;
    }

    public boolean hasSwimlane() {
        return hasSwimlane;
    }

    public String getProcessId() {
        return processId;
    }

    public String getExecutionPath() {
        return executionPath;
    }

    public WfVariable getVariable(String name) {
        WfVariable variable = getVariable(name, VariableScope.TASK);
        if (variable != null)
            return variable;

        return getVariable(name, VariableScope.GLOBAL);
    }

    public WfVariable getVariable(String name, VariableScope scope) {
        for (WfVariable variable : variables) {
            if (variable.getName().equals(name) && variable.getScope() == scope) {
                return variable;
            }
        }
        return null;
    }

    public List<WfVariable> getVariables() {
        return variables;
    }

    public WfTaskDefinition getDefinition() {
        return taskDefinition;
    }

    public TaskDocument getXml() {
        TaskDocument document = TaskDocument.Factory.newInstance();
        TaskDocument.Task xml = document.addNewTask();

        xml.setId(id);
        xml.setExecutionPath(executionPath);
        xml.setPriority(PriorityType.Enum.forString(priority.toString()));
        xml.setActorId(actorId);
        xml.setHasPools(hasPools);
        xml.setHasSwimlane(hasSwimlane);
        xml.setCreated(WfXmlHelper.getCalendar(created));
        if (dueDate != null)
            xml.setDueDate(WfXmlHelper.getCalendar(dueDate));
        if (end != null)
            xml.setEnd(WfXmlHelper.getCalendar(end));
        xml.setProcessId(processId);

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
        XmlCursor cursor = xml.newCursor();
        cursor.toNextToken();
        cursor.insertAttributeWithValue("createdFormatted", dateFormat.format(created));
        if (dueDate != null)
            cursor.insertAttributeWithValue("dueDateFormatted", dateFormat.format(dueDate));
        if (end != null)
            cursor.insertAttributeWithValue("endFormatted", dateFormat.format(end));
        cursor.dispose();

        VariableDocument.Variable[] variablesXml = new VariableDocument.Variable[variables.size()];
        int i = 0;
        for (WfVariable variable : variables) {
            variablesXml[i] = variable.getXml().getVariable();
            i++;
        }
        xml.addNewVariables().setVariableArray(variablesXml);

        xml.setTaskDefinition(taskDefinition.getXml().getTaskDefinition());

        return document;
    }
}
