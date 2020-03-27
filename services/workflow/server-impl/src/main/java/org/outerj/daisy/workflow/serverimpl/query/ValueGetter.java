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

import java.util.Locale;

public interface ValueGetter {
    Object getValue(Provider provider) throws WorkflowException;

    Object getLabel(Provider provider, Object value, Locale locale) throws WorkflowException;

    public interface Provider {
        TaskInstance getTaskInstance() throws WorkflowException;

        WfTaskDefinition getTaskDefinition() throws WorkflowException;

        ProcessInstance getProcessInstance();

        WfProcessDefinition getProcessDefinition() throws WorkflowException;

        Timer getTimer();

        IntWfContext getContext();
    }
}
