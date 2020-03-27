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
package org.outerj.daisy.tools.importexport.model.schema;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.tools.importexport.util.XmlizerUtil;
import org.outerj.daisy.tools.importexport.ImportExportException;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.Locale;

/**
 * Constructs an ImpExpSchema from an XML document.
 */
public class ImpExpSchemaDexmlizer {
    private Document xmlDoc;
    private Repository repository;
    private Listener listener;

    public static ImpExpSchema fromXml(InputStream is, Repository repository, Listener listener) throws Exception {
        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document xmlDoc = factory.newDocumentBuilder().parse(is);

        return new ImpExpSchemaDexmlizer(xmlDoc, repository, listener).run();
    }

    private ImpExpSchemaDexmlizer(Document xmlDoc, Repository repository, Listener listener) {
        this.xmlDoc = xmlDoc;
        this.repository = repository;
        this.listener = listener;
    }

    private ImpExpSchema run() throws Exception {
        ImpExpSchema schema = new ImpExpSchema();

        Element docEl = xmlDoc.getDocumentElement();
        if (docEl.getNamespaceURI() != null || !docEl.getLocalName().equals("schema")) {
            throw new ImportExportException("Expected root element <schema>");
        }

        Element[] elements = DocumentHelper.getElementChildren(docEl);
        for (Element element : elements) {
            if (element.getNamespaceURI() == null) {
                if (element.getLocalName().equals("fieldType")) {
                    ImpExpFieldType fieldType = buildFieldType(element);
                    if (schema.hasFieldType(fieldType.getName()))
                        listener.info("Duplicate field type detected: " + fieldType.getName());
                    schema.addFieldType(fieldType);
                } else if (element.getLocalName().equals("partType")) {
                    ImpExpPartType partType = buildPartType(element);
                    if (schema.hasPartType(partType.getName()))
                        listener.info("Duplicate part type detected: " + partType.getName());
                    schema.addPartType(partType);
                } else if (element.getLocalName().equals("documentType")) {
                    ImpExpDocumentType documentType = buildDocumentType(element);
                    if (schema.hasDocumentType(documentType.getName()))
                        listener.info("Duplicate document type detected: " + documentType.getName());
                    schema.addDocumentType(documentType);
                }
            }
        }

        return schema;
    }

