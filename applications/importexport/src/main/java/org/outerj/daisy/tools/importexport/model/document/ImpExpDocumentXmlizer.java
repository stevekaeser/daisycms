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

import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.tools.importexport.export.config.DocumentExportCustomizer;
import org.outerj.daisy.tools.importexport.util.XmlProducer;
import org.outerj.daisy.tools.importexport.util.XmlizerUtil;
import org.xml.sax.SAXException;

public class ImpExpDocumentXmlizer {
    private ImpExpDocument impExpDocument;
    private OutputStream outputStream;
    private PartWriter partWriter;
    private Repository repository;
    private XmlProducer xmlProducer;

    public static void toXml(ImpExpDocument impExpDocument, OutputStream outputStream, PartWriter partWriter, Repository repository) throws Exception {
        new ImpExpDocumentXmlizer(impExpDocument, outputStream, partWriter, repository).run();
    }

    private ImpExpDocumentXmlizer(ImpExpDocument impExpDocument, OutputStream outputStream, PartWriter partWriter, Repository repository) {
        this.impExpDocument = impExpDocument;
        this.outputStream = outputStream;
        this.partWriter = partWriter;
        this.repository = repository;
    }

    public void run() throws Exception {
        xmlProducer = new XmlProducer(outputStream);

        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("type", impExpDocument.getType());

        // owner
        if (impExpDocument.getOwner() != null) {
            attrs.put("owner", impExpDocument.getOwner());
        }
        // version state
        if (impExpDocument.getVersionState() != null) {
            attrs.put("versionState", impExpDocument.getVersionState().toString());
        }
        // reference language
        if (impExpDocument.getReferenceLanguage() != null) {
            attrs.put("referenceLanguage", impExpDocument.getReferenceLanguage());
        }

        xmlProducer.startElement("document", attrs);

        // document name
        xmlProducer.simpleElement("name", impExpDocument.getName());

        // parts
        ImpExpPart[] parts = impExpDocument.getParts();
        if (parts.length > 0) {
            xmlProducer.startElement("parts");
            for (ImpExpPart part : parts) {
                attrs.clear();
                attrs.put("type", part.getType().getName());
                attrs.put("mimeType", part.getMimeType());
                if (part.getFileName() != null) {
                    attrs.put("fileName", part.getFileName());
                }
                String dataRef = partWriter.writePart(part);
                attrs.put("dataRef", dataRef);
                xmlProducer.emptyElement("part", attrs);
            }
            xmlProducer.endElement("parts");
        }

        // links
        ImpExpLink[] links = impExpDocument.getLinks();
        if (links.length > 0) {
            xmlProducer.startElement("links");
            for (ImpExpLink link : links) {
                xmlProducer.startElement("link");
                xmlProducer.simpleElement("title", link.getTitle());
                xmlProducer.simpleElement("target", link.getTarget());
                xmlProducer.endElement("link");
            }
            xmlProducer.endElement("links");
        }

        // fields
        ImpExpField[] fields = impExpDocument.getFields();
        if (fields.length > 0) {
            xmlProducer.startElement("fields");
            for (ImpExpField field : fields) {
                attrs.clear();
                attrs.put("type", field.getType().getName());
                Object value = field.getValue();
                if (field.getType().isHierarchical() || field.getType().isMultiValue()) {
                    xmlProducer.startElement("field", attrs);
                    if (field.getType().isMultiValue()) {
                        Object[] values = (Object[]) value;
                        for (Object subValue : values) {
                            if (field.getType().isHierarchical())
                                outputValueHierarchical((HierarchyPath)subValue, field.getType().getValueType());
                            else
                                xmlProducer.simpleElement("value", XmlizerUtil.formatValue(subValue, field.getType().getValueType(), repository));
                        }
                    } else if (field.getType().isHierarchical()) {
                        outputValueHierarchical((HierarchyPath)value, field.getType().getValueType());
                    }
                    xmlProducer.endElement("field");
                } else {
                    attrs.put("value", XmlizerUtil.formatValue(value, field.getType().getValueType(), repository));
                    xmlProducer.emptyElement("field", attrs);
                }
            }
            xmlProducer.endElement("fields");
        }

        // collections
        String[] collections = impExpDocument.getCollections();
        if (collections.length > 0) {
            xmlProducer.startElement("collections");
            for (String collection : collections) {
                xmlProducer.simpleElement("collection", collection);
            }
            xmlProducer.endElement("collections");
        }

        // custom fields
        ImpExpCustomField[] customFields = impExpDocument.getCustomFields();
        if (customFields.length > 0) {
            xmlProducer.startElement("customFields");
            for (ImpExpCustomField customField : customFields) {
                attrs.clear();
                attrs.put("name", customField.getName());
                attrs.put("value", customField.getValue());
                xmlProducer.emptyElement("customField", attrs);
            }
            xmlProducer.endElement("customFields");
        }

        xmlProducer.endElement("document");
        xmlProducer.flush();
    }

    private void outputValueHierarchical(HierarchyPath path, ValueType valueType) throws SAXException {
        xmlProducer.startElement("hierarchyPath");
        for (Object element : path.getElements()) {
            xmlProducer.simpleElement("value", XmlizerUtil.formatValue(element, valueType, repository));
        }
        xmlProducer.endElement("hierarchyPath");
    }

    public static interface PartWriter {
        String writePart(ImpExpPart part) throws Exception;
    }
}
