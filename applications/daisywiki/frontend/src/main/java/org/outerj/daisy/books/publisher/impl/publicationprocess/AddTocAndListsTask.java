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
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.cocoon.xml.dom.DOMStreamer;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParser;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class AddTocAndListsTask implements PublicationProcessTask {
    private final String input;
    private final String output;

    public AddTocAndListsTask(String input, String output) {
        this.input = input;
        this.output = output;
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running add toc and lists task.");
        int tocDepth = Integer.MAX_VALUE;
        String tocDepthParam = context.getProperties().get("toc.depth");
        if (tocDepthParam != null) {
            try {
                tocDepth = Integer.parseInt(tocDepthParam);
            } catch (NumberFormatException e) {
                throw new Exception("Invalid value in toc.depth property: " + tocDepthParam);
            }
        }

        // determine types of figures and tables for which to build a lists
        String listOfFiguresTypes = context.getProperties().get("list-of-figures.include-types");
        String[] figureTypes = listOfFiguresTypes != null ? parseCSV(listOfFiguresTypes) : new String[0];
        String listOfTablesTypes = context.getProperties().get("list-of-tables.include-types");
        String[] tableTypes = listOfTablesTypes != null ? parseCSV(listOfTablesTypes) : new String[0];

        String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
        String inputXmlPath = publicationOutputPath + input;
        String outputXmlPath = publicationOutputPath + output;

        // Read input document in a DOM tree
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        Document inputDocument;
        InputStream is = null;
        try {
            is = context.getBookInstance().getResource(inputXmlPath);
            inputDocument = documentBuilder.parse(is);
        } finally {
            if (is != null)
                is.close();
        }

        Document tocDocument = documentBuilder.newDocument();
        TocBuilder tocBuilder = new TocBuilder();
        tocBuilder.buildToc(inputDocument, tocDocument, tocDepth);
        DOMStreamer domStreamer = new DOMStreamer();
        SaxBuffer tocBuffer = new SaxBuffer();
        domStreamer.setContentHandler(tocBuffer);
        domStreamer.stream(tocDocument.getDocumentElement());

        ArtifactListBuilder listBuilder = new ArtifactListBuilder(figureTypes, tableTypes);
        listBuilder.build(inputDocument);

        OutputStream os = null;
        is = null;
        try {
            is = context.getBookInstance().getResource(inputXmlPath);
            os = context.getBookInstance().getResourceOutputStream(outputXmlPath);
            XmlSerializer serializer = new XmlSerializer(os);
            MergeTocAndListsHandler mergeTocAndListsHandler = new MergeTocAndListsHandler(serializer, tocBuffer, listBuilder.getFigureListBuffers(), listBuilder.getTableListBuffers());

            SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
            parser.getXMLReader().setContentHandler(mergeTocAndListsHandler);
            parser.getXMLReader().parse(new InputSource(is));
        } finally {
            if (is != null)
                is.close();
            if (os != null)
                os.close();
        }
    }

    private static String[] parseCSV(String data) {
        List<String> values = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(data, ",");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (token.length() > 0) {
                values.add(token);
            }
        }
        return values.toArray(new String[values.size()]);
    }

    // Note: TOC building is done DOM-based since that makes it easier to copy the content of
    // header elements in a correct way (the DOMStreamer will cleanup missing namespace declarations etc.)
    static class TocBuilder {
        private int currentTocLevel;
        private Document tocDocument;
        private Element currentTocElement;
        private int tocDepth;
        private static final Pattern headerPattern = Pattern.compile("h([0-9]+)");

        void buildToc(Document inputDocument, Document tocDocument, int tocDepth) throws Exception {
            Element tocElement = tocDocument.createElementNS(null, "toc");
            tocDocument.appendChild(tocElement);

            currentTocLevel = 0;
            currentTocElement = tocElement;
            this.tocDocument = tocDocument;
            this.tocDepth = tocDepth;

            buildTocRecursive(inputDocument.getDocumentElement());
        }


        private void buildTocRecursive(Element element) throws Exception {
            NodeList nodeList = element.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element)node;
                    if (childElement.getNamespaceURI() == null) {
                        Matcher matcher = headerPattern.matcher(childElement.getLocalName());
                        if (matcher.matches()) {
                            int headerLevel = Integer.parseInt(matcher.group(1));
                            if (headerLevel <= currentTocLevel + 1 && headerLevel <= tocDepth) {
                                for (int z = headerLevel; z <= currentTocLevel; z++) {
                                    currentTocElement = (Element)currentTocElement.getParentNode();
                                }
                                Element tocEntryEl =  tocDocument.createElementNS(null, "tocEntry");
                                Element captionEl = tocDocument.createElementNS(null, "caption");

                                tocEntryEl.appendChild(tocDocument.createTextNode("\n"));
                                tocEntryEl.appendChild(captionEl);
                                tocEntryEl.appendChild(tocDocument.createTextNode("\n"));

                                copyCaptionChildren(childElement, captionEl, tocDocument);

                                String targetId = childElement.getAttribute("id");
                                if (targetId.length() == 0)
                                    throw new Exception("Error during TOC generation: encountered a header without id attribute.");

                                tocEntryEl.setAttribute("targetId", targetId);
                                String sectionNumber = childElement.getAttribute("daisyNumber");
                                String sectionPartialNumber = childElement.getAttribute("daisyPartialNumber");
                                String sectionRawNumber = childElement.getAttribute("daisyRawNumber");

                                if (sectionNumber.length() > 0)
                                    tocEntryEl.setAttribute("daisyNumber", sectionNumber);
                                if (sectionPartialNumber.length() > 0)
                                    tocEntryEl.setAttribute("daisyPartialNumber", sectionPartialNumber);
                                if (sectionRawNumber.length() > 0)
                                    tocEntryEl.setAttribute("daisyRawNumber", sectionRawNumber);


                                currentTocElement.appendChild(tocEntryEl);
                                currentTocElement.appendChild(tocDocument.createTextNode("\n"));
                                currentTocLevel = headerLevel;
                                currentTocElement = tocEntryEl;
                            }
                        } else {
                            buildTocRecursive(childElement);
                        }
                    }
                }
            }
        }

        private void copyCaptionChildren(Element fromEl, Element toEl, Document toDocument) {
            NodeList children = fromEl.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                toEl.appendChild(toDocument.importNode(child, true));
            }

            // remove any footnotes or indexentries that might occur in the caption
            List<Element> elementsToBeRemoved = new ArrayList<Element>();
            collectUnwantedCaptionElement(toEl, elementsToBeRemoved);
            for (Element element : elementsToBeRemoved) {
                element.getParentNode().removeChild(element);
            }
        }

        private void collectUnwantedCaptionElement(Element element, List<Element> elementsToBeRemoved) {
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);

                // don't include footnotes and indexentries that occur inside headers in the table of contents
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    boolean remove = false;
                    Element childEl = (Element)child;
                    if (childEl.getNamespaceURI() == null && childEl.getLocalName().equals("span") && (childEl.getAttribute("class").equals("footnote") || childEl.getAttribute("class").equals("indexentry"))) {
                        remove = true;
                        elementsToBeRemoved.add(childEl);
                    }

                    if (!remove)
                        collectUnwantedCaptionElement(childEl, elementsToBeRemoved);
                }

            }
        }

    }


    static class ArtifactListBuilder {
        private SaxBuffer[] figureListBuffers;
        private SaxBuffer[] tableListBuffers;
        private String[] figureTypes;
        private String[] tableTypes;
        private static final String FIGURES_NAME = "figures";
        private static final String TABLES_NAME = "tables";

        public ArtifactListBuilder(String[] figureTypes, String[] tableTypes) {
            this.figureTypes = figureTypes;
            this.tableTypes = tableTypes;
            figureListBuffers = new SaxBuffer[figureTypes.length];
            tableListBuffers = new SaxBuffer[tableTypes.length];
        }

        public SaxBuffer[] getFigureListBuffers() {
            return figureListBuffers;
        }

        public SaxBuffer[] getTableListBuffers() {
            return tableListBuffers;
        }

        public void build(Document inputDocument) throws Exception {
            buildRecursive(inputDocument.getDocumentElement());
            closeLists(FIGURES_NAME, figureListBuffers);
            closeLists(TABLES_NAME, tableListBuffers);
        }

        public void buildRecursive(Element element) throws Exception {
            NodeList childNodes = element.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element childElement = (Element)childNode;
                    if (childNode.getNamespaceURI() == null && childElement.getLocalName().equals("img")) {
                        String caption = childElement.getAttribute("daisy-caption");
                        if (caption.length() > 0) {
                            String id = childElement.getAttribute("id");
                            if (id.length() == 0)
                                throw new Exception("Missing id attribute on image during list-of-figures building.");
                            addFigure(childElement.getAttribute("daisy-image-type"), id, caption, childElement.getAttribute("daisyNumber"), childElement.getAttribute("daisyPartialNumber"), childElement.getAttribute("daisyRawNumber"));
                        }
                    } else if (childNode.getNamespaceURI() == null && childElement.getLocalName().equals("table")) {
                        String caption = childElement.getAttribute("daisy-caption");
                        if (caption.length() > 0) {
                            String id = childElement.getAttribute("id");
                            if (id.length() == 0)
                                throw new Exception("Missing id attribute on table during list-of-tables building.");
                            addTable(childElement.getAttribute("daisy-table-type"), id, caption, childElement.getAttribute("daisyNumber"), childElement.getAttribute("daisyPartialNumber"), childElement.getAttribute("daisyRawNumber"));
                        }

                    }
                    buildRecursive(childElement);
                }
            }
        }

        private void addFigure(String type, String id, String caption, String daisyNumber, String daisyPartialNumber, String daisyRawNumber) throws SAXException {
            addItem(FIGURES_NAME, figureListBuffers, figureTypes, type, id, caption, daisyNumber, daisyPartialNumber, daisyRawNumber);
        }

        private void addTable(String type, String id, String caption, String daisyNumber, String daisyPartialNumber, String daisyRawNumber) throws SAXException {
            addItem(TABLES_NAME, tableListBuffers, tableTypes, type, id, caption, daisyNumber, daisyPartialNumber, daisyRawNumber);
        }

        private void addItem(String artifactName, SaxBuffer[] buffers, String[] types, String type, String id, String caption, String daisyNumber, String daisyPartialNumber, String daisyRawNumber) throws SAXException {
            int index = -1;
            for (int i = 0; i < types.length; i++) {
                if (types[i].equals(type))
                    index = i;
            }
            if (index == -1)
                return;

            if (buffers[index] == null) {
                buffers[index] = new SaxBuffer();
                AttributesImpl listAttrs = new AttributesImpl();
                listAttrs.addCDATAAttribute("type", type);
                String elementName = "list-of-" + artifactName;
                buffers[index].characters(new char[] {'\n'}, 0, 1);
                buffers[index].characters(new char[] {'\n'}, 0, 1);
                buffers[index].startElement("", elementName, elementName, listAttrs);
                buffers[index].characters(new char[] {'\n'}, 0, 1);
            }


            AttributesImpl attrs = new AttributesImpl();
            attrs.addCDATAAttribute("targetId", id);
            if (daisyNumber.length() > 0)
                attrs.addCDATAAttribute("daisyNumber", daisyNumber);
            if (daisyPartialNumber.length() > 0)
                attrs.addCDATAAttribute("daisyPartialNumber", daisyPartialNumber);
            if (daisyRawNumber.length() > 0)
                attrs.addCDATAAttribute("daisyRawNumber", daisyRawNumber);
            buffers[index].startElement("", "list-item", "list-item", attrs);
            buffers[index].characters(caption.toCharArray(), 0, caption.length());
            buffers[index].endElement("", "list-item", "list-item");
            buffers[index].characters(new char[] {'\n'}, 0, 1);
        }

        private void closeLists(String artifactName, SaxBuffer[] buffers) throws SAXException {
            for (SaxBuffer buffer : buffers) {
                if (buffer != null) {
                    String elementName = "list-of-" + artifactName;
                    buffer.endElement("", elementName, elementName);
                    buffer.characters(new char[]{'\n'}, 0, 1);
                }
            }
        }
    }

    static class MergeTocAndListsHandler extends ForwardingContentHandler {
        private int level = 0;
        private final SaxBuffer toc;
        private final SaxBuffer[] figureBuffers;
        private final SaxBuffer[] tableBuffers;

        public MergeTocAndListsHandler(ContentHandler consumer, SaxBuffer toc, SaxBuffer[] figureBuffers, SaxBuffer[] tableBuffers) {
            super(consumer);
            this.toc = toc;
            this.figureBuffers = figureBuffers;
            this.tableBuffers = tableBuffers;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            level++;
            super.startElement(namespaceURI, localName, qName, atts);
            if (level == 2 && namespaceURI.equals("") && localName.equals("body")) {
                toc.toSAX(consumer);
                for (SaxBuffer figureBuffer : figureBuffers) {
                    if (figureBuffer != null)
                        figureBuffer.toSAX(consumer);
                }
                for (SaxBuffer tableBuffer : tableBuffers) {
                    if (tableBuffer != null)
                        tableBuffer.toSAX(consumer);
                }
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            level--;
            super.endElement(namespaceURI, localName, qName);
        }
    }
}
