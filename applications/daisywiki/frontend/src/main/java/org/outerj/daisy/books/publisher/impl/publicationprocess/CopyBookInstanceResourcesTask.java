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
import org.outerj.daisy.xmlutil.ForwardingContentHandler;
import org.outerj.daisy.books.publisher.impl.util.PublicationLog;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.outerx.daisy.x10Bookstoremeta.ResourcePropertiesDocument;
import org.apache.cocoon.xml.AttributesImpl;

import javax.xml.parsers.SAXParser;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class CopyBookInstanceResourcesTask implements PublicationProcessTask {
    private final String input;
    private final String output;
    private final String baseToPath;
    private static Map<String, String> fileExtensions = new HashMap<String, String>();
    static {
        fileExtensions.put("image/png", ".png");
        fileExtensions.put("image/x-png", ".png");
        fileExtensions.put("image/jpeg", ".jpg");
        fileExtensions.put("image/pjpeg", ".jpg");
        fileExtensions.put("image/gif", ".gif");
    }

    public CopyBookInstanceResourcesTask(String input, String output, String baseToPath) {
        this.input = input;
        this.output = output;
        this.baseToPath = baseToPath;
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running copy book instance resources task.");

        String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
        String inputXmlPath = publicationOutputPath + input;
        String outputXmlPath = publicationOutputPath + output;
        String outputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());

        InputStream is = null;
        OutputStream os = null;
        try {
            SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
            os = context.getBookInstance().getResourceOutputStream(outputXmlPath);
            XmlSerializer serializer = new XmlSerializer(os);
            ImageCopyHandler imageCopyHandler = new ImageCopyHandler(serializer, context.getBookInstance(),
                    outputPath, context.getPublicationLog());
            parser.getXMLReader().setContentHandler(imageCopyHandler);
            is = context.getBookInstance().getResource(inputXmlPath);
            InputSource inputSource = new InputSource(is);
            parser.getXMLReader().parse(inputSource);
        } finally {
            if (os != null)
                os.close();
            if (is != null)
                is.close();
        }
    }

    class ImageCopyHandler extends ForwardingContentHandler {
        private final BookInstance bookInstance;
        private final String outputPath;
        private final PublicationLog log;

        public ImageCopyHandler(ContentHandler consumer, BookInstance bookInstance, String outputPath, PublicationLog log) {
            super(consumer);
            this.bookInstance = bookInstance;
            this.outputPath = outputPath;
            this.log = log;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (namespaceURI.equals("")) {
                String attrName = null;
                if (localName.equals("img"))
                    attrName = "src";
                else if (localName.equals("a"))
                    attrName = "href";
                else if (localName.equals("object"))
                    attrName = "data";
                else if (localName.equals("embed"))
                    attrName = "src";

                if (attrName != null) {
                    String newLink = handleLinkAttr(atts.getValue(attrName));
                    if (newLink != null) {
                        AttributesImpl newAttrs = new AttributesImpl(atts);
                        newAttrs.setValue(newAttrs.getIndex(attrName), newLink);
                        atts = newAttrs;
                    }
                }
            }
            super.startElement(namespaceURI, localName, qName, atts);
        }

        private String handleLinkAttr(String href) throws SAXException {
            if (href == null)
                return null;
            if (!href.startsWith("bookinstance:"))
                return null;
            if (href.startsWith("bookinstance:output/")) {
                // resource is already in output dir, just translate to relative link
                return href.substring("bookinstance:output/".length());
            }

            String resourcePath = href.substring("bookinstance:".length());
            String newResourcePath = baseToPath + href.substring(("bookinstance:" + BookInstanceLayout.geResourceStorePath()).length());

            ResourcePropertiesDocument propertiesDocument = bookInstance.getResourceProperties(resourcePath);
            String mimeType = (propertiesDocument != null && propertiesDocument.getResourceProperties() != null) ? propertiesDocument.getResourceProperties().getMimeType() : null;
            if (mimeType != null) {
                String extension = fileExtensions.get(mimeType);
                if (extension != null)
                    newResourcePath = newResourcePath + extension;
            }

            InputStream is = null;
            try {
                is = bookInstance.getResource(resourcePath);
                bookInstance.storeResource(outputPath + newResourcePath, is);
            } finally {
                if (is != null)
                    try {
                        is.close();
                    } catch (IOException e) {
                        log.error("Error closing input stream of " + resourcePath, e);
                    }
            }

            String newSrc = newResourcePath.startsWith("output/") ? newResourcePath.substring("output/".length()) : newResourcePath;
            return newSrc;
        }
    }
}
