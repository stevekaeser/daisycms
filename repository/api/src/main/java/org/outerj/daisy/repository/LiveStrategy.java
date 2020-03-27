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
 * <p>LiveStrategy indicates whether a newly created version should be immediately made the live version when saving a document.</p>
 * 
 * <ul>
 * <li>DEFAULT: only make live if the version state is VersionState.PUBLISH. (This is the behaviour from before 2.4)</li>
 * <li>ALWAYS: always make live, independent of version state.</li>
 * <li>NEVER: never make live, independent on version state.</li>
 * </ul>
 * 
 * @since 2.4
 */
public enum LiveStrategy {
    DEFAULT("default"),
    ALWAYS("always"),
    NEVER("never");

    private final String name;

    private LiveStrategy(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static LiveStrategy fromString(String name) {
        if (name.equals("default"))
            return LiveStrategy.DEFAULT;
        else if (name.equals("always"))
            return LiveStrategy.ALWAYS;
        else if (name.equals("never"))
            return LiveStrategy.NEVER;
        else
            throw new RuntimeException("Invalid LiveStrategy name: " + name);
    }

}
