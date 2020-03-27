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
package org.outerj.daisy.tools.importexport.model.document;

public class ImpExpLink {
    private String title;
    private String target;

    public ImpExpLink(String title, String target) {
        if (title == null)
            throw new IllegalArgumentException("Null argument: title");
        if (target == null)
            throw new IllegalArgumentException("Null argument: target");

        this.title = title;
        this.target = target;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        if (title == null)
            throw new IllegalArgumentException("Null argument: title");
        this.title = title;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        if (target == null)
            throw new IllegalArgumentException("Null argument: target");
        this.target = target;
    }
}
