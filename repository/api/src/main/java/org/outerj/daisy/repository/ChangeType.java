/*
 * Copyright 2008 Outerthought bvba and Schaubroeck nv
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
 * Indicates if a {@link Version} contains major or minor changes.
 *
 * <p>See {@link Version#setChangeType} for more details on intended usage.
 */
public enum ChangeType {
    MAJOR("major", "M"),
    MINOR("minor", "N");

    private final String name;
    private final String code;

    private ChangeType(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String toString() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public static ChangeType getByCode(String code) {
        if (code.equals("M")) {
            return ChangeType.MAJOR;
        } else if (code.equals("N")) {
            return ChangeType.MINOR;
        } else {
            throw new RuntimeException("Invalid ChangeType code: " + code);
        }
    }

    public static ChangeType fromString(String name) {
        if (name.equals("major"))
            return ChangeType.MAJOR;
        else if (name.equals("minor"))
            return ChangeType.MINOR;
        else
            throw new RuntimeException("Invalid ChangeType name: " + name);
    }

}
