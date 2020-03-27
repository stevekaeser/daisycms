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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.outerj.daisy.publisher.serverimpl.AbstractHandler;

public class AnnotateDocumentHandler extends AbstractHandler {
    private PublisherContext publisherContext;
    private DocumentRequest documentRequest;
    private String elementName;
    private String elementNamespace;

    public AnnotateDocumentHandler(DocumentRequest documentRequest, String elementName, String elementNamespace,
            ContentHandler consumer, PublisherContext publisherContext) {
        super(consumer);
        this.publisherContext = publisherContext;
        this.documentRequest = documentRequest;
        this.elementName = elementName;
        this.elementNamespace = elementNamespace;
    }

    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        super.startElement(uri, localName, qName, atts);
        if (localName.equals(elementName) && uri.equals(elementNamespace)) {
            PublisherContextImpl childPublisherContext = new PublisherContextImpl(publisherContext);
            String documentId = atts.getValue("documentId");
            long branchId = Long.parseLong(atts.getValue("branchId"));
            long languageId = Long.parseLong(atts.getValue("languageId"));
            childPublisherContext.setDocumentVariant(documentId, branchId, languageId);
            try {
                documentRequest.process(consumer, childPublisherContext);
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
    }
}
