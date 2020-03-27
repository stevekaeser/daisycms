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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

import org.xml.sax.*;
import org.xml.sax.helpers.NamespaceSupport;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.cocoon.xml.SaxBuffer;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.xmlutil.ForwardingContentHandler;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;

import javax.xml.parsers.SAXParser;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.*;
import java.io.InputStream;
import java.io.OutputStream;

public class SplitInChunksTask implements PublicationProcessTask {
    private final String input;
    private final String output;
    private final String chunkNamePrefix;
    private final String publishExtension;
    private final String firstChunkName;
    private static final Pattern headerPattern = Pattern.compile("h([0-9]+)");

    public SplitInChunksTask(String input, String output, String chunkNamePrefix,
                             String firstChunkName, String publishExtension) {
        this.input = input;
        this.output = output;
        this.chunkNamePrefix = chunkNamePrefix;
        this.firstChunkName = firstChunkName;
        this.publishExtension = publishExtension;
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running split in chunks task.");
        
        String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
        String inputPath = publicationOutputPath + input;
        String outputPath = publicationOutputPath + output;

        int chunkLevel = 1;
        String chunkLevelProp = context.getProperties().get("chunker.chunklevel");
        if (chunkLevelProp != null) {
            try {
                chunkLevel = Integer.parseInt(chunkLevelProp);
            } catch (NumberFormatException e) {
                context.getPublicationLog().error("Invalid value in chunker.chunklevel property: \"" + chunkLevelProp + "\", defaulting to " + chunkLevel);
            }
        }

        //
        // Step 1: make chunks and capture result in a sax buffer
        //
        SaxBuffer buffer = new SaxBuffer();
        ChunkerHandler chunkerHandler;
        InputStream is = null;
        try {
            SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
            is = context.getBookInstance().getResource(inputPath);
            chunkerHandler = new ChunkerHandler(buffer, chunkLevel);
            parser.getXMLReader().setContentHandler(chunkerHandler);
            parser.getXMLReader().parse(new InputSource(is));
        } finally {
            if (is != null)
                is.close();
        }

        //
        // Step: adjust links
        //
        OutputStream os = null;
        try {
            os = context.getBookInstance().getResourceOutputStream(outputPath);
            XmlSerializer serializer = new XmlSerializer(os);
            ChunkLinkFixerHandler linkFixer = new ChunkLinkFixerHandler(serializer, chunkerHandler.getIdToChunkMap());
            buffer.toSAX(linkFixer);
        } finally {
            if (os != null)
                os.close();
        }
    }

    class ChunkerHandler implements ContentHandler {
        private boolean inBody = false;
        private int nesting = 0;
        private boolean inChunk = false;
        private ContentHandler consumer;
        private Set<String> createdChunks = new HashSet<String>();
        private int chunkNameCounter = 0;
        private Map<String, String> idToChunkMap = new HashMap<String, String>();
        private String currentChunkName;
        private boolean firstChunk = true;
        private final int chunkLevel;
        private int prevHeaderLevel = -1;
        private NamespaceSupport namespaceSupport = new NamespaceSupport();

        public ChunkerHandler(ContentHandler consumer, int chunkLevel) {
            this.consumer = consumer;
            this.chunkLevel = chunkLevel;
        }

        public Map getIdToChunkMap() {
            return idToChunkMap;
        }

        public void startDocument() throws SAXException {
            consumer.startDocument();
            consumer.startElement("", "chunks", "chunks", new AttributesImpl());
        }

