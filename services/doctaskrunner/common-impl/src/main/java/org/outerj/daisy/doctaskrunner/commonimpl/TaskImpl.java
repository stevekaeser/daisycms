/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.doctaskrunner.commonimpl;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.outerj.daisy.doctaskrunner.Task;
import org.outerj.daisy.doctaskrunner.TaskState;
import org.outerx.daisy.x10Doctaskrunner.TaskDocument;

public class TaskImpl implements Task {
    private final long id;
    private final String description;
    private final TaskState state;
    private final long ownerId;
    private final String progress;
    private final String details;
    private final String actionParameters;
    private final String actionType;
    private final Date startedAt;
    private final Date finishedAt;
    private final int tryCount;
    private final int maxTries;
    private final int retryInterval;

    public TaskImpl(long id, String description, TaskState state, long ownerId, String progress, String details,
            String actionType, String actionParameters, Date startedAt, Date finishedAt, int tryCount, int maxTries, int retryInterval) {
        this.id = id;
        this.description = description;
        this.state = state;
        this.ownerId = ownerId;
        this.progress = progress;
        this.details = details;
        this.actionType = actionType;
        this.actionParameters = actionParameters;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.tryCount = tryCount;
        this.maxTries = maxTries;
        this.retryInterval = retryInterval;
    }

    public long getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public TaskState getState() {
        return state;
    }

    public long getOwnerId() {
        return ownerId;
    }

    public String getProgressIndication() {
        return progress;
    }

    public String getDetails() {
        return details;
    }

    public String getActionParameters() {
        return actionParameters;
    }

    public String getActionType() {
        return actionType;
    }

    public Date getStartedAt() {
        if  (startedAt == null)
            return null;
        return (Date)startedAt.clone();
    }

    public Date getFinishedAt() {
        if (finishedAt == null)
            return null;
        return (Date)finishedAt.clone();
    }
    
    public int getTryCount() {
        return this.tryCount;
    }
   
    public int getRetryInterval() {
        return retryInterval;
    }

    public int getMaxTries() {
        return maxTries;
    }

    public TaskDocument getXml() {
        TaskDocument taskDocument = TaskDocument.Factory.newInstance();
        TaskDocument.Task taskXml = taskDocument.addNewTask();

        taskXml.setId(id);
        taskXml.setDescription(description);
        taskXml.setOwnerId(ownerId);
        taskXml.setProgress(progress);
        TaskDocument.Task.Action actionXml = taskXml.addNewAction();
        actionXml.setType(actionType);
        if  (actionParameters != null)
            actionXml.setParameters(actionParameters);
        taskXml.setStartedAt(getCalendar(startedAt));
        if (finishedAt != null)
            taskXml.setFinishedAt(getCalendar(finishedAt));
        taskXml.setState(TaskDocument.Task.State.Enum.forString(state.toString()));
        if (details != null)
            taskXml.setDetails(details);
        taskXml.setTryCount(this.tryCount);
        taskXml.setMaxTries(this.maxTries);
        taskXml.setRetryInterval(this.retryInterval);
        return taskDocument;
    }

    private Calendar getCalendar(Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }
    
    public String toString() {
        return new StringBuffer("TaskImpl[id=")
        .append(id)
        .append(",description=")
        .append(description)
        .append(",state=")
        .append(state)
        .append(",ownerId=")
        .append(ownerId)
        .append(",progress=")
        .append(progress)
        .append(",details=")
        .append(details)
        .append(",actionParameters=")
        .append(actionParameters)
        .append(",actionType=")
        .append(actionType)
        .append(",startedAt=")
        .append(startedAt)
        .append(",finishedAt=")
        .append(finishedAt).toString();
    }

}
