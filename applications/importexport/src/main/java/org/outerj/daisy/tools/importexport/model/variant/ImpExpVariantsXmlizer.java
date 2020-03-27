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
package org.outerj.daisy.tools.importexport.model.variant;

import org.outerj.daisy.tools.importexport.util.XmlProducer;
import org.xml.sax.SAXException;

import java.io.OutputStream;
import java.util.Map;
import java.util.LinkedHashMap;

public class ImpExpVariantsXmlizer {
    private ImpExpVariants impExpVariants;
    private OutputStream outputStream;
    private XmlProducer xmlProducer;

    public static void toXml(ImpExpVariants variants, OutputStream outputStream) throws Exception {
        new ImpExpVariantsXmlizer(variants, outputStream).run();
    }

    private ImpExpVariantsXmlizer(ImpExpVariants variants, OutputStream outputStream) {
        this.impExpVariants = variants;
        this.outputStream = outputStream;
    }

    private void run() throws Exception {
        xmlProducer = new XmlProducer(outputStream);
        xmlProducer.startElement("variants");

        writeVariants(impExpVariants.getBranches(), "branch", "branches");
        xmlProducer.newLine();
        writeVariants(impExpVariants.getLanguages(), "language", "languages");

        xmlProducer.endElement("variants");
        xmlProducer.flush();
    }

    private void writeVariants(ImpExpVariant[] variants, String tagName, String parentTagName) throws SAXException {
        xmlProducer.startElement(parentTagName);

        for (ImpExpVariant variant : variants) {
            Map<String, String> attrs = new LinkedHashMap<String, String>();
            attrs.put("name", variant.getName());
            if (variant.getDescription() != null) {
                attrs.put("description", variant.getDescription());
            }
            attrs.put("required", String.valueOf(variant.isRequired()));
            xmlProducer.emptyElement(tagName, attrs);
        }

        xmlProducer.endElement(parentTagName);
    }

}
