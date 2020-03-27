/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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

import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.RepositoryException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class SeparatorNode extends AbstractParentNode {
    public void add(Node node) {
        // do nothing, separator does not allow children
    }

    public Object getLabel() {
        return null;
    }

    public ValueType getLabelValueType() {
        return ValueType.STRING;
    }

    public Object getResolvedLabel() {
        return null;
    }

    public QueryParams getQueryParams() {
        return null;
    }

    public boolean generateXml(ContentHandler contentHandler, Node[] activeNodePath, int pos, int depth, String path, long userId, long[] roleIds, NavigationValueFormatter valueFormatter, boolean includeChildCounts) throws RepositoryException, SAXException {
        AttributesImpl attrs = new AttributesImpl();
        contentHandler.startElement(NAVIGATION_NS, "separator", "separator", attrs);
        contentHandler.endElement(NAVIGATION_NS, "separator", "separator");
        return true;
    }

    public boolean checkId(String id, long branchId, long languageId) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    public boolean isIdentifiable() throws RepositoryException {
        return false;
    }

    public boolean isVisible(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        // Returns false on purpose.
        // A separator node is only visible when the nodes around it are visible.
        // E.g. a group node containing non-visible document nodes and some separators is not visible,
        // therefore it is easier to default to non-visible here. The actual determination of the visibility
        // is done in AbstractParentNode.generateChildNodes
        return false;
    }

    public boolean exists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        return false;
    }

    public boolean[] getVisibleAndExists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        return new boolean[] { false, false };
    }
}
