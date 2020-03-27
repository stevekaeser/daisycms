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
package org.outerj.daisy.xmlutil;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.NamespaceSupport;

import java.util.Enumeration;

/**
 * ContentHandler that only passes on the content of the HTML body element.
 */
public class HtmlBodyRemovalHandler implements ContentHandler {
    private int nestingLevel = 0;
    private boolean inHtml = false;
    private boolean inBody = false;
    private ContentHandler consumer;
    private NamespaceSupport namespaceSupport = new NamespaceSupport();

    public HtmlBodyRemovalHandler(ContentHandler consumer) {
        this.consumer = consumer;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        nestingLevel++;

        namespaceSupport.pushContext();

        if (inBody) {
            // Given that there is no grouping element around the content anymore, we need to redeclare
            // the namespace prefixes (if any) for each child element of body
            if (nestingLevel == 3)
                declarePrefixes();

            consumer.startElement(namespaceURI, localName, qName, atts);
        } else if (nestingLevel == 1 && localName.equals("html") && namespaceURI.equals("")) {
            inHtml = true;
        } else if (inHtml && nestingLevel == 2 && localName.equals("body") && namespaceURI.equals("")) {
            inBody = true;
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        namespaceSupport.popContext();
        if (inBody && nestingLevel == 3)
            undeclarePrefixes();

        if (inBody && nestingLevel == 2 && localName.equals("body") && namespaceURI.equals("")) {
            inBody = false;
        } else if (inBody) {
            consumer.endElement(namespaceURI, localName, qName);
        }
        nestingLevel--;
    }

    private void declarePrefixes() throws SAXException {
        Enumeration prefixEnum = namespaceSupport.getPrefixes();
        while (prefixEnum.hasMoreElements()) {
            String prefix = (String)prefixEnum.nextElement();
            if (!prefix.equals("xml"))
                consumer.startPrefixMapping(prefix, namespaceSupport.getURI(prefix));
        }
    }

    private void undeclarePrefixes() throws SAXException {
        Enumeration prefixEnum = namespaceSupport.getPrefixes();
        while (prefixEnum.hasMoreElements()) {
            String prefix = (String)prefixEnum.nextElement();
            if (!prefix.equals("xml"))
                consumer.endPrefixMapping(prefix);
        }
    }

    public void endDocument() throws SAXException {
        // ignore
    }

    public void startDocument() throws SAXException {
        // ignore
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (inBody)
            consumer.characters(ch, start, length);
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        if (inBody)
            consumer.ignorableWhitespace(ch, start, length);
    }

    public void skippedEntity(String name) throws SAXException {
        if (inBody)
            consumer.skippedEntity(name);
    }

    public void setDocumentLocator(Locator locator) {
        if (inBody)
            consumer.setDocumentLocator(locator);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        if (inBody)
            consumer.processingInstruction(target, data);
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (!inBody)
            namespaceSupport.declarePrefix(prefix, uri);
        else
            consumer.startPrefixMapping(prefix, uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (!inBody)
            consumer.endPrefixMapping(prefix);
    }
}