    private ImpExpFieldType buildFieldType(Element element) throws Exception {
        String name = DocumentHelper.getAttribute(element, "name", true);
        String valueTypeName = DocumentHelper.getAttribute(element, "valueType", true);
        ValueType valueType = ValueType.fromString(valueTypeName);
        boolean aclAllowed = DocumentHelper.getBooleanAttribute(element, "aclAllowed", false);
        boolean multiValue = DocumentHelper.getBooleanAttribute(element, "multiValue", false);
        boolean hierarchical = DocumentHelper.getBooleanAttribute(element, "hierarchical", false);
        boolean allowFreeEntry = DocumentHelper.getBooleanAttribute(element, "allowFreeEntry", false);
        boolean loadSelectionListAsync = DocumentHelper.getBooleanAttribute(element, "loadSelectionListAsync", false);
        boolean deprecated = DocumentHelper.getBooleanAttribute(element, "deprecated", false);

        ImpExpFieldType fieldType = new ImpExpFieldType(name, valueType);

        String sizeAttr = element.getAttribute("size");
        int size = sizeAttr.equals("") ? 0 : Integer.parseInt(sizeAttr);
        fieldType.setSize(size);

        fieldType.setAclAllowed(aclAllowed);
        fieldType.setAllowFreeEntry(allowFreeEntry);
        fieldType.setLoadSelectionListAsync(loadSelectionListAsync);
        fieldType.setDeprecated(deprecated);
        fieldType.setMultiValue(multiValue);
        fieldType.setHierarchical(hierarchical);

        loadLabels(element, fieldType);
        loadDescriptions(element, fieldType);

        Object selectionList = null;
        Element staticSelectionListEl = DocumentHelper.getElementChild(element, "staticSelectionList", false);
        if (staticSelectionListEl != null) {
            ImpExpStaticSelectionList list = new ImpExpStaticSelectionList(valueType);
            Element[] itemElements = DocumentHelper.getElementChildren(staticSelectionListEl, "item");
            for (Element itemElement : itemElements) {
                list.addItem(buildStaticListItem(itemElement, valueType));
            }
            selectionList = list;
        }

        Element querySelectionList = DocumentHelper.getElementChild(element, "querySelectionList", false);
        if (querySelectionList != null) {
            String query = DocumentHelper.getAttribute(querySelectionList, "query", true);
            boolean filterVariants = Boolean.valueOf(DocumentHelper.getAttribute(querySelectionList, "filterVariants", true)).booleanValue();
            SortOrder order = SortOrder.fromString(DocumentHelper.getAttribute(querySelectionList, "sortOrder", true));
            selectionList = new ImpExpQuerySelectionList(query, order, filterVariants);
        }

        Element linkQuerySelectionList = DocumentHelper.getElementChild(element, "linkQuerySelectionList", false);
        if (linkQuerySelectionList != null) {
            String whereClause = DocumentHelper.getAttribute(linkQuerySelectionList, "whereClause", true);
            boolean filterVariants = Boolean.valueOf(DocumentHelper.getAttribute(linkQuerySelectionList, "filterVariants", true)).booleanValue();
            selectionList = new ImpExpLinkQuerySelectionList(whereClause, filterVariants);
        }

        Element hierQuerySelectionList = DocumentHelper.getElementChild(element, "hierarchicalQuerySelectionList", false);
        if (hierQuerySelectionList != null) {
            String whereClause = DocumentHelper.getAttribute(hierQuerySelectionList, "whereClause", true);
            boolean filterVariants = Boolean.valueOf(DocumentHelper.getAttribute(hierQuerySelectionList, "filterVariants", true)).booleanValue();
            String[] linkFields;
            Element linkFieldsEl = DocumentHelper.getElementChild(hierQuerySelectionList, "linkFields", false);
            if (linkFieldsEl != null) {
                Element[] linkFieldEls = DocumentHelper.getElementChildren(linkFieldsEl, "linkField");
                linkFields = new String[linkFieldEls.length];
                for (int i = 0; i < linkFieldEls.length; i++) {
                    linkFields[i] = DocumentHelper.getElementText(linkFieldEls[i], true);
                }
            } else {
                linkFields = new String[0];
            }
            selectionList = new ImpExpHierarchicalQuerySelectionList(whereClause, linkFields, filterVariants);
        }

        Element parentLinkedSelectionList = DocumentHelper.getElementChild(element, "parentLinkedSelectionList", false);
        if (parentLinkedSelectionList != null) {
            String whereClause = DocumentHelper.getAttribute(parentLinkedSelectionList, "whereClause", true);
            boolean filterVariants = Boolean.valueOf(DocumentHelper.getAttribute(parentLinkedSelectionList, "filterVariants", true)).booleanValue();
            String parentLinkField = DocumentHelper.getAttribute(parentLinkedSelectionList, "parentLinkField", true);
            selectionList = new ImpExpParentLinkedSelectionList(whereClause, parentLinkField, filterVariants);
        }

        fieldType.setSelectionList(selectionList);

        return fieldType;
    }

    private ImpExpStaticListItem buildStaticListItem(Element itemElement, ValueType valueType) throws Exception {
        String valueString = DocumentHelper.getAttribute(itemElement, "value", true);
        Object value = XmlizerUtil.parseValue(valueType, valueString, repository);
        ImpExpStaticListItem item = new ImpExpStaticListItem(value, valueType);
        loadLabels(itemElement, item);
        Element[] children = DocumentHelper.getElementChildren(itemElement, "item");
        for (Element child : children) {
            item.addItem(buildStaticListItem(child, valueType));
        }
        return item;
    }

