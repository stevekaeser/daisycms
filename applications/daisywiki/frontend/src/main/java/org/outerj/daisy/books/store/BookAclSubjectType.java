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
package org.outerj.daisy.books.store;

public final class BookAclSubjectType {
    public static final BookAclSubjectType USER = new BookAclSubjectType("user");
    public static final BookAclSubjectType ROLE = new BookAclSubjectType("role");
    public static final BookAclSubjectType EVERYONE = new BookAclSubjectType("everyone");

    private final String name;

    private BookAclSubjectType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static BookAclSubjectType fromString(String value) {
        if (value.equals(USER.name)) {
            return USER;
        } else if (value.equals(ROLE.name)) {
            return ROLE;
        } else if (value.equals(EVERYONE.name)) {
            return EVERYONE;
        } else {
            throw new RuntimeException("Unrecognized BookAclSubjectType name: \"" + value + "\".");
        }
    }
}
