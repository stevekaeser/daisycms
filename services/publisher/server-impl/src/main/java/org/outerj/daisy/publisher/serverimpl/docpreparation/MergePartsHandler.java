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

import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.PartType;
import org.apache.commons.logging.Log;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.xmlutil.XmlEncodingDetector;
import org.outerj.daisy.xmlutil.XmlMimeTypeHelper;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.diff.Diff;
import org.outerj.daisy.publisher.serverimpl.AbstractHandler;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContext;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerj.daisy.util.Constants;

import javax.xml.parsers.SAXParser;

import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Locale;
import java.util.Set;

/**
 * A ContentHandler that merges parts for which the daisyHtml flag
 * on the PartType is set to true, and optionally for other part types.
 * It reacts on the XML produced by the document object.
 */
public class MergePartsHandler extends AbstractHandler implements ContentHandler {
    private static final String PART_EL = "part";

    private RepositorySchema repositorySchema;
    private Locale locale;
    private Log logger;
    private Set inlineParts;
    private Version version, diffVersion;
    private boolean doDiff; 

    public MergePartsHandler(Version version, boolean doDiff, Version diffVersion, Set inlineParts, ContentHandler consumer, PublisherContext publisherContext) {
        super(consumer);
        this.version = version;
        this.inlineParts = inlineParts;
        this.repositorySchema = publisherContext.getRepository().getRepositorySchema();
        this.locale = publisherContext.getLocale();
        this.logger = publisherContext.getLogger();
        this.doDiff = doDiff;
        this.diffVersion = diffVersion;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (localName.equals(PART_EL) && namespaceURI.equals(Constants.DAISY_NAMESPACE)) {
            long typeId = Long.parseLong(atts.getValue("", "typeId"));
            PartType partType = null;
            try {
                partType = repositorySchema.getPartTypeById(typeId, false);
            } catch (RepositoryException e) {
                consumer.startElement(namespaceURI, localName, qName, atts);
                outputErrorAsDoc("Error retrieving part type info for part " + typeId + ": " + e.getMessage());
            }

            if (partType != null) {
                Part part = version.getPart(typeId);
                boolean isXmlMimeType = XmlMimeTypeHelper.isXmlMimeType(part.getMimeType()); 
                boolean inlinePart = partType.isDaisyHtml() ||
                        (
                                (inlineParts.contains(partType.getName()) || inlineParts.contains(String.valueOf(typeId)))
                                && (isXmlMimeType || part.getMimeType().startsWith("text/"))
                        );

                AttributesImpl newAtts = new AttributesImpl(atts);
                String label = partType.getLabel(locale);
                newAtts.addAttribute("", "label", "label", "CDATA", label);
                newAtts.addAttribute("", "name", "name", "CDATA", partType.getName());
                newAtts.addAttribute("", "daisyHtml", "daisyHtml", "CDATA", String.valueOf(partType.isDaisyHtml()));
                newAtts.addAttribute("", "inlined", "inlined", "CDATA", String.valueOf(inlinePart));
                consumer.startElement(namespaceURI, localName, qName, newAtts);

                if (inlinePart) {
                    Throwable error = null;
                    SaxBuffer saxBuffer = new SaxBuffer();
                    try {
                        InputStream is = null;
                        try {
                        	if(doDiff){
                        		String part1String, part2String;
                                if (version.getPart(typeId).getMimeType().equals("text/xml"))
                                    part1String = new String(version.getPart(typeId).getData(), XmlEncodingDetector.detectEncoding(version.getPart(typeId).getData()));
                                else
                                    part1String = new String(version.getPart(typeId).getData());
                                if (diffVersion.getPart(typeId).getMimeType().equals("text/xml"))
                                    part2String = new String(diffVersion.getPart(typeId).getData(), XmlEncodingDetector.detectEncoding(diffVersion.getPart(typeId).getData()));
                                else
                                    part2String = new String(diffVersion.getPart(typeId).getData());

                                SaxBuffer diffBuffer = new SaxBuffer();
                                saxBuffer.startDocument();
                                saxBuffer.startElement("", "html", "html", new AttributesImpl());
                                saxBuffer.startElement("", "body", "body", new AttributesImpl());
                                Diff.diffHTML(part2String, part1String, saxBuffer, String.valueOf(typeId),  locale);
                                saxBuffer.endElement("", "body", "body");
                                saxBuffer.endElement("", "html", "html");
                                saxBuffer.endDocument();
                        	}else{
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
                        	}
                            
                        } finally {
                            if (is != null)
                                is.close();
                        }
                    } catch (Throwable e) {
                        error = e;
                        logger.error("Error including part content.", e);
                    }

                    if (error == null) {
                   		saxBuffer.toSAX(new StripDocumentHandler(consumer));
                    } else {
                        StringBuilder message = new StringBuilder("Error including part content: ");
                        message.append(error.getMessage());
                        Throwable cause = error.getCause();
                        while (cause != null) {
                            message.append(": ").append(cause.getMessage());
                            cause = cause.getCause();
                        }
                        outputErrorAsDoc(message.toString());
                    }
                }
            }
        } else {
            consumer.startElement(namespaceURI, localName, qName, atts);
        }
    }

}
