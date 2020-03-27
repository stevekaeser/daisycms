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
package org.outerj.daisy.books.publisher.impl.dataretrieval;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;

/**
 * A handler whose purpose is to only let through the /p:publisherResponse/p:document/p:preparedDocuments element.
 * Currently rather rough implemented, but should work just fine as long as the input format is OK.
 */
public class PreparedDocumentsExtractor implements ContentHandler {
    private int elementNesting = 0;
    private ContentHandler consumer;

    public PreparedDocumentsExtractor(ContentHandler consumer) {
        this.consumer = consumer;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        elementNesting++;

        if (elementNesting > 2)
            consumer.startElement(namespaceURI, localName, qName, atts);
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (elementNesting > 2)
            consumer.endElement(namespaceURI, localName, qName);
        elementNesting--;
    }

    public void endDocument() throws SAXException {
        consumer.endDocument();
    }

    public void startDocument() throws SAXException {
        consumer.startDocument();
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (elementNesting > 3)
            consumer.characters(ch, start, length);
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        if (elementNesting > 3)
            consumer.characters(ch, start, length);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        consumer.endPrefixMapping(prefix);
    }

    public void skippedEntity(String name) throws SAXException {
        if (elementNesting > 3)
            consumer.skippedEntity(name);
    }

    public void setDocumentLocator(Locator locator) {
        consumer.setDocumentLocator(locator);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        if (elementNesting > 3)
            consumer.processingInstruction(target, data);
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        consumer.startPrefixMapping(prefix, uri);
    }
}
