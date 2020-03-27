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

import java.util.ArrayList;
import java.util.regex.Matcher;

import org.outerj.daisy.publisher.serverimpl.AbstractHandler;
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContext;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContextImpl;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerx.daisy.x10.DocumentDocument;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This ContentHandler handles document includes. These are recognized
 * by HTML p elements having a class attribute with value "include".
 * The content should contain either a document id, or a document id
 * and version id separated by the character '@' (eg "10@3"). By
 * default the live version is included. The special
 * version "LAST" can be specified (eg "10@LAST") to include the latest
 * version. Inclusion processing is done recursively (with detection
 * of recursive include of the same document).
 *
 * <p>The included document is not directly inlined on the location of inclusion,
 * rather a new publisher request is executed for it, and the result of this
 * is stored as a separate SaxBuffer in a {@link PreparedDocuments} instance.
 * The original include tag is replaced by a tag &lt;daisyPreparedInclude&gt; with
 * an id attribute referring the id under which it is stored in the PreparedDocuments.
 */
public class IncludesProcessor extends AbstractHandler implements ContentHandler {
    private boolean inInclude;
    private String shiftHeadingsParam;
    private StringBuilder includeBuffer;
    private SaxBuffer includeSaxBuffer;
    private int nestedElementCounter = 0;
    private int includeElementNesting;
    private ContentProcessor owner;
    private long documentBranchId = -1;
    private long documentLanguageId;

    // the below members contain the details of the include currently happening through this handler
    private String currentDocumentId;
    private long currentBranchId;
    private long currentLanguageId;
    private long currentVersionId;

    public IncludesProcessor(Document document, ContentHandler consumer, ContentProcessor owner) {
        super(consumer);
        this.owner = owner;
        this.documentBranchId = document.getBranchId();
        this.documentLanguageId = document.getLanguageId();
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        nestedElementCounter++;

        if (!inInclude) {
            if (localName.equals("pre") && namespaceURI.equals("")) {
                String clazz = atts.getValue("class");
                if (clazz != null && clazz.equals("include")) {
                    inInclude = true;
                    shiftHeadingsParam = atts.getValue("daisy-shift-headings");
                    includeBuffer = new StringBuilder();
                    includeSaxBuffer = new SaxBuffer();
                    includeElementNesting = nestedElementCounter;
                }
            }
        }

        if (!inInclude)
            consumer.startElement(namespaceURI, localName, qName, atts);
        else
            includeSaxBuffer.startElement(namespaceURI, localName, qName, atts);
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (!inInclude) {
            consumer.endElement(namespaceURI, localName, qName);
        } else {
            includeSaxBuffer.endElement(namespaceURI, localName, qName);
        }

        if (inInclude && includeElementNesting == nestedElementCounter) {
            inInclude = false;

            String url = includeBuffer.toString();
            url = url.trim();

            if (url.startsWith("daisy:")) {
                // everything after the first whitespace is ignored (can be used for info about the include)
                int spacePos = url.indexOf(' ');
                if (spacePos != -1)
                    url = url.substring(0, spacePos);
                processDaisyInclude(url);
            } else {
                // we don't handle it, so stream on the <p> element as if nothing happened
                includeSaxBuffer.toSAX(consumer);
            }
        }

        nestedElementCounter--;
    }

