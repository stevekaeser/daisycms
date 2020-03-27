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

public enum TaskPriority {
    HIGHEST("highest"),
    HIGH("high"),
    NORMAL("normal"),
    LOW("low"),
    LOWEST("lowest");

    private final String name;

    private TaskPriority(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static TaskPriority fromString(String name) {
        if (HIGHEST.name.equals(name)) {
            return HIGHEST;
        } else if (HIGH.name.equals(name)) {
            return HIGH;
        } else if (NORMAL.name.equals(name)) {
            return NORMAL;
        } else if (LOW.name.equals(name)) {
            return LOW;
        } else if (LOWEST.name.equals(name)) {
            return LOWEST;
        } else {
            throw new RuntimeException("Invalid task priority name: " + name);
        }
    }
}
