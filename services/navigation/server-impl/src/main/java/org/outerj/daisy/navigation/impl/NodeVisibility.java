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
package org.outerj.daisy.navigation.impl;

/**
 * Enumeration class for the possible visibilties of a node.
 */
public enum NodeVisibility {
    ALWAYS("always"),
    HIDDEN("hidden"),
    WHEN_ACTIVE("when-active");

    private final String name;

    private NodeVisibility(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static NodeVisibility fromString(String value) {
        if (value.equals(HIDDEN.name))
            return HIDDEN;
        else if (value.equals(WHEN_ACTIVE.name))
            return WHEN_ACTIVE;
        else // invalid values default to 'always' by design
            return ALWAYS;
    }
}
