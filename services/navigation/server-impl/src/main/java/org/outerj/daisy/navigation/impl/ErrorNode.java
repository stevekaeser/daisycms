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

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.List;
import java.util.Collections;

public class ErrorNode extends AbstractParentNode {
    private String message;

    public ErrorNode(String message) {
        this.message = message;
    }

    public void add(Node node) {
        throw new UnsupportedOperationException();
    }

    public Object getLabel() {
        return message;
    }

    public ValueType getLabelValueType() {
        return ValueType.STRING;
    }

    public List<Node> getChildren() {
        return Collections.emptyList();
    }

    public QueryParams getQueryParams() {
        return null;
    }

    public Object getResolvedLabel() {
        return message;
    }

    public boolean checkId(String id, long branchId, long languageId) {
        return false;
    }

    public boolean generateXml(ContentHandler contentHandler, Node[] activeNodePath, int pos, int depth, String path, long userId, long[] roleIds, NavigationValueFormatter valueFormatter, boolean includeChildCounts) throws RepositoryException, SAXException {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "message", "message", "CDATA", message);
        contentHandler.startElement(NAVIGATION_NS, "error", "error", attrs);
        contentHandler.endElement(NAVIGATION_NS, "error", "error");
        return true;
    }

    public boolean isIdentifiable() {
        return false;
    }

    public String getId() {
        return null;
    }

    public boolean isVisible(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        return true;
    }

    public boolean exists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        return true;
    }

    public boolean[] getVisibleAndExists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        return new boolean[] { true, true };
    }
}
