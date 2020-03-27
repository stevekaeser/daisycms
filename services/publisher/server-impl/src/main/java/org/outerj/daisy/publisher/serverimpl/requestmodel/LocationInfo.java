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

public class LocationInfo {
    private final String source;
    private final int line;
    private final int column;
    private final String tagName;

    public LocationInfo(String source, int line, int column, String tagName) {
        this.source = source;
        this.line = line;
        this.column = column;
        this.tagName = tagName;
    }

    public String getSource() {
        return source;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getFormattedLocation() {
        if (source == null)
            return tagName + " in (unknown source)";
        else if (line == -1)
            return tagName + " in " + source;
        else
            return tagName + " in " + source + " - " + line + ":" + column;
    }
}
