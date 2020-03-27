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

import org.outerj.daisy.workflow.WfProcessInstance;
import org.outerj.daisy.workflow.WfExecutionPath;
import org.outerj.daisy.workflow.WfTask;
import org.outerx.daisy.x10Workflow.ProcessDocument;
import org.outerx.daisy.x10Workflow.TaskDocument;
import org.apache.xmlbeans.XmlCursor;

import java.util.*;
import java.text.DateFormat;

public class WfProcessInstanceImpl implements WfProcessInstance {
    private String id;
    private String definitionId;
    private Date start;
    private Date end;
    private boolean suspended;
    private WfExecutionPath rootExecutionPath;
    private Map<String, WfTask> tasks = new HashMap<String, WfTask>();
    private Locale locale;

    public WfProcessInstanceImpl(String id, String definitionId, Date start, Date end, boolean suspended,
            Map<String, WfTask> tasks, WfExecutionPath rootExecutionPath, Locale locale)  {
        if (id == null)
            throw new IllegalArgumentException("Null argument: id");
        if (definitionId == null)
            throw new IllegalArgumentException("Null argument: definitionId");
        if (start == null)
            throw new IllegalArgumentException("Null argument: start");
        if (tasks == null)
            throw new IllegalArgumentException("Null argument: tasks");
        if (rootExecutionPath == null)
            throw new IllegalArgumentException("Null argument: rootExecutionPath");
        if (locale == null)
            throw new IllegalArgumentException("Null argument: locale");

        this.id = id;
        this.definitionId = definitionId;
        this.start = start;
        this.end = end;
        this.rootExecutionPath = rootExecutionPath;
        this.tasks = tasks;
        this.suspended = suspended;
        this.locale = locale;
    }

    public String getId() {
        return id;
    }

    public Date getStart() {
        return (Date)start.clone();
    }

    public Date getEnd() {
        return end != null ? (Date)end.clone() : null;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public WfExecutionPath getRootExecutionPath() {
        return rootExecutionPath;
    }

    public WfTask getTask(String name) {
        return tasks.get(name);
    }

    public Collection<WfTask> getTasks() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public ProcessDocument getXml() {
        ProcessDocument document = ProcessDocument.Factory.newInstance();
        ProcessDocument.Process xml = document.addNewProcess();

        xml.setId(id);
        xml.setDefinitionId(definitionId);
        xml.setStart(WfXmlHelper.getCalendar(start));
        if (end != null)
            xml.setEnd(WfXmlHelper.getCalendar(end));
        xml.setSuspended(suspended);
        xml.setExecutionPath(rootExecutionPath.getXml().getExecutionPath());

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
        XmlCursor cursor = xml.newCursor();
        cursor.toNextToken();
        cursor.insertAttributeWithValue("startFormatted", dateFormat.format(start));
        if (end != null)
            cursor.insertAttributeWithValue("endFormatted", dateFormat.format(end));
        cursor.dispose();

        Collection<WfTask> taskCollection = tasks.values();
        TaskDocument.Task[] tasksXml = new TaskDocument.Task[taskCollection.size()];
        int i = 0;
        for (WfTask task : taskCollection) {
            tasksXml[i] = task.getXml().getTask();
            i++;
        }
        xml.addNewTasks().setTaskArray(tasksXml);

        return document;
    }
}
