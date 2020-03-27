/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.tools.artifacter;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.ext.LexicalHandler;

import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;

/**
 * Keeps attributes on artifact elements in a "logical" order. This is because
 * some XML parsers (Xerces' DOM notably) sort attributes alphabetically.
 *
 * <p>This thing of course relies on a serializer that doesn't reorder
 * the attributes.
 */
class AttributeReorderHandler extends DefaultHandler implements LexicalHandler {
    private TransformerHandler handler;

    public AttributeReorderHandler(TransformerHandler handler) {
        this.handler = handler;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (uri.equals("") && localName.equals("artifact")) {
            AttributesImpl newAttrs = new AttributesImpl();

            String id = attributes.getValue("", "id");
            String groupId = attributes.getValue("", "groupId");
            String artifactId = attributes.getValue("", "artifactId");
            String version = attributes.getValue("", "version");

            if (id != null)
                newAttrs.addAttribute("", "id", "id", "CDATA", id);
            if (groupId != null)
                newAttrs.addAttribute("", "groupId", "groupId", "CDATA", groupId);
            if (artifactId != null)
                newAttrs.addAttribute("", "artifactId", "artifactId", "CDATA", artifactId);
            if (version != null)
                newAttrs.addAttribute("", "version", "version", "CDATA", version);

            // copy over remaining attributes
            for (int i = 0; i < attributes.getLength(); i++) {
                // if not already in target
                if (newAttrs.getIndex(attributes.getURI(i), attributes.getLocalName(i)) == -1) {
                    newAttrs.addAttribute(attributes.getURI(i), attributes.getLocalName(i),
                            attributes.getQName(i), attributes.getType(i), attributes.getValue(i));
                }
            }

            attributes = newAttrs;
        }
        handler.startElement(uri, localName, qName, attributes);
    }

    public void setResult(Result result) throws IllegalArgumentException {
        handler.setResult(result);
    }

    public void setSystemId(String systemID) {
        handler.setSystemId(systemID);
    }

    public String getSystemId() {
        return handler.getSystemId();
    }

    public Transformer getTransformer() {
        return handler.getTransformer();
    }

    public void setDocumentLocator(Locator locator) {
        handler.setDocumentLocator(locator);
    }

    public void startDocument() throws SAXException {
        handler.startDocument();
    }

    public void endDocument() throws SAXException {
        handler.endDocument();
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        handler.startPrefixMapping(prefix, uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        handler.endPrefixMapping(prefix);
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        handler.endElement(uri, localName, qName);
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        handler.characters(ch, start, length);
    }

    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        handler.ignorableWhitespace(ch, start, length);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        handler.processingInstruction(target, data);
    }

    public void skippedEntity(String name) throws SAXException {
        handler.skippedEntity(name);
    }

    public void startDTD(String name, String publicId, String systemId) throws SAXException {
        handler.startDTD(name, publicId, systemId);
    }

    public void endDTD() throws SAXException {
        handler.endDTD();
    }

    public void startEntity(String name) throws SAXException {
        handler.startEntity(name);
    }

    public void endEntity(String name) throws SAXException {
        handler.endEntity(name);
    }

    public void startCDATA() throws SAXException {
        handler.startCDATA();
    }

    public void endCDATA() throws SAXException {
        handler.endCDATA();
    }

    public void comment(char[] ch, int start, int length) throws SAXException {
        handler.comment(ch, start, length);
    }

    public void notationDecl(String name, String publicId, String systemId) throws SAXException {
        handler.notationDecl(name, publicId, systemId);
    }

    public void unparsedEntityDecl(String name, String publicId, String systemId, String notationName) throws SAXException {
        handler.unparsedEntityDecl(name, publicId, systemId, notationName);
    }
}
