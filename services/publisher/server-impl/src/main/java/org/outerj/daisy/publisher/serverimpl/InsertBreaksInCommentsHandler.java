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
package org.outerj.daisy.publisher.serverimpl;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

public class InsertBreaksInCommentsHandler implements ContentHandler {
    private static final String DAISY_NS = "http://outerx.org/daisy/1.0";
    private boolean inComment = false;
    private ContentHandler consumer;

    public InsertBreaksInCommentsHandler(ContentHandler consumer) {
        this.consumer = consumer;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (namespaceURI.equals(DAISY_NS) && localName.equals("comment")) {
            inComment = true;
        }
        consumer.startElement(namespaceURI, localName, qName, atts);
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (inComment) {
            int prevBreakPos = start - 1;
            int end = start + length;
            int i;
            for (i = start; i < end; i++) {
                if (ch[i] == '\n') {
                    if (prevBreakPos != i - 1)
                        consumer.characters(ch, prevBreakPos + 1, i - prevBreakPos - 1);
                    consumer.startElement("", "br", "br", new AttributesImpl());
                    consumer.endElement("", "br", "br");
                    prevBreakPos = i;
                }
            }
            if (prevBreakPos != i) {
                consumer.characters(ch, prevBreakPos + 1, i - prevBreakPos - 1);
            }
        } else {
            consumer.characters(ch, start, length);
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (namespaceURI.equals(DAISY_NS) && localName.equals("comment")) {
            inComment = false;
        }
        consumer.endElement(namespaceURI, localName, qName);
    }

    public void endDocument() throws SAXException {
        consumer.endDocument();
    }

    public void startDocument() throws SAXException {
        consumer.startDocument();
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        consumer.ignorableWhitespace(ch, start, length);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        consumer.endPrefixMapping(prefix);
    }

    public void skippedEntity(String name) throws SAXException {
        consumer.skippedEntity(name);
    }

    public void setDocumentLocator(Locator locator) {
        consumer.setDocumentLocator(locator);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        consumer.processingInstruction(target, data);
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        consumer.startPrefixMapping(prefix, uri);
    }
}
