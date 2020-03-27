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

import org.outerj.daisy.workflow.WfTimer;
import org.outerx.daisy.x10Workflow.TimerDocument;
import org.apache.xmlbeans.XmlCursor;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.text.DateFormat;

public class WfTimerImpl implements WfTimer {
    private final String id;
    private final String name;
    private final Date dueDate;
    private final String recurrence;
    private final String exception;
    private final boolean suspended;
    private final String processId;
    private final String executionPath;
    private final String transitionName;
    private final Locale locale;

    public WfTimerImpl(String id, String name, Date dueDate, String recurrence, String exception, boolean suspended, String processId,
            String executionPath, String transitionName, Locale locale) {
        if (id == null)
            throw new IllegalArgumentException("Null argument: id");
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        if (dueDate == null)
            throw new IllegalArgumentException("Null argument: dueDate");
        if (processId == null)
            throw new IllegalArgumentException("Null argument: processId");
        if (executionPath == null)
            throw new IllegalArgumentException("Null argument: executionPath");
        if (locale == null)
            throw new IllegalArgumentException("Null argument: locale");

        this.id = id;
        this.name = name;
        this.dueDate = dueDate;
        this.recurrence = recurrence;
        this.exception = exception;
        this.suspended = suspended;
        this.processId = processId;
        this.executionPath = executionPath;
        this.transitionName = transitionName;
        this.locale = locale;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Date getDueDate() {
        return (Date)dueDate.clone();
    }
    
    public String getRecurrence() {
        return recurrence;
    }

    public String getException() {
        return exception;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public String getProcessId() {
        return processId;
    }

    public String getExecutionPath() {
        return executionPath;
    }

    public String getTransitionName() {
        return transitionName;
    }

    public TimerDocument getXml() {
        TimerDocument document = TimerDocument.Factory.newInstance();
        TimerDocument.Timer xml = document.addNewTimer();

        xml.setId(id);
        xml.setName(name);  
        
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(dueDate);
        xml.setDueDate(calendar);
        
        if (recurrence != null) {
            xml.setRecurrence(recurrence);
        }

        if (exception != null)
            xml.setException(exception);

        xml.setSuspended(suspended);
        xml.setProcessId(processId);
        xml.setExecutionPath(executionPath);

        if (transitionName != null)
            xml.setTransitionName(transitionName);

        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
        XmlCursor cursor = xml.newCursor();
        cursor.toNextToken();
        cursor.insertAttributeWithValue("dueDateFormatted", dateFormat.format(dueDate));
        cursor.dispose();
        
        return document;
    }
}
