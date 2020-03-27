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
package org.outerj.daisy.frontend;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.cocoon.environment.Request;
import org.outerj.daisy.repository.VariantKey;

import java.util.Stack;

/**
 * Handles preparedDocuments (resulting from a publisher request). If the applyDocumentTypeStyling
 * attribute of the preparedDocuments element is true, this will apply document type specific styling
 * to the prepared documents and store the result in a request attribute (for later merging
 * by the {@link IncludePreparedDocumentsTransformer}.
 */
public class PreparedDocumentsHandler implements ContentHandler {
    private static final String PUBLISHER_NS = "http://outerx.org/daisy/1.0#publisher";
    private ContentHandler consumer;
    private int elementNesting = 0;
    private PreparedDocInfo preparedDocInfo;
    private Stack<PreparedDocInfo> preparedDocumentStack = new Stack<PreparedDocInfo>();
    private DocumentTypeSpecificStyler documentTypeSpecificStyler;
    private Request request;

    public PreparedDocumentsHandler(ContentHandler consumer, Request request, DocumentTypeSpecificStyler documentTypeSpecificStyler) {
        this.request = request;
        this.consumer = consumer;
        this.documentTypeSpecificStyler = documentTypeSpecificStyler;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        elementNesting++;

        if (namespaceURI.equals(PUBLISHER_NS) && localName.equals("preparedDocuments")
                && atts.getValue("styledResultsId") == null && "true".equals(atts.getValue("applyDocumentTypeStyling"))) {
            if (preparedDocInfo != null) {
                preparedDocumentStack.push(preparedDocInfo);
            }
            preparedDocInfo = new PreparedDocInfo();
            preparedDocInfo.displayContext = atts.getValue("displayContext");
            preparedDocInfo.prepDocsNesting = elementNesting;
        } else if (preparedDocInfo != null && preparedDocInfo.prepDocNesting == 0
                && namespaceURI.equals(PUBLISHER_NS) && localName.equals("preparedDocument")) {
            preparedDocInfo.prepDocNesting = elementNesting;
            int id = Integer.parseInt(atts.getValue("id"));
            String documentId = atts.getValue("documentId");
            long branchId = Long.parseLong(atts.getValue("branchId"));
            long languageId = Long.parseLong(atts.getValue("languageId"));
            VariantKey documentKey = new VariantKey(documentId, branchId, languageId);
            preparedDocInfo.buffer = new SaxBuffer();
            preparedDocInfo.preparedDocuments.putPreparedDocument(id, documentKey, preparedDocInfo.buffer);
        } else if (preparedDocInfo != null && preparedDocInfo.prepDocNesting > 0) {
            preparedDocInfo.buffer.startElement(namespaceURI, localName, qName, atts);
        } else if (preparedDocInfo == null) {
            consumer.startElement(namespaceURI, localName, qName, atts);
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (preparedDocInfo != null && preparedDocInfo.prepDocsNesting == elementNesting) {

            try {
                documentTypeSpecificStyler.transformPreparedDocuments(preparedDocInfo.preparedDocuments, preparedDocInfo.displayContext);
            } catch (Exception e) {
                throw new SAXException(e);
            }

            Long styledResultIdCounter = (Long)request.getAttribute("styledResultIdCounter");
            if (styledResultIdCounter == null)
                styledResultIdCounter = new Long(1);
            else
                styledResultIdCounter = new Long(styledResultIdCounter.longValue() + 1);
            request.setAttribute("styledResultIdCounter", styledResultIdCounter);
            String styledResultId = "styledResult-" + String.valueOf(styledResultIdCounter.longValue());

            request.setAttribute(styledResultId, preparedDocInfo.preparedDocuments);

            // Leave this prepared document first (required to work correctly for nested preparedDocuments)
            if (preparedDocumentStack.size() > 0) {
                preparedDocInfo = preparedDocumentStack.pop();
            } else {
                preparedDocInfo = null;
            }

            // And now generate preparedDocuemnts tag
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "styledResultsId", "styledResultsId", "CDATA", styledResultId);
            startElement(PUBLISHER_NS, "preparedDocuments", "p:preparedDocuments", attrs);
            endElement(PUBLISHER_NS, "preparedDocuments", "p:preparedDocuments");

        } else if (preparedDocInfo != null && preparedDocInfo.prepDocNesting == elementNesting) {
            preparedDocInfo.prepDocNesting = 0;
            preparedDocInfo.buffer = null;
        } else if (preparedDocInfo != null && preparedDocInfo.prepDocNesting > 0) {
            preparedDocInfo.buffer.endElement(namespaceURI, localName, qName);
        } else if (preparedDocInfo == null) {
            consumer.endElement(namespaceURI, localName, qName);
        }

        elementNesting--;
    }

    public void endDocument() throws SAXException {
        consumer.endDocument();
    }

    public void startDocument() throws SAXException {
        consumer.startDocument();
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (preparedDocInfo != null && preparedDocInfo.prepDocNesting > 0) {
            preparedDocInfo.buffer.characters(ch, start, length);
        } else if (preparedDocInfo == null) {
            consumer.characters(ch, start, length);
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        if (preparedDocInfo != null && preparedDocInfo.prepDocNesting > 0) {
            preparedDocInfo.buffer.ignorableWhitespace(ch, start, length);
        } else if (preparedDocInfo == null) {
            consumer.ignorableWhitespace(ch, start, length);
        }
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (preparedDocInfo != null && preparedDocInfo.prepDocNesting > 0) {
            preparedDocInfo.buffer.endPrefixMapping(prefix);
        } else if (preparedDocInfo == null) {
            consumer.endPrefixMapping(prefix);
        }
    }

    public void skippedEntity(String name) throws SAXException {
        if (preparedDocInfo != null && preparedDocInfo.prepDocNesting > 0) {
            preparedDocInfo.buffer.skippedEntity(name);
        } else if (preparedDocInfo == null) {
            consumer.skippedEntity(name);
        }
    }

    public void setDocumentLocator(Locator locator) {
        // ignore
    }

    public void processingInstruction(String target, String data) throws SAXException {
        if (preparedDocInfo != null && preparedDocInfo.prepDocNesting > 0) {
            preparedDocInfo.buffer.processingInstruction(target, data);
        } else if (preparedDocInfo == null) {
            consumer.processingInstruction(target, data);
        }
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (preparedDocInfo != null && preparedDocInfo.prepDocNesting > 0) {
            preparedDocInfo.buffer.startPrefixMapping(prefix, uri);
        } else if (preparedDocInfo == null) {
            consumer.startPrefixMapping(prefix, uri);
        }
    }

    static class PreparedDocInfo {
        PreparedDocuments preparedDocuments = new PreparedDocuments();
        SaxBuffer buffer;
        int prepDocNesting = 0;
        int prepDocsNesting;
        String displayContext;
    }

}
