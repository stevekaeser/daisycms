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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.outerj.daisy.navigation.NavigationException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentNotFoundException;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionNotFoundException;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class DocumentNode extends AbstractParentNode {
    private final VariantKey variantKey;
    private String nodeId;
    private String branch;
    private String language;
    private VersionMode versionMode;
    private final NodeVisibility nodeVisibility;
    private boolean docInfoFetched = false;
    private Object label;
    private ValueType labelValueType;
    private QueryParams queryParams;
    private boolean dontShow;
    private CommonNavigationManager.Context context;

    /**
     *
     * @param label can be null, in which case the name of the document will serve as label
     */
    public DocumentNode(VariantKey variantKey, String nodeId, Object label, NodeVisibility nodeVisibility,
            CommonNavigationManager.Context context,
            long navigationBranchId, long navigationLanguageId, VersionMode versionMode) {
        this(variantKey, nodeId, label, null, null, nodeVisibility, context, navigationBranchId,
                navigationLanguageId, versionMode);
    }

    public DocumentNode(VariantKey variantKey, String nodeId, Object label, ValueType labelValueType, QueryParams queryParams,
            NodeVisibility nodeVisibility, CommonNavigationManager.Context context, long navigationBranchId,
            long navigationLanguageId, VersionMode versionMode) {
        this.variantKey = variantKey;
        if (nodeId != null)
            this.nodeId = NavigationUtil.makeNodeIdValid(nodeId);
        if (this.nodeId == null)
            this.nodeId = variantKey.getDocumentId();
        this.label = label;
        this.labelValueType = labelValueType;
        this.queryParams = queryParams;
        this.nodeVisibility = nodeVisibility;
        this.context = context;

        // About the getBranch/LanguageNameSafe methods: if getting the branch or language name fails,
        // it is most likely because they don't exist, and this will be handled in loadDocInfo
        branch = variantKey.getBranchId() != navigationBranchId ? getBranchNameSafe(context, variantKey.getBranchId()) : null;
        language = variantKey.getLanguageId() != navigationLanguageId ? getLanguageNameSafe(context, variantKey.getLanguageId()) : null;

        this.versionMode = versionMode;
    }

    private String getBranchNameSafe(CommonNavigationManager.Context context, long branchId) {
        try {
            return context.getBranchName(variantKey.getBranchId());
        } catch (RepositoryException e) {
            return String.valueOf(branchId);
        }
    }

    private String getLanguageNameSafe(CommonNavigationManager.Context context, long languageId) {
        try {
            return context.getLanguageName(variantKey.getLanguageId());
        } catch (RepositoryException e) {
            return String.valueOf(languageId);
        }
    }

    public Object getLabel() {
        return label;
    }

    public Object getResolvedLabel() {
        try {
            loadDocInfo();
        } catch (NavigationException e) {
            throw new RuntimeException(e);
        }
        return label;
    }

    public ValueType getLabelValueType() {
        return labelValueType;
    }

    public QueryParams getQueryParams() {
        return queryParams;
    }

    public boolean checkId(String id, long branchId, long languageId) {
        return nodeId.equals(id) && (branchId == -1 || variantKey.getBranchId() == branchId) && (languageId == -1 || variantKey.getLanguageId() == languageId);
    }

    public List<Node> searchDocument(VariantKey key) throws RepositoryException {
        if (this.variantKey.equals(key)) {
            List<Node> foundNodePath = new ArrayList<Node>(5);
            foundNodePath.add(this);
            return foundNodePath;
        } else {
            return super.searchDocument(key);
        }
    }

    public void populateNodeLookupMap(Map<VariantKey, String> map, String path) throws RepositoryException {
        loadDocInfo();
        if (dontShow)
            return;

        path = path + "/" + nodeId;
        if (!map.containsKey(variantKey)) {
            map.put(variantKey, path);
        }
        super.populateNodeLookupMap(map, path);
    }

    public boolean generateXml(ContentHandler contentHandler, Node[] activeNodePath, int pos, int depth,
            String path, long userId, long[] roleIds, NavigationValueFormatter valueFormatter, boolean addChildCounts) throws RepositoryException, SAXException {
        AclResultInfo aclInfo = myIsVisible(userId, roleIds, activeNodePath, pos);
        if (aclInfo == null)
            return false;

        path = path + "/" + nodeId;
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "id", "id", "CDATA", nodeId);
        attrs.addAttribute("", "documentId", "documentId", "CDATA", variantKey.getDocumentId());
        attrs.addAttribute("", "branchId", "branchId", "CDATA", String.valueOf(variantKey.getBranchId()));
        attrs.addAttribute("", "languageId", "languageId", "CDATA", String.valueOf(variantKey.getLanguageId()));
        if (branch != null)
            attrs.addAttribute("", "branch", "branch", "CDATA", branch);
        if (language != null)
            attrs.addAttribute("", "language", "language", "CDATA", language);
        attrs.addAttribute("", "label", "label", "CDATA", getFormattedLabel(valueFormatter));
        attrs.addAttribute("", "path", "path", "CDATA", path);

        if (activeNodePath != null) {
            if (pos < activeNodePath.length && activeNodePath[pos] == this)
                attrs.addAttribute("", "selected", "selected", "CDATA", "true");
            if (pos == activeNodePath.length -1 && activeNodePath[pos] == this)
                attrs.addAttribute("", "active", "active", "CDATA", "true");
        }

        attrs.addAttribute("", "hasChildren", "hasChildren", "CDATA", String.valueOf(hasVisibleChildren(userId, roleIds)));
        attrs.addAttribute("", "access", "access", "CDATA", aclInfo.getCompactString());

        if (addChildCounts) {
            addChildCountAttrs(attrs, userId, roleIds, activeNodePath, pos);
        }

        contentHandler.startElement(NAVIGATION_NS, "doc", "doc", attrs);

        generateChildXml(contentHandler, activeNodePath, pos + 1, depth, path, userId, roleIds, valueFormatter, addChildCounts);

        contentHandler.endElement(NAVIGATION_NS, "doc", "doc");

        return true;
    }

    private String getFormattedLabel(NavigationValueFormatter valueFormatter) {
        if (labelValueType == null) {
            return (String)label;
        } else {
            return valueFormatter.format(labelValueType, label);
        }
    }

    public boolean isVisible(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        AclResultInfo aclInfo = myIsVisible(userId, roleIds, activeNodePath, activeNodePathPos);
        return aclInfo != null;
    }

    /**
     * Returns null if not visible, otherwise the AclResultInfo object.
     */
    public AclResultInfo myIsVisible(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        if (!canBeVisisble(userId, roleIds, activeNodePath, activeNodePathPos))
            return null;

        return myExists(userId, roleIds, activeNodePath, activeNodePathPos);
    }

    public boolean canBeVisisble(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws NavigationException {
        loadDocInfo();
        if (nodeVisibility == NodeVisibility.HIDDEN) {
            return false;
        } else if (nodeVisibility == NodeVisibility.WHEN_ACTIVE) {
            // Note: we can assume the node will only be in the active node path when read-access is OK
            return activeNodePath != null && activeNodePathPos < activeNodePath.length && activeNodePath[activeNodePathPos] == this;
        }
        return true;
    }

    public boolean exists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        AclResultInfo info = myExists(userId, roleIds, activeNodePath, activeNodePathPos);
        return info != null;
    }

    /**
     * Returns null if not exists, otherwise the AclResultInfo object.
     */
    public AclResultInfo myExists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        if (dontShow) {
            return null;
        } else {
            AclResultInfo aclInfo = context.getAclInfo(variantKey, userId, roleIds);
            boolean exists;
            if (versionMode.isLast())
                exists = aclInfo.isNonLiveAllowed(AclPermission.READ);
            else
                exists = aclInfo.isAllowed(AclPermission.READ);
            return exists ? aclInfo : null;
        }
    }

    public boolean[] getVisibleAndExists(long userId, long[] roleIds, Node[] activeNodePath, int activeNodePathPos) throws RepositoryException {
        boolean canBeVisible = canBeVisisble(userId, roleIds, activeNodePath, activeNodePathPos);
        boolean exists = exists(userId, roleIds, activeNodePath, activeNodePathPos);
        return new boolean[] { canBeVisible && exists, exists };
    }

    private synchronized void loadDocInfo() throws NavigationException {
        if (!docInfoFetched) {
            try {
                Document document = context.getRepository().getDocument(variantKey, false);

                dontShow = document.isRetired();
                Version version = null;
                if (!dontShow) {
                    try {
                        version = document.getVersion(versionMode);
                        if (version == null) {
                            dontShow = true;
                        }
                    } catch (VersionNotFoundException vnfe) {
                        dontShow = true;
                    }
                }

                // label member is set even if dontShow is true, because the QueryNode sorting
                // code relies on it being processed similarly for all nodes (e.g. if label
                // value type is link, the label should become a string or null for all nodes)

                // If (this.label == null && this.labelValueType == null) means that this was a literal
                // document node in the navigation tree without a label (in contrast with a document
                // node resulting from a query, whereby the query value was null, in which case
                // it is the task of the valueformatter to generate an appropriate label).
                if ((this.label == null && this.labelValueType == null) || this.labelValueType == ValueType.LINK) {
                    label = version != null ? version.getDocumentName() : null;
                    labelValueType = ValueType.STRING;
                }
                docInfoFetched = true;
            } catch (Exception e) {
                if (e instanceof DocumentNotFoundException || e instanceof DocumentVariantNotFoundException) {
                    dontShow = true;
                    docInfoFetched = true;
                    context.getLogger().warn("A navigation tree references the non-existing " + variantKey);
                } else {
                    throw new NavigationException("Error retrieving info for " + variantKey, e);
                }
            }
        }
    }

    public boolean isIdentifiable() {
        return true;
    }

    public Document getDocument() throws RepositoryException {
        return context.getRepository().getDocument(variantKey, false);
    }

    public VariantKey getVariantKey() {
        return variantKey;
    }

    public String getId() {
        return nodeId;
    }

    private boolean hasVisibleChildren(long userId, long[] roleIds) throws RepositoryException {
        for (Node child : children) {
            if (child.isVisible(userId, roleIds, null, -1))
                return true;
        }
        return false;
    }
}
