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
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class VerifyIdsAndLinksTask implements PublicationProcessTask {
    private final String input;
    private final String output;
    private static final Pattern headerPattern = Pattern.compile("h([0-9]+)");

    public VerifyIdsAndLinksTask(String input, String output) {
        this.input = input;
        this.output = output;
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running verify IDs and links task.");
        LinkLog linkLog = new LinkLog(context.getBookInstance());

        try {
            verifyIdsAndLinks(context, linkLog);
        } finally {
            linkLog.dispose();
        }
    }

    private void verifyIdsAndLinks(PublicationContext context, LinkLog linkLog) throws Exception {
        String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
        String inputXmlPath = publicationOutputPath + input;
        String outputXmlPath = publicationOutputPath + output;
        BookInstance bookInstance = context.getBookInstance();

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        SAXParser parser = parserFactory.newSAXParser();

        IdGatherer idGatherer = new IdGatherer(linkLog);
        parser.getXMLReader().setContentHandler(idGatherer);

        InputStream is = bookInstance.getResource(inputXmlPath);
        try {
            InputSource inputSource = new InputSource(is);
            parser.getXMLReader().parse(inputSource);
        } finally {
            is.close();
        }

        OutputStream os = null;
        is = null;
        try {
            os = bookInstance.getResourceOutputStream(outputXmlPath);
            XmlSerializer xmlSerializer = new XmlSerializer(os);
            IdAssigner idAssigner = new IdAssigner(xmlSerializer, idGatherer.getIds());
            LinkCheckerHandler linkCheckerHandler = new LinkCheckerHandler(idAssigner, idGatherer.getIds(), linkLog);
            parser.getXMLReader().setContentHandler(linkCheckerHandler);
            is = bookInstance.getResource(inputXmlPath);
            InputSource inputSource = new InputSource(is);
            parser.getXMLReader().parse(inputSource);
        } finally {
            if (os != null)
                os.close();
            if (is != null)
                is.close();
        }
    }

    private static class IdGatherer extends DefaultHandler {
        private Set<String> ids = new HashSet<String>();
        private LinkLog linkLog;

        public IdGatherer(LinkLog linkLog) {
            this.linkLog = linkLog;
        }

        public Set<String> getIds() {
            return ids;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (namespaceURI.equals("")) {
                String id = atts.getValue("id");
                if (id != null) {
                    if (ids.contains(id)) {
                        linkLog.error("Duplicate ID encountered: " + id);
                    }
                    ids.add(id);
                }
            }
        }
    }

    private static class LinkCheckerHandler extends ForwardingContentHandler {
        private Set<String> ids;
        private LinkLog linkLog;
        private String currentDocument;
        private String currentBranch;
        private String currentLanguage;

        public LinkCheckerHandler(ContentHandler consumer, Set<String> ids, LinkLog linkLog) {
            super(consumer);
            this.ids = ids;
            this.linkLog = linkLog;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (uri.length() == 0) {
                if (headerPattern.matcher(localName).matches()) {
                    String document = attributes.getValue("daisyDocument");
                    if (document != null) {
                        this.currentDocument = document;
                        this.currentBranch = attributes.getValue("daisyBranch");
                        this.currentLanguage = attributes.getValue("daisyLanguage");
                    }
                } else if (localName.equals("a")) {
                    String href = attributes.getValue("href");
                    if (href != null && href.startsWith("#")) {
                        if (!ids.contains(href.substring(1))) {
                            linkLog.error("Link pointing to non-defined ID: " + href + getContext());
                        }
                    }
                } else if (localName.equals("span") && "crossreference".equals(attributes.getValue("class"))) {
                    String crossRefBookTarget = attributes.getValue("crossRefBookTarget");
                    if (crossRefBookTarget != null && crossRefBookTarget.length() > 1) {
                        if (!ids.contains(crossRefBookTarget.substring(1))) {
                            linkLog.error("Cross reference pointing to non-defined ID: " + crossRefBookTarget + getContext());
                        }
                    }
                } else if (localName.equals("img")) {
                    // Sometimes images might be inserted by accident with a file: URL instead
                    // of uploading them into the Daisy repository.
                    String src = attributes.getValue("src");
                    if (src != null && src.startsWith("file:")) {
                        linkLog.error("Image refering to a file: " + src + getContext());
                    }
                }
            }
            super.startElement(uri, localName, qName, attributes);
        }

        private String getContext() {
            return " (source document: " + currentDocument + ", branch: " + currentBranch + ", language: " + currentLanguage + ")";
        }
    }

    /**
     * A handler that assigns IDs to certain elements that have no ID but need one.
     */
    private static class IdAssigner extends ForwardingContentHandler {
        private Set<String> existingIds;
        private int sectionCounter = 0;
        private int figureCounter = 0;
        private int tableCounter = 0;

        public IdAssigner(ContentHandler consumer, Set<String> existingIds) {
            super(consumer);
            this.existingIds = existingIds;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (namespaceURI.equals("")) {
                Matcher matcher = headerPattern.matcher(localName);
                String newId = null;
                if (atts.getValue("id") == null) {
                    if (matcher.matches()) {
                        newId = generateSectionId();
                    } else if (localName.equals("img") && atts.getValue("daisy-caption") != null) {
                        newId = generateFigureId();
                    } else if (localName.equals("table") && atts.getValue("daisy-caption") != null) {
                        newId = generateTableId();
                    }
                }

                if (newId != null) {
                    AttributesImpl newAttrs = new AttributesImpl(atts);
                    newAttrs.addAttribute("", "id", "id", "CDATA", newId);
                    atts = newAttrs;
                }
            }
            super.startElement(namespaceURI, localName, qName, atts);
        }

        private String generateSectionId() {
            String result = "s" + ++sectionCounter;
            while (existingIds.contains(result)) {
                result = "s" + ++sectionCounter;
            }
            return result;
        }

        private String generateFigureId() {
            String result = "dsy_fig_" + ++figureCounter;
            while (existingIds.contains(result)) {
                result = "dsy_fig_" + ++figureCounter;
            }
            return result;
        }

        private String generateTableId() {
            String result = "dsy_tbl_" + ++tableCounter;
            while (existingIds.contains(result)) {
                result = "dsy_tbl_" + ++tableCounter;
            }
            return result;
        }
    }

    static class LinkLog {
        private PrintWriter pw;

        public LinkLog(BookInstance bookInstance) throws Exception {
            OutputStream os = bookInstance.getResourceOutputStream(BookInstanceLayout.getLinkLogPath());
            pw = new PrintWriter(os);
            pw.println("If this file is empty, then no link errors were detected.");
        }

        public void dispose() {
            if (pw != null)
                pw.close();
        }

        public void error(String message) {
            pw.println(message);
            pw.flush();
        }
    }
}
