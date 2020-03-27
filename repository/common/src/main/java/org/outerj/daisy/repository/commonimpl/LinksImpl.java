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

import org.outerj.daisy.repository.Links;
import org.outerj.daisy.repository.Link;
import org.outerx.daisy.x10.LinksDocument;

public class LinksImpl implements Links {
    private final Link[] links;

    public LinksImpl(Link[] links) {
        this.links = links;
    }

    public Link[] getArray() {
        return links;
    }

    public LinksDocument getXml() {
        LinksDocument linksDocument = LinksDocument.Factory.newInstance();
        LinksDocument.Links linksXml = linksDocument.addNewLinks();
        for (int i = 0; i < links.length; i++) {
            LinksDocument.Links.Link linkXml = linksXml.addNewLink();
            linkXml.setTitle(links[i].getTitle());
            linkXml.setTarget(links[i].getTarget());
        }
        return linksDocument;
    }
}
