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

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.InvalidDocumentIdException;
import org.outerj.daisy.navigation.NavigationException;
import org.outerx.daisy.x10Bookdef.BookDocument;
import org.outerx.daisy.x10Bookdef.SectionDocument;
import org.outerx.daisy.x10Bookdef.QueryDocument;
import org.outerx.daisy.x10Bookdef.ImportNavigationTreeDocument;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.QNameSet;

import java.util.ArrayList;

public class BookNavigationBuilder implements NavigationFactory.NavigationBuilder, NodeBuilder {
    private CommonNavigationManager.Context context;

    public void buildTree(Node parentNode, XmlObject xmlObject, BuildContext buildContext) throws RepositoryException {
        context = buildContext.getNavContext();
        build(parentNode, ((BookDocument)xmlObject).getBook(), buildContext);
    }

    public SchemaType getSchemaType() {
        return BookDocument.type;
    }

    private void build(Node parentNode, BookDocument.Book bookXml, BuildContext buildContext) {
        XmlObject[] nodesXml = bookXml.getContent().selectChildren(QNameSet.ALL);
        for (int i = 0; i < nodesXml.length; i++) {
            build(parentNode, nodesXml[i], buildContext);
        }
    }

    private void build(Node parentNode, SectionDocument.Section nodeXml, BuildContext buildContext) {
        AbstractParentNode node;
        if (nodeXml.isSetDocumentId()) {
            long branchId = buildContext.getBranchId();
            long languageId = buildContext.getLanguageId();
            try {
                if (nodeXml.isSetBranch())
                    branchId = context.getBranchId(nodeXml.getBranch());
                if (nodeXml.isSetLanguage())
                    languageId = context.getLanguageId(nodeXml.getLanguage());
            } catch (Exception e) {
                parentNode.add(new ErrorNode("Error: " + e.getMessage()));
                return;
            }
            String documentId = nodeXml.getDocumentId().trim();
            try {
                documentId = context.getRepository().normalizeDocumentId(documentId);
            } catch (InvalidDocumentIdException e) {
                parentNode.add(new ErrorNode("Invalid doc ID: " + documentId));
                return;
            }
            VariantKey variantKey = new VariantKey(documentId, branchId, languageId);
            node = new DocumentNode(variantKey, null, nodeXml.getNavlabel(), NodeVisibility.ALWAYS, context, buildContext.getRootBranchId(), buildContext.getRootLanguageId(), buildContext.getVersionMode());
        } else {
            String id = "g" + buildContext.getNextValue(BuildContext.CounterType.GROUP_COUNTER);
            node = new GroupNode(id, nodeXml.getTitle(), NodeVisibility.ALWAYS);
        }

        try {
            buildContext.pushCounters(BuildContext.getNewCounters());
            XmlObject[] nodesXml = nodeXml.selectChildren(QNameSet.ALL);
            for (int i = 0; i < nodesXml.length; i++) {
                build(node, nodesXml[i], buildContext);
            }
        } finally {
            buildContext.popCounters();
        }

        parentNode.add(node);
    }

    private void build(Node parentNode, QueryDocument.Query nodeXml, BuildContext buildContext) {
        String select = nodeXml.getQ();

        StringBuilder extraCond = null;
        if (!nodeXml.isSetFilterVariants() /* missing attribute means true */ || nodeXml.getFilterVariants()) {
            extraCond = new StringBuilder();
            extraCond.append(" branchId = ").append(buildContext.getBranchId()).append(" and languageId = ").append(buildContext.getLanguageId());
        }

        XmlObject[] childNodes = nodeXml.selectChildren(QNameSet.ALL);
        QueryTreeBuilder.buildQueryBasedTree(parentNode, select, extraCond != null ? extraCond.toString() : null,
                -1, NodeVisibility.ALWAYS, new ArrayList<QueryTreeBuilder.ColumnConfig>(), this, childNodes, buildContext);
    }

    private void build(Node parentNode, ImportNavigationTreeDocument.ImportNavigationTree nodeXml, BuildContext buildContext) {
        String documentId = nodeXml.getId().trim();
        try {
            documentId = context.getRepository().normalizeDocumentId(documentId);
        } catch (Exception e) {
            parentNode.add(new ErrorNode("Invalid doc ID: " + documentId));
            return;
        }
        long branchId = buildContext.getBranchId();
        long languageId = buildContext.getLanguageId();
        try {
            if (nodeXml.isSetBranch())
                branchId = context.getBranchId(nodeXml.getBranch());
            if (nodeXml.isSetLanguage())
                languageId = context.getLanguageId(nodeXml.getLanguage());
        } catch (Exception e) {
            parentNode.add(new ErrorNode("Error: " + e.getMessage()));
            return;
        }

        VariantKey navigationDoc = new VariantKey(documentId, branchId, languageId);
        if (buildContext.containsImport(navigationDoc)) {
            parentNode.add(new ErrorNode("(error: recursive import of " + navigationDoc.getDocumentId() + ")"));
        } else {
            buildContext.pushImport(navigationDoc);
            try {
                NavigationFactory.build(parentNode, new VariantKey(documentId, branchId, languageId), buildContext);
            } catch (NavigationException e) {
                parentNode.add(new ErrorNode("(failed import of " + navigationDoc.getDocumentId() + ")"));
            } finally {
                buildContext.popImport();
            }
        }
    }

    public void build(Node parentNode, XmlObject object, BuildContext buildContext) {
        if (object instanceof SectionDocument.Section)
            build(parentNode, (SectionDocument.Section)object, buildContext);
        else if (object instanceof QueryDocument.Query)
            build(parentNode, (QueryDocument.Query)object, buildContext);
        else if (object instanceof ImportNavigationTreeDocument.ImportNavigationTree)
            build(parentNode, (ImportNavigationTreeDocument.ImportNavigationTree)object, buildContext);
        else
            throw new RuntimeException("Unexpected type: " + object.getClass().getName());
    }
}
