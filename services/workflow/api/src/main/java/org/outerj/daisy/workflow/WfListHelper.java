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

import org.outerx.daisy.x10Workflow.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class WfListHelper {
    /**
     * Gets the XML representation of all the items in the list.
     */
    public static ProcessDefinitionsDocument getProcessDefinitionsAsXml(List<WfProcessDefinition> processDefinitions) {
        ProcessDefinitionsDocument doc = ProcessDefinitionsDocument.Factory.newInstance();
        ProcessDefinitionsDocument.ProcessDefinitions xml = doc.addNewProcessDefinitions();

        ProcessDefinitionDocument.ProcessDefinition[] defsXml = new ProcessDefinitionDocument.ProcessDefinition[processDefinitions.size()];
        for (int i = 0; i < processDefinitions.size(); i++) {
            defsXml[i] = processDefinitions.get(i).getXml().getProcessDefinition();
        }

        xml.setProcessDefinitionArray(defsXml);

        return doc;
    }

    /**
     * Gets the XML representation of all the items in the list.
     */
    public static TasksDocument getTasksAsXml(List<WfTask> tasks) {
        TaskDocument.Task[] tasksXml = new TaskDocument.Task[tasks.size()];
        for (int i = 0; i < tasks.size(); i++) {
            tasksXml[i] = tasks.get(i).getXml().getTask();
        }

        TasksDocument doc = TasksDocument.Factory.newInstance();
        doc.addNewTasks().setTaskArray(tasksXml);
        return doc;
    }

    public static ProcessesDocument getProcessesAsXml(List<WfProcessInstance> processes) {
        ProcessDocument.Process[] processesXml = new ProcessDocument.Process[processes.size()];
        for (int i = 0; i < processes.size(); i++) {
            processesXml[i] = processes.get(i).getXml().getProcess();
        }

        ProcessesDocument doc = ProcessesDocument.Factory.newInstance();
        doc.addNewProcesses().setProcessArray(processesXml);
        return doc;
    }

    public static TimersDocument getTimersAsXml(List<WfTimer> timers) {
        TimerDocument.Timer[] timersXml = new TimerDocument.Timer[timers.size()];
        for (int i = 0; i < timers.size(); i++) {
            timersXml[i] = timers.get(i).getXml().getTimer();
        }

        TimersDocument doc = TimersDocument.Factory.newInstance();
        doc.addNewTimers().setTimerArray(timersXml);
        return doc;
    }

    /**
     * Gets the XML representation of all the items in the list.
     */
    public static PoolsDocument getPoolsAsXml(List<WfPool> pools) {
        PoolDocument.Pool[] poolsXml = new PoolDocument.Pool[pools.size()];
        for (int i = 0; i < poolsXml.length; i++) {
            poolsXml[i] = pools.get(i).getXml().getPool();
        }

        PoolsDocument doc = PoolsDocument.Factory.newInstance();
        doc.addNewPools().setPoolArray(poolsXml);
        return doc;
    }

    public static UsersDocument getUserIdsAsXml(List<Long> userIdsList) {
        long[] userIds = new long[userIdsList.size()];
        for (int i = 0; i < userIds.length; i++)
            userIds[i] = userIdsList.get(i);

        UsersDocument document = UsersDocument.Factory.newInstance();
        document.addNewUsers().setIdArray(userIds);
        return document;
    }

    public static ProcessInstanceCountsDocument getProcessInstanceCountsAsXml(Map<String, Integer> counts) {
        List<Map.Entry<String, Integer>> countEntries = new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());

        ProcessInstanceCountsDocument.ProcessInstanceCounts.ProcessInstanceCount[] countsXml = new ProcessInstanceCountsDocument.ProcessInstanceCounts.ProcessInstanceCount[countEntries.size()];
        for (int i = 0; i < countsXml.length; i++) {
            countsXml[i] = ProcessInstanceCountsDocument.ProcessInstanceCounts.ProcessInstanceCount.Factory.newInstance();
            countsXml[i].setDefinitionId(countEntries.get(i).getKey());
            countsXml[i].setCount(countEntries.get(i).getValue());
        }

        ProcessInstanceCountsDocument document = ProcessInstanceCountsDocument.Factory.newInstance();
        document.addNewProcessInstanceCounts().setProcessInstanceCountArray(countsXml);
        return document;
    }
}
