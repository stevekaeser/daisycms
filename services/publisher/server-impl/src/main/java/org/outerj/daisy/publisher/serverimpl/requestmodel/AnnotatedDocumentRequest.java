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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.xml.parsers.SAXParser;

import org.outerj.daisy.publisher.serverimpl.AbstractHandler;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerj.daisy.publisher.serverimpl.docpreparation.PreparationPipe;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerj.daisy.xmlutil.XmlMimeTypeHelper;
import org.outerx.daisy.x10.DocumentDocument;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

public class AnnotatedDocumentRequest extends AbstractRequest {
    public static enum PartContentInclusionType { NONE, ALL, DAISY_HTML }
    private final PartContentInclusionType partContentInclusionType;

    public AnnotatedDocumentRequest(PartContentInclusionType partContentInclusionType, LocationInfo locationInfo) {
        super(locationInfo);
        this.partContentInclusionType = partContentInclusionType;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        Document document = publisherContext.getDocument();
        Version version = publisherContext.getVersion();
        DocumentDocument documentDocument = version != null ? document.getXml(version.getId()) : document.getXml(document.getLastVersionId());
        Repository repository = publisherContext.getRepository();
        PreparationPipe.annotateDocument(documentDocument.getDocument(), publisherContext);
        PreparationPipe.annotateFields(documentDocument.getDocument(), repository, publisherContext.getLocale(), publisherContext.getVersionMode());
        PreparationPipe.annotateTimeline(documentDocument.getDocument(), publisherContext);
        ContentHandler annotatePartHandler = new AnnotatePartHandler(contentHandler, version, publisherContext);
        documentDocument.save(new StripDocumentHandler(annotatePartHandler), new DummyLexicalHandler());
    }

    class AnnotatePartHandler extends AbstractHandler {
        private final PublisherContext publisherContext;
        private final Version version;

        public AnnotatePartHandler(ContentHandler consumer, Version version, PublisherContext publisherContext) {
            super(consumer);
            this.version = version;
            this.publisherContext = publisherContext;
        }

        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (uri.equals(Constants.DAISY_NAMESPACE) && localName.equals("part")) {
                long typeId = Long.parseLong(atts.getValue("", "typeId"));
                PartType partType;
                try {
                    partType = publisherContext.getRepository().getRepositorySchema().getPartTypeById(typeId, false);
                } catch (RepositoryException e) {
                    throw new SAXException("Error getting part type info.", e);
                }
                String mimeType = atts.getValue("mimeType");

                AttributesImpl newAtts = new AttributesImpl(atts);
                String label = partType.getLabel(publisherContext.getLocale());
                newAtts.addAttribute("", "label", "label", "CDATA", label);
                newAtts.addAttribute("", "name", "name", "CDATA", partType.getName());
                newAtts.addAttribute("", "daisyHtml", "daisyHtml", "CDATA", String.valueOf(partType.isDaisyHtml()));

                if ((partContentInclusionType == PartContentInclusionType.ALL && mimeType.startsWith("text/"))
                        || (partContentInclusionType == PartContentInclusionType.DAISY_HTML && partType.isDaisyHtml())) {
                    boolean isXmlMimeType = XmlMimeTypeHelper.isXmlMimeType("text/xml");
                    boolean error = false;
                    SaxBuffer saxBuffer = new SaxBuffer();
                    try {
                        InputStream is = null;
                        try {
                            is = new BufferedInputStream(version.getPart(typeId).getDataStream());
                            if (partType.isDaisyHtml() || isXmlMimeType) {
                                SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
                                XMLReader xmlReader = parser.getXMLReader();
                                xmlReader.setContentHandler(saxBuffer);
                                InputSource inputSource = new InputSource(is);
                                xmlReader.parse(inputSource);
                            } else {
                                Reader reader = new InputStreamReader(is);
                                char[] buffer = new char[8192];
                                int read;
                                while ((read = reader.read(buffer)) != -1) {
                                    saxBuffer.characters(buffer, 0, read);
                                }
                            }
                        } finally {
                            if (is != null)
                                is.close();
                        }
                    } catch (Throwable e) {
                        error = true;
                    }
                    newAtts.addAttribute("", "inlined", "inlined", "CDATA", error ? "error" : "true");
                    super.startElement(uri, localName, qName, newAtts);
                    saxBuffer.toSAX(new StripDocumentHandler(consumer));
                } else {
                    super.startElement(uri, localName, qName, newAtts);
                }
            } else {
                super.startElement(uri, localName, qName, atts);
            }
        }
    }

}
