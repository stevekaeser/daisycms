/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.frontend;

import org.apache.cocoon.xml.AbstractXMLPipe;
import org.apache.cocoon.xml.XMLConsumer;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.cocoon.xml.EmbeddedXMLPipe;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.outerj.daisy.xmlutil.HtmlBodyRemovalHandler;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// SAX is a lot of fun!
//
// This handler contains some special handling of prefix mappings, in order to properly handle the
// prefix mappings of the dropped daisyPreparedInclude element.
//
// The longer explanation is as follows:
//   * we drop the p:daisyPreparedInclude element
//   * in its place, we insert multiple (0..n) elements (all the children of html/body of the inserted document)
//   * the start prefix mapping of the daisyPreparedInclude element would then be associated with
//     the startElement event of the first inserted element, which is not good, especially for the default namespace
//   * the end prefix mapping would be associated with the endElement event of the last inserted element (thus not
//     properly balanced with the start prefix mapping)
//
// Therefore, when we drop the p:daisyPreparedInclude element, we want to drop its associated
// start/endPrefixMapping events too.
//
// The associated prefix mappings are:
//  * for startElement event: those before the event
//  * for endElement event: those after the event
//
// Note that the events are not recorded in a nested manner, for the purpose of what happens here, it is best
// to look at the SAX events as a serial event stream.

/**
 * Nests included documents inside their parents.
 *
 * <p>The root document should be piped through this SAX handler, the included documents
 * will then be merged by this handler (recursively), optionally shifting headers
 * in the included documents.
 */
public class PreparedIncludeHandler extends AbstractXMLPipe {
    private static final String PUBLISHER_NAMESPACE = "http://outerx.org/daisy/1.0#publisher";
    private static final Pattern HEADING_PATTERN = Pattern.compile("^h([0-9]+)$");
    private final PreparedDocuments preparedDocuments;
    private Map<String, String> startPrefixMappings = new HashMap<String, String>();
    private Set<String> endPrefixMappings = new HashSet<String>();
    private int lastEncounteredHeadingLevel = 0;
    private boolean assumeHtmlBody;
    private boolean lastEndElementWasPreparedInclude = false;

    /**
     *
     * @param assumeHtmlBody true if the prepared documents have html/body tags or false if they are supposed
     *                       to contain an embeddable piece of XML (= books vs wiki publishing)
     */
    public PreparedIncludeHandler(XMLConsumer consumer, PreparedDocuments preparedDocuments, boolean assumeHtmlBody) {
        setConsumer(consumer);
        this.preparedDocuments = preparedDocuments;
        this.assumeHtmlBody = assumeHtmlBody;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        // keep track of current heading level
        if (namespaceURI.equals("") && localName.startsWith("h")) {
            String clazz = atts.getValue("class");
            if (localName.equals("h1") && clazz != null && clazz.indexOf("daisy-document-name") != -1) {
                lastEncounteredHeadingLevel = 0;
            } else {
                Matcher matcher = HEADING_PATTERN.matcher(localName);
                if (matcher.matches()) {
                    lastEncounteredHeadingLevel = Integer.parseInt(matcher.group(1));
                }
            }
        }

        if (namespaceURI.equals(PUBLISHER_NAMESPACE) && localName.equals("daisyPreparedInclude")) {
            int id = Integer.parseInt(atts.getValue("id"));
            PreparedDocuments.PreparedDocument preparedDocument = preparedDocuments.getPreparedDocument(id);

            // emit end prefix mappings, these belong to the last ended element
            emitEndPrefixMappings();
            // drop the start prefix mappings, these belong to the daisyPreparedInclude element which we are dropping
            startPrefixMappings.clear();

            if (preparedDocument == null) {
                outputError("Unexpected error in IncludePreparedDocumentsTransformer: missing preparedDocument: " + id);
            } else {
                String shiftHeadingsAttr = atts.getValue("shiftHeadings");
                XMLConsumer consumer = null;
                if (shiftHeadingsAttr != null) {
                    int shiftHeadingsAmount = 0;
                    boolean shiftHeadingsError = false;
                    if (shiftHeadingsAttr.equals("child")) {
                        shiftHeadingsAmount = lastEncounteredHeadingLevel + 1;
                    } else if (shiftHeadingsAttr.equals("sibling")) {
                        shiftHeadingsAmount = lastEncounteredHeadingLevel;
                    } else {
                        try {
                            shiftHeadingsAmount = Integer.parseInt(shiftHeadingsAttr);
                            if (shiftHeadingsAmount < 0) {
                                shiftHeadingsError = true;
                            }
                        } catch (NumberFormatException e) {
                            shiftHeadingsError = true;
                        }
                    }

                    if (shiftHeadingsError) {
                        outputError("Invalid shiftHeadings specification on include instruction: " + shiftHeadingsAttr);
                    } else {
                        consumer = new HeadingShifter(shiftHeadingsAmount, this);
                    }
                } else {
                    consumer = this;
                }

                if (consumer != null) {
                    int currentLastHeadingLevel = lastEncounteredHeadingLevel;
                    lastEncounteredHeadingLevel = 0;
                    ContentHandler resultHandler;
                    if (assumeHtmlBody) {
                        resultHandler = new HtmlBodyRemovalHandler(consumer);
                    } else {
                        resultHandler = new EmbeddedXMLPipe(consumer);
                    }
                    preparedDocument.getSaxBuffer().toSAX(resultHandler);
                    lastEncounteredHeadingLevel = currentLastHeadingLevel;
                }
            }
        } else {
            emitEndPrefixMappings();
            emitStartPrefixMappings();
            super.startElement(namespaceURI, localName, qName, atts);
        }
    }

