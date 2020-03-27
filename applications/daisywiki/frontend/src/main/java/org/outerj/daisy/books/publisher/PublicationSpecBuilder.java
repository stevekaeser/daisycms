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
package org.outerj.daisy.books.publisher;

import org.outerx.daisy.x10Bookpubspecs.PublicationSpecsDocument;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

/**
 * Builds {@link PublicationSpec}s from XML.
 */
public class PublicationSpecBuilder {
    public static PublicationSpec[] build(InputStream is) throws Exception {
        try {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            PublicationSpecsDocument pubSpecsDoc = PublicationSpecsDocument.Factory.parse(is, xmlOptions);
            return build(pubSpecsDoc.getPublicationSpecs());
        } finally {
            if (is != null)
                is.close();
        }
    }

    public static PublicationSpec[] build(Reader reader) throws Exception {
        try {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            PublicationSpecsDocument pubSpecsDoc = PublicationSpecsDocument.Factory.parse(reader, xmlOptions);
            return build(pubSpecsDoc.getPublicationSpecs());
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    public static PublicationSpec[] build(PublicationSpecsDocument.PublicationSpecs publicationSpecsXml) {
        PublicationSpec[] publicationSpecs = new PublicationSpec[publicationSpecsXml.sizeOfPublicationSpecArray()];
        int i = 0;
        for (PublicationSpecsDocument.PublicationSpecs.PublicationSpec publicationSpecXml : publicationSpecsXml.getPublicationSpecList()) {
            String type = publicationSpecXml.getType();
            String name = publicationSpecXml.getName();
            String label = publicationSpecXml.getLabel();
            if (type == null || name == null || type.trim().length() == 0 || name.trim().length() == 0)
                continue;
            Map<String, String> properties;
            if (publicationSpecXml.isSetProperties()) {
                properties = buildProperties(publicationSpecXml.getProperties());
            } else {
                properties = Collections.emptyMap();
            }
            publicationSpecs[i++] = new PublicationSpec(type, name, label, properties);
        }
        return publicationSpecs;
    }

    private static Map<String, String> buildProperties(PublicationSpecsDocument.PublicationSpecs.PublicationSpec.Properties propertiesXml) {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        for (PublicationSpecsDocument.PublicationSpecs.PublicationSpec.Properties.Entry entryXml : propertiesXml.getEntryList()) {
            String key = entryXml.getKey();
            if (key == null || key.trim().length() == 0)
                continue;
            String value = entryXml.getStringValue();
            if (value == null)
                value = "";
            properties.put(key, value);
        }
        return properties;
    }
}
