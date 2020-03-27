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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;


public class QueryConditions {
    private boolean allCriteria = true;
    private List<PropertyConditionInfo> propertyConditions = new ArrayList<PropertyConditionInfo>();
    private List<VariableConditionInfo> taskVariableConditions = new ArrayList<VariableConditionInfo>();
    private List<VariableConditionInfo> processVariableConditions = new ArrayList<VariableConditionInfo>();
    private List<SpecialConditionInfo> specialConditions = new ArrayList<SpecialConditionInfo>();

    /**
     *
     * @param allCriteria true if all conditions must be met (AND), or false if just one
     *        condition needs to be met (OR)
     */
    public void setMeetAllCriteria(boolean allCriteria) {
        this.allCriteria = allCriteria;
    }

    public void addCondition(String propertyName, WfValueType type, String operatorName, Object... values) {
        if (propertyName == null)
            throw new IllegalArgumentException("Null argument: propertyName");
        if (type == null)
            throw new IllegalArgumentException("Null argument: type");
        if (operatorName == null)
            throw new IllegalArgumentException("Null argument: operatorName");
        checkValues(type, values);

        propertyConditions.add(new PropertyConditionInfo(propertyName, type, operatorName, values));
    }

    public void addTaskVariableCondition(String variableName, WfValueType type, String operatorName, Object... values) {
        if (variableName == null)
            throw new IllegalArgumentException("Null argument: variableName");
        if (type == null)
            throw new IllegalArgumentException("Null argument: type");
        if (operatorName == null)
            throw new IllegalArgumentException("Null argument: operatorName");
        checkValues(type, values);
        taskVariableConditions.add(new VariableConditionInfo(variableName, type, operatorName, values));
    }

    public void addProcessVariableCondition(String variableName, WfValueType type, String operatorName, Object... values) {
        if (variableName == null)
            throw new IllegalArgumentException("Null argument: variableName");
        if (type == null)
            throw new IllegalArgumentException("Null argument: type");
        if (operatorName == null)
            throw new IllegalArgumentException("Null argument: operatorName");
        checkValues(type, values);
        processVariableConditions.add(new VariableConditionInfo(variableName, type, operatorName, values));
    }

    private void checkValues(WfValueType valueType, Object[] values) {
        for (Object value : values) {
            if (value == null)
                throw new IllegalArgumentException("One of the supplied query condition values is null, which is not allowed.");
            if (!valueType.getTypeClass().isAssignableFrom(value.getClass()))
                throw new IllegalArgumentException("One of the supplied query condition values is of an unexpected type. Expected " + valueType.getTypeClass().getName() + " but got a " + value.getClass().getName() + " for the value " + value.toString());
        }
    }

    public void addSpecialCondition(String name, WfValueType[] argTypes, Object[] argValues) {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        if (argTypes == null)
            throw new IllegalArgumentException("Null argument: argTypes");
        if (argValues == null)
            throw new IllegalArgumentException("Null argument: argValues");

        if (argTypes.length != argValues.length)
            throw new IllegalArgumentException("argTypes and argValues sizes do not correspond.");

        for (int i = 0; i < argValues.length; i++) {
            if (!argTypes[i].getTypeClass().isAssignableFrom(argValues[i].getClass()))
                throw new IllegalArgumentException("Value of argument " + i + " of the special condition " + name + " is of an incorrect type. You specified " + argTypes[i].getTypeClass().getName() + " but gave a " + argValues[i].getClass().getName());
        }
        specialConditions.add(new SpecialConditionInfo(name, argTypes, argValues));
    }

    public List<PropertyConditionInfo> getPropertyConditions() {
        return Collections.unmodifiableList(propertyConditions);
    }

    public List<VariableConditionInfo> getTaskVariableConditions() {
        return Collections.unmodifiableList(taskVariableConditions);
    }

    public List<VariableConditionInfo> getProcessVariableConditions() {
        return Collections.unmodifiableList(processVariableConditions);
    }

    public List<SpecialConditionInfo> getSpecialConditions() {
        return Collections.unmodifiableList(specialConditions);
    }

    public boolean getMatchAllCriteria() {
        return allCriteria;
    }

    public static class PropertyConditionInfo {
        public final String propertyName;
        public final WfValueType type;
        public final String operatorName;
        public final List<Object> values;

        public PropertyConditionInfo(String propertyName, WfValueType type, String operatorName, Object[] values) {
            this.propertyName = propertyName;
            this.type = type;
            this.operatorName = operatorName;
            this.values = Collections.unmodifiableList(Arrays.asList(values));
        }
    }

    public static class VariableConditionInfo {
        public final String name;
        public final WfValueType type;
        public final String operatorName;
        public final List<Object> values;

        public VariableConditionInfo(String name, WfValueType type, String operatorName, Object[] values) {
            this.name = name;
            this.type = type;
            this.operatorName = operatorName;
            this.values = Collections.unmodifiableList(Arrays.asList(values));
        }
    }

    public static class SpecialConditionInfo {
        public final String name;
        public final List<WfValueType> argTypes;
        public final List<Object> argValues;

        public SpecialConditionInfo(String name, WfValueType[] argTypes, Object[] argValues) {
            this.name = name;
            this.argTypes = Collections.unmodifiableList(Arrays.asList(argTypes));
            this.argValues = Collections.unmodifiableList(Arrays.asList(argValues));
        }
    }
}
