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
 * Enumeration of the different actions an {@link AclEntry} can specify for a certain
 * {@link AclPermission}.
 *
 */
public enum AclActionType {
    GRANT("grant", true),
    DENY("deny", false),
    DO_NOTHING("nothing", false);

    private final String name;
    private final boolean supportsAccessDetails;

    private AclActionType(String name, boolean supportsAccessDetails) {
        this.name = name;
        this.supportsAccessDetails = supportsAccessDetails;
    }

    /**
     * Indicates whether this action type supports {@link AccessDetails}.
     */
    public boolean supportsAccessDetails() {
        return supportsAccessDetails;
    }

    public String toString() {
        return name;
    }

    public static AclActionType fromString(String value) {
        if (value.equals("grant"))
            return GRANT;
        else if (value.equals("deny"))
            return DENY;
        else if (value.equals("nothing"))
            return DO_NOTHING;
        else
            throw new RuntimeException("Unrecognized action type: " + value);
    }

}
