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
package org.outerj.daisy.linkextraction.impl;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.outerj.daisy.linkextraction.LinkCollector;
import org.outerj.daisy.linkextraction.LinkType;
import org.outerj.daisy.plugin.PluginRegistry;

/**
 * Link extractor that extracts "daisy:" links specified in the values of Java 5 style XML properties.
 * (e.g. used for the book meta data).
 */
public class PropertiesLinkExtractor extends AbstractLinkExtractor {

    public PropertiesLinkExtractor(String name, String description, PluginRegistry pluginRegistry) {
        super(name, description, pluginRegistry);
    }

    public PropertiesLinkExtractor() {
        super();
    }

    protected ContentHandler getContentHandler(LinkCollector linkCollector, String defaultBranch, String defaultLanguage) {
        return new PropertiesLinkExtractionHandler(linkCollector);
    }

    public class PropertiesLinkExtractionHandler extends DefaultHandler {
        private final LinkCollector linkCollector;
        private StringBuilder buffer;
        private int nestingLevel = 0;

        public PropertiesLinkExtractionHandler(LinkCollector linkCollector) {
            this.linkCollector = linkCollector;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            nestingLevel++;
            if (nestingLevel == 2 && localName.equals("entry") && uri.equals("")) {
                buffer = new StringBuilder();
            }
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (buffer != null)
                buffer.append(ch, start, length);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (nestingLevel == 2 && buffer != null) {
                if (buffer.length() > 0)
                    linkCollector.addLink(LinkType.OTHER, buffer.toString().trim());
                buffer = null;
            }
            nestingLevel--;
        }
    }
}
