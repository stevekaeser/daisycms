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
package org.outerj.daisy.i18n.impl;

import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.outerj.daisy.i18n.DResourceBundle;
import org.outerj.daisy.i18n.I18nMessage;
import org.xml.sax.*;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class DResourceBundleFactory {
    /**
     * Builds a resource bundle from an XML file, in "Cocoon i18n transformer" format.
     *
     * <p>This means something like this:
     * <pre>
     *   &lt;catalogue&gt;
     *     &lt;message key='something'&gt;something&lt;/message&gt;
     *   &lt;/catalogue&gt;
     *
     * <p>Messages can contain mixed content (there's no special treatment to make sure
     * namespace prefix declaration are included though, so better not use namespaces).
     *
     * </pre>
     * @param is <b>the caller is responsible for closing the input stream!</b>
     */
    public static DResourceBundle build(InputSource is) throws SAXException, ParserConfigurationException, IOException {
        SAXParserFactory parserFactory = LocalSAXParserFactory.getSAXParserFactory();
        SAXParser parser = parserFactory.newSAXParser();
        XmlResourceBundleHandler handler = new XmlResourceBundleHandler();
        parser.getXMLReader().setContentHandler(handler);
        parser.getXMLReader().parse(is);
        return new DResourceBundleImpl(handler.getEntries());
    }

    private static class XmlResourceBundleHandler implements ContentHandler {
        private SaxBuffer buffer;
        private int elementNesting = 0;
        private String key;
        private Map<String, I18nMessage> entries = new HashMap<String, I18nMessage>();

        public Map<String, I18nMessage> getEntries() {
            return entries;
        }

        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (buffer != null) {
                buffer.startElement(uri, localName, qName, atts);
            } else if (elementNesting  == 0) {
                if (!uri.equals("") || !localName.equals("catalogue")) {
                    throw new SAXException("Root element of resource bundle should be called 'catalogue'.");
                }
            } else if (elementNesting == 1 && uri.equals("") && localName.equals("message")) {
                key = atts.getValue("key");
                if (key.trim().length() == 0)
                    key = null;
                if (key != null) // <message> tags with missing key attribute are silently skipped
                    buffer = new SaxBuffer();
            }

            elementNesting++;
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (elementNesting == 2 && buffer != null) {
                entries.put(key, new I18nMessageImpl(buffer));
                key = null;
                buffer = null;
            } else if (buffer != null) {
                buffer.endElement(uri, localName, qName);
            }
            elementNesting--;
        }

        public void setDocumentLocator(Locator locator) {
            // do nothing
        }

        public void startDocument() throws SAXException {
            // do nothing
        }

        public void endDocument() throws SAXException {
            // do nothing
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (buffer != null)
                buffer.startPrefixMapping(prefix, uri);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            if (buffer != null)
                buffer.endPrefixMapping(prefix);
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (buffer != null)
                buffer.characters(ch, start, length);
        }

        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
            if (buffer != null)
                buffer.ignorableWhitespace(ch, start, length);
        }

        public void processingInstruction(String target, String data) throws SAXException {
            if (buffer != null)
                buffer.processingInstruction(target, data);
        }

        public void skippedEntity(String name) throws SAXException {
            if (buffer != null)
                buffer.skippedEntity(name);
        }
    }
}
