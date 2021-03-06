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
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.util.Constants;

/**
 * A contentHandler that records the passing &lt;d:document&gt; elements as book
 * dependencies.
 */
public class DependencyCollector implements ContentHandler {
    private BookDependencies bookDependencies;
    private ContentHandler consumer;

    public DependencyCollector(BookDependencies bookDependencies, ContentHandler consumer) {
        this.bookDependencies = bookDependencies;
        this.consumer = consumer;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (namespaceURI.equals(Constants.DAISY_NAMESPACE) && localName.equals("document")) {
            String documentId = atts.getValue("id");
            long branchId = Long.parseLong(atts.getValue("branchId"));
            long languageId = Long.parseLong(atts.getValue("languageId"));
            long versionId = Long.parseLong(atts.getValue("dataVersionId"));
            VersionKey key = new VersionKey(documentId, branchId, languageId, versionId);
            bookDependencies.addDependency(key);
        }
        consumer.startElement(namespaceURI, localName, qName, atts);
    }

    public void endDocument() throws SAXException {
        consumer.endDocument();
    }

    public void startDocument() throws SAXException {
        consumer.startDocument();
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        consumer.characters(ch, start, length);
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

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        consumer.endElement(namespaceURI, localName, qName);
    }
}
