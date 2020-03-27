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
package org.outerj.daisy.workflow.serverimpl.query;

import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.job.Timer;
import org.outerj.daisy.workflow.WfTaskDefinition;
import org.outerj.daisy.workflow.WfProcessDefinition;
import org.outerj.daisy.workflow.WorkflowException;
import org.outerj.daisy.workflow.serverimpl.IntWfContext;

public class ValueGetterProvider implements ValueGetter.Provider {
    private ProcessInstance processInstance;
    private TaskInstance taskInstance;
    private Timer timer;
    private WfTaskDefinition taskDefinition;
    private WfProcessDefinition processDefinition;
    private IntWfContext context;

    public ValueGetterProvider(ProcessInstance processInstance, TaskInstance taskInstance, Timer timer, IntWfContext context) {
        this.processInstance = processInstance;
        this.taskInstance = taskInstance;
        this.timer = timer;
        this.context = context;
    }

    public TaskInstance getTaskInstance() throws WorkflowException {
        if (taskInstance == null)
            throw new WorkflowException("Trying to access task information in query while no task is available.");
        return taskInstance;
    }

    public WfTaskDefinition getTaskDefinition() throws WorkflowException {
        if (taskInstance == null) {
            throw new WorkflowException("Trying to access task information in query while no task is available.");
        } else {
            if (taskDefinition == null) {
                taskDefinition = context.getWfObjectBuilder().getTaskDefinition(taskInstance.getTask(), context.getLocale());
            }
            return taskDefinition;
        }
    }

    public ProcessInstance getProcessInstance() {
        return processInstance;
    }

    public WfProcessDefinition getProcessDefinition() throws WorkflowException {
        if (processDefinition == null) {
            processDefinition = context.getWfObjectBuilder().getProcessDefinition(getProcessInstance().getProcessDefinition(), context.getLocale());
        }
        return processDefinition;
    }

    public Timer getTimer() {
        return timer;
    }

    public IntWfContext getContext() {
        return context;
    }

    public static interface ValueGetterProviderBuilder {
        ValueGetter.Provider getProvider(Object object);
    }

    public static ValueGetterProviderBuilder getTaskValueProviderBuilder(final IntWfContext context) {
        return new ValueGetterProviderBuilder() {
            public ValueGetter.Provider getProvider(Object object) {
                TaskInstance task = (TaskInstance)object;
                return new ValueGetterProvider(task.getToken().getProcessInstance(), task, null, context);
            }
        };
    }

    public static ValueGetterProviderBuilder getProcessValueProviderBuilder(final IntWfContext context) {
        return new ValueGetterProviderBuilder() {
            public ValueGetter.Provider getProvider(Object object) {
                return new ValueGetterProvider((ProcessInstance)object, null, null, context);
            }
        };
    }

    public static ValueGetterProviderBuilder getTimerValueProviderBuilder(final IntWfContext context) {
        return new ValueGetterProviderBuilder() {
            public ValueGetter.Provider getProvider(Object object) {
                Timer timer = (Timer)object;
                return new ValueGetterProvider(timer.getProcessInstance(), null, timer, context);
            }
        };
    }
}
