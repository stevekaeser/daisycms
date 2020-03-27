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

import org.outerj.daisy.tools.importexport.util.XmlizerUtil;
import org.outerj.daisy.tools.importexport.util.XmlProducer;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.LocaleHelper;
import org.xml.sax.SAXException;

import java.io.OutputStream;
import java.util.*;

/**
 * Converts an ImpExpSchema to an XML document, which can be parsed
 * again using {@link ImpExpSchemaDexmlizer}.
 */
public class ImpExpSchemaXmlizer {
    private ImpExpSchema schema;
    private OutputStream outputStream;
    private XmlProducer xmlProducer;
    private Repository repository;

    public static void toXml(ImpExpSchema schema, OutputStream outputStream, Repository repository) throws Exception {
        new ImpExpSchemaXmlizer(schema, outputStream, repository).run();
    }

    private ImpExpSchemaXmlizer(ImpExpSchema schema, OutputStream outputStream, Repository repository) {
        this.schema = schema;
        this.outputStream = outputStream;
        this.repository = repository;
    }

    private void run() throws Exception {
        xmlProducer = new XmlProducer(outputStream);
        xmlProducer.startElement("schema");

        writeFieldTypes(schema.getFieldTypes());
        writePartTypes(schema.getPartTypes());
        writeDocumentTypes(schema.getDocumentTypes());

        xmlProducer.endElement("schema");
        xmlProducer.flush();
    }

    private void writeFieldTypes(ImpExpFieldType[] fieldTypes) throws SAXException {
        Arrays.sort(fieldTypes);
        for (ImpExpFieldType fieldType : fieldTypes) {
            writeFieldType(fieldType);
            xmlProducer.newLine();
        }
    }

    private void writePartTypes(ImpExpPartType[] partTypes) throws SAXException {
        Arrays.sort(partTypes);
        for (ImpExpPartType partType : partTypes) {
            writePartType(partType);
            xmlProducer.newLine();
        }
    }

    private void writeDocumentTypes(ImpExpDocumentType[] documentTypes) throws SAXException {
        Arrays.sort(documentTypes);
        for (ImpExpDocumentType documentType : documentTypes) {
            writeDocumentType(documentType);
            xmlProducer.newLine();
        }
    }

    private void writeFieldType(ImpExpFieldType fieldType) throws SAXException {
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("name", fieldType.getName());
        attrs.put("valueType", fieldType.getValueType().toString());
        attrs.put("multiValue", String.valueOf(fieldType.isMultiValue()));
        attrs.put("hierarchical", String.valueOf(fieldType.isHierarchical()));
        attrs.put("aclAllowed", String.valueOf(fieldType.isAclAllowed()));
        attrs.put("allowFreeEntry", String.valueOf(fieldType.getAllowFreeEntry()));
        attrs.put("loadSelectionListAsync", String.valueOf(fieldType.getLoadSelectionListAsync()));
        attrs.put("deprecated", String.valueOf(fieldType.isDeprecated()));
        attrs.put("size", String.valueOf(fieldType.getSize()));
        xmlProducer.startElement("fieldType", attrs);

        Object selectionList = fieldType.getSelectionList();
        if (selectionList == null) {
            // do nothing
        } else if (selectionList instanceof ImpExpStaticSelectionList) {
            ImpExpStaticSelectionList impExpList = (ImpExpStaticSelectionList)selectionList;
            List<ImpExpStaticListItem> items = impExpList.getItems();
            xmlProducer.startElement("staticSelectionList");
            for (ImpExpStaticListItem item : items) {
                outputStaticListItem(item);
            }
            xmlProducer.endElement("staticSelectionList");
        } else if (selectionList instanceof ImpExpQuerySelectionList) {
            ImpExpQuerySelectionList impExpList = (ImpExpQuerySelectionList)selectionList;
            attrs.clear();
            attrs.put("query", impExpList.getQuery());
            attrs.put("filterVariants", String.valueOf(impExpList.getFilterVariants()));
            attrs.put("sortOrder", impExpList.getSortOrder().toString());
            xmlProducer.emptyElement("querySelectionList", attrs);
        } else if (selectionList instanceof ImpExpLinkQuerySelectionList) {
            ImpExpLinkQuerySelectionList impExpList = (ImpExpLinkQuerySelectionList)selectionList;
            attrs.clear();
            attrs.put("whereClause", impExpList.getWhereClause());
            attrs.put("filterVariants", String.valueOf(impExpList.getFilterVariants()));
            xmlProducer.emptyElement("linkQuerySelectionList", attrs);
        } else if (selectionList instanceof ImpExpHierarchicalQuerySelectionList) {
            ImpExpHierarchicalQuerySelectionList impExpList = (ImpExpHierarchicalQuerySelectionList)selectionList;
            attrs.clear();
            attrs.put("whereClause", impExpList.getWhereClause());
            attrs.put("filterVariants", String.valueOf(impExpList.getFilterVariants()));
            xmlProducer.startElement("hierarchicalQuerySelectionList", attrs);
            xmlProducer.startElement("linkFields");
            for (String linkField : impExpList.getLinkFields()) {
                xmlProducer.simpleElement("linkField", linkField);
            }
            xmlProducer.endElement("linkFields");
            xmlProducer.endElement("hierarchicalQuerySelectionList");
        } else if (selectionList instanceof ImpExpParentLinkedSelectionList) {
            ImpExpParentLinkedSelectionList impExpList = (ImpExpParentLinkedSelectionList)selectionList;
            attrs.clear();
            attrs.put("whereClause", impExpList.getWhereClause());
            attrs.put("parentLinkField", impExpList.getParentLinkField());
            attrs.put("filterVariants", String.valueOf(impExpList.getFilterVariants()));
            xmlProducer.emptyElement("parentLinkedSelectionList", attrs);
        } else {
            throw new RuntimeException("Unsupported/unexpected sort of selection list: " + selectionList.getClass().getName());
        }

        writeLocaleValues(fieldType.getLabels(), "label");
        writeLocaleValues(fieldType.getDescriptions(), "description");

        xmlProducer.endElement("fieldType");
    }

