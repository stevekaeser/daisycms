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
package org.outerj.daisy.query.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.serverimpl.query.LocalQueryManager;

/**
 * This object is used to gather items from the where clause of a query, which need
 * additional ACL checking. This includes:
 *  - all distinct dereferenced link expressions (to check if moving through all these
 *     links is allowed by the ACL, the SQL engine on which the query is executed doesn't
 *     have knowledge of the Daisy ACL rules).
 *  - all field and part names, for cases where one only has partial read access to documents.
 *
 * <p>This object is populated by {@link Expression#collectAccessRestrictions}.
 *
 * <p>It is designed such that each distinct link expression will only need to be checked once,
 * regardless of how many times it occurs in the query where-clause.
 */
public class AccessRestrictions {
    private LinkNode root;
    private LinkNode current;
    private boolean fullTextQueryPresent = false;

    public AccessRestrictions() {
        root = new LinkNode("root", null);
        current = root;
    }

    public void setFullTextQueryPresent(boolean fullTextQueryPresent) {
        this.fullTextQueryPresent = fullTextQueryPresent;
    }

    /**
     * Call this to push a dereference context.
     */
    void startRefExpr(ValueExpr refExpr) {
        String key = refExpr.getExpression();
        LinkNode linkNode = current.getChild(key);
        if (linkNode == null) {
            linkNode = new LinkNode(key, refExpr);
            current.addChild(linkNode);
        }
        current = linkNode;
    }

    /**
     * Call this at the end of the dereference context.
     */
    void end() {
        current = current.getParent();
    }

    void addFieldReference(String fieldName) {
        current.addFieldReference(fieldName);
    }

    void addPartReference(String partName) {
        current.addPartReference(partName);
    }

    /**
     * Returns true if the document is accessible, that is:
     *    - all dereferenced-links can be followed through. False is also returned if a linked document
     *      or the version of the document does not exist (which will normally not be the case, unless
     *      this changed since the execution of the SQL query).
     *    - all fields used in the where clause are accessible
     *    - if there is a full text search clause, full text should be allowed
     */
    public boolean canRead(Document document, AclResultInfo aclInfo, EvaluationInfo evalutionInfo, LocalQueryManager.DocumentRetriever documentRetriever) throws RepositoryException {
        if (fullTextQueryPresent) {
            AccessDetails details = aclInfo.getAccessDetails(AclPermission.READ);
            if (details != null && !details.isGranted(AclDetailPermission.FULLTEXT_INDEX))
                return false;
        }

        return root.canRead(document, aclInfo, evalutionInfo, documentRetriever);
    }

    /**
     * Debug-method for visualising the found deref's.
     */
    public void dump() {
        root.dump("");
    }

    /**
     * Returns true if no dereference expressions have been found.
     */
    public boolean isEmpty() {
        return !root.hasChildren() && root.fieldReferences == null && root.partReferences == null && !fullTextQueryPresent;
    }

    private static class LinkNode {
        private String key;
        private ValueExpr expr;
        private List<LinkNode> children;
        private LinkNode parent;
        private Set<String> fieldReferences;
        private Set<String> partReferences;

        public LinkNode(String key, ValueExpr expr) {
            this.key = key;
            this.expr = expr;
        }

        public String getKey() {
            return key;
        }

        public void addChild(LinkNode linkNode) {
            if (children == null)
                children = new ArrayList<LinkNode>();
            linkNode.parent = this;
            children.add(linkNode);
        }

        public LinkNode getChild(String key) {
            if (children == null)
                return null;

            for (LinkNode linkNode : children) {
                if (linkNode.key.equals(key))
                    return linkNode;
            }
            return null;
        }

        public LinkNode getParent() {
            return parent;
        }

        void addFieldReference(String fieldName) {
            if (fieldReferences == null)
                fieldReferences = new HashSet<String>();
            fieldReferences.add(fieldName);
        }

        void addPartReference(String partName) {
            if (partReferences == null)
                partReferences = new HashSet<String>();
            partReferences.add(partName);
        }

        void dump(String prefix) {
            System.out.println(prefix + key);
            if (children != null) {
                for (LinkNode child : children) {
                    child.dump(prefix + "  ");
                }
            }
        }

        boolean hasChildren() {
            return children != null;
        }

        boolean canRead(Document document, AclResultInfo aclInfo, EvaluationInfo evaluationInfo, LocalQueryManager.DocumentRetriever documentRetriever) throws RepositoryException {
            AccessDetails details = aclInfo.getAccessDetails(AclPermission.READ);
            if (details != null) {
                if (fieldReferences != null && !details.isGranted(AclDetailPermission.ALL_FIELDS)) {
                    for (String fieldName : fieldReferences) {
                        if (!details.canAccessField(fieldName))
                            return false;
                    }
                }

                if (partReferences != null && !details.isGranted(AclDetailPermission.ALL_PARTS)) {
                    for (String partName : partReferences) {
                        if (!details.canAccessPart(partName))
                            return false;
                    }
                }
            }

            Document newDoc;
            AclResultInfo newAclInfo;
            if (expr != null) {
                Version version = document.getVersion(evaluationInfo.getVersionMode());

                if (version == null) {
                    // points to non-existing thing, hence can't access it
                    return false;
                }

                VariantKey variantKey = (VariantKey)expr.evaluate(QValueType.LINK, new ExprDocData(document, version), evaluationInfo);
                if (variantKey == null) {
                    // link points to nowhere, (e.g. a link field has no value), so ACL is irrelevant
                    return true;
                }
                newDoc = documentRetriever.getDocument(variantKey);
                if (newDoc == null) {
                    return false;
                }
                newAclInfo = documentRetriever.getAclInfo();
            } else {
                // this.expr = null for the root node, just move on to children
                newDoc = document;
                newAclInfo = aclInfo;
            }

            if (children != null) {
                for (LinkNode child : children) {
                   boolean canRead = child.canRead(newDoc, newAclInfo, evaluationInfo, documentRetriever);
                    if (!canRead)
                        return false;
                }
            }

            return true;
        }
    }
}
