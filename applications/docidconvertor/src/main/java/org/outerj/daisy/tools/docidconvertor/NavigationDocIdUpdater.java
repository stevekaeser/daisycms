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
package org.outerj.daisy.tools.docidconvertor;

import org.outerj.daisy.repository.Part;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.parsers.SAXParser;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

public class NavigationDocIdUpdater implements PartDocIdUpdater {
    private static final String NAV_NS = "http://outerx.org/daisy/1.0#navigationspec";
    private DocIdConvertor docIdConvertor;
    private boolean anyUpdates = false;

    public byte[] update(Part part, DocIdConvertor docIdConvertor) throws Exception {
        this.docIdConvertor = docIdConvertor;

        byte[] startData = part.getData();

        ByteArrayOutputStream bos = new ByteArrayOutputStream(startData.length);
        XmlSerializer serializer = new XmlSerializer(bos);
        NavigationDocIdHandler handler = new NavigationDocIdHandler(serializer);
        SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
        parser.getXMLReader().setContentHandler(handler);
        parser.getXMLReader().parse(new InputSource(new ByteArrayInputStream(startData)));

        if (anyUpdates) {
            return bos.toByteArray();
        }

        return null;
    }

    private class NavigationDocIdHandler extends AbstractContentHandler {
        public NavigationDocIdHandler(ContentHandler consumer) {
            super(consumer);
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (namespaceURI.equals(NAV_NS) && (localName.equals("doc") || localName.equals("import"))) {
                String idAttrName = localName.equals("doc") ? "id" : "docId";
                String id = atts.getValue(idAttrName);
                if (id != null) {
                    String updatedId = docIdConvertor.updateId(id);
                    if (updatedId != null) {
                        AttributesImpl newAttrs = new AttributesImpl(atts);
                        newAttrs.setAttribute(newAttrs.getIndex(idAttrName), "", idAttrName, idAttrName, "CDATA", updatedId);
                        atts = newAttrs;
                        anyUpdates = true;
                    }
                }
            }
            super.startElement(namespaceURI, localName, qName, atts);
        }
    }
}
