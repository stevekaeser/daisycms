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
package org.outerj.daisy.tools.importexport.model.document;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.util.XmlizerUtil;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.jaxen.dom.DOMXPath;
import org.jaxen.JaxenException;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.List;

public class ImpExpDocumentDexmlizer {
    private String documentId;
    private String branch;
    private String language;
    private Document xmlDoc;
    private Repository repository;
    private ImpExpDocument document;
    private DataRefResolver dataRefResolver;

    private static DOMXPath NAME_XPATH;
    private static DOMXPath TYPE_XPATH;
    private static DOMXPath VERSION_STATE_XPATH;
    private static DOMXPath REF_LANG_XPATH;
    private static DOMXPath PARTS_XPATH;
    private static DOMXPath FIELDS_XPATH;
    private static DOMXPath CUSTOMFIELDS_XPATH;
    private static DOMXPath COLLECTIONS_XPATH;
    private static DOMXPath LINKS_XPATH;
    private static DOMXPath LINK_TITLE_XPATH;
    private static DOMXPath LINK_TARGET_XPATH;

    static {
        try {
            NAME_XPATH = new DOMXPath("string(/document/name[1])");
            TYPE_XPATH = new DOMXPath("string(/document/@type)");
            VERSION_STATE_XPATH = new DOMXPath("string(/document/@versionState)");
            REF_LANG_XPATH = new DOMXPath("string(/document/@referenceLanguage)");
            PARTS_XPATH = new DOMXPath("/document/parts/part");
            FIELDS_XPATH = new DOMXPath("/document/fields/field");
            CUSTOMFIELDS_XPATH = new DOMXPath("/document/customFields/customField");
            COLLECTIONS_XPATH = new DOMXPath("/document/collections/collection");
            LINKS_XPATH = new DOMXPath("/document/links/link");
            LINK_TITLE_XPATH = new DOMXPath("string(title)");
            LINK_TARGET_XPATH = new DOMXPath("string(target)");
        } catch (JaxenException e) {
            throw new RuntimeException("Error initializing xpath expressions in document loader.", e);
        }
    }

    public static ImpExpDocument fromXml(String documentId, String branch, String language, InputStream is,
            Repository repository, DataRefResolver dataRefResolver) throws Exception {
        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document xmlDoc = factory.newDocumentBuilder().parse(is);

        return new ImpExpDocumentDexmlizer(documentId, branch, language, xmlDoc, repository, dataRefResolver).run();
    }

    private ImpExpDocumentDexmlizer(String documentId, String branch, String language, Document xmlDoc,
            Repository repository, DataRefResolver dataRefResolver) {
        this.documentId = documentId;
        this.branch = branch;
        this.language = language;
        this.xmlDoc = xmlDoc;
        this.repository = repository;
        this.dataRefResolver = dataRefResolver;
    }

    private ImpExpDocument run() throws Exception {
        String type = getDocumentType();
        String name = getName();
        document = new ImpExpDocument(documentId, branch, language, type, name);
        loadVersionState();
        loadReferenceLanguage();
        loadFields();
        loadParts();
        loadCustomFields();
        loadCollections();
        loadLinks();
        loadOwner();
        return document;
    }

    private String getName() throws ImportExportException {
        String name;
        try {
            name = NAME_XPATH.stringValueOf(xmlDoc);
        } catch (JaxenException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the document name.", e);
        }
        if (name == null || name.trim().equals(""))
            throw new ImportExportException("Missing or empty name for " + getDescription());
        return name;
    }

    private String getDocumentType() throws ImportExportException {
        String type;
        try {
            type = TYPE_XPATH.stringValueOf(xmlDoc);
        } catch (JaxenException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the document type.", e);
        }
        if (type == null || type.trim().equals(""))
            throw new ImportExportException("Missing or empty type attribute for " + getDescription());
        return type;
    }

    private void loadVersionState() throws ImportExportException {
        String versionStateName;
        try {
            versionStateName = VERSION_STATE_XPATH.stringValueOf(xmlDoc);
        } catch (JaxenException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the version state.", e);
        }
        if (versionStateName != null && versionStateName.trim().length() > 0) {
            document.setVersionState(VersionState.fromString(versionStateName));
        }
    }

    private void loadReferenceLanguage() throws ImportExportException {
        String refLangName;
        try {
            refLangName = REF_LANG_XPATH.stringValueOf(xmlDoc);
        } catch (JaxenException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the reference language.", e);
        }
        if (refLangName != null && refLangName.trim().length() > 0) {
            document.setReferenceLanguage(refLangName);
        }
    }

    private void loadFields() throws Exception {
        List fieldElements;
        try {
             fieldElements = FIELDS_XPATH.selectNodes(xmlDoc);
        } catch (JaxenException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the fields.", e);
        }

        for (Object fieldElement : fieldElements) {
            Element fieldEl = (Element)fieldElement;

            String type = DocumentHelper.getAttribute(fieldEl, "type", true);
            FieldType fieldType = repository.getRepositorySchema().getFieldTypeByName(type, false);

            Object value = getFieldValue(fieldEl, fieldType);
            document.addField(new ImpExpField(fieldType, value));
        }
    }

