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
 * Enumeration of the available lock types.
 */
public enum LockType {
    WARN("warn", "W"),
    PESSIMISTIC("pessimistic", "P");

    private final String name;
    private final String code;

    private LockType(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String toString() {
        return name;
    }

    /**
     * A short (one-letter) unique code representing this lock type.
     */
    public String getCode() {
        return code;
    }

    public static LockType getByCode(String code) {
        if (code.equals("W")) {
            return WARN;
        } else if (code.equals("P")) {
            return PESSIMISTIC;
        } else {
            throw new RuntimeException("Invalid LockType code: " + code);
        }
    }

    public static LockType fromString(String lockTypeString) {
        if (lockTypeString.equals("warn"))
            return WARN;
        else if (lockTypeString.equals("pessimistic"))
            return PESSIMISTIC;
        else
            throw new RuntimeException("Invalid LockType string: " + lockTypeString);
    }
}
