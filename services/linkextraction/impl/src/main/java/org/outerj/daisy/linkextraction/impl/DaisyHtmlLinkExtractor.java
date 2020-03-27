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

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.outerj.daisy.linkextraction.LinkCollector;
import org.outerj.daisy.linkextraction.LinkType;
import org.outerj.daisy.plugin.PluginRegistry;

public class DaisyHtmlLinkExtractor extends AbstractLinkExtractor {

    public DaisyHtmlLinkExtractor(String name, String description, PluginRegistry pluginRegistry) {
        super(name, description, pluginRegistry);
    }

    public DaisyHtmlLinkExtractor() {
        super();
    }

    protected ContentHandler getContentHandler(LinkCollector linkCollector, String defaultBranch, String defaultLanguage) {
        return new DaisyHtmlLinkExtractionHandler(linkCollector);
    }

    public class DaisyHtmlLinkExtractionHandler extends DefaultHandler {
        private int includeLevel = -1;
        private StringBuilder includeBuffer;
        private LinkCollector linkCollector;

        public DaisyHtmlLinkExtractionHandler(LinkCollector linkCollector) {
            this.linkCollector = linkCollector;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (namespaceURI.equals("")) {
                if (includeLevel > -1) {
                    includeLevel++;
                } else if (localName.equals("a")) {
                    String href = atts.getValue("href");
                    linkCollector.addLink(LinkType.INLINE, href);
                } else if (localName.equals("img")) {
                    String src = atts.getValue("src");
                    linkCollector.addLink(LinkType.IMAGE, src);
                } else if (localName.equals("pre")) {
                    String clazz = atts.getValue("class");
                    if (clazz != null && clazz.equals("include")) {
                        includeLevel = 0;
                        includeBuffer = new StringBuilder();
                    }
                }
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (includeLevel > -1) {
                includeLevel--;
                if (includeLevel == -1) {
                    String includeLink = includeBuffer.toString().trim();
                    // Include-links can contain a comment after the 'daisy:' link,
                    // separated from the link by a space
                    int spacePos = includeLink.indexOf(' ');
                    if (spacePos != -1) {
                        includeLink = includeLink.substring(0, spacePos);
                    }
                    linkCollector.addLink(LinkType.INCLUDE, includeLink);
                }
            }
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (includeLevel > -1) {
                includeBuffer.append(ch, start, length);
            }
        }
    }
}
