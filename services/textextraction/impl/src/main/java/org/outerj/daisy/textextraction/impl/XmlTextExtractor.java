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
package org.outerj.daisy.textextraction.impl;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.*;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.textextraction.TextExtractor;
import org.outerj.daisy.plugin.PluginRegistry;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.io.InputStream;
import java.util.Set;
import java.util.HashSet;
import java.util.List;

/**
 * Extracts all text between tags in an XML document. Only works (of course)
 * for well formed XML documents.
 *
 */
public class XmlTextExtractor extends AbstractTextExtractor implements TextExtractor {

    public XmlTextExtractor() {
        super();
    }

    public XmlTextExtractor(List<String> mimeTypes, PluginRegistry pluginRegistry) {
        super(mimeTypes, pluginRegistry);
    }

    protected String getName() {
        return getClass().getName();
    }

    public String getText(InputStream is) throws Exception {
        SAXParserFactory factory = LocalSAXParserFactory.getSAXParserFactory();
        SAXParser parser = factory.newSAXParser();

        // Try to disable loading of external things as much as possible
        // (e.g. external DTD, can be slow or impossible to load)
        XMLReader xmlReader = parser.getXMLReader();
        safeSetFeature(xmlReader, "http://xml.org/sax/features/validation", false);
        safeSetFeature(xmlReader, "http://xml.org/sax/features/external-general-entities", false);
        safeSetFeature(xmlReader, "http://xml.org/sax/features/external-parameter-entities", false);
        safeSetFeature(xmlReader, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        MyHandler handler = new MyHandler();
        parser.parse(is, handler);
        return handler.getText();
    }

    private void safeSetFeature(XMLReader reader, String feature, boolean value) {
        try {
            reader.setFeature(feature, value);
        } catch (SAXNotRecognizedException e) {
            // ignore
        } catch (SAXNotSupportedException e) {
            // ignore
        }
    }

    private static class MyHandler extends DefaultHandler {
        private StringBuilder text = new StringBuilder();
        private int nestingLevel = -1;
        private int ignoreContentNestingLevel = -1;

        private static Set<String> SPECIAL_PRE_CLASSES;
        static {
            SPECIAL_PRE_CLASSES = new HashSet<String>();
            SPECIAL_PRE_CLASSES.add("query");
            SPECIAL_PRE_CLASSES.add("include");
            SPECIAL_PRE_CLASSES.add("query-and-include");
        }

        private static Set<String> SPECIAL_SPAN_CLASSES;
        static {
            SPECIAL_SPAN_CLASSES = new HashSet<String>();
            SPECIAL_SPAN_CLASSES.add("indexentry");
            SPECIAL_SPAN_CLASSES.add("crossreference");
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            // Special handling to ignore the content of includes instructions etc.
            nestingLevel++;
            if (ignoreContentNestingLevel != -1) {
                // we're already ignoring things
            } else if (uri.equals("") && localName.equals("pre") && SPECIAL_PRE_CLASSES.contains(attributes.getValue("class"))) {
                ignoreContentNestingLevel = nestingLevel;
            } else if (uri.equals("") && localName.equals("span") && SPECIAL_SPAN_CLASSES.contains(attributes.getValue("class"))) {
                ignoreContentNestingLevel = nestingLevel;
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (ignoreContentNestingLevel != -1 && ignoreContentNestingLevel == nestingLevel) {
                ignoreContentNestingLevel = -1;
            }
            nestingLevel--;
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (ignoreContentNestingLevel == -1)
                text.append(ch, start, length);
        }

        public String getText() {
            return text.toString();
        }
    }
}
