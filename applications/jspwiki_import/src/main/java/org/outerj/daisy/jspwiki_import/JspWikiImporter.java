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
package org.outerj.daisy.jspwiki_import;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.xerces.parsers.DOMParser;
import org.cyberneko.html.HTMLConfiguration;
import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.outerj.daisy.htmlcleaner.HtmlCleanerFactory;
import org.outerj.daisy.htmlcleaner.HtmlCleanerTemplate;
import org.outerj.daisy.htmlcleaner.HtmlCleaner;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;

import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.sax.SAXResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.util.*;
import java.io.*;
import java.net.URLDecoder;

/**
 * Standalone app to import contents of a JSP Wiki into daisy. Currently
 * only written with the purpose of importing the Cocoon Wiki content to
 * have some meaningful, and meaningful-sized testdata.
 *
 * <p>The import runs in two passes: first all wiki pages are imported
 * into daisy, then links are translated from wiki page names to daisy
 * document ids.
 *
 * <p>To run, after maven build, execute target/runimport.sh.
 *
 * <p>To make this usable as a generic utility, at least the hardcoded
 * wiki location and daisy username, collection and url should be specifiable
 * using command line parameters.
 *
 */
public class JspWikiImporter {
    private String wikiPageURL = "http://wiki.daisycms.org/Wiki.jsp?page=";
    private String collectionName = "cocoon";
    private String daisyUser = "jspwiki-import";
    private String daisyPassword = "topsecret";
    private HashSet allPageNames = new HashSet();
    private DocumentBuilder documentBuilder;
    private HtmlCleanerTemplate htmlCleanerTemplate;
    private SAXTransformerFactory transformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
    private Repository repository;
    private HashMap importPages = new HashMap();
    private HashMap importedImages = new HashMap();
    private HashMap importedAttachments = new HashMap();
    private DocumentCollection collection;
    private static HashSet skipPages = new HashSet();
    static {
        skipPages.add("UndefinedPages");
        skipPages.add("UnusedPages");
        skipPages.add("IndexPage");
        skipPages.add("RecentChanges");
        skipPages.add("FullRecentChanges");
    }

    public static void main(String[] args) throws Exception {
        new JspWikiImporter().run();
    }

    public void run() throws Exception {
        // initialize some stuff
        System.out.println("Doing preparations...");
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        File htmlCleanerConfig = new File("../daisywiki/frontend/src/cocoon/webapp/daisy/resources/conf/htmlcleaner.xml");
        htmlCleanerTemplate = new HtmlCleanerFactory().buildTemplate(new InputSource(new FileInputStream(htmlCleanerConfig)));

        // connect to daisy
        System.out.println("Connecting to daisy...");
        Credentials credentials = new Credentials(daisyUser, daisyPassword);
        RepositoryManager repositoryManager = new RemoteRepositoryManager("http://localhost:9263", credentials);
        repository = repositoryManager.getRepository(credentials);
        collection = repository.getCollectionManager().getCollectionByName(collectionName, false);

        // load wiki page names
        System.out.println("Fetching list of all pages on the wiki...");
        loadPageNames();
        System.out.println(allPageNames.size() + " pages found on the wiki.");
        System.out.println();

        String[] pages = (String[])allPageNames.toArray(new String[allPageNames.size()]);
        for (int i = 0; i < pages.length; i++) {
            if (pages[i].startsWith("Wyona")) {
                System.out.println("Skipping page " + pages[i]);
            } else if (skipPages.contains(pages[i])) {
                System.out.println("Skipping page " + pages[i]);
            } else {
                System.out.println("Fetching page " + pages[i] + "... (" + i + " of " + pages.length + ")");
                byte[] pageData = fetchPage(pages[i]);

                System.out.println("Parsing and cleaning HTML...");
                org.w3c.dom.Document pageDocument = parseHtml(pageData);
                DOMXPath xpath = new DOMXPath("//div[@class='content']");
                Element contentDiv = (Element)xpath.selectSingleNode(pageDocument);
                if (contentDiv == null)
                    throw new Exception("No content found in page " + pages[i]);
                String contentData = serialize(contentDivToDoc(contentDiv));
                byte[] cleanedContent = clean(contentData);

                System.out.println("Storing page in Daisy...");
                Document document = repository.createDocument(pages[i], "SimpleDocument");
                document.setPart("SimpleDocumentContent", "text/xml", cleanedContent);
                document.addToCollection(collection);
                document.save();
                importPages.put(pages[i], new Long(document.getId()));
                System.out.println("Done\n");
            }
        }

        System.out.println("\n\nWILL NOW START LINK TRANSLATION\n\n");

        Iterator importPagesIt = importPages.entrySet().iterator();
        while (importPagesIt.hasNext()) {
            Map.Entry entry = (Map.Entry)importPagesIt.next();
            String pageName = (String)entry.getKey();
            long pageId = ((Long)entry.getValue()).longValue();

            System.out.println("Translating links for document " + pageName + "...");
            Document document = repository.getDocument(pageId, true);
            byte[] pageData = document.getPart("SimpleDocumentContent").getData();
            byte[] newData = clean(translateLinks(pageData));
            document.setPart("SimpleDocumentContent", "text/xml", newData);
            document.save();
            System.out.println("Done\n");
        }

    }

