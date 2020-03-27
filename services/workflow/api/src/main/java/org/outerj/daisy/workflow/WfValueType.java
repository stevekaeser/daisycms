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

public enum WfValueType {
    STRING("string", java.lang.String.class),
    DAISY_LINK("daisy-link", org.outerj.daisy.workflow.WfVersionKey.class),
    LONG("long", java.lang.Long.class),
    DATE("date", java.util.Date.class),
    DATETIME("datetime", java.util.Date.class),
    ACTOR("actor", org.outerj.daisy.workflow.WfActorKey.class),
    USER("user", org.outerj.daisy.workflow.WfUserKey.class),
    BOOLEAN("boolean", java.lang.Boolean.class),
    ID("id", java.lang.String.class);

    private String name;
    private Class clazz;

    private WfValueType(String name, Class clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public String toString() {
        return name;
    }

    public Class getTypeClass() {
        return clazz;
    }

    public static WfValueType fromString(String name) {
        if (STRING.name.equals(name))
            return STRING;
        else if (DAISY_LINK.name.equals(name))
            return DAISY_LINK;
        else if (LONG.name.equals(name))
            return LONG;
        else if (DATE.name.equals(name))
            return DATE;
        else if (DATETIME.name.equals(name))
            return DATETIME;
        else if (ACTOR.name.equals(name))
            return ACTOR;
        else if (USER.name.equals(name))
            return USER;
        else if (BOOLEAN.name.equals(name))
            return BOOLEAN;
        else if (ID.name.equals(name))
            return ID;
        else
            throw new RuntimeException("Unexpected value type: " + name);
    }
}
