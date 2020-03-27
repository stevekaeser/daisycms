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

import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.linkextraction.LinkExtractor;
import org.outerj.daisy.linkextraction.LinkCollector;
import org.outerj.daisy.plugin.PluginRegistry;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;

import javax.xml.parsers.SAXParser;
import javax.annotation.PreDestroy;
import java.io.InputStream;

public abstract class AbstractLinkExtractor implements LinkExtractor {
    private final String name;
    private final String description;
    private PluginRegistry pluginRegistry;

    public AbstractLinkExtractor(String name, String description, PluginRegistry pluginRegistry) {
        this.name = name;
        this.description = description;
        this.pluginRegistry = pluginRegistry;
        pluginRegistry.addPlugin(LinkExtractor.class, getName(), this);
    }

    @PreDestroy
    public void destroy() {
        pluginRegistry.removePlugin(LinkExtractor.class, getName(), this);
    }

    public AbstractLinkExtractor() {
        this.name = null;
        this.description = null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void extractLinks(InputStream is, LinkCollector linkCollector, String defaultBranch, String defaultLanguage) throws Exception {
        try {
            SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
            parser.getXMLReader().setContentHandler(getContentHandler(linkCollector, defaultBranch, defaultLanguage));
            parser.getXMLReader().parse(new InputSource(is));
        } finally {
            if (is != null)
                is.close();
        }
    }

    protected abstract ContentHandler getContentHandler(LinkCollector linkCollector, String defaultBranch, String defaultLanguage);
}