    private byte[] clean(String htmlData) throws Exception {
        HtmlCleaner cleaner = htmlCleanerTemplate.newHtmlCleaner();
        return cleaner.cleanToByteArray(htmlData);
    }

    private org.w3c.dom.Document contentDivToDoc(Element contentDiv) {
        org.w3c.dom.Document doc = documentBuilder.newDocument();
        Element htmlEl = doc.createElementNS(null, "html");
        doc.appendChild(htmlEl);
        Element bodyEl = doc.createElementNS(null, "body");
        htmlEl.appendChild(bodyEl);
        NodeList childNodes = contentDiv.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            boolean append = true;
            if (node instanceof Element && node.getLocalName().equals("h1")) {
                Element divEl = (Element)node;
                if (divEl.getAttribute("class").equals("pagename")) {
                    append = false;
                }
            } else if (node instanceof Element && node.getLocalName().equals("div")) {
                Element divEl = (Element)node;
                // detect end of content by presence of a div with class bottom.
                if (divEl.getAttribute("class").equals("bottom")) {
                    return doc;
                }
            }
            if (append)
                bodyEl.appendChild(doc.importNode(node, true));
        }
        return doc;
    }

    private String serialize(org.w3c.dom.Document doc) throws Exception {
        TransformerHandler serializer = transformerFactory.newTransformerHandler();
        StringWriter writer = new StringWriter();
        serializer.setResult(new StreamResult(writer));

        Transformer streamer = transformerFactory.newTransformer();
        streamer.transform(new DOMSource(doc), new SAXResult(new ExtraCleanup(serializer)));
        return writer.toString();
    }

    private void loadPageNames() throws Exception {
        byte[] indexPageData = fetchPage("IndexPage");
        org.w3c.dom.Document document = parseHtml(indexPageData);
        DOMXPath xpath = new DOMXPath("//a[@class='wikipage']");
        List nodes = xpath.selectNodes(document);
        Iterator nodesIt = nodes.iterator();
        while (nodesIt.hasNext()) {
            Element element = (Element)nodesIt.next();
            String href = element.getAttribute("href");
            if (href.startsWith(wikiPageURL))
                allPageNames.add(href.substring(wikiPageURL.length()));
        }
    }

    private byte[] fetchPage(String pageName) throws Exception {
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(wikiPageURL + pageName);
        int status = client.executeMethod(method);
        if (status != HttpStatus.SC_OK)
            throw new Exception("Problem retrieving wiki page " + pageName + " : " + method.getStatusCode() + " : " + HttpStatus.getStatusText(method.getStatusCode()));
        return method.getResponseBody();
    }

    private org.w3c.dom.Document parseHtml(byte[] data) throws Exception {
        DOMParser parser = new DOMParser(new HTMLConfiguration());
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser.setFeature("http://cyberneko.org/html/features/override-namespaces", false);
        parser.setFeature("http://cyberneko.org/html/features/insert-namespaces", false);
        parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
        parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "lower");

        parser.parse(new InputSource(new ByteArrayInputStream(data)));
        return parser.getDocument();
    }

    private String translateLinks(byte[] data) throws Exception {
        TransformerHandler serializer = transformerFactory.newTransformerHandler();
        StringWriter writer = new StringWriter();
        serializer.setResult(new StreamResult(writer));

        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        SAXParser parser = parserFactory.newSAXParser();
        parser.getXMLReader().setContentHandler(new LinkTranslator(serializer));
        parser.getXMLReader().parse(new InputSource(new ByteArrayInputStream(data)));

        return writer.toString();
    }

    class AbstractTransformer implements ContentHandler {
        protected ContentHandler consumer;

        public AbstractTransformer(ContentHandler consumer) {
            this.consumer = consumer;
        }

        public void endDocument()
        throws SAXException {
            consumer.endDocument();
        }

        public void startDocument ()
        throws SAXException {
            consumer.startDocument();
        }

        public void characters (char ch[], int start, int length)
        throws SAXException {
            consumer.characters(ch, start, length);
        }

        public void ignorableWhitespace (char ch[], int start, int length)
        throws SAXException {
            consumer.ignorableWhitespace(ch, start, length);
        }

        public void endPrefixMapping (String prefix)
        throws SAXException {
            consumer.endPrefixMapping(prefix);
        }

        public void skippedEntity (String name)
        throws SAXException {
            consumer.skippedEntity(name);
        }

        public void setDocumentLocator (Locator locator) {
            consumer.setDocumentLocator(locator);
        }

        public void processingInstruction (String target, String data)
        throws SAXException {
            consumer.processingInstruction(target, data);
        }

        public void startPrefixMapping (String prefix, String uri)
        throws SAXException {
            consumer.startPrefixMapping(prefix, uri);
        }

        public void endElement (String namespaceURI, String localName,
                    String qName)
        throws SAXException {
            consumer.endElement(namespaceURI, localName, qName);
        }

        public void startElement (String namespaceURI, String localName,
                      String qName, Attributes atts)
        throws SAXException {
            consumer.startElement(namespaceURI, localName, qName, atts);
        }
    }

    class LinkTranslator extends AbstractTransformer {

        public LinkTranslator(ContentHandler consumer) {
            super(consumer);
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (uri.equals("") && localName.equals("a")) {
                int index = attributes.getIndex("href");
                String href = (index != -1 ? attributes.getValue(index) : null);
                if (href != null && href.startsWith(wikiPageURL)) {
                    String linkedPage = href.substring(wikiPageURL.length());
                    Long linkedPageId = (Long)importPages.get(linkedPage);
                    System.out.println("attempt translation of " + linkedPage + " to " + linkedPageId);
                    if (linkedPageId != null) {
                        AttributesImpl newAttrs = new AttributesImpl(attributes);
                        newAttrs.setAttribute(newAttrs.getIndex("href"), "", "href", "href", "CDATA", "daisy:" + linkedPageId.longValue());
                        attributes = newAttrs;
                    }
                }
            }
            consumer.startElement(uri, localName, qName, attributes);
        }
    }

    class ExtraCleanup extends AbstractTransformer {
        private boolean dropNextImgEndTag = false;

        public ExtraCleanup(ContentHandler consumer) {
            super(consumer);
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (namespaceURI.equals("") && localName.equals("img") && ("http://wiki.daisycms.org/images/out.png".equals(atts.getValue("src")) || "images/attachment_small.png".equals(atts.getValue("src")))) {
                dropNextImgEndTag = true;
                // skip element
            } else if (namespaceURI.equals("") && localName.equals("img")) {
                String src = atts.getValue("src");
                if (src != null) {
                    if (importedImages.containsKey(src)) {
                        AttributesImpl newAttrs = new AttributesImpl();
                        newAttrs.addAttribute("", "src", "src", "CDATA", "daisy:" + importedImages.get(src));
                    } else {
                        try {
                            HttpClient client = new HttpClient();
                            HttpMethod method = new GetMethod(src);
                            int status = client.executeMethod(method);
                            if (status >= 300 && status < 400) {
                                method = new GetMethod(method.getResponseHeader("location").getValue());
                                status = client.executeMethod(method);
                            }
                            if (status != HttpStatus.SC_OK)
                                throw new Exception("Problem retrieving image " + src + " : " + method.getStatusCode() + " : " + HttpStatus.getStatusText(method.getStatusCode()));
                            byte[] data = method.getResponseBody();
                            String name = getImageName(src);
                            Document imageDocument = repository.createDocument(name, "Image");
                            imageDocument.setPart("ImageData", method.getResponseHeader("Content-Type").getValue(), data);
                            imageDocument.addToCollection(collection);
                            imageDocument.save();
                            importedImages.put(src, String.valueOf(imageDocument.getId()));
                            AttributesImpl newAttrs = new AttributesImpl();
                            newAttrs.addAttribute("", "src", "src", "CDATA", "daisy:" + imageDocument.getId());
                            super.startElement("", "img", "img", newAttrs);
                            System.out.println("Imported image " + src + " as " + name);
                        } catch (Exception e) {
                            throw new SAXException("Error getting image " + src, e);
                        }
                    }
                }
            } else if (namespaceURI.equals("") && localName.equals("a") && "attachment".equals(atts.getValue("class"))) {
                String src = atts.getValue("href");
                String decodedSrc = null;
                try {
                    decodedSrc = URLDecoder.decode(src, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new SAXException(e);
                }
                if (importedAttachments.containsKey(src)) {
                    AttributesImpl newAttrs = new AttributesImpl();
                    newAttrs.addAttribute("", "src", "src", "CDATA", "daisy:" + importedAttachments.get(src));
                } else {
                    try {
                        HttpClient client = new HttpClient();
                        HttpMethod method = new GetMethod(src);
                        int status = client.executeMethod(method);
                        if (status != HttpStatus.SC_OK)
                            throw new Exception("Problem retrieving attachment " + src + " : " + method.getStatusCode() + " : " + HttpStatus.getStatusText(method.getStatusCode()));
                        byte[] data = method.getResponseBody();
                        String name = getImageName(decodedSrc);
                        Document attachmentDocument = repository.createDocument(name, "Attachment");
                        attachmentDocument.setPart("AttachmentData", method.getResponseHeader("Content-Type").getValue(), data);
                        attachmentDocument.addToCollection(collection);
                        attachmentDocument.save();
                        importedAttachments.put(src, String.valueOf(attachmentDocument.getId()));
                        AttributesImpl newAttrs = new AttributesImpl();
                        newAttrs.addAttribute("", "href", "href", "CDATA", "daisy:" + attachmentDocument.getId());
                        super.startElement("", "a", "a", newAttrs);
                        System.out.println("Imported attachment " + src + " as " + name);
                    } catch (Exception e) {
                        throw new SAXException("Error getting attachment " + src, e);
                    }
                }
            } else {
                super.startElement(namespaceURI, localName, qName, atts);
            }
        }

        private String getImageName(String src) {
            String name = src.substring(src.lastIndexOf('/') + 1);
            int dotpos = name.lastIndexOf('.');
            if (dotpos != -1) {
                name = name.substring(0, dotpos);
            }
            return name;
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (dropNextImgEndTag && namespaceURI.equals("") && localName.equals("img")) {
                // skip
                dropNextImgEndTag = false;
                // note that this code assumes img elements are never nested.
            } else {
                super.endElement(namespaceURI, localName, qName);
            }
        }
    }
}