    private ImpExpPartType buildPartType(Element element) throws Exception {
        String name = DocumentHelper.getAttribute(element, "name", true);
        ImpExpPartType partType = new ImpExpPartType(name);

        String mimeTypes = element.getAttribute("mimeTypes");
        boolean daisyHtml = DocumentHelper.getBooleanAttribute(element, "daisyHtml", false);
        boolean deprecated = DocumentHelper.getBooleanAttribute(element, "deprecated", false);
        String linkExtractor = element.getAttribute("linkExtractor");

        partType.setDaisyHtml(daisyHtml);
        partType.setDeprecated(deprecated);
        if (linkExtractor.length() > 0)
            partType.setLinkExtractor(linkExtractor);
        partType.setMimeTypes(mimeTypes);

        loadLabels(element, partType);
        loadDescriptions(element, partType);

        return partType;
    }

    private ImpExpDocumentType buildDocumentType(Element element) throws Exception {
        String name = DocumentHelper.getAttribute(element, "name", true);
        ImpExpDocumentType documentType = new ImpExpDocumentType(name);

        boolean deprecated = DocumentHelper.getBooleanAttribute(element, "deprecated", false);
        documentType.setDeprecated(deprecated);

        Element[] fieldTypeUseElements = DocumentHelper.getElementChildren(element, "fieldTypeUse");
        for (Element fieldTypeUseEl : fieldTypeUseElements) {
            String typeName = DocumentHelper.getAttribute(fieldTypeUseEl, "fieldTypeName", true);
            boolean required = DocumentHelper.getBooleanAttribute(fieldTypeUseEl, "required", true);
            boolean editable = DocumentHelper.getBooleanAttribute(fieldTypeUseEl, "editable", true);
            ImpExpFieldTypeUse fieldTypeUse = new ImpExpFieldTypeUse(typeName, required);
            fieldTypeUse.setEditable(editable);
            documentType.addFieldTypeUse(fieldTypeUse);
        }

        Element[] partTypeUseElements = DocumentHelper.getElementChildren(element, "partTypeUse");
        for (Element partTypeUseEl : partTypeUseElements) {
            String typeName = DocumentHelper.getAttribute(partTypeUseEl, "partTypeName", true);
            boolean required = DocumentHelper.getBooleanAttribute(partTypeUseEl, "required", true);
            boolean editable = DocumentHelper.getBooleanAttribute(partTypeUseEl, "editable", true);
            ImpExpPartTypeUse partTypeUse = new ImpExpPartTypeUse(typeName, required);
            partTypeUse.setEditable(editable);
            documentType.addPartTypeUse(partTypeUse);
        }

        loadLabels(element, documentType);
        loadDescriptions(element, documentType);

        return documentType;
    }

    private void loadLabels(Element element, ImpExpLabelEnabled labelEnabled) throws Exception {
        Element[] labels = DocumentHelper.getElementChildren(element, "label");
        for (Element label : labels) {
            Locale locale = LocaleHelper.parseLocale(DocumentHelper.getAttribute(label, "locale", true));
            String text = DocumentHelper.getAttribute(label, "text", true);
            if (text.length() > 0) // can be empty in case people leave 'placeholder' tags for languages to be completed
                labelEnabled.addLabel(locale, text);
        }
    }

    private void loadDescriptions(Element element, ImpExpDescriptionEnabled descriptionEnabled) throws Exception {
        Element[] labels = DocumentHelper.getElementChildren(element, "description");
        for (Element label : labels) {
            Locale locale = LocaleHelper.parseLocale(DocumentHelper.getAttribute(label, "locale", true));
            String text = DocumentHelper.getAttribute(label, "text", true);
            if (text.length() > 0) // can be empty in case people leave 'placeholder' tags for languages to be completed
                descriptionEnabled.addDescription(locale, text);
        }
    }

    public static interface Listener {
        void info(String message);
    }
}
