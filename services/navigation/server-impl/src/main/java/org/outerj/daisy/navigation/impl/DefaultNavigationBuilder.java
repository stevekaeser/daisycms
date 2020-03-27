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

import org.outerx.daisy.x10Navigationspec.*;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.navigation.NavigationException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.QNameSet;

import java.util.List;
import java.util.ArrayList;
import java.util.EmptyStackException;

/**
 * Default NavigationBuilder builds a navigation tree from the "normal" navigation
 * XML (as opposed to the book trees).
 */
public class DefaultNavigationBuilder implements NavigationFactory.NavigationBuilder, NodeBuilder {
    private String[] collectionNames;

    /**
     * Builds a navigation tree from an XML navigation description provided as a string argument.
     */
    public void buildTree(Node parentNode, XmlObject xmlObject, BuildContext buildContext) throws RepositoryException {
        build(parentNode, (NavigationTreeDocument)xmlObject, buildContext);
    }

    public SchemaType getSchemaType() {
        return NavigationTreeDocument.type;
    }

    private void build(Node parentNode, NavigationTreeDocument navTreeXml, BuildContext buildContext) {
        if (navTreeXml.getNavigationTree().isSetCollections()) {
            List<NavigationTreeDocument.NavigationTree.Collections.Collection> collections = navTreeXml.getNavigationTree().getCollections().getCollectionList();
            collectionNames = new String[collections.size()];
            for (int i = 0; i < collections.size(); i++) {
                NavigationTreeDocument.NavigationTree.Collections.Collection collection = collections.get(i);
                collectionNames[i] = collection.getName();
            }
        }

        XmlObject[] nodesXml = navTreeXml.getNavigationTree().selectChildren(QNameSet.ALL);
        for (XmlObject nodeXml : nodesXml) {
            build(parentNode, nodeXml, buildContext);
        }
    }

