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

public final class BookAclActionType {
    public static final BookAclActionType GRANT = new BookAclActionType("grant");
    public static final BookAclActionType DENY = new BookAclActionType("deny");
    public static final BookAclActionType NOTHING = new BookAclActionType("nothing");

    private final String name;

    private BookAclActionType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static BookAclActionType fromString(String value) {
        if (value.equals(GRANT.name)) {
            return GRANT;
        } else if (value.equals(DENY.name)) {
            return DENY;
        } else if (value.equals(NOTHING.name)) {
            return NOTHING;
        } else {
            throw new RuntimeException("Unrecognized BookAclActionType name: \"" + value + "\".");
        }
    }
}
