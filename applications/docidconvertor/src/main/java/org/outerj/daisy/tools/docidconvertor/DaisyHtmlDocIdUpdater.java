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

/**
 * Update links in a/@href, img/@src, pre[@class='include]
 */
public class DaisyHtmlDocIdUpdater implements PartDocIdUpdater {
    private DocIdConvertor docIdConvertor;
    private boolean anyUpdates = false;

    public byte[] update(Part part, DocIdConvertor docIdConvertor) throws Exception {
        this.docIdConvertor = docIdConvertor;

        byte[] startData = part.getData();

        ByteArrayOutputStream bos = new ByteArrayOutputStream(startData.length);
        XmlSerializer serializer = new XmlSerializer(bos);
        DaisyHtmlDocIdHandler handler = new DaisyHtmlDocIdHandler(serializer);
        SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
        parser.getXMLReader().setContentHandler(handler);
        parser.getXMLReader().parse(new InputSource(new ByteArrayInputStream(startData)));

        if (anyUpdates) {
            byte[] newData = docIdConvertor.getHtmlCleaner().cleanToByteArray(bos.toString("UTF-8"));
            return newData;
        }

        return null;
    }

    private class DaisyHtmlDocIdHandler extends AbstractContentHandler {
        private StringBuilder includeBuffer;

        public DaisyHtmlDocIdHandler(ContentHandler consumer) {
            super(consumer);
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            boolean skipElement = false;
            if (namespaceURI.equals("") && (localName.equals("a") || localName.equals("img"))) {
                String attName = localName.equals("a") ? "href" : "src";
                String href = atts.getValue(attName);
                if (href != null) {
                    String updatedHref = docIdConvertor.updateLink(href);
                    if (updatedHref != null) {
                        AttributesImpl newAttrs = new AttributesImpl(atts);
                        newAttrs.setAttribute(newAttrs.getIndex(attName), "", attName, attName, "CDATA", updatedHref);
                        atts = newAttrs;
                        anyUpdates = true;
                    }
                }
            } else if (namespaceURI.equals("") && localName.equals("pre") && "include".equals(atts.getValue("class"))) {
                if (includeBuffer != null) {
                    throw new SAXException("Found nested pre[@class='include'] tags.");
                }
                includeBuffer = new StringBuilder();
            } else if (includeBuffer != null && localName.equals("pre")) {
                throw new SAXException("Can't handle nested pre elements in an include.");
            } else if (includeBuffer != null) {
                // removed nested markup in includes
                skipElement = true;
            }

            if (!skipElement)
                super.startElement(namespaceURI, localName, qName, atts);
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (includeBuffer != null) {
                includeBuffer.append(ch, start, length);
            } else {
                super.characters(ch, start, length);
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (includeBuffer != null && namespaceURI.equals("") && localName.equals("pre")) {
                String link = includeBuffer.toString().trim();
                String comment = null;
                int commentPos = link.indexOf(" ");
                if (commentPos != -1) {
                    comment = link.substring(commentPos);
                    link = link.substring(0, commentPos);
                }

                String updatedLink = docIdConvertor.updateLink(link);
                String text = includeBuffer.toString();
                if (updatedLink != null) {
                    text = updatedLink + (comment != null ? comment : "");
                    anyUpdates = true;
                }
                super.characters(text.toCharArray(), 0, text.length());
                includeBuffer = null;
            }

            // possible nested markup in an include is stripped
            if (includeBuffer == null)
                super.endElement(namespaceURI, localName, qName);
        }
    }
}
