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

import java.util.Map;

import org.outerj.daisy.navigation.NavigationException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentNotFoundException;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Link to an (external) URL.
 */
public class LinkNode extends AbstractParentNode {
    private String url;
    private String label;
    private String id;
    /** link node will be hidden if no read access to this document */
    private VariantKey aclVariantKey;
    private boolean aclDocExists;
    private CommonNavigationManager.Context context;
    private VersionMode versionMode;

    public LinkNode(String id, String url, String label, VariantKey aclVariantKey,
            VersionMode versionMode, CommonNavigationManager.Context context) throws NavigationException {
        this.id = id;
        this.url = url;
        this.label = label;
        this.aclVariantKey = aclVariantKey;
        this.versionMode = versionMode;
        this.context = context;

        checkAclDocExists();
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
        attrs.addAttribute("", "label", "label", "CDATA", label);
        attrs.addAttribute("", "id", "id", "CDATA", id);
        attrs.addAttribute("", "path", "path", "CDATA", path);
        attrs.addAttribute("", "url", "url", "CDATA", url);
        if (activeNodePath != null) {
            if (pos < activeNodePath.length && activeNodePath[pos] == this)
                attrs.addAttribute("", "selected", "selected", "CDATA", "true");
            if (pos == activeNodePath.length - 1 && activeNodePath[pos] == this)
                attrs.addAttribute("", "active", "active", "CDATA", "true");
        }

        if (addChildCounts) {
            addChildCountAttrs(attrs, userId, roleIds, activeNodePath, pos);
        }

        contentHandler.startElement(NAVIGATION_NS, "link", "link", attrs);
        generateChildXml(contentHandler, activeNodePath, pos + 1, depth, path, userId, roleIds, valueFormatter, addChildCounts);
        contentHandler.endElement(NAVIGATION_NS, "link", "link");

        return true;
    }

    public boolean isIdentifiable() {
        return true;
    }

    public String getId() {
        return id;
    }

    public boolean isVisible(long userId, long[] roleId, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        return exists(userId, roleId, activeNodePath, activeNodePathPos);
    }

    public boolean exists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        if (aclVariantKey == null)
            return true;
        else if (!aclDocExists)
            return false;
        else {
            Document document = context.getRepository().getDocument(aclVariantKey, false);
            long versionId = document.getVersionId(versionMode);
            if (versionId == -1) { // no version in specified mode
                return false;
            } else if (document.getTimeline().hasLiveHistoryEntry(versionId)) {
                return context.canRead(aclVariantKey, userId, roleIds);
            } else {
                return context.canReadNonLive(aclVariantKey, userId, roleIds);
            }
        }
    }

    public boolean[] getVisibleAndExists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        boolean exists = exists(userId, roleIds, activeNodePath, activeNodePathPos);
        return new boolean[] { exists, exists };
    }

    public Object getLabel() {
        return label;
    }

    public ValueType getLabelValueType() {
        return ValueType.STRING;
    }

    public QueryParams getQueryParams() {
        return null;
    }

    public Object getResolvedLabel() {
        return label;
    }

    private void checkAclDocExists() throws NavigationException {
        if (aclVariantKey == null)
            return;

        try {
            context.getRepository().getDocument(aclVariantKey, false);
            this.aclDocExists = true;
        } catch (Exception e) {
            if (e instanceof DocumentNotFoundException || e instanceof DocumentVariantNotFoundException) {
                aclDocExists = true;
                context.getLogger().warn("A navigation tree link-node references the non-existing " + aclVariantKey);
            } else {
                throw new NavigationException("Error retrieving info for " + aclVariantKey, e);
            }
        }
    }
}
