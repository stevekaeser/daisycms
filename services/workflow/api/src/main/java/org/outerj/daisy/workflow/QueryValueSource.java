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
 * Used by {@link QueryOrderByItem} and {@link QuerySelectItem} to indicate
 * the scope of a variable name.
 */
public enum QueryValueSource {
    PROPERTY("property"),
    TASK_VARIABLE("task_variable"),
    PROCESS_VARIABLE("process_variable");

    private final String name;

    private QueryValueSource(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static QueryValueSource fromString(String name) {
        if (PROPERTY.name.equals(name)) {
            return PROPERTY;
        } else if (TASK_VARIABLE.name.equals(name)) {
            return TASK_VARIABLE;
        } else if (PROCESS_VARIABLE.name.equals(name)) {
            return PROCESS_VARIABLE;
        } else {
            throw new RuntimeException("Unrecognized value source: " + name);
        }
    }
}
