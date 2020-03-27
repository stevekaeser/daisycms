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

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.RepositoryException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

public abstract class AbstractParentNode implements Node {
    protected final List<Node> children = new ArrayList<Node>();
    protected int position;

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void add(Node node) {
        children.add(node);
        node.setPosition(children.size());
    }

    public List<Node> getChildren() {
        return children;
    }

    public void searchPath(String[] path, int pos, long branchId, long languageId, Node[] foundPath) throws RepositoryException {
        for (Node node : getChildren()) {
            if (node.isIdentifiable()) {
                // branch and language should only be checked for the last part of the path,
                // since those only apply to that specific document node (if it is a document node)
                long checkBranchId = pos == path.length - 1 ? branchId : -1;
                long checkLanguageId = pos == path.length -1 ? languageId : -1;
                if (node.isIdentifiable() && node.checkId(path[pos], checkBranchId, checkLanguageId)) {
                    foundPath[pos] = node;
                    if (pos != path.length - 1) {
                        node.searchPath(path, pos + 1, branchId, languageId, foundPath);
                    }
                    return;
                }
            }
        }
    }

    public List<Node> searchDocument(VariantKey key) throws RepositoryException {
        for (Node node : getChildren()) {
            List<Node> foundNodePath = node.searchDocument(key);
            if (foundNodePath != null) {
                if (isIdentifiable())
                    foundNodePath.add(this);
                return foundNodePath;
            }
        }
        return null;
    }

    public void populateNodeLookupMap(Map<VariantKey, String> map, String path) throws RepositoryException {
        for (Node node : getChildren()) {
            node.populateNodeLookupMap(map, path);
        }
    }

    public void generateChildXml(final ContentHandler contentHandler, final Node[] activeNodePath, final int pos,
            final int depth, final String path, final long userId, final long[] roleIds,
            final NavigationValueFormatter valueFormatter, final boolean includeChildCounts) throws RepositoryException, SAXException {
        int parentPos = pos - 1;
        if (pos < depth || (activeNodePath != null && parentPos < activeNodePath.length && activeNodePath[parentPos] == this)) {
            generateChildNodes(new NodeGenerator() {
                public boolean generate(Node node) throws RepositoryException, SAXException {
                    return node.generateXml(contentHandler, activeNodePath, pos, depth, path, userId, roleIds, valueFormatter, includeChildCounts);
                }
            }, userId, roleIds);
        }
    }

    private static interface NodeGenerator {
        boolean generate(Node node) throws RepositoryException, SAXException;
    }

    private void generateChildNodes(NodeGenerator nodeGenerator, long userId, long[] roleIds) throws SAXException, RepositoryException {
        // This method performs some action for each of the child nodes, except for unwanted
        // separator nodes, which are:
        //  - separator nodes as first and last child of its parent node
        //  - multiple separator nodes without any other visible nodes in between them

        boolean previousIsSeparator = false;
        int childCount = children.size();
        for (int i = 0; i < childCount; i++) {
            Node node = children.get(i);
            boolean isSeparator = node instanceof SeparatorNode;
            boolean visible = false;
            boolean showNode = true;

            if (isSeparator) {
                if (i == 0 || previousIsSeparator) {
                    showNode = false;
                } else {
                    boolean otherNodeFound = false;
                    for (int k = i + 1; k < childCount; k++) {
                        if (children.get(k).isVisible(userId, roleIds, null, -1)) {
                            otherNodeFound = true;
                            break;
                        }
                    }
                    showNode = otherNodeFound;
                }
            }

            if (showNode) {
                visible = nodeGenerator.generate(node);
            }

            if (isSeparator || visible) {
                previousIsSeparator = isSeparator;
            }
        }
    }

    public String getId() {
        throw new UnsupportedOperationException();
    }

    public String findFirstDocumentNode(String path, long userId, long[] roleIds) throws RepositoryException {
        for (Node child : getChildren()) {
            boolean representsDocument = child instanceof DocumentNode;
            if (representsDocument && child.isVisible(userId, roleIds, null, -1)) {
                return path + "/" + child.getId();
            } else if (!representsDocument) {
                String node = child.findFirstDocumentNode(path + "/" + child.getId(), userId, roleIds);
                if (node != null)
                    return node;
            }
        }
        return null;
    }

    protected int[] getChildCounts(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        int visibleChildCount = 0;
        int totalChildCount = 0;
        List<Node> children = getChildren();
        for (Node child : children) {
            boolean[] visAndEx = child.getVisibleAndExists(userId, roleIds, activeNodePath, activeNodePathPos);
            if (visAndEx[0])
                visibleChildCount++;
            if (visAndEx[1])
                totalChildCount++;
        }
        return new int[] { visibleChildCount, totalChildCount };
    }

    protected void addChildCountAttrs(AttributesImpl attrs, long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        int[] counts = getChildCounts(userId, roleIds, activeNodePath, activeNodePathPos);
        attrs.addAttribute("", "visibleChildCount", "visibleChildCount", "CDATA", String.valueOf(counts[0]));
        attrs.addAttribute("", "childCount", "childCount", "CDATA", String.valueOf(counts[1]));
    }
}
