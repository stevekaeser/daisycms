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
import org.outerj.daisy.xmlutil.HtmlBodyRemovalHandler;
import org.outerj.daisy.frontend.PreparedDocuments;
import org.outerj.daisy.frontend.PreparedIncludeHandler;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.cocoon.xml.ContentHandlerWrapper;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.io.OutputStream;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class AssembleBookTask implements PublicationProcessTask {
    private final String output;

    public AssembleBookTask(String output) {
        this.output = output;
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running assemble book task.");
        String outputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName()) + output;
        OutputStream os = context.getBookInstance().getResourceOutputStream(outputPath);
        try {
            XmlSerializer xmlSerializer = new XmlSerializer(os);
            xmlSerializer.startDocument();
            xmlSerializer.startElement("", "html", "html", new AttributesImpl());
            xmlSerializer.startElement("", "body", "body", new AttributesImpl());

            processSections(context.getBook(), context, xmlSerializer, 1);

            xmlSerializer.endElement("", "body", "body");
            xmlSerializer.endElement("", "html", "html");
            xmlSerializer.endDocument();
        } finally {
            os.close();
        }
    }

    private void processSections(SectionContainer sectionContainer, PublicationContext context, ContentHandler consumer, int level) throws Exception {
        Section[] sections = sectionContainer.getSections();
        for (int i = 0; i < sections.length; i++) {
            if (sections[i].getBookStorePath() != null) {
                String location = BookInstanceLayout.getDocumentInPublicationStorePath(sections[i].getBookStorePath(), context.getPublicationOutputName());
                PreparedDocuments preparedDocuments = PreparedDocumentsBuilder.build(context.getBookInstance().getResource(location));
                HeaderShifterHandler headerShifterHandler = new HeaderShifterHandler(consumer, level - 1);
                PreparedIncludeHandler preparedIncludeHandler = new PreparedIncludeHandler(new ContentHandlerWrapper(headerShifterHandler), preparedDocuments, true);
                SaxBuffer buffer = preparedDocuments.getPreparedDocument(1).getSaxBuffer();
                buffer.toSAX(new HtmlBodyRemovalHandler(preparedIncludeHandler));
            } else if (sections[i].getTitle() != null) {
                String headerTag = "h" + level;
                AttributesImpl attrs = new AttributesImpl();
                if (sections[i].getType() != null) {
                    attrs.addCDATAAttribute("daisySectionType", sections[i].getType());
                }
                consumer.startElement("", headerTag, headerTag, attrs);
                char[] title = sections[i].getTitle().toCharArray();
                consumer.characters(title, 0, title.length);
                consumer.endElement("", headerTag, headerTag);
            }
            processSections(sections[i], context, consumer, level + 1);
        }
    }

    static class HeaderShifterHandler extends ForwardingContentHandler {
        private Stack headerStack = new Stack();
        private final int shiftAmount;
        private static final Pattern headerPattern = Pattern.compile("h([0-9]+)");

        public HeaderShifterHandler(ContentHandler consumer, int shiftAmount) {
            super(consumer);
            this.shiftAmount = shiftAmount;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            Matcher matcher = headerPattern.matcher(localName);
            if (matcher.matches() && namespaceURI.equals("")) {
                int currentLevel = Integer.parseInt(matcher.group(1));
                int newLevel = currentLevel + shiftAmount + 1;
                consumer.startElement(namespaceURI, "h" + newLevel, "h" + newLevel, atts);
                headerStack.push(new Integer(newLevel));
            } else {
                consumer.startElement(namespaceURI, localName, qName, atts);
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            Matcher matcher = headerPattern.matcher(localName);
            if (matcher.matches() && namespaceURI.equals("")) {
                int level = ((Integer)headerStack.pop()).intValue();
                consumer.endElement(namespaceURI, "h" + level, "h" + level);
            } else {
                consumer.endElement(namespaceURI, localName, qName);
            }
        }
    }

}
