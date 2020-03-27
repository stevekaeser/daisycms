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
package org.outerj.daisy.repository;

/**
 * Enumeration of the states a {@link Version} can be in.
 */
public enum VersionState {
    DRAFT("draft", "D"),
    PUBLISH("publish", "P");

    private final String name;
    private final String code;

    private VersionState(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String toString() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public static VersionState getByCode(String code) {
        if (code.equals("D")) {
            return VersionState.DRAFT;
        } else if (code.equals("P")) {
            return VersionState.PUBLISH;
        } else {
            throw new RuntimeException("Invalid VersionState code: " + code);
        }
    }

    public static VersionState fromString(String name) {
        if (name.equals("draft"))
            return VersionState.DRAFT;
        else if (name.equals("publish"))
            return VersionState.PUBLISH;
        else
            throw new RuntimeException("Invalid VersionState name: " + name);
    }
}
