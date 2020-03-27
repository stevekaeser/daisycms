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
package org.outerj.daisy.repository.testsupport;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

/**
 * Helper class to build a DOM from SAX events.
 */
public class DOMBuilder implements ContentHandler {
    private TransformerHandler handler;
    private DOMResult result;

    public DOMBuilder() throws Exception {
        SAXTransformerFactory factory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
        handler = factory.newTransformerHandler();
        result = new DOMResult();
        handler.setResult(result);
    }

    public Document getDocument() {
        return (Document)result.getNode();
    }

    public Node getNode() {
        return result.getNode();
    }

    public void endDocument() throws SAXException {
        handler.endDocument();
    }

    public void startDocument () throws SAXException {
        handler.startDocument();
    }

    public void characters (char ch[], int start, int length) throws SAXException {
        handler.characters(ch, start, length);
    }

    public void ignorableWhitespace (char ch[], int start, int length) throws SAXException {
        handler.ignorableWhitespace(ch, start, length);
    }

    public void endPrefixMapping (String prefix) throws SAXException {
        handler.endPrefixMapping(prefix);
    }

    public void skippedEntity (String name) throws SAXException {
        handler.skippedEntity(name);
    }

    public void setDocumentLocator (Locator locator) {
        handler.setDocumentLocator(locator);
    }

    public void processingInstruction (String target, String data) throws SAXException {
        handler.processingInstruction(target, data);
    }

    public void startPrefixMapping (String prefix, String uri) throws SAXException {
        handler.startPrefixMapping(prefix, uri);
    }

    public void endElement (String namespaceURI, String localName, String qName) throws SAXException {
        handler.endElement(namespaceURI, localName, qName);
    }

    public void startElement (String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        handler.startElement(namespaceURI, localName, qName, atts);
    }

}
