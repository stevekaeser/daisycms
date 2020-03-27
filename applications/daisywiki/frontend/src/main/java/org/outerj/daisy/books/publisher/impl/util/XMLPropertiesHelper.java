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
package org.outerj.daisy.books.publisher.impl.util;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;

import javax.xml.parsers.SAXParser;
import java.util.Map;
import java.util.LinkedHashMap;
import java.io.InputStream;

/**
 * Reads properties from a Java 1.5 style XML properties file.
 */
public class XMLPropertiesHelper {

    public static Map<String, String> load(InputStream is) throws Exception {
        return load(is, (Map<String, String>)null);
    }

    public static Map<String, String> load(InputStream is, Map<String, String> defaults) throws Exception {
        return load(is, defaults, "properties");
    }

    public static Map<String, String> load(InputStream is, String rootElement) throws Exception {
        return load(is, null, rootElement);
    }

    public static Map<String, String> load(InputStream is, Map<String, String> defaults, String rootElement) throws Exception {
        return load(new InputSource(is), defaults, rootElement);
    }

    public static Map<String, String> load(InputSource is, Map<String, String> defaults, String rootElement) throws Exception {
        SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
        // A LinkedHashMap is used because we want to maintain the order of the properties
        //    (the properties are displayed to the user, having them displayed in random order would not be nice)
        Map<String, String> properties = new LinkedHashMap<String, String>();
        if (defaults != null)
            properties.putAll(defaults);
        PropertiesHandler propertiesHandler = new PropertiesHandler(properties, rootElement);
        parser.getXMLReader().setContentHandler(propertiesHandler);
        parser.getXMLReader().parse(is);
        return properties;
    }

    public static void generateSaxFragment(Map<String, String> map, ContentHandler contentHandler) throws SAXException {
        generateSaxFragment(map, "properties", contentHandler);
    }

    public static void generateSaxFragment(Map<String, String> map, String rootElement, ContentHandler contentHandler) throws SAXException {
        contentHandler.startElement("", rootElement, rootElement, new AttributesImpl());

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "key", "key", "CDATA", key);
            contentHandler.startElement("", "entry", "entry", attrs);
            contentHandler.characters(value.toCharArray(), 0, value.length());
            contentHandler.endElement("", "entry", "entry");
        }

        contentHandler.endElement("", rootElement, rootElement);
    }

    private static class PropertiesHandler extends DefaultHandler {
        private String rootElementName;
        private final Map<String, String> properties;
        private int nesting = 0;
        private String key = null;
        private StringBuilder entry = new StringBuilder();

        public PropertiesHandler(Map<String, String> properties, String rootElementName) {
            this.rootElementName = rootElementName;
            this.properties = properties;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            nesting++;
            if (nesting == 1 && !(uri.equals("") && localName.equals(rootElementName))) {
                throw new SAXException("Expected <" + rootElementName + "> root element.");
            } else if (nesting == 2 && uri.equals("") && localName.equals("entry")) {
                entry.setLength(0);
                key = attributes.getValue("key");
                if (key == null || key.trim().equals("")) {
                    throw new SAXException("Missing or empty key attribute on <entry> element.");
                }
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (nesting == 2 && key != null) {
                properties.put(key, entry.toString());
                key = null;
            }
            nesting--;
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (key != null)
                entry.append(ch, start, length);
        }
    }
}
