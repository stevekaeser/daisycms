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

import org.outerx.daisy.x10Workflow.ProcessDocument;

import java.util.Date;
import java.util.Collection;

public interface WfProcessInstance {
    String getId();

    Date getStart();

    /**
     * The end time of this workflow instance, if it has ended already,
     * otherwise null.
     */
    Date getEnd();

    boolean isSuspended();

    WfExecutionPath getRootExecutionPath();

    WfTask getTask(String name);

    Collection<WfTask> getTasks();

    String getDefinitionId();

    ProcessDocument getXml();
}
