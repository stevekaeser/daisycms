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
package org.outerj.daisy.repository.query;

public enum SortOrder {
    ASCENDING("ascending", 'A'),
    DESCENDING("descending", 'D'),
    NONE("none", 'N');

    private final String name;
    private final char code;

    private SortOrder(String name, char code) {
        this.name = name;
        this.code = code;
    }

    public String toString() {
        return name;
    }

    public char getCode() {
        return code;
    }

    public static SortOrder fromString(String name) {
        if (ASCENDING.name.equals(name))
            return ASCENDING;
        else if (DESCENDING.name.equals(name))
            return DESCENDING;
        else if (NONE.name.equals(name))
            return NONE;
        else
            throw new RuntimeException("Invalid sort order name: " + name);
    }

    public static SortOrder fromCode(char code) {
        if (ASCENDING.code == code)
            return ASCENDING;
        else if (DESCENDING.code == code)
            return DESCENDING;
        else if (NONE.code == code)
            return NONE;
        else
            throw new RuntimeException("Invalid sort order code: " + code);
    }
}