    /**
     * Processes an include for an URL starting with "daisy:".
     */
    private void processDaisyInclude(String url) throws SAXException {
        if (documentBranchId == -1)
            throw new SAXException("Error in " + getClass().getName() + ": documentBranchId was not yet determined when an include was encountered.");

        Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(url);
        if (!matcher.matches()) {
            outputError("Error including document: Invalid document reference: " + url);
            return;
        }

        String documentId = matcher.group(1);
        String branch = matcher.group(2);
        String language = matcher.group(3);
        String versionString = matcher.group(4);

        // The documentId must be normalized as it is used to detect recursive includes
        documentId = owner.getPublisherContext().getRepository().normalizeDocumentId(documentId);

        // If the link doesn't specify branch and language, use the branch and language
        // of the including document
        if (branch == null || branch.equals(""))
            branch = String.valueOf(documentBranchId);
        if (language == null || language.equals(""))
            language = String.valueOf(documentLanguageId);

        Document document;
        Version version = null;
        DocumentDocument documentDocument;

        try {
            document = owner.getPublisherContext().getRepository().getDocument(documentId, branch, language, false);
            if (versionString != null) {
                try {
                    VersionMode versionMode = VersionMode.get(versionString);
                    version = document.getVersion(versionMode);
                } catch (IllegalArgumentException e) {
                    try {
                        version = document.getVersion(Long.parseLong(versionString));
                    } catch (NumberFormatException nfe) {
                        outputError("Error including document: Invalid document reference: " + url);
                        return;
                    }
                }
            } else {
                version = document.getVersion(owner.getPublisherContext().getVersionMode());
            }

            if (version == null) {
                outputError("Error including document: no live version available. (document id: " + documentId + ")");
                return;
            }
            
            documentDocument = document.getXml(version.getId());
        } catch (Exception e) {
            StringBuilder error = new StringBuilder("Error including document: ");
            error.append(e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) {
                error.append(": ").append(cause.getMessage());
                cause = cause.getCause();
            }
            outputError(error.toString());
            return;
        }

        currentDocumentId = documentId;
        currentBranchId = documentDocument.getDocument().getBranchId();
        currentLanguageId = documentDocument.getDocument().getLanguageId();
        currentVersionId = documentDocument.getDocument().getDataVersionId();

        String recursionDescription = detectRecursiveInclude();
        if (recursionDescription != null) {
            outputError("Error including document: recursive include detected: " + recursionDescription);
        } else {
            PublisherContext publisherContext = owner.getPublisherContext();
            PreparedDocuments preparedDocuments = publisherContext.getPreparedDocuments();
            if (preparedDocuments == null)
                throw new SAXException("Unexpected error: preparedDocuments is not available in IncludesProcessor.");
            PublisherImpl publisher = publisherContext.getPublisher();
            PublisherContextImpl childPublisherContext = new PublisherContextImpl(publisherContext);
            childPublisherContext.setContentProcessor(owner);
            childPublisherContext.setDocumentVariant(document.getId(), document.getBranchId(), document.getLanguageId());
            childPublisherContext.setVersionId(version.getId());
            childPublisherContext.setVersionMode(publisherContext.getVersionMode());

            PreparedDocuments.PreparedDocument preparedDocument = preparedDocuments.getNewPreparedDocument(document.getVariantKey());
            try {
                publisher.performRequest(preparedDocuments.getPubReqSet(), document, version, childPublisherContext,
                        new StripDocumentHandler(preparedDocument.getSaxBuffer()));
            } catch (Exception e) {
                throw new SAXException(e);
            }

            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "id", "id", "CDATA", String.valueOf(preparedDocument.getId()));
            if (shiftHeadingsParam != null && shiftHeadingsParam.length() > 0) {
                attrs.addAttribute("", "shiftHeadings", "shiftHeadings", "CDATA", shiftHeadingsParam);
            }
            consumer.startPrefixMapping("p", PublisherImpl.NAMESPACE);
            consumer.startElement(PublisherImpl.NAMESPACE, "daisyPreparedInclude", "p:daisyPreparedInclude", attrs);
            consumer.endElement(PublisherImpl.NAMESPACE, "daisyPreparedInclude", "p:daisyPreparedInclude");
            consumer.endPrefixMapping("p");
        }

        currentDocumentId = null;
        currentBranchId = -1;
        currentLanguageId = -1;
        currentVersionId = -1;
    }

    /**
     * Detects recursive includes, if none is found null is returned, otherwise
     * a description of the documents/versions/parts causing the recursive include.
     */
    private String detectRecursiveInclude() {
        ArrayList<IncludesProcessor> parents = new ArrayList<IncludesProcessor>();
        parents.add(this);

        ContentProcessor cpParent = owner.getParent();
        while (cpParent != null) {
            IncludesProcessor parent = cpParent.getIncludesProcessor();
            parents.add(parent);

            if (this.currentDocumentId.equals(parent.currentDocumentId)
                    && parent.currentBranchId == this.currentBranchId
                    && parent.currentLanguageId == this.currentLanguageId
                    && parent.currentVersionId == this.currentVersionId) {

                // generate description of recursive include
                StringBuilder recursionDescription = new StringBuilder();
                for (int i = parents.size() - 1; i >=0; i--) {
                    IncludesProcessor current = parents.get(i);
                    if (recursionDescription.length() > 0)
                        recursionDescription.append(" -> ");
                    recursionDescription.append("document ").append(current.currentDocumentId).append(", branch ").append(current.currentBranchId).append(", language ").append(current.currentLanguageId).append(", version ").append(current.currentVersionId);
                }

                return recursionDescription.toString();
            }
            cpParent = cpParent.getParent();
        }
        return null;
    }

    public void endDocument() throws SAXException {
        if (!inInclude)
            consumer.endDocument();
        else
            includeSaxBuffer.endDocument();
    }

    public void startDocument() throws SAXException {
        if (!inInclude)
            consumer.startDocument();
        else
            includeSaxBuffer.startDocument();
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (inInclude) {
            includeBuffer.append(ch, start, length);
            includeSaxBuffer.characters(ch, start, length);
        } else {
            consumer.characters(ch, start, length);
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        if (!inInclude)
            consumer.ignorableWhitespace(ch, start, length);
        else
            includeSaxBuffer.characters(ch, start, length);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (!inInclude)
            consumer.endPrefixMapping(prefix);
        else
            includeSaxBuffer.endPrefixMapping(prefix);
    }

    public void skippedEntity(String name) throws SAXException {
        if (!inInclude)
            consumer.skippedEntity(name);
        else
            includeSaxBuffer.skippedEntity(name);
    }

    public void setDocumentLocator(Locator locator) {
        if (!inInclude)
            consumer.setDocumentLocator(locator);
        else
            includeSaxBuffer.setDocumentLocator(locator);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        if (!inInclude)
            consumer.processingInstruction(target, data);
        else
            includeSaxBuffer.processingInstruction(target, data);
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (!inInclude)
            consumer.startPrefixMapping(prefix, uri);
        else
            includeSaxBuffer.startPrefixMapping(prefix, uri);
    }

}