    private void outputError(String message) throws SAXException {
        AttributesImpl errorAttrs = new AttributesImpl();
        errorAttrs.addCDATAAttribute("class", "daisy-error");
        this.startElement("", "p", "p", errorAttrs);
        this.characters(message.toCharArray(), 0, message.length());
        this.endElement("", "p", "p");
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        // drop lingering start prefix mapping events, they are not relevant anymore after this end element event
        startPrefixMappings.clear();

        if (namespaceURI.equals(PUBLISHER_NAMESPACE) && localName.equals("daisyPreparedInclude")) {
            // this will also work ok if we have nested PreparedIncludeHandler uses (= nested includes)
            lastEndElementWasPreparedInclude = true;
        } else {
            // if there are any end prefix mappings, they don't belong to the current endElement
            // event, so emit them first.
            emitEndPrefixMappings();
            super.endElement(namespaceURI, localName, qName);
        }
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        startPrefixMappings.put(prefix, uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (startPrefixMappings.containsKey(prefix)) {
            startPrefixMappings.remove(prefix);
        } else {
            endPrefixMappings.add(prefix);
        }
    }

    private void emitStartPrefixMappings() throws SAXException {
        for (Map.Entry<String, String> entry : startPrefixMappings.entrySet()) {
            super.startPrefixMapping(entry.getKey(), entry.getValue());
        }
        startPrefixMappings.clear();
    }

    private void emitEndPrefixMappings() throws SAXException {
        if (lastEndElementWasPreparedInclude) {
            // don't emit (drop) the end prefix mapping events, they belong to te dropped daisyPreparedInclude element
            lastEndElementWasPreparedInclude = false;
            endPrefixMappings.clear();
        } else {
            for (String prefix : endPrefixMappings) {
                super.endPrefixMapping(prefix);
            }
            endPrefixMappings.clear();
        }
    }

    private static class HeadingShifter extends AbstractXMLPipe {
        private int shiftHeadingsAmount;
        private List<EndElementInfo> endElementInfos = new ArrayList<EndElementInfo>();

        public HeadingShifter(int shiftHeadingsAmount, XMLConsumer consumer) {
            this.shiftHeadingsAmount = shiftHeadingsAmount;
            setConsumer(consumer);
        }

        public void startElement(String uri, String localName, String qName, Attributes attrs) throws SAXException {
            if (uri.equals("") && localName.startsWith("h")) {
                String clazz = attrs.getValue("class");
                if (localName.equals("h1") && clazz != null && clazz.indexOf("daisy-document-name") != -1) {
                    if (shiftHeadingsAmount > 0) {
                        localName = "h" + shiftHeadingsAmount;
                        qName = localName;
                        AttributesImpl newAttrs = new AttributesImpl(attrs);
                        newAttrs.setValue(newAttrs.getIndex("class"), removeClass(clazz, "daisy-document-name"));
                        attrs = newAttrs;
                    }
                } else {
                    Matcher matcher = HEADING_PATTERN.matcher(localName);
                    if (matcher.matches()) {
                        int currentLevel = Integer.parseInt(matcher.group(1));
                        localName = "h" + (currentLevel + shiftHeadingsAmount);
                        qName = localName;
                    }
                }
            }
            endElementInfos.add(new EndElementInfo(uri, localName, qName));
            super.startElement(uri, localName, qName, attrs);
        }

        private String removeClass(String classes, String clazz) {
            int i = classes.indexOf(clazz);
            if (i != -1) {
                return classes.substring(0, i) + classes.substring(i + clazz.length());
            } else {
                return classes;
            }
        }


        public void endElement(String uri, String loc, String raw) throws SAXException {
            EndElementInfo endElementInfo = endElementInfos.remove(endElementInfos.size() - 1);
            super.endElement(endElementInfo.uri, endElementInfo.localName, endElementInfo.qName);
        }

        private static class EndElementInfo {
            private String uri;
            private String localName;
            private String qName;

            public EndElementInfo(String uri, String localName, String qName) {
                this.uri = uri;
                this.localName = localName;
                this.qName = qName;
            }
        }
    }
}
