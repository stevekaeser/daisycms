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
package org.outerj.daisy.publisher.serverimpl.docpreparation;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContext;

/**
 * Processes special instructions in the content, such as queries
 * and includes.
 *
 * <p>A ContentProcessor can be instantiated recursively by the
 * IncludeProcessor: if the IncludeProcessor includes some content,
 * it will create a new ContentProcessor and push the content
 * through that ContentProcessor. ContentProcessors have a reference
 * to their parent processor so that recursive includes can be
 * detected.
 *
 * <p>From Daisy 1.5, this has been updated to perform a new publisher
 * request for each included document.
 *
 * <pre>
 * +--ContentProcessor---------------------------------------------+
 * | +--------------+  +---------------------+  +----------------+ |
 * | |QueryProcessor|->|QueryIncludeProcessor|->|IncludeProcessor| |
 * | +--------------+  +---------------------+  +-------+--------+ |
 * +----------------------------------------------------|----------+
 *         +____________________________________________|
 *         |
 * +-------|-------------------------------------------------------+
 * | +-----v--------+  +---------------------+  +----------------+ |
 * | |QueryProcessor|->|QueryIncludeProcessor|->|IncludeProcessor| |
 * | +--------------+  +---------------------+  +-------+--------+ |
 * +----------------------------------------------------|----------+
 *                                                      |
 *                                                      v
 *                                                 and so on
 * </pre>
 *
 */
public class ContentProcessor implements ContentHandler {
    private Document document;
    private Version version;

    private ContentHandler pipe;
    private ContentProcessor parent;

    private IncludesProcessor includesProcessor;
    private PublisherContext publisherContext;

    /**
     * @param document the document whose data is being processed through this pipe
     * @param version the version whose data is being processed through this pipe
     * @param parent optional, can be null
     */
    public ContentProcessor(Document document, Version version, ContentHandler consumer,
            PublisherContext publisherContext, ContentProcessor parent) {
        this.document = document;
        this.version = version;
        this.includesProcessor = new IncludesProcessor(document, consumer, this);
        this.publisherContext = publisherContext;
        this.parent = parent;
        this.pipe = new QueriesProcessor(new QueryIncludeProcessor(includesProcessor, this), this);
    }

    public ContentProcessor getParent() {
        return parent;
    }

    public void setParent(ContentProcessor parent) {
        this.parent = parent;
    }

    public IncludesProcessor getIncludesProcessor() {
        return includesProcessor;
    }

    public PublisherContext getPublisherContext() {
        return publisherContext;
    }

    public Document getDocument() {
        return document;
    }

    public Version getVersion() {
        return version;
    }

    public void endDocument() throws SAXException {
        pipe.endDocument();
    }

    public void startDocument () throws SAXException {
        pipe.startDocument();
    }

    public void characters (char ch[], int start, int length) throws SAXException {
        pipe.characters(ch, start, length);
    }

    public void ignorableWhitespace (char ch[], int start, int length) throws SAXException {
        pipe.ignorableWhitespace(ch, start, length);
    }

    public void endPrefixMapping (String prefix) throws SAXException {
        pipe.endPrefixMapping(prefix);
    }

    public void skippedEntity (String name) throws SAXException {
        pipe.skippedEntity(name);
    }

    public void setDocumentLocator (Locator locator) {
        pipe.setDocumentLocator(locator);
    }

    public void processingInstruction (String target, String data) throws SAXException {
        pipe.processingInstruction(target, data);
    }

    public void startPrefixMapping (String prefix, String uri) throws SAXException {
        pipe.startPrefixMapping(prefix, uri);
    }

    public void endElement (String namespaceURI, String localName,
                            String qName) throws SAXException {
        pipe.endElement(namespaceURI, localName, qName);
    }

    public void startElement (String namespaceURI, String localName,
                              String qName, Attributes atts) throws SAXException {
        pipe.startElement(namespaceURI, localName, qName, atts);
    }
}
