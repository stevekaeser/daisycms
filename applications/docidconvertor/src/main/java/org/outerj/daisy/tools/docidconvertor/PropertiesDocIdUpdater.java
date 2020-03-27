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
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

public class PropertiesDocIdUpdater implements PartDocIdUpdater {
    private DocIdConvertor docIdConvertor;
    private boolean anyUpdates = false;

    public byte[] update(Part part, DocIdConvertor docIdConvertor) throws Exception {
        this.docIdConvertor = docIdConvertor;

        byte[] startData = part.getData();

        ByteArrayOutputStream bos = new ByteArrayOutputStream(startData.length);
        XmlSerializer serializer = new XmlSerializer(bos);
        PropertiesDocIdHandler handler = new PropertiesDocIdHandler(serializer);
        SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
        parser.getXMLReader().setContentHandler(handler);
        parser.getXMLReader().parse(new InputSource(new ByteArrayInputStream(startData)));

        if (anyUpdates) {
            return bos.toByteArray();
        }

        return null;
    }

    private class PropertiesDocIdHandler extends AbstractContentHandler {
        private StringBuilder entryBuffer;

        public PropertiesDocIdHandler(ContentHandler consumer) {
            super(consumer);
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (localName.equals("entry")) {
                if (entryBuffer != null)
                    throw new SAXException("Nested <entry> elements in properties, shouldn't be the case and can't handle this.");
                entryBuffer = new StringBuilder();
            }
            super.startElement(namespaceURI, localName, qName, atts);
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (entryBuffer != null) {
                entryBuffer.append(ch, start, length);
            } else {
                super.characters(ch, start, length);
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (localName.equals("entry")) {
                String entry = entryBuffer.toString();
                String updatedLink = docIdConvertor.updateLink(entry);
                entry = updatedLink != null ? updatedLink : entry;
                super.characters(entry.toCharArray(), 0, entry.length());
                entryBuffer = null;
                if (updatedLink != null)
                    anyUpdates = true;
            }
            super.endElement(namespaceURI, localName, qName);
        }
    }
}


