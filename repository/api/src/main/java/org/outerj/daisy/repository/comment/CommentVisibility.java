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
package org.outerj.daisy.repository.comment;

public enum CommentVisibility {
    PUBLIC("public", "U"),
    EDITORS("editors", "E"),
    PRIVATE("private", "P");

    private final String name;
    private final String code;

    private CommentVisibility(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String toString() {
        return name;
    }

    public static CommentVisibility fromString(String name) {
        if (name.equals("public")) {
            return PUBLIC;
        } else if (name.equals("editors")) {
            return EDITORS;
        } else if (name.equals("private")) {
            return PRIVATE;
        } else {
            throw new RuntimeException("Invalid CommentVisiblity name: " + name);
        }
    }

    public String getCode() {
        return code;
    }

    public static CommentVisibility getByCode(String code) {
        if (code.equals("U")) {
            return PUBLIC;
        } else if (code.equals("E")) {
            return EDITORS;
        } else if (code.equals("P")) {
            return PRIVATE;
        } else {
            throw new RuntimeException("Invalid CommentVisibility code: " + code);
        }
    }
}
