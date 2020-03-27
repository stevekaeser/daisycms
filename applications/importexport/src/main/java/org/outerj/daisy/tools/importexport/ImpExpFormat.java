/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.tools.importexport;

/**
 * Enumeration of the available import/export algorithms.
 */
public enum ImpExpFormat {
    DEFAULT("default"), TRANSLATION_MANAGEMENT("tm");

    private final String name;

    private ImpExpFormat(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static ImpExpFormat fromString(String name) {
        if (name.equals(DEFAULT.name)) {
            return DEFAULT;
        } else if (name.equals(TRANSLATION_MANAGEMENT.name)) {
            return TRANSLATION_MANAGEMENT;
        } else {
            throw new RuntimeException("Invalid import export format: " + name);
        }
    }
}
