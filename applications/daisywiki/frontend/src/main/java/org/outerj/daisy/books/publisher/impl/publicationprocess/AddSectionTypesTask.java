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

import org.outerj.daisy.books.publisher.impl.bookmodel.SectionContainer;
import org.outerj.daisy.books.publisher.impl.bookmodel.Section;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.xmlutil.ForwardingContentHandler;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.cocoon.xml.AttributesImpl;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import java.io.InputStream;
import java.io.OutputStream;

public class AddSectionTypesTask implements PublicationProcessTask {
    private static final String PUBLISHER_NAMESPACE = "http://outerx.org/daisy/1.0#publisher";

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running add section type task.");
        processSections(context.getBook(), context, 0);
    }

    private void processSections(SectionContainer sectionContainer, PublicationContext context, int shiftLevel) throws Exception {
        SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
        Section[] sections = sectionContainer.getSections();
        for (Section section : sections) {
            if (section.getType() != null && section.getBookStorePath() != null) {
                // Add the section type, catch result in a SaxBuffer
                String location = BookInstanceLayout.getDocumentInPublicationStorePath(section.getBookStorePath(), context.getPublicationOutputName());
                SaxBuffer buffer = new SaxBuffer();
                AddSectionTypeHandler addSectionTypeHandler = new AddSectionTypeHandler(buffer, section.getType());
                parser.getXMLReader().setContentHandler(addSectionTypeHandler);
                InputStream is = context.getBookInstance().getResource(location);
                try {
                    InputSource inputSource = new InputSource(is);
                    parser.getXMLReader().parse(inputSource);
                } finally {
                    is.close();
                }

                // Store the result
                OutputStream os = context.getBookInstance().getResourceOutputStream(location);
                try {
                    XmlSerializer xmlSerializer = new XmlSerializer(os);
                    buffer.toSAX(xmlSerializer);
                } finally {
                    os.close();
                }
            }
            processSections(section, context, shiftLevel + 1);
        }
    }

    static class AddSectionTypeHandler extends ForwardingContentHandler {
        private final String sectionType;
        private boolean inPreparedDocument = false;

        public AddSectionTypeHandler(ContentHandler consumer, String sectionType) {
            super(consumer);
            this.sectionType = sectionType;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (!inPreparedDocument && namespaceURI.equals(PUBLISHER_NAMESPACE) && localName.equals("preparedDocument")
                    && atts.getValue("id").equals("1")) {
                inPreparedDocument = true;
            } else if (inPreparedDocument && namespaceURI.equals("") && localName.equals("h0")) {
                AttributesImpl newAttrs = new AttributesImpl(atts);
                newAttrs.addCDATAAttribute("daisySectionType", sectionType);
                atts = newAttrs;
            }
            super.startElement(namespaceURI, localName, qName, atts);
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (inPreparedDocument && namespaceURI.equals(PUBLISHER_NAMESPACE) && localName.equals("preparedDocument"))
                inPreparedDocument = false;

            super.endElement(namespaceURI, localName, qName);
        }

    }
}
