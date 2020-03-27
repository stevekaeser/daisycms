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
package org.outerj.daisy.xmlutil;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.OutputKeys;
import java.util.*;
import java.io.StringWriter;
import java.io.OutputStream;

public class XmlSerializer implements ContentHandler {
    private static boolean needsNamespaceFix = false;
    private static boolean needsNamespaceFixInitialized = false;
    private static final String XML_NAMESPACE_URI = "http://www.w3.org/XML/1998/namespace";

    private ContentHandler nextHandler;

    public XmlSerializer(OutputStream outputStream) throws Exception {
        this(outputStream, null);
    }

    public XmlSerializer(OutputStream outputStream, Properties properties) throws Exception {
        if (!needsNamespaceFixInitialized) {
            synchronized(this) {
                // PS: I know that double checking does not work reliably, but
                // that's not important here. It can't do harm if this is executed
                // multiple times.
                if (!needsNamespaceFixInitialized) {
                    needsNamespaceFix = needsNamespacesAsAttributes();
                    needsNamespaceFixInitialized = true;
                }
            }
        }

        TransformerHandler serializer = getTransformerHandler();
        serializer.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
        if (properties != null)
            serializer.getTransformer().setOutputProperties(properties);
        serializer.setResult(new StreamResult(outputStream));

        if (needsNamespaceFix) {
            nextHandler = new NamespaceAsAttributes(serializer);
        } else {
            nextHandler = serializer;
        }
    }

    private TransformerHandler getTransformerHandler() throws TransformerConfigurationException {
        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        return transformerFactory.newTransformerHandler();
    }


    /**
     * Checks if the used Trax implementation correctly handles namespaces set using
     * <code>startPrefixMapping()</code>, but wants them also as 'xmlns:' attributes.
     * <p>
     * The check consists in sending SAX events representing a minimal namespaced document
     * with namespaces defined only with calls to <code>startPrefixMapping</code> (no
     * xmlns:xxx attributes) and check if they are present in the resulting text.
     *
     * <p>THIS METHOD IS COPIED FROM COCOON'S AbstractTextSerializer with some modifications
     */
    private boolean needsNamespacesAsAttributes() throws Exception {

            // Serialize a minimal document to check how namespaces are handled.
            StringWriter writer = new StringWriter();

            String uri = "namespaceuri";
            String prefix = "nsp";
            String check = "xmlns:" + prefix + "='" + uri + "'";

            TransformerHandler handler = getTransformerHandler();

            handler.setResult(new StreamResult(writer));

            // Output a single element
            handler.startDocument();
            handler.startPrefixMapping(prefix, uri);
            handler.startElement(uri, "element", "", new AttributesImpl());
            handler.endPrefixMapping(prefix);
            handler.endDocument();

            String text = writer.toString();

            // Check if the namespace is there (replace " by ' to be sure of what we search in)
            boolean needsIt = (text.replace('"', '\'').indexOf(check) == -1);

            // String msg = needsIt ? " needs namespace attributes (will be slower)." : " handles correctly namespaces.";
            // logger.debug("Trax handler " + handler.getClass().getName() + msg);

            return needsIt;
    }

    /**
     * A pipe that ensures that all namespace prefixes are also present as
     * 'xmlns:' attributes. This used to circumvent Xalan's serialization behaviour
     * which is to ignore namespaces if they're not present as 'xmlns:xxx' attributes.
     *
     * <p>THIS CLASS IS COPIED FROM COCOON'S AbstractTextSerializer with some
     * modifications
     */
    public class NamespaceAsAttributes implements ContentHandler {

        /**
         * The prefixes of startPreficMapping() declarations for the coming element.
         */
        private List prefixList = new ArrayList();

        /**
         * The URIs of startPrefixMapping() declarations for the coming element.
         */
        private List uriList = new ArrayList();

        /**
         * Maps of URI<->prefix mappings. Used to work around a bug in the Xalan
         * serializer.
         */
        private Map uriToPrefixMap = new HashMap();
        private Map prefixToUriMap = new HashMap();