    private void loadParts() throws Exception {
        List partElements;
        try {
             partElements = PARTS_XPATH.selectNodes(xmlDoc);
        } catch (JaxenException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the parts.", e);
        }

        for (Object partElement : partElements) {
            Element partEl = (Element)partElement;
            String type = DocumentHelper.getAttribute(partEl, "type", true);
            PartType partType = repository.getRepositorySchema().getPartTypeByName(type, false);
            String mimeType = DocumentHelper.getAttribute(partEl, "mimeType", true);
            final String dataRef = DocumentHelper.getAttribute(partEl, "dataRef", true);
            String fileName = DocumentHelper.getAttribute(partEl, "fileName", false);

            ImpExpPart part = new ImpExpPart(partType, mimeType, fileName, new ImpExpPart.PartDataAccess() {
                public InputStream getInputStream() throws Exception {
                    return dataRefResolver.getInputStream(dataRef);
                }

                public long getSize() throws Exception {
                    return dataRefResolver.getSize(dataRef);
                }
            });

            document.addPart(part);
        }
    }

    private void loadCustomFields() throws Exception {
        List customFieldElements;
        try {
             customFieldElements = CUSTOMFIELDS_XPATH.selectNodes(xmlDoc);
        } catch (JaxenException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the custom fields.", e);
        }

        for (Object customFieldElement : customFieldElements) {
            Element fieldEl = (Element)customFieldElement;

            String name = DocumentHelper.getAttribute(fieldEl, "name", true);
            String value = DocumentHelper.getAttribute(fieldEl, "value", true);

            document.addCustomField(new ImpExpCustomField(name, value));
        }
    }

    private void loadCollections() throws Exception {
        List collectionElements;
        try {
             collectionElements = COLLECTIONS_XPATH.selectNodes(xmlDoc);
        } catch (JaxenException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the collections.", e);
        }

        for (Object collectionElement : collectionElements) {
            Element collectionEl = (Element)collectionElement;
            String collectioName = DocumentHelper.getElementText(collectionEl, true);
            document.addCollection(collectioName);
        }
    }

    private void loadLinks() throws Exception {
        List linkElements;
        try {
             linkElements = LINKS_XPATH.selectNodes(xmlDoc);
        } catch (JaxenException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the links.", e);
        }

        for (Object linkElement : linkElements) {
            Element linkEl = (Element)linkElement;
            String title = LINK_TITLE_XPATH.stringValueOf(linkEl);
            String target = LINK_TARGET_XPATH.stringValueOf(linkEl);
            ImpExpLink link = new ImpExpLink(title, target);
            document.addLink(link);
        }
    }

    private Object getFieldValue(Element fieldEl, FieldType fieldType) throws Exception {
        if (fieldType.isMultiValue()) {
            Element[] valueEls = DocumentHelper.getElementChildren(fieldEl, fieldType.isHierarchical() ? "hierarchyPath" : "value");
            Object[] parsedValues = new Object[valueEls.length];
            for (int i = 0; i < valueEls.length; i++) {
                if (fieldType.isHierarchical()) {
                    parsedValues[i] = getHierarchicalFieldValue(valueEls[i], fieldType.getValueType());
                } else {
                    String value = DocumentHelper.getElementText(valueEls[i], true);
                    parsedValues[i] = XmlizerUtil.parseValue(fieldType.getValueType(), value, repository);
                }
            }
            return parsedValues;
        } else {
            if (fieldType.isHierarchical()) {
                Element hierarchyEl = DocumentHelper.getElementChild(fieldEl, "hierarchyPath", true);
                return getHierarchicalFieldValue(hierarchyEl, fieldType.getValueType());
            } else {
                String value = DocumentHelper.getAttribute(fieldEl, "value", true);
                Object parsedValue = XmlizerUtil.parseValue(fieldType.getValueType(), value, repository);
                return parsedValue;
            }
        }
    }

    private HierarchyPath getHierarchicalFieldValue(Element element, ValueType valueType) throws Exception {
        Element[] valueEls = DocumentHelper.getElementChildren(element, "value");
        Object[] parsedValues = new Object[valueEls.length];
        for (int i = 0; i < valueEls.length; i++) {
            String value = DocumentHelper.getElementText(valueEls[i], true);
            parsedValues[i] = XmlizerUtil.parseValue(valueType, value, repository);
        }
        return new HierarchyPath(parsedValues);
    }

    private void loadOwner() throws Exception {
        String owner = DocumentHelper.getAttribute(xmlDoc.getDocumentElement(), "owner", false);
        if (owner != null)
            document.setOwner(owner);
    }

    protected String getDescription() {
        return documentId + "~" + branch + "~" + language;
    }

    public static interface DataRefResolver {
        InputStream getInputStream(String dataRef) throws Exception;
        long getSize(String dataRef) throws Exception;
    }

}
