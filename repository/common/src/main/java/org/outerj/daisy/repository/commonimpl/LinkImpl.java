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
package org.outerj.daisy.repository.commonimpl;

import org.outerj.daisy.repository.Link;

public class LinkImpl implements Link {
    private String title;
    private String target;

    public LinkImpl(String title, String target) {
        this.title = title;
        this.target = target;
    }

    public String getTitle() {
        return title;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Link))
            return false;

        Link otherLink = (Link)obj;
        return title.equals(otherLink.getTitle()) && target.equals(otherLink.getTarget());
    }
}
