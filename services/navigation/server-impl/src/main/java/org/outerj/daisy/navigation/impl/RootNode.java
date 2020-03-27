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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.outerj.daisy.navigation.NavigationException;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class RootNode extends AbstractParentNode {
    private Set<VariantKey> navigationDocs = new HashSet<VariantKey>();
    private long invalidSince = -1;

    public RootNode() {
    }

    public boolean checkId(String id, long branchId, long languageId) throws RepositoryException {
        throw new UnsupportedOperationException();
    }

    public boolean generateXml(ContentHandler contentHandler, Node[] activeNodePath, int pos, int depth,
            String path, long userId, long[] roleIds, NavigationValueFormatter valueFormatter, boolean addChildCounts) throws RepositoryException, SAXException {
        generateHeader(contentHandler, activeNodePath, depth < 0 || depth == NavigationParams.DEFAULT_NONCONTEXTUALIZED_DEPTH);
        generateChildXml(contentHandler, activeNodePath, pos, depth, path, userId, roleIds,
                valueFormatter, addChildCounts);
        generateFooter(contentHandler);
        return true;
    }

    private void generateHeader(ContentHandler contentHandler, Node[] activeNodePath, boolean completeTree) throws SAXException, RepositoryException {
        contentHandler.startDocument();
        AttributesImpl attrs = new AttributesImpl();
        if (activeNodePath != null) {
            StringBuilder selectedPath = new StringBuilder(activeNodePath.length * 8);
            for (int i = 0; i < activeNodePath.length; i++)
                selectedPath.append("/").append(activeNodePath[i].getId());
            attrs.addAttribute("", "selectedPath", "selectedPath", "CDATA", selectedPath.toString());
        }
        attrs.addAttribute("", "completeTree", "completeTree", "CDATA", String.valueOf(completeTree));
        if (invalidSince >= 0) {
            attrs.addAttribute("", "invalidMillis", "invalidMillis", "CDATA", String.valueOf(System.currentTimeMillis() - invalidSince));
        }
        contentHandler.startPrefixMapping("", NAVIGATION_NS);
        contentHandler.startElement(NAVIGATION_NS, "navigationTree", "navigationTree", attrs);
    }

    private void generateFooter(ContentHandler contentHandler) throws SAXException {
        contentHandler.endElement(NAVIGATION_NS, "navigationTree", "navigationTree");
        contentHandler.endPrefixMapping("");
        contentHandler.endDocument();
    }

    public boolean isIdentifiable() {
        return false;
    }

    public String getId() {
        throw new UnsupportedOperationException();
    }

    public boolean isVisible(long userId, long[] roleId, Node[] activeNodePath, int activeNodePathPos) throws NavigationException {
        return true;
    }

    public boolean exists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        return true;
    }

    public boolean[] getVisibleAndExists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        return new boolean[] { true, true };
    }

    public Object getLabel() {
        throw new UnsupportedOperationException();
    }

    public ValueType getLabelValueType() {
        throw new UnsupportedOperationException();
    }

    public QueryParams getQueryParams() {
        throw new UnsupportedOperationException();
    }

    public Object getResolvedLabel() {
        throw new UnsupportedOperationException();
    }

    /**
     * Adds a navigation doc to the list of navigation docs used
     * in the tree below this root node. It's important this is
     * done for all imported navigation trees.
     */
    public void addDependency(VariantKey variantKey) {
        this.navigationDocs.add(variantKey);
    }

    public Collection<VariantKey> getDependencies() {
        return navigationDocs;
    }
    
    public void invalidate() {
        invalidSince = System.currentTimeMillis();
    }
}
