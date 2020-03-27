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

import java.util.*;

/**
 * Holds the information with which to update a task.
 * Information missing in this object is simply left untouched in the task
 * when updating it. Note that to remove variables, you need to explicitely
 * call {@link #deleteVariable}.
 */
public class TaskUpdateData {
    private Map<VariableKey, VariableValue> variables = new HashMap<VariableKey, VariableValue>();
    private Set<VariableKey> deletedVariables = new HashSet<VariableKey>();
    private TaskPriority priority;
    private Date dueDate;
    private boolean clearDueDate = false;

    public void setVariable(String name, VariableScope scope, WfValueType type, Object value) {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        if (scope == null)
            throw new IllegalArgumentException("Null argument: scope");
        if (value == null)
            throw new IllegalArgumentException("Null argument: value");
        if (type == null)
            throw new IllegalArgumentException("Null argument: type");

        if (!type.getTypeClass().isAssignableFrom(value.getClass()))
            throw new IllegalArgumentException("Illegal value for given variable type, expected a " + type.getTypeClass().getName() + " but got a " + value.getClass().getName());

        VariableKey key = new VariableKey(name, scope);
        deletedVariables.remove(key);
        variables.put(key, new VariableValue(type, value));
    }

    public Object getVariable(String name, VariableScope scope) {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        if (scope == null)
            throw new IllegalArgumentException("Null argument: scope");

        VariableValue variableValue = variables.get(new VariableKey(name, scope));
        return variableValue != null ? variableValue.getValue() : null;
    }

    public void deleteVariable(String name, VariableScope scope) {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        if (scope == null)
            throw new IllegalArgumentException("Null argument: scope");

        VariableKey key = new VariableKey(name, scope);
        variables.remove(key);
        deletedVariables.add(key);
    }

    public boolean isVariableDeleted(String name, VariableScope scope) {
        return deletedVariables.contains(new VariableKey(name, scope));
    }

    /**
     * Clears information about set and deleted variables.
     */
    public void clearVariables() {
        variables.clear();
        deletedVariables.clear();
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void clearDueDate() {
        this.dueDate = null;
        this.clearDueDate = true;
    }

    public boolean getClearDueDate() {
        return clearDueDate;
    }

    public Map<VariableKey, VariableValue> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public Set<VariableKey> getDeletedVariables() {
        return Collections.unmodifiableSet(deletedVariables);
    }

    public final class VariableKey {
        private final String name;
        private final VariableScope scope;
        private final int hash;

        public VariableKey(String name, VariableScope scope) {
            this.name = name;
            this.scope = scope;
            this.hash = (name + scope).hashCode();
        }

        public String getName() {
            return name;
        }

        public VariableScope getScope() {
            return scope;
        }

        public int hashCode() {
            return hash;
        }

        public boolean equals(Object obj) {
            VariableKey other = (VariableKey)obj;
            return name.equals(other.name) && scope == other.scope;
        }
    }

    public final class VariableValue {
        private final WfValueType type;
        private final Object value;

        public VariableValue(WfValueType type, Object value) {
            this.type = type;
            this.value = value;
        }

        public WfValueType getType() {
            return type;
        }

        public Object getValue() {
            return value;
        }
    }
}