        public void endDocument() throws SAXException {
            if (inChunk)
                endChunk();
            consumer.endElement("", "chunks", "chunks");
            consumer.endDocument();
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            namespaceSupport.pushContext();
            nesting++;
            Matcher headerMatcher = headerPattern.matcher(localName);

            if (nesting == 2 && namespaceURI.equals("") && localName.equals("body")) {
                inBody = true;
            } else if (inBody && nesting == 3 && headerMatcher.matches()) {
                int headerLevel = Integer.parseInt(headerMatcher.group(1));
                if (headerLevel <= chunkLevel && headerLevel != prevHeaderLevel + 1) {
                    if (inChunk)
                        endChunk();

                    // determine chunk name
                    String chunkName = null;
                    if (firstChunk && firstChunkName != null)
                        chunkName = firstChunkName;
                    firstChunk = false;
                    if (chunkName == null) {
                        String headerId = atts.getValue("id");
                        if (headerId != null && headerId.trim().length() > 0) {
                            chunkName = headerId;
                        } else {
                            chunkName = generateChunkName();
                        }
                    }
                    // check if there is not already a chunk named this way and generate new name until we have a unique one
                    while (createdChunks.contains(chunkName)) {
                        chunkName = generateChunkName();
                    }
                    createdChunks.add(chunkName);
                    currentChunkName = chunkName;

                    AttributesImpl chunkAttrs = new AttributesImpl();
                    chunkAttrs.addCDATAAttribute("name", chunkName);
                    consumer.startElement("", "chunk", "chunk", chunkAttrs);
                    consumer.startElement("", "html", "html", new AttributesImpl());
                    declarePrefixes();
                    consumer.startElement("", "body", "body", new AttributesImpl());
                    inChunk = true;
                }
                prevHeaderLevel = headerLevel;
            }

            if (inChunk) {
                consumer.startElement(namespaceURI, localName, qName, atts);

                String id = atts.getValue("id");
                if (id != null && id.trim().length() > 0) {
                    idToChunkMap.put(id, currentChunkName);
                }
            }
        }

        private String generateChunkName() {
            return chunkNamePrefix + ++chunkNameCounter;
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            namespaceSupport.popContext();
            if (inBody && nesting == 2 && namespaceURI.equals("") && localName.equals("body")) {
                inBody = false;
                if (inChunk) {
                    endChunk();
                    inChunk = false;
                }
            } else if (inChunk) {
                consumer.endElement(namespaceURI, localName, qName);
            }
            nesting--;
        }

        private void endChunk() throws SAXException {
            consumer.endElement("", "body", "body");
            undeclarePrefixes();
            consumer.endElement("", "html", "html");
            consumer.endElement("", "chunk", "chunk");
        }

        private void declarePrefixes() throws SAXException {
            Enumeration prefixEnum = namespaceSupport.getPrefixes();
            while (prefixEnum.hasMoreElements()) {
                String prefix = (String)prefixEnum.nextElement();
                if (!prefix.equals("xml"))
                    consumer.startPrefixMapping(prefix, namespaceSupport.getURI(prefix));
            }
        }

        private void undeclarePrefixes() throws SAXException {
            Enumeration prefixEnum = namespaceSupport.getPrefixes();
            while (prefixEnum.hasMoreElements()) {
                String prefix = (String)prefixEnum.nextElement();
                if (!prefix.equals("xml"))
                    consumer.endPrefixMapping(prefix);
            }
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (inChunk)
                consumer.characters(ch, start, length);
        }

        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
            if (inChunk)
                consumer.ignorableWhitespace(ch, start, length);
        }

        public void skippedEntity(String name) throws SAXException {
            if (inChunk)
                consumer.skippedEntity(name);
        }

        public void processingInstruction(String target, String data) throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            namespaceSupport.declarePrefix(prefix, uri);
            consumer.startPrefixMapping(prefix, uri);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            consumer.endPrefixMapping(prefix);
        }

        public void setDocumentLocator(Locator locator) {
            // ignore
        }
    }

    class ChunkLinkFixerHandler extends ForwardingContentHandler {
        private Map idToChunkMap;
        private int nesting = 0;
        private String currentChunkName = null;

        public ChunkLinkFixerHandler(ContentHandler consumer, Map idToChunkMap) {
            super(consumer);
            this.idToChunkMap = idToChunkMap;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            nesting++;
            if (nesting == 2 && namespaceURI.equals("") && localName.equals("chunk")) {
                currentChunkName = atts.getValue("name");
            } else if (currentChunkName != null && namespaceURI.equals("") && localName.equals("a")) {
                String href = atts.getValue("href");
                if (href != null && href.length() > 1 && href.charAt(0) == '#') {
                    String targetChunk = (String)idToChunkMap.get(href.substring(1));
                    if (targetChunk != null && !targetChunk.equals(currentChunkName)) {
                        String newHref = targetChunk + publishExtension + href;
                        AttributesImpl newAttrs = new AttributesImpl(atts);
                        newAttrs.setValue(newAttrs.getIndex("href"), newHref);
                        atts = newAttrs;
                    }
                }
            }
            super.startElement(namespaceURI, localName, qName, atts);
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (nesting == 2 && namespaceURI.equals("") && localName.equals("chunk")) {
                currentChunkName = null;
            }
            nesting--;
            super.endElement(namespaceURI, localName, qName);
        }
    }
}
