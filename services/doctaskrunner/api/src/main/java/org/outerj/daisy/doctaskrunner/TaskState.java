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
package org.outerj.daisy.doctaskrunner;

public enum TaskState {
    INITIALISING("initialising", "I"),
    RUNNING("running", "R"),
    FINISHED("finished", "F"),
    FINISHED_WITH_ERRORS("finished_with_errors", "G"),
    FINISHED_WITH_FAILURES("finished_with_failures", "H"),
    INTERRUPTED_BY_ERROR("interrupted_by_error", "E"),
    INTERRUPTED_BY_USER("interrupted_by_user", "U"),
    INTERRUPTED_BY_SHUTDOWN("interrupted_by_shutdown", "S"),
    RESUMING_AFTER_SHUTDOWN("resuming_after_shutdown", "X");

    private final String name;
    private final String code;

    private TaskState(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String toString() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public static TaskState getByCode(String code) {
        if (code.equals(INITIALISING.code))
            return INITIALISING;
        else if (code.equals(RUNNING.code))
            return RUNNING;
        else if (code.equals(FINISHED.code))
            return FINISHED;
        else if (code.equals(FINISHED_WITH_ERRORS.code))
            return FINISHED_WITH_ERRORS;
        else if (code.equals(FINISHED_WITH_FAILURES.code))
            return FINISHED_WITH_FAILURES;
        else if (code.equals(INTERRUPTED_BY_ERROR.code))
            return INTERRUPTED_BY_ERROR;
        else if (code.equals(INTERRUPTED_BY_USER.code))
            return INTERRUPTED_BY_USER;
        else if (code.equals(INTERRUPTED_BY_SHUTDOWN.code))
            return INTERRUPTED_BY_SHUTDOWN;
        else
            throw new RuntimeException("TaskState: unrecognized code: \"" + code + "\".");
    }

    public static TaskState fromString(String name) {
        if (name.equals(INITIALISING.name))
            return INITIALISING;
        else if (name.equals(RUNNING.name))
            return RUNNING;
        else if (name.equals(FINISHED.name))
            return FINISHED;
        else if (name.equals(FINISHED_WITH_ERRORS.name))
            return FINISHED_WITH_ERRORS;
        else if (name.equals(FINISHED_WITH_FAILURES.name))
            return FINISHED_WITH_FAILURES;
        else if (name.equals(INTERRUPTED_BY_ERROR.name))
            return INTERRUPTED_BY_ERROR;
        else if (name.equals(INTERRUPTED_BY_USER.name))
            return INTERRUPTED_BY_USER;
        else if (name.equals(INTERRUPTED_BY_SHUTDOWN.name))
            return INTERRUPTED_BY_SHUTDOWN;
        else
            throw new RuntimeException("TaskState: unrecognized name: \"" + name + "\".");
    }

    public boolean isStoppedState() {
        return (this == FINISHED || this == FINISHED_WITH_ERRORS || this == FINISHED_WITH_FAILURES ||  this == INTERRUPTED_BY_ERROR || this == INTERRUPTED_BY_USER || this == INTERRUPTED_BY_SHUTDOWN);
    }

}
