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
 * Enumeration of all available ACL Permissions.
 */
public enum AclPermission {
    READ("read", 0),
    WRITE("write", 1),
    PUBLISH("publish", 2),
    DELETE("delete", 3);

    private String name;
    private int pos;                

    private AclPermission(String name, int pos) {
        this.name = name;
        this.pos = pos;
    }

    /**
     * Returns a number corresponding to the position in the {@link #ENUM} array.
     *
     * @deprecated This method will be removed in a future version. It has no purpose
     *             since moving to Java 1.5 enums.
     */
    public int getPos() {
        return pos;
    }

    public String toString() {
        return name;
    }

    public static AclPermission fromString(String value) {
        if (value.equals("read"))
            return AclPermission.READ;
        else if (value.equals("write"))
            return AclPermission.WRITE;
        else if (value.equals("publish"))
            return AclPermission.PUBLISH;
        else if (value.equals("delete"))
            return AclPermission.DELETE;
        else
            throw new RuntimeException("Unrecognized ACL permission: " + value);
    }

    /**
     * @deprecated This member will be removed in a future version. It has no purpose
     *             since moving to Java 1.5 enums.
     */
    public static final AclPermission[] ENUM = {READ, WRITE, PUBLISH, DELETE};
}
