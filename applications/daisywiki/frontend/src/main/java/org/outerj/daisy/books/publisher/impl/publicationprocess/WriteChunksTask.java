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

import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.xml.sax.*;

import javax.xml.parsers.SAXParser;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;

public class WriteChunksTask implements PublicationProcessTask {
    private final String chunkFileExtension;
    private final String outputPrefix;
    private final String input;
    private final String applyPipeline;
    private final String pipelineOutputPrefix;
    private final String chunkAfterPipelineFileExtension;

    public WriteChunksTask(String input, String outputPrefix, String chunkFileExtension, String applyPipeline,
                           String pipelineOutputPrefix, String chunkAfterPipelineFileExtension) {
        this.input = input;
        this.outputPrefix = outputPrefix;
        this.chunkFileExtension = chunkFileExtension;
        this.applyPipeline = applyPipeline;
        this.pipelineOutputPrefix = pipelineOutputPrefix;
        this.chunkAfterPipelineFileExtension = chunkAfterPipelineFileExtension != null ? chunkAfterPipelineFileExtension : "";
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running write chunks task.");

        String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
        String inputXml = publicationOutputPath + input;

        InputStream is = null;
        ChunkWriterHandler chunkWriter = null;
        try {
            SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
            chunkWriter = new ChunkWriterHandler(context);
            parser.getXMLReader().setContentHandler(chunkWriter);
            is = context.getBookInstance().getResource(inputXml);
            parser.getXMLReader().parse(new InputSource(is));
        } finally {
            if (is != null)
                is.close();
            if (chunkWriter != null)
                chunkWriter.dispose();
        }

        // Optionally apply a pipeline to each of the written chunks and write results to new files
        if (applyPipeline != null) {
            for (String chunkName : chunkWriter.getWrittenChunks()) {
                String input = publicationOutputPath + outputPrefix + chunkName + chunkFileExtension;
                String output = publicationOutputPath + pipelineOutputPrefix + chunkName + chunkAfterPipelineFileExtension;
                ApplyPipelineTask.applyPipeline(context, applyPipeline, input, output);
            }
        }
    }

    class ChunkWriterHandler implements ContentHandler {
        private final PublicationContext context;
        private OutputStream os;
        private XmlSerializer consumer;
        private int nesting = 0;
        private Set<String> writtenChunks = new HashSet<String>();

        public ChunkWriterHandler(PublicationContext context) {
            this.context = context;
        }

        public void dispose() throws IOException {
            if (os != null)
                os.close();
        }

        public Set<String> getWrittenChunks() {
            return writtenChunks;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            nesting++;
            if (nesting == 2 && namespaceURI.equals("") && localName.equals("chunk")) {
                if (os != null || consumer != null)
                    throw new SAXException("Chunk writer assertion error: new chunk and previous one is not yet finished.");

                String chunkName = atts.getValue("name");
                String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
                String outputBasePath = publicationOutputPath + outputPrefix + chunkName;
                writtenChunks.add(chunkName);
                String outputPath = outputBasePath + chunkFileExtension;
                try {
                    os = context.getBookInstance().getResourceOutputStream(outputPath);
                    consumer = new XmlSerializer(os);
                } catch (Exception e) {
                    throw new SAXException("Error initialising chunk output.", e);
                }
                consumer.startDocument();
            } else if (nesting == 1 && !(namespaceURI.equals("") && localName.equals("chunks"))) {
                throw new SAXException("Invalid input: expected 'chunks' root element but got: " + qName);
            } else if (consumer != null) {
                consumer.startElement(namespaceURI, localName, qName, atts);
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (nesting == 2 && namespaceURI.equals("") && localName.equals("chunk")) {
                consumer.endDocument();
                consumer = null;
                try {
                    os.close();
                    os = null;
                } catch (IOException e) {
                    throw new SAXException("Error closing chunk output stream.", e);
                }
            } else if (consumer != null) {
                consumer.endElement(namespaceURI, localName, qName);
            }
            nesting--;
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (consumer != null)
                consumer.characters(ch, start, length);
        }

        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
            if (consumer != null)
                consumer.ignorableWhitespace(ch, start, length);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            if (consumer != null)
                consumer.endPrefixMapping(prefix);
        }

        public void skippedEntity(String name) throws SAXException {
            if (consumer != null)
                consumer.skippedEntity(name);
        }

        public void setDocumentLocator(Locator locator) {
            // ignore
        }

        public void processingInstruction(String target, String data) throws SAXException {
            if (consumer != null)
                consumer.processingInstruction(target, data);
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (consumer != null)
                consumer.startPrefixMapping(prefix, uri);
        }

        public void startDocument() throws SAXException {
            // ignore
        }

        public void endDocument() throws SAXException {
            // ignore
        }
    }
}