    private void buildDoc(Node parentNode, DocDocument.Doc nodeXml, BuildContext buildContext) {
        long branchId = buildContext.getBranchId();
        long languageId = buildContext.getLanguageId();
        try {
            if (nodeXml.isSetBranch()) {
                String branch = ContextValuesResolver.resolve(nodeXml.getBranch(), buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
                branchId = buildContext.getNavContext().getBranchId(branch);
            }
            if (nodeXml.isSetLanguage()) {
                String language = ContextValuesResolver.resolve(nodeXml.getLanguage(), buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
                languageId = buildContext.getNavContext().getLanguageId(language);
            }
        } catch (Exception e) {
            parentNode.add(new ErrorNode("Error: " + e.getMessage()));
            return;
        }
        NodeVisibility nodeVisibility = nodeXml.isSetVisibility() ? NodeVisibility.fromString(nodeXml.getVisibility()) : NodeVisibility.ALWAYS;

        // The document ID needs to be normalized so that the VariantKeys can be
        // correctly compared.
        String documentId = nodeXml.getId().trim();
        documentId = ContextValuesResolver.resolve(documentId, buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
        try {
            documentId = buildContext.getNavContext().getRepository().normalizeDocumentId(documentId);
        } catch (Exception e) {
            parentNode.add(new ErrorNode("Invalid doc ID: " + documentId));
            return;
        }
        VariantKey variantKey = new VariantKey(documentId, branchId, languageId);

        String id = nodeXml.getNodeId();
        if (id != null)
            id = ContextValuesResolver.resolve(id, buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
        String label = nodeXml.getLabel();
        if (label != null)
            label = ContextValuesResolver.resolve(label, buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);

        DocumentNode node = new DocumentNode(variantKey, id, label, nodeVisibility, buildContext.getNavContext(),
                buildContext.getRootBranchId(), buildContext.getRootLanguageId(), buildContext.getVersionMode());

        buildContext.pushCounters(BuildContext.getNewCounters());
        try {
            XmlObject[] nodesXml = nodeXml.selectChildren(QNameSet.ALL);
            for (int i = 0; i < nodesXml.length; i++) {
                build(node, nodesXml[i], buildContext);
            }
        } finally {
            buildContext.popCounters();
        }

        parentNode.add(node);
    }

    private void buildQuery(Node parentNode, QueryDocument.Query nodeXml, BuildContext buildContext) {
        String select = ContextValuesResolver.resolve(nodeXml.getQ(), buildContext.getContextValuesStack(), ContextValuesResolver.Format.QUERY);

        StringBuilder extraCond = null;
        if (collectionNames != null) {
            extraCond = new StringBuilder();
            extraCond.append("InCollection(");
            for (int i = 0; i < collectionNames.length; i++) {
                extraCond.append("'").append(collectionNames[i]).append("'");
                if (i < collectionNames.length - 1)
                    extraCond.append(",");
            }
            extraCond.append(")");
        }
        if (!nodeXml.isSetFilterVariants() /* missing attribute means true */ || nodeXml.getFilterVariants()) {
            if (extraCond == null)
                extraCond = new StringBuilder();
            else
                extraCond.append(" and ");
            extraCond.append(" branchId = ").append(buildContext.getBranchId()).append(" and languageId = ").append(buildContext.getLanguageId());
        }

        NodeVisibility nodeVisibility = nodeXml.isSetVisibility() ? NodeVisibility.fromString(nodeXml.getVisibility()) : NodeVisibility.ALWAYS;

        List<QueryDocument.Query.Column> columns = nodeXml.getColumnList();
        List<QueryTreeBuilder.ColumnConfig> columnConfigs = new ArrayList<QueryTreeBuilder.ColumnConfig>(columns.size());
        for (QueryDocument.Query.Column column : columns) {
            NodeVisibility columnVisibility = column.isSetVisibility() ? NodeVisibility.fromString(column.getVisibility()) : null;
            boolean sort = column.isSetSortOrder() && !column.getSortOrder().equals("none");
            boolean ascending = "ascending".equals(column.getSortOrder());
            columnConfigs.add(new QueryTreeBuilder.ColumnConfig(sort, ascending, columnVisibility));
        }

        int maxColumns = nodeXml.isSetUseSelectValues() ? nodeXml.getUseSelectValues() : -1;
        XmlObject[] childNodes = nodeXml.selectChildren(QNameSet.ALL);
        QueryTreeBuilder.buildQueryBasedTree(parentNode, select, extraCond != null ? extraCond.toString() : null,
                maxColumns, nodeVisibility, columnConfigs, this, childNodes, buildContext);
    }

    private void buildGroup(Node parentNode, GroupDocument.Group nodeXml, BuildContext buildContext) {
        String label = ContextValuesResolver.resolve(nodeXml.getLabel(), buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
        String id = NavigationUtil.makeNodeIdValid(nodeXml.getId());
        if (id == null)
            id = "g" + buildContext.getNextValue(BuildContext.CounterType.GROUP_COUNTER);
        else
            id = ContextValuesResolver.resolve(id, buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
        NodeVisibility nodeVisibility = nodeXml.isSetVisibility() ? NodeVisibility.fromString(nodeXml.getVisibility()) : NodeVisibility.ALWAYS;
        GroupNode node = new GroupNode(id, label, nodeVisibility);

        buildContext.pushCounters(BuildContext.getNewCounters());
        try {
            XmlObject[] nodesXml = nodeXml.selectChildren(QNameSet.ALL);
            for (XmlObject childNodeXml : nodesXml) {
                build(node, childNodeXml, buildContext);
            }
        } finally {
            buildContext.popCounters();
        }

        parentNode.add(node);
    }

    private void buildImport(Node parentNode, ImportDocument.Import nodeXml, BuildContext buildContext) {
        long branchId = buildContext.getBranchId();
        long languageId = buildContext.getLanguageId();
        try {
            if (nodeXml.isSetBranch()) {
                String branch = ContextValuesResolver.resolve(nodeXml.getBranch(), buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
                branchId = buildContext.getNavContext().getBranchId(branch);
            }
            if (nodeXml.isSetLanguage()) {
                String language = ContextValuesResolver.resolve(nodeXml.getLanguage(), buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
                languageId = buildContext.getNavContext().getLanguageId(language);
            }
        } catch (RepositoryException e) {
            parentNode.add(new ErrorNode("Import error: " + e.getMessage()));
            return;
        }

        String navDocId = ContextValuesResolver.resolve(nodeXml.getDocId().trim(), buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
        try {
            navDocId = buildContext.getNavContext().getRepository().normalizeDocumentId(navDocId);
        } catch (Exception e) {
            parentNode.add(new ErrorNode("Invalid doc ID: " + navDocId));
            return;
        }
        VariantKey navigationDoc = new VariantKey(navDocId, branchId, languageId);

        if (buildContext.containsImport(navigationDoc)) {
            parentNode.add(new ErrorNode("(error: recursive import of " + navigationDoc.getDocumentId() + ")"));
        } else {
            buildContext.pushImport(navigationDoc);
            try {
                NavigationFactory.build(parentNode, navigationDoc, buildContext);
            } catch (Throwable e) {
                buildContext.getNavContext().getLogger().error("Error building navtree in " + navigationDoc.getDocumentId() + " (specified in an import node).", e);
                parentNode.add(new ErrorNode("(failed import of " + navigationDoc.getDocumentId() + ")"));
            } finally {
                buildContext.popImport();
            }
        }
    }

    private void buildLink(Node parentNode, LinkDocument.Link nodeXml, BuildContext buildContext) {
        String id = NavigationUtil.makeNodeIdValid(nodeXml.getId());
        if (id == null)
            id = "l" + buildContext.getNextValue(BuildContext.CounterType.LINK_COUNTER);
        else
            id = ContextValuesResolver.resolve(id, buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);

        String url = ContextValuesResolver.resolve(nodeXml.getUrl(), buildContext.getContextValuesStack(), ContextValuesResolver.Format.URL);
        String label = ContextValuesResolver.resolve(nodeXml.getLabel(), buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);

        VariantKey aclVariantKey = null;
        if (nodeXml.isSetInheritAclDocId()) {

            String aclDocId = nodeXml.getInheritAclDocId().trim();
            if (aclDocId.equals("this")) {
                try {
                    aclVariantKey = buildContext.peekImport();
                } catch (EmptyStackException e) {
                    // ignore
                    // will only occur if the navigation doc is not stored in a navigation tree
                    // in which case we default to having the link node visible
                }
            } else {
                long branchId = buildContext.getBranchId();
                long languageId = buildContext.getLanguageId();
                try {
                    if (nodeXml.isSetInheritAclBranch()) {
                        String branch = ContextValuesResolver.resolve(nodeXml.getInheritAclBranch(), buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
                        branchId = buildContext.getNavContext().getBranchId(branch);
                    }
                    if (nodeXml.isSetInheritAclLanguage()) {
                        String language = ContextValuesResolver.resolve(nodeXml.getInheritAclLanguage(), buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
                        languageId = buildContext.getNavContext().getLanguageId(language);
                    }
                } catch (Exception e) {
                    parentNode.add(new ErrorNode("Error: " + e.getMessage()));
                    return;
                }

                aclDocId = ContextValuesResolver.resolve(aclDocId, buildContext.getContextValuesStack(), ContextValuesResolver.Format.STRING);
                try {
                    aclDocId = buildContext.getNavContext().getRepository().normalizeDocumentId(aclDocId);
                } catch (Exception e) {
                    parentNode.add(new ErrorNode("Invalid doc ID: " + aclDocId));
                    return;
                }
                aclVariantKey = new VariantKey(aclDocId, branchId, languageId);
            }
        }

        LinkNode node;
        try {
            node = new LinkNode(id, url, label, aclVariantKey, buildContext.getVersionMode(), buildContext.getNavContext());
        } catch (NavigationException e) {
            parentNode.add(new ErrorNode("Link node error: " + e.getMessage()));
            return;
        }

        buildContext.pushCounters(BuildContext.getNewCounters());
        try {
            XmlObject[] nodesXml = nodeXml.selectChildren(QNameSet.ALL);
            for (XmlObject childNodeXml : nodesXml) {
                build(node, childNodeXml, buildContext);
            }
        } finally {
            buildContext.popCounters();
        }

        parentNode.add(node);
    }

    private void buildSeparator(Node parentNode) {
        SeparatorNode node = new SeparatorNode();
        parentNode.add(node);
    }

    public void build(Node parentNode, XmlObject object, BuildContext buildContext) {
        if (object instanceof DocDocument.Doc)
            buildDoc(parentNode, (DocDocument.Doc)object, buildContext);
        else if (object instanceof QueryDocument.Query)
            buildQuery(parentNode, (QueryDocument.Query)object, buildContext);
        else if (object instanceof GroupDocument.Group)
            buildGroup(parentNode, (GroupDocument.Group)object, buildContext);
        else if (object instanceof ImportDocument.Import)
            buildImport(parentNode, (ImportDocument.Import)object, buildContext);
        else if (object instanceof LinkDocument.Link)
            buildLink(parentNode, (LinkDocument.Link)object, buildContext);
        else if (object instanceof SeparatorDocument.Separator)
            buildSeparator(parentNode);
        else if (object instanceof NavigationTreeDocument.NavigationTree.Collections)
            ; // ignore
        else if (object instanceof QueryDocument.Query.Column)
            ; // ignore
        else
            throw new RuntimeException("Unexpected type: " + object.getClass().getName());
    }

}
