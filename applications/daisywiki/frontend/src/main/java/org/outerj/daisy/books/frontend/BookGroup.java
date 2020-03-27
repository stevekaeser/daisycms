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
package org.outerj.daisy.books.frontend;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.apache.cocoon.xml.AttributesImpl;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;

class BookGroup implements Comparable {
    private final String name;
    private List childGroups = new ArrayList();
    private List childNodes = new ArrayList();

    public BookGroup(String name) {
        this.name = name;
    }

    public BookGroup getGroup(String path) {
        path = path.trim();

        // removing leading slash if any, also handles case where user enters stupid things '///'
        while (path.length() > 0 && path.charAt(0) == '/') {
            path = path.substring(1);
        }

        if (path.length() == 0) {
            return this;
        }

        String name;
        String subpath = null;

        int slashPos = path.indexOf('/');
        if (slashPos != -1) {
            name = path.substring(0, slashPos);
            subpath = path.substring(slashPos + 1);
        } else {
            name = path;
        }

        BookGroup childGroup = getChild(name);
        if (childGroup == null) {
            childGroup = new BookGroup(name);
            childGroups.add(childGroup);
        }

        if (subpath != null) {
            return childGroup.getGroup(subpath);
        } else {
            return childGroup;
        }
    }

    private BookGroup getChild(String name) {
        Iterator childGroupsIt = childGroups.iterator();
        while (childGroupsIt.hasNext()) {
            BookGroup group = (BookGroup)childGroupsIt.next();
            if (group.name.equals(name))
                return group;
        }
        return null;
    }

    public void addChild(BookGroupChild child) {
        childNodes.add(child);
    }

    public int compareTo(Object o) {
        BookGroup otherGroup = (BookGroup)o;
        return name.compareTo(otherGroup.name);
    }

    public void generateSaxFragment(ContentHandler contentHandler) throws SAXException {
        AttributesImpl groupAttrs = new AttributesImpl();
        groupAttrs.addAttribute("", "name", "name", "CDATA", name);
        contentHandler.startElement("", "group", "group", groupAttrs);

        Collections.sort(childGroups);
        Collections.sort(childNodes);

        Iterator childGroupsIt = childGroups.iterator();
        while (childGroupsIt.hasNext()) {
            ((BookGroup)childGroupsIt.next()).generateSaxFragment(contentHandler);
        }

        Iterator childNodesIt = childNodes.iterator();
        while (childNodesIt.hasNext()) {
            ((BookGroupChild)childNodesIt.next()).generateSaxFragment(contentHandler);
        }

        contentHandler.endElement("", "group", "group");
    }

    interface BookGroupChild extends Comparable {
        void generateSaxFragment(ContentHandler contentHandler) throws SAXException;
    }
}
