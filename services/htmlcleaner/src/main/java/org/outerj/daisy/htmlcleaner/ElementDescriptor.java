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
package org.outerj.daisy.htmlcleaner;

import java.util.Set;
import java.util.HashSet;

class ElementDescriptor {
    /** Allowed attributes. */
    private Set<String> attributes = new HashSet<String>();
    private String[] attributeNames;
    /** Allowed children. */
    private Set<String> children = new HashSet<String>();
    private String name;

    public ElementDescriptor(String name) {
        this.name = name;
    }

    public void addAttribute(String name) {
        attributes.add(name);
        attributeNames = null;
    }

    public void addChild(String name) {
        children.add(name);
    }

    public Set<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Set<String> attributes) {
        this.attributes = attributes;
        this.attributeNames = null;
    }

    public Set<String> getChildren() {
        return children;
    }

    public void setChildren(Set<String> children) {
        this.children = children;
    }

    public String getName() {
        return name;
    }

    public String[] getAttributeNames() {
        if (attributeNames == null)
            attributeNames = attributes.toArray(new String[attributes.size()]);
        return attributeNames;
    }

    public boolean childAllowed(String name) {
        return children.contains(name);
    }

    public boolean attributeAllowed(String name) {
        return attributes.contains(name);
    }
}
