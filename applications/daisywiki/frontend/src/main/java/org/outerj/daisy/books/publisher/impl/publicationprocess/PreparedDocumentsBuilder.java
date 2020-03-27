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
package org.outerj.daisy.books.publisher.impl.publicationprocess;


import org.xml.sax.*;
import org.xml.sax.helpers.NamespaceSupport;
import org.apache.cocoon.xml.SaxBuffer;
import org.outerj.daisy.frontend.PreparedDocuments;
import org.outerj.daisy.repository.VariantKey;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.io.InputStream;
import java.util.Enumeration;

public class PreparedDocumentsBuilder {
    private static final String PUBLISHER_NAMESPACE = "http://outerx.org/daisy/1.0#publisher";

    public static PreparedDocuments build(InputStream is) throws Exception {
        SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(true);
        saxParserFactory.setValidating(false);
        SAXParser saxParser = saxParserFactory.newSAXParser();

        PreparedDocumentsHandler preparedDocumentsHandler = new PreparedDocumentsHandler();
        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(preparedDocumentsHandler);

        InputSource inputSource = new InputSource(is);
        xmlReader.parse(inputSource);

        PreparedDocuments preparedDocuments = preparedDocumentsHandler.getPreparedDocuments();
        if (preparedDocuments.getPreparedDocument(1) == null) {
            throw new Exception("Invalid preparedDocuments, there is no prepared document with ID 1.");
        }
        return preparedDocuments;
    }

    private static class PreparedDocumentsHandler implements ContentHandler {
        private int elementNesting = 0;
        private int currentPreparedDocumentId;
        private VariantKey documentKey;
        private SaxBuffer buffer;
        private NamespaceSupport namespaceSupport = new NamespaceSupport();
        private PreparedDocuments preparedDocuments = new PreparedDocuments();

        public PreparedDocuments getPreparedDocuments() {
            return preparedDocuments;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (elementNesting < 2)
                namespaceSupport.pushContext();

            if (elementNesting == 0) {
                if (!(namespaceURI.equals(PUBLISHER_NAMESPACE) && localName.equals("preparedDocuments"))) {
                    throw new SAXException("Expect as root element \"p:preparedDocuments\" but got \"" + qName + "\".");
                }
            } else if (elementNesting == 1) {
                if (namespaceURI.equals(PUBLISHER_NAMESPACE) && localName.equals("preparedDocument")) {
                    currentPreparedDocumentId = Integer.parseInt(atts.getValue("id"));
                    String documentId = atts.getValue("documentId");
                    long branchId = Long.parseLong(atts.getValue("branchId"));
                    long languageId = Long.parseLong(atts.getValue("languageId"));
                    documentKey = new VariantKey(documentId, branchId, languageId);
                    buffer = new SaxBuffer();
                    buffer.startDocument();
                    // Make sure current namespace declarations are passed on
                    Enumeration prefixEnum = namespaceSupport.getPrefixes();
                    while (prefixEnum.hasMoreElements()) {
                        String prefix = (String)prefixEnum.nextElement();
                        if (!prefix.equals("xml"))
                            buffer.startPrefixMapping(prefix, namespaceSupport.getURI(prefix));
                    }
                } else {
                    throw new SAXException("Encountered an unexpected element in preparedDocuments: \"" + qName + "\".");
                }
            } else if (elementNesting >= 2) {
                buffer.startElement(namespaceURI, localName, qName, atts);
            }
            elementNesting++;
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            elementNesting--;
            if (elementNesting == 1) {
                buffer.endDocument();
                preparedDocuments.putPreparedDocument(currentPreparedDocumentId, documentKey, buffer);
                buffer = null;
                documentKey = null;
            } else if (elementNesting >= 2) {
                buffer.endElement(namespaceURI, localName, qName);
            }
        }

        public void endDocument() throws SAXException {
            // do nothing
        }

        public void startDocument() throws SAXException {
            // do nothing
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (elementNesting >= 2)
                buffer.characters(ch, start, length);
        }

        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
            if (elementNesting >= 2)
                buffer.ignorableWhitespace(ch, start, length);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            if (elementNesting >= 2)
                buffer.endPrefixMapping(prefix);
        }

        public void skippedEntity(String name) throws SAXException {
            if (elementNesting >= 2)
                buffer.skippedEntity(name);
        }

        public void setDocumentLocator(Locator locator) {
            if (elementNesting >= 2)
                buffer.setDocumentLocator(locator);
        }

        public void processingInstruction(String target, String data) throws SAXException {
            if (elementNesting >= 2)
                buffer.processingInstruction(target, data);
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (elementNesting >= 2)
                buffer.startPrefixMapping(prefix, uri);
            else
                namespaceSupport.declarePrefix(prefix, uri);
        }
    }
}
