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

public class NavigationLinkExtractor extends AbstractLinkExtractor {

    public NavigationLinkExtractor(String name, String description, PluginRegistry pluginRegistry) {
        super(name, description, pluginRegistry);
    }

    public NavigationLinkExtractor() {
        super();
    }

    protected ContentHandler getContentHandler(LinkCollector linkCollector, String defaultBranch, String defaultLanguage) {
        return new NavigationLinkExtractionHandler(linkCollector, defaultBranch, defaultLanguage);
    }

    public class NavigationLinkExtractionHandler extends DefaultHandler {
        private final LinkCollector linkCollector;
        private final String defaultBranch;
        private final String defaultLanguage;
        private static final String NAVIGATION_NS = "http://outerx.org/daisy/1.0#navigationspec";

        public NavigationLinkExtractionHandler(LinkCollector linkCollector, String defaultBranch, String defaultLanguage) {
            this.linkCollector = linkCollector;
            this.defaultBranch = defaultBranch;
            this.defaultLanguage = defaultLanguage;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if ((localName.equals("doc") || localName.equals("import")) && uri.equals(NAVIGATION_NS)) {
                String idString = attributes.getValue(localName.equals("doc") ? "id" : "docId");
                if (idString != null) {
                    String docId = idString.trim();
                    String branch = attributes.getValue("branch");
                    if (branch == null)
                        branch = defaultBranch;
                    String language = attributes.getValue("language");
                    if (language == null)
                        language = defaultLanguage;
                    linkCollector.addLink(LinkType.OTHER, docId, branch, language, -1);
                }
            }
        }
    }
}
