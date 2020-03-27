/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.tools.importexport.export.tm;

import org.outerj.daisy.tools.importexport.model.document.*;
import org.outerj.daisy.tools.importexport.util.XmlProducer;
import org.outerj.daisy.tools.importexport.tm.TMConfig;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.xmlutil.XmlMimeTypeHelper;

import java.io.OutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Produces XML format for translation management export.
 */
public class TMDocumentXmlizer {
    private ImpExpDocument impExpDocument;
    private Version version;
    private OutputStream outputStream;
    private Repository repository;
    private XmlProducer xmlProducer;
    private TMConfig tmConfig;
    private static final String NS = "http://outerx.org/daisy/1.0#tm-impexp";

    public static void toXml(ImpExpDocument impExpDocument, Version version, OutputStream outputStream,
            Repository repository, TMConfig tmConfig) throws Exception {
        new TMDocumentXmlizer(impExpDocument, version, outputStream, repository, tmConfig).run();
    }

    private TMDocumentXmlizer(ImpExpDocument impExpDocument, Version version, OutputStream outputStream,
            Repository repository, TMConfig tmConfig) {
        this.impExpDocument = impExpDocument;
        this.version = version;
        this.outputStream = outputStream;
        this.repository = repository;
        this.tmConfig = tmConfig;
    }

    public void run() throws Exception {
        xmlProducer = new XmlProducer(outputStream);

        xmlProducer.declarePrefix("ie", NS);

        Map<String, String> attrs = new LinkedHashMap<String, String>();
        attrs.put("exportedLanguage", impExpDocument.getLanguage());
        attrs.put("exportedVersion", String.valueOf(version.getId()));
        xmlProducer.startElement(NS, "document", attrs);

        // document name
        xmlProducer.simpleElement(NS, "title", impExpDocument.getName());

        // parts
        ImpExpPart[] parts = impExpDocument.getParts();
        if (parts.length > 0) {
            for (ImpExpPart part : parts) {
                if (XmlMimeTypeHelper.isXmlMimeType(part.getMimeType())
                        && !tmConfig.isLanguageIndependentPart(impExpDocument.getType(), part.getType().getName())) {
                    attrs.clear();
                    attrs.put("name", part.getType().getName());
                    attrs.put("mimeType", part.getMimeType());

                    xmlProducer.startElement(NS, "part", attrs);
                    InputStream is = null;
                    try {
                        is = part.getDataAccess().getInputStream();
                        xmlProducer.embedXml(is);
                    } finally {
                        if (is != null)
                            is.close();
                    }
                    xmlProducer.endElement(NS, "part");
                }
            }
        }

        // links
        ImpExpLink[] links = impExpDocument.getLinks();
        if (links.length > 0) {
            for (ImpExpLink link : links) {
                xmlProducer.startElement(NS, "link");
                xmlProducer.simpleElement(NS, "title", link.getTitle());
                xmlProducer.simpleElement(NS, "target", link.getTarget());
                xmlProducer.endElement(NS, "link");
            }
        }

        // fields
        ImpExpField[] fields = impExpDocument.getFields();
        if (fields.length > 0) {
            for (ImpExpField field : fields) {
                FieldType fieldType = field.getType();
                // for translation purposes, only string fields make sense
                if (fieldType.getValueType() == ValueType.STRING
                        && !fieldType.isHierarchical()
                        && !fieldType.hasSelectionList()
                        && !tmConfig.isLanguageIndependentField(impExpDocument.getType(), field.getType().getName())) {
                    attrs.clear();
                    attrs.put("name", field.getType().getName());
                    Object value = field.getValue();
                    if (field.getType().isMultiValue()) {
                        xmlProducer.startElement(NS, "field", attrs);
                        if (field.getType().isMultiValue()) {
                            Object[] values = (Object[]) value;
                            for (Object subValue : values) {
                                xmlProducer.simpleElement(NS, "value", (String)subValue);
                            }
                        }
                        xmlProducer.endElement(NS, "field");
                    } else {
                        String stringValue = (String)value;
                        xmlProducer.simpleElement(NS, "field", stringValue, attrs);
                    }
                }
            }
        }

        xmlProducer.closeElement(NS, "document");
        xmlProducer.flush();
    }
}
