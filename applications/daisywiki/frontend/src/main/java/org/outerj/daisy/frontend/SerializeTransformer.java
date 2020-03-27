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
package org.outerj.daisy.frontend;

import org.apache.cocoon.transformation.AbstractTransformer;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.ProcessingException;
import org.apache.avalon.framework.parameters.Parameters;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.OutputKeys;
import java.util.Map;
import java.io.IOException;
import java.io.CharArrayWriter;

/**
 * A transformer which serializes any content of a serialize element it encounters.
 */
public class SerializeTransformer extends AbstractTransformer {
    private int nestingLevel;
    private int serializeNestingLevel;
    private boolean inSerialize;
    private TransformerHandler serializer;
    private CharArrayWriter writer;
    private static final String NAMESPACE = "http://outerx.org/daisywiki/1.0#serializer";
    private static final String TRIGGER_EL = "serialize";

    SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

    public void setup(SourceResolver sourceResolver, Map map, String s, Parameters parameters) throws ProcessingException, SAXException, IOException {
        this.nestingLevel = 0;
    }

    public void recycle() {
        super.recycle();
        this.writer = null;
        this.serializer = null;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        nestingLevel++;
        if (namespaceURI.equals(NAMESPACE) && localName.equals(TRIGGER_EL)) {
            inSerialize = true;
            serializeNestingLevel = nestingLevel;

            // some output properties
            String method = atts.getValue("method");
            if (method == null) method = "html";
            String encoding = atts.getValue("encoding");
            if (encoding == null) encoding = "UTF-8";

            try {
                this.serializer = transformerFactory.newTransformerHandler();
                this.serializer.getTransformer().setOutputProperty(OutputKeys.METHOD, method);
                this.serializer.getTransformer().setOutputProperty(OutputKeys.ENCODING, encoding);
            } catch (TransformerConfigurationException e) {
                throw new SAXException(e);
            }
            this.writer = new CharArrayWriter(5000);
            serializer.setResult(new StreamResult(writer));
            this.serializer.startDocument();
        } else if (inSerialize) {
            serializer.startElement(namespaceURI, localName, qName, atts);
        } else {
            super.startElement(namespaceURI, localName, qName, atts);
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (!inSerialize) {
            super.endElement(namespaceURI, localName, qName);
        } else {
            if (nestingLevel == serializeNestingLevel && inSerialize && namespaceURI.equals(NAMESPACE) && localName.equals(TRIGGER_EL)) {
                inSerialize = false;
                this.serializer.endDocument();
                char[] text = writer.toCharArray();
                super.characters(text, 0, text.length);
                this.serializer = null;
                this.writer = null;
            } else {
                serializer.endElement(namespaceURI, localName, qName);
            }
        }
        nestingLevel--;
    }

    public void setDocumentLocator(Locator locator) {
        if (!inSerialize)
            super.setDocumentLocator(locator);
    }

    public void startDocument() throws SAXException {
        if (!inSerialize)
            super.startDocument();
    }

    public void endDocument() throws SAXException {
        if (!inSerialize)
        super.endDocument();
    }

    public void startPrefixMapping(String s, String s1) throws SAXException {
        if (!inSerialize)
            super.startPrefixMapping(s, s1);
        else
            serializer.startPrefixMapping(s, s1);
    }

    public void endPrefixMapping(String s) throws SAXException {
        if (!inSerialize)
            super.endPrefixMapping(s);
        else
            serializer.endPrefixMapping(s);
    }

    public void characters(char[] chars, int i, int i1) throws SAXException {
        if (!inSerialize)
            super.characters(chars, i, i1);
        else
            serializer.characters(chars, i, i1);
    }

    public void ignorableWhitespace(char[] chars, int i, int i1) throws SAXException {
        if (!inSerialize)
            super.ignorableWhitespace(chars, i, i1);
    }

    public void processingInstruction(String s, String s1) throws SAXException {
        if (!inSerialize)
            super.processingInstruction(s, s1);
        else
            serializer.processingInstruction(s, s1);
    }

    public void skippedEntity(String s) throws SAXException {
        if (!inSerialize)
            super.skippedEntity(s);
    }

    public void startDTD(String s, String s1, String s2) throws SAXException {
        if (!inSerialize)
            super.startDTD(s, s1, s2);
    }

    public void endDTD() throws SAXException {
        if (!inSerialize)
            super.endDTD();
    }

    public void startEntity(String s) throws SAXException {
        if (!inSerialize)
            super.startEntity(s);
        else
            serializer.startEntity(s);
    }

    public void endEntity(String s) throws SAXException {
        if (!inSerialize)
            super.endEntity(s);
        else
            serializer.endEntity(s);
    }

    public void startCDATA() throws SAXException {
        if (!inSerialize)
            super.startCDATA();
        else
            serializer.startCDATA();
    }

    public void endCDATA() throws SAXException {
        if (!inSerialize)
            super.endCDATA();
        else
            serializer.endCDATA();
    }

    public void comment(char[] chars, int i, int i1) throws SAXException {
        if (!inSerialize)
            super.comment(chars, i, i1);
        else
            serializer.comment(chars, i, i1);
    }


}