        /**
         * True if there has been some startPrefixMapping() for the coming element.
         */
        private boolean hasMappings = false;

        private ContentHandler nextHandler;

        public NamespaceAsAttributes(ContentHandler nextHandler) {
            this.nextHandler = nextHandler;
        }

        public void startDocument() throws SAXException {
            // Cleanup
            this.uriToPrefixMap.clear();
            this.prefixToUriMap.clear();
            clearMappings();
            nextHandler.startDocument();
        }

        /**
         * Track mappings to be able to add <code>xmlns:</code> attributes
         * in <code>startElement()</code>.
         */
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            // Store the mappings to reconstitute xmlns:attributes
            // except prefixes starting with "xml": these are reserved
            // VG: (uri != null) fixes NPE in startElement
            if (uri != null && !prefix.startsWith("xml") && !this.prefixList.contains(prefix)) {
                this.hasMappings = true;
                this.prefixList.add(prefix);
                this.uriList.add(uri);

                // append the prefix colon now, in order to save concatenations later, but
                // only for non-empty prefixes.
                if (prefix.length() > 0) {
                    this.uriToPrefixMap.put(uri, prefix + ":");
                } else {
                    this.uriToPrefixMap.put(uri, prefix);
                }

                this.prefixToUriMap.put(prefix, uri);
            }
            nextHandler.startPrefixMapping(prefix, uri);
        }

        /**
         * Ensure all namespace declarations are present as <code>xmlns:</code> attributes
         * and add those needed before calling superclass. This is a workaround for a Xalan bug
         * (at least in version 2.0.1) : <code>org.apache.xalan.serialize.SerializerToXML</code>
         * ignores <code>start/endPrefixMapping()</code>.
         */
        public void startElement(String eltUri, String eltLocalName, String eltQName, Attributes attrs)
                throws SAXException {

            // try to restore the qName. The map already contains the colon
            if (null != eltUri && eltUri.length() != 0 && this.uriToPrefixMap.containsKey(eltUri)) {
                eltQName = this.uriToPrefixMap.get(eltUri) + eltLocalName;
            }
            if (this.hasMappings) {
                // Add xmlns* attributes where needed

                // New Attributes if we have to add some.
                AttributesImpl newAttrs = null;

                int mappingCount = this.prefixList.size();
                int attrCount = attrs.getLength();

                for (int mapping = 0; mapping < mappingCount; mapping++) {

                    // Build infos for this namespace
                    String uri = (String) this.uriList.get(mapping);
                    String prefix = (String) this.prefixList.get(mapping);
                    String qName = prefix.equals("") ? "xmlns" : ("xmlns:" + prefix);

                    // Search for the corresponding xmlns* attribute
                    boolean found = false;
                    for (int attr = 0; attr < attrCount; attr++) {
                        if (qName.equals(attrs.getQName(attr))) {
                            // Check if mapping and attribute URI match
                            if (!uri.equals(attrs.getValue(attr))) {
                                System.err.println("XML serializer: URI in prefix mapping and attribute do not match : '"
                                                  + uri + "' - '" + attrs.getURI(attr) + "'");
                                throw new SAXException("URI in prefix mapping and attribute do not match");
                            }
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        // Need to add this namespace
                        if (newAttrs == null) {
                            // Need to test if attrs is empty or we go into an infinite loop...
                            // Well know SAX bug which I spent 3 hours to remind of :-(
                            if (attrCount == 0) {
                                newAttrs = new AttributesImpl();
                            } else {
                                newAttrs = new AttributesImpl(attrs);
                            }
                        }

                        if (prefix.equals("")) {
                            newAttrs.addAttribute(XML_NAMESPACE_URI, "xmlns", "xmlns", "CDATA", uri);
                        } else {
                            newAttrs.addAttribute(XML_NAMESPACE_URI, prefix, qName, "CDATA", uri);
                        }
                    }
                } // end for mapping

                // Cleanup for the next element
                clearMappings();

                // Start element with new attributes, if any
                nextHandler.startElement(eltUri, eltLocalName, eltQName, newAttrs == null ? attrs : newAttrs);
            } else {
                // Normal job
                nextHandler.startElement(eltUri, eltLocalName, eltQName, attrs);
            }
        }


        /**
         * Receive notification of the end of an element.
         * Try to restore the element qName.
         */
        public void endElement(String eltUri, String eltLocalName, String eltQName) throws SAXException {
            // try to restore the qName. The map already contains the colon
            if (null != eltUri && eltUri.length() != 0 && this.uriToPrefixMap.containsKey(eltUri)) {
                eltQName = this.uriToPrefixMap.get(eltUri) + eltLocalName;
            }
            nextHandler.endElement(eltUri, eltLocalName, eltQName);
        }

        /**
         * End the scope of a prefix-URI mapping:
         * remove entry from mapping tables.
         */
        public void endPrefixMapping(String prefix) throws SAXException {
            // remove mappings for xalan-bug-workaround.
            // Unfortunately, we're not passed the uri, but the prefix here,
            // so we need to maintain maps in both directions.
            if (this.prefixToUriMap.containsKey(prefix)) {
                this.uriToPrefixMap.remove(this.prefixToUriMap.get(prefix));
                this.prefixToUriMap.remove(prefix);
            }

            if (hasMappings) {
                // most of the time, start/endPrefixMapping calls have an element event between them,
                // which will clear the hasMapping flag and so this code will only be executed in the
                // rather rare occasion when there are start/endPrefixMapping calls with no element
                // event in between. If we wouldn't remove the items from the prefixList and uriList here,
                // the namespace would be incorrectly declared on the next element following the
                // endPrefixMapping call.
                int pos = prefixList.lastIndexOf(prefix);
                if (pos != -1) {
                    prefixList.remove(pos);
                    uriList.remove(pos);
                }
            }

            nextHandler.endPrefixMapping(prefix);
        }

        /**
         *
         */
        public void endDocument() throws SAXException {
            // Cleanup
            this.uriToPrefixMap.clear();
            this.prefixToUriMap.clear();
            clearMappings();
            nextHandler.endDocument();
        }

        private void clearMappings() {
            this.hasMappings = false;
            this.prefixList.clear();
            this.uriList.clear();
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            nextHandler.characters(ch, start, length);
        }

        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
            nextHandler.ignorableWhitespace(ch, start, length);
        }

