/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.publisher.serverimpl.requestmodel;

public enum ContentDiffType {
    HTML("html"), HTMLSOURCE("htmlsource"), TEXT("text");
    private String name;

    private ContentDiffType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    public static ContentDiffType fromString(String string) {
        if (HTML.name.equals(string)) {
            return HTML;
        } else if (HTMLSOURCE.name.equals(string)) {
            return HTMLSOURCE;
        } else if (TEXT.name.equals(string)) {
            return TEXT;
        } else {
            throw new RuntimeException("Unrecognized content diff type: \"" + string + "\".");
        }
    }
}
