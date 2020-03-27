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

import org.outerx.daisy.x10Workflow.TimerDocument;

import java.util.Date;

/**
 * A timer in a workflow process instance.
 */
public interface WfTimer {
    String getId();

    String getName();

    Date getDueDate();
    
    String getRecurrence();

    /**
     * If the execution of the timer action caused an error, this will contain
     * the error description. Is always null before timer is executed.
     */
    String getException();

    /**
     * Returns true if this timer is suspended.
     *
     * <p>A timer becomes suspended when the process it belongs to is suspended.
     */
    boolean isSuspended();

    /**
     * Returns the ID of the process instance to which this timer belongs.
     */
    String getProcessId();

    /**
     * Returns the execution path in the workflow to where
     * this timer was created.
     */
    String getExecutionPath();

    /**
     * Returns the name of the transition that is or will be followed
     * when the timer completes. Can be null when no transition should
     * be followed.
     */
    String getTransitionName();

    TimerDocument getXml();
}
