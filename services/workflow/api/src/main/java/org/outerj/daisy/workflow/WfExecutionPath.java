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

import org.outerx.daisy.x10Workflow.ExecutionPathDocument;

import java.util.Date;
import java.util.Collection;

/**
 * An execution path in a workflow instance. There is
 * always one root execution path, which could have
 * childs e.g. in case of "fork" nodes. Progressing
 * the execution happens by completing {@link WfTask}s
 * or manually 'signalling' them using {@link WorkflowManager#signal}.
 */
public interface WfExecutionPath {
    String getPath();

    Date getStart();

    Date getEnd();

    /**
     * Child execution paths, returns an empty collection
     * if there are no children.
     */
    Collection<WfExecutionPath> getChildren();

    /**
     * Node where the execution is located.
     */
    WfNodeDefinition getNode();

    ExecutionPathDocument getXml();
}
