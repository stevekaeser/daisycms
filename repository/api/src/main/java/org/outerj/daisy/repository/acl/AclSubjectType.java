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
package org.outerj.daisy.repository.acl;

/**
 * Enumeration of the subject types.
 */
public enum AclSubjectType {
    USER("user"),
    EVERYONE("everyone"),
    ROLE("role"),
    OWNER("owner");

    private final String name;

    private AclSubjectType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static AclSubjectType fromString(String value) {
        if (value.equals("user"))
            return USER;
        else if (value.equals("role"))
            return ROLE;
        else if (value.equals("everyone"))
            return EVERYONE;
        else if (value.equals("owner"))
            return OWNER;
        else
            throw new RuntimeException("Unrecognized subject type: " + value);
    }
}
