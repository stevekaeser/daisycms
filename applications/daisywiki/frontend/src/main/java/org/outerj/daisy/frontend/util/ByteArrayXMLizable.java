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
package org.outerj.daisy.frontend.util;

import org.apache.excalibur.xml.sax.XMLizable;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ByteArrayXMLizable implements XMLizable {
    private byte[] data;

    public ByteArrayXMLizable(byte[] data) {
        this.data = data;
    }

    public void toSAX(ContentHandler contentHandler) throws SAXException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        safeSetFeature(parserFactory, "http://xml.org/sax/features/validation", false);
        safeSetFeature(parserFactory, "http://xml.org/sax/features/external-general-entities", false);
        safeSetFeature(parserFactory, "http://xml.org/sax/features/external-parameter-entities", false);
        safeSetFeature(parserFactory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);        
        parserFactory.setNamespaceAware(true);
        
        SAXParser parser;
        try {
            parser = parserFactory.newSAXParser();
        } catch (ParserConfigurationException e) {
            throw new SAXException("Error creating SAX parser.", e);
        }
        parser.getXMLReader().setContentHandler(contentHandler);
        InputSource is = new InputSource(new ByteArrayInputStream(data));
        try {
            parser.getXMLReader().parse(is);
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }
    
    private void safeSetFeature(SAXParserFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (SAXNotRecognizedException e) {
            // ignore
        } catch (SAXNotSupportedException e) {
            // ignore
        } catch (ParserConfigurationException e) {
         // ignore
        }
    }
}
