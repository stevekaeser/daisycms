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
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.navigation.NavigationException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.Map;

public class GroupNode extends AbstractParentNode {
    private final String id;
    private final Object label;
    private final ValueType labelValueType;
    private final QueryParams queryParams;
    private final NodeVisibility nodeVisibility;

    /**
     *
     * @param id an ID that should be unique within the parent node.
     */
    public GroupNode(String id, String label, NodeVisibility nodeVisibility) {
        this(id, label, ValueType.STRING, null, nodeVisibility);
    }

    public GroupNode(String id, Object label, ValueType labelValueType, QueryParams queryParams, NodeVisibility nodeVisibility) {
        this.id = id;
        this.label = label;
        this.labelValueType = labelValueType;
        this.queryParams = queryParams;
        this.nodeVisibility = nodeVisibility;
    }

    public Object getLabel() {
        return label;
    }

    public Object getResolvedLabel() {
        return label;
    }

    public ValueType getLabelValueType() {
        return labelValueType;
    }

    public QueryParams getQueryParams() {
        return queryParams;
    }

    public boolean checkId(String id, long branchId, long languageId) {
        return this.id.equals(id);
    }

    public void populateNodeLookupMap(Map<VariantKey, String> map, String path) throws RepositoryException {
        path = path + "/" + id;
        super.populateNodeLookupMap(map, path);
    }

    public boolean generateXml(ContentHandler contentHandler, Node[] activeNodePath, int pos, int depth,
            String path, long userId, long[] roleIds, NavigationValueFormatter valueFormatter, boolean addChildCounts) throws RepositoryException, SAXException {
        if (!isVisible(userId, roleIds, activeNodePath, pos))
            return false;

        path = path + "/" + id;
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "label", "label", "CDATA", valueFormatter.format(labelValueType, label));
        attrs.addAttribute("", "id", "id", "CDATA", id);
        attrs.addAttribute("", "path", "path", "CDATA", path);

        if (activeNodePath != null) {
            if (pos < activeNodePath.length && activeNodePath[pos] == this)
                attrs.addAttribute("", "selected", "selected", "CDATA", "true");
            if (pos == activeNodePath.length -1 && activeNodePath[pos] == this)
                attrs.addAttribute("", "active", "active", "CDATA", "true");
        }
        
        if (addChildCounts) {
            addChildCountAttrs(attrs, userId, roleIds, activeNodePath, pos);
        }

        contentHandler.startElement(NAVIGATION_NS, "group", "group", attrs);
        generateChildXml(contentHandler, activeNodePath, pos + 1, depth, path, userId, roleIds, valueFormatter, addChildCounts);
        contentHandler.endElement(NAVIGATION_NS, "group", "group");

        return true;
    }

    public boolean isVisible(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        return canBeVisisble(userId, roleIds, activeNodePath, activeNodePathPos) && exists(userId, roleIds, activeNodePath, activeNodePathPos);
    }

    public boolean canBeVisisble(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws NavigationException {
        if (nodeVisibility == NodeVisibility.HIDDEN) {
            return false;
        } else if (nodeVisibility == NodeVisibility.WHEN_ACTIVE
                && !(activeNodePath != null && activeNodePathPos < activeNodePath.length && activeNodePath[activeNodePathPos] == this)) {
            return false;
        }
        return true;
    }

    public boolean exists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        activeNodePathPos++;
        for (Node child : getChildren()) {
            if (child.isVisible(userId, roleIds, activeNodePath, activeNodePathPos))
                return true;
        }

        return false;
    }

    public boolean[] getVisibleAndExists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        boolean canBeVisible = canBeVisisble(userId, roleIds, activeNodePath, activeNodePathPos);
        boolean exists = exists(userId, roleIds, activeNodePath, activeNodePathPos);
        return new boolean[] { canBeVisible && exists, exists };
    }

    public boolean isIdentifiable() {
        return true;
    }

    public String getId() {
        return id;
    }
}
