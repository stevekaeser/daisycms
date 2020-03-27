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

import java.util.HashMap;
import java.util.Map;

public enum WfVariableType {
    STRING(WfValueType.STRING, false),
    MULTI_STRING(WfValueType.STRING, true),
    DAISY_LINK(WfValueType.DAISY_LINK, false),
    LONG(WfValueType.LONG, false),
    DATE(WfValueType.DATE, false),
    DATETIME(WfValueType.DATETIME, false),
    ACTOR(WfValueType.ACTOR, false),
    USER(WfValueType.USER, false),
    BOOLEAN(WfValueType.BOOLEAN, false),
    ID(WfValueType.ID, false);
    
    private static Map<String, WfVariableType> lookupTable = new HashMap<String, WfVariableType>();
    static {
        lookupTable.put(STRING.toString(), STRING);
        lookupTable.put(MULTI_STRING.toString(), MULTI_STRING);
        lookupTable.put(DAISY_LINK.toString(), DAISY_LINK);
        lookupTable.put(LONG.toString(), LONG);
        lookupTable.put(DATE.toString(), DATE);
        lookupTable.put(DATETIME.toString(), DATETIME);
        lookupTable.put(ACTOR.toString(), ACTOR);
        lookupTable.put(USER.toString(), USER);
        lookupTable.put(BOOLEAN.toString(), BOOLEAN);
        lookupTable.put(ID.toString(), ID);
    }
    
    private WfValueType valueType;
    
    private boolean multiValue;
    
    private WfVariableType(WfValueType valueType, boolean multiValue) {
        if (valueType == null) 
            throw new NullPointerException("valueType can not be null");
        this.valueType = valueType;
        this.multiValue = multiValue;
    }

    public WfValueType getValueType() {
        return valueType;
    }

    public boolean isMultiValue() {
        return multiValue;
    }
    
    public String toString() {
        return toString(valueType, multiValue);
    }
    
    private static String toString(WfValueType valueType, boolean multiValue) {
        return valueType.toString() + (multiValue?"(multi)":"(single)");
    }
    
    public static WfVariableType get(WfValueType valueType, boolean multiValue) {
        return lookupTable.get(toString(valueType, multiValue));
    }

    public void checkType(Object value) {

        if (!multiValue && !valueType.getTypeClass().isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException("Illegal value for given variable type, expected a " + valueType.getTypeClass().getName() + " but got a " + value.getClass().getName());
        } else if (multiValue) {
            if (!value.getClass().isArray())
                throw new IllegalArgumentException("The value for the multivalue-variable should be an array.");
            Object[] values = (Object[])value;
            for (Object val: values) {
                if (!valueType.getTypeClass().isAssignableFrom(val.getClass()))
                    throw new IllegalArgumentException("Illegal value for given variable type, expected a " + valueType.getTypeClass().getName() + " but got a " + value.getClass().getName());
            }
        }
    }
    
}