        public void skippedEntity(String name) throws SAXException {
            nextHandler.skippedEntity(name);
        }

        public void setDocumentLocator(Locator locator) {
            nextHandler.setDocumentLocator(locator);
        }

        public void processingInstruction(String target, String data) throws SAXException {
            nextHandler.processingInstruction(target, data);
        }
    }

    public void endDocument() throws SAXException {
        nextHandler.endDocument();
    }

    public void startDocument () throws SAXException {
        nextHandler.startDocument();
    }

    public void characters (char ch[], int start, int length) throws SAXException {
        nextHandler.characters(ch, start, length);
    }

    public void ignorableWhitespace (char ch[], int start, int length) throws SAXException {
        nextHandler.ignorableWhitespace(ch, start, length);
    }

    public void endPrefixMapping (String prefix) throws SAXException {
        nextHandler.endPrefixMapping(prefix);
    }

    public void skippedEntity (String name) throws SAXException {
        nextHandler.skippedEntity(name);
    }

    public void setDocumentLocator (Locator locator) {
        nextHandler.setDocumentLocator(locator);
    }

    public void processingInstruction (String target, String data) throws SAXException {
        nextHandler.processingInstruction(target, data);
    }

    public void startPrefixMapping (String prefix, String uri) throws SAXException {
        nextHandler.startPrefixMapping(prefix, uri);
    }

    public void endElement (String namespaceURI, String localName, String qName) throws SAXException {
        nextHandler.endElement(namespaceURI, localName, qName);
    }

    public void startElement (String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        nextHandler.startElement(namespaceURI, localName, qName, atts);
    }
}