    private void outputStaticListItem(ImpExpStaticListItem item) throws SAXException {
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("value", XmlizerUtil.formatValue(item.getValue(), item.getValueType(), repository));
        Map<Locale, String> labels = item.getLabels();
        List<ImpExpStaticListItem> children = item.getItems();
        if (labels.size() > 0 || children.size() > 0) {
            xmlProducer.startElement("item", attrs);
            writeLocaleValues(labels, "label");
            for (ImpExpStaticListItem child : children) {
                outputStaticListItem(child);
            }
            xmlProducer.endElement("item");
        } else {
            xmlProducer.emptyElement("item", attrs);
        }
    }

    private void writePartType(ImpExpPartType partType) throws SAXException {
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("name", partType.getName());
        attrs.put("mimeTypes", partType.getMimeTypes());
        attrs.put("daisyHtml", String.valueOf(partType.isDaisyHtml()));
        attrs.put("deprecated", String.valueOf(partType.isDeprecated()));
        if (partType.getLinkExtractor() != null) {
            attrs.put("linkExtractor", partType.getLinkExtractor());
        }
        xmlProducer.startElement("partType", attrs);

        writeLocaleValues(partType.getLabels(), "label");
        writeLocaleValues(partType.getDescriptions(), "description");

        xmlProducer.endElement("partType");
    }

    private void writeDocumentType(ImpExpDocumentType documentType) throws SAXException {
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("name", documentType.getName());
        attrs.put("deprecated", String.valueOf(documentType.isDeprecated()));
        xmlProducer.startElement("documentType", attrs);

        ImpExpFieldTypeUse[] fieldTypeUses = documentType.getFieldTypeUses();
        for (ImpExpFieldTypeUse fieldTypeUse : fieldTypeUses) {
            attrs.clear();
            attrs.put("fieldTypeName", fieldTypeUse.getFieldTypeName());
            attrs.put("required", String.valueOf(fieldTypeUse.isRequired()));
            attrs.put("editable", String.valueOf(fieldTypeUse.isEditable()));
            xmlProducer.emptyElement("fieldTypeUse", attrs);
        }

        ImpExpPartTypeUse[] partTypeUses = documentType.getPartTypeUses();
        for (ImpExpPartTypeUse partTypeUse : partTypeUses) {
            attrs.clear();
            attrs.put("partTypeName", partTypeUse.getPartTypeName());
            attrs.put("required", String.valueOf(partTypeUse.isRequired()));
            attrs.put("editable", String.valueOf(partTypeUse.isEditable()));
            xmlProducer.emptyElement("partTypeUse", attrs);
        }

        writeLocaleValues(documentType.getLabels(), "label");
        writeLocaleValues(documentType.getDescriptions(), "description");

        xmlProducer.endElement("documentType");
    }

    private void writeLocaleValues(Map<Locale, String> localeValues, String tagName) throws SAXException {
        List<Map.Entry> entries = new ArrayList<Map.Entry>(localeValues.entrySet());
        Collections.sort(entries, LOCALE_MAP_ENTRY_COMPARATOR);
        Map<String, String> attrs = new LinkedHashMap<String, String>();
        for (Map.Entry<Locale, String> entry : entries) {
            attrs.clear();
            attrs.put("locale", LocaleHelper.getString(entry.getKey()));
            attrs.put("text", entry.getValue());
            xmlProducer.emptyElement(tagName, attrs);
        }
    }

    private static LocaleMapEntryComparator LOCALE_MAP_ENTRY_COMPARATOR = new LocaleMapEntryComparator();

    static class LocaleMapEntryComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            Map.Entry entry1 = (Map.Entry)o1;
            Map.Entry entry2 = (Map.Entry)o2;
            Locale locale1 = (Locale)entry1.getKey();
            Locale locale2 = (Locale)entry2.getKey();
            return locale1.toString().compareTo(locale2.toString());
        }
    }

}
