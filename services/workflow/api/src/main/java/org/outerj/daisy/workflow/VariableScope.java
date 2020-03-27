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

/**
 * The scope of task variables. Currently supported are either
 * global variables (bound to the root execution path) and
 * task variables (associated with a task). Variables bound
 * to specific execution paths are currently not supported
 * (they are supported in jBPM of course, but can't be accessed
 * through the Daisy meta-variable and query systems).
 */
public enum VariableScope {
    TASK("task"),
    GLOBAL("global");

    private String name;

    private VariableScope(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static VariableScope fromString(String name) {
        if (GLOBAL.name.equals(name))
            return GLOBAL;
        else if (TASK.name.equals(name))
            return TASK;
        else
            throw new RuntimeException("Unexpected variable scope: " + name);
    }
}
