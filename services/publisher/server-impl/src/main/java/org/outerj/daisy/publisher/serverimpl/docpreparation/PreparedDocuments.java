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
import java.util.List;
import java.util.Map;

import org.outerj.daisy.publisher.serverimpl.PublisherImpl;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * An object containing a 'prepared' copy of a document and of all
 * the documents it includes, 'prepared' meaning that it includes
 * the content of HTML parts, with queries executed, includes processed, ...
 *
 * <p>This object also serves as a place for other contextual/config
 * information related to the prepared documents processing.
 */
public class PreparedDocuments {
    private List<PreparedDocument> preparedDocuments = new ArrayList<PreparedDocument>();
    private final VariantKey navigationDoc;
    private final String pubReqSet;
    private final boolean doDiff;
	private final String diffList;

    public PreparedDocuments(VariantKey navigationDoc, String pubReqSet, boolean doDiff, String diffList) {
        this.navigationDoc = navigationDoc;
        this.pubReqSet = pubReqSet;
        this.doDiff = doDiff;
        this.diffList = diffList;
    }

    public PreparedDocument getNewPreparedDocument(VariantKey document) {
        int id = preparedDocuments.size() + 1;
        PreparedDocument preparedDocument = new PreparedDocument(new SaxBuffer(), id, document);
        preparedDocuments.add(preparedDocument);
        return preparedDocument;
    }

    public static class PreparedDocument {
        private VariantKey documentKey;
        private SaxBuffer saxBuffer;
        private int id;

        public PreparedDocument(SaxBuffer saxBuffer, int id, VariantKey documentKey) {
            this.saxBuffer = saxBuffer;
            this.id = id;
            this.documentKey = documentKey;
        }

        public SaxBuffer getSaxBuffer() {
            return saxBuffer;
        }

        public int getId() {
            return id;
        }

        public VariantKey getDocumentKey() {
            return documentKey;
        }
    }

    public void generateSax(ContentHandler contentHandler, Map<String, String> params) throws SAXException {
        AttributesImpl rootAttrs = new AttributesImpl();
        for (Map.Entry<String, String> param : params.entrySet())
            rootAttrs.addAttribute("", param.getKey(), param.getKey(), "CDATA", param.getValue());
        contentHandler.startElement(PublisherImpl.NAMESPACE, "preparedDocuments", "p:preparedDocuments", rootAttrs);

        for (PreparedDocument preparedDocument : preparedDocuments) {
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "id", "id", "CDATA", String.valueOf(preparedDocument.getId()));
            VariantKey documentKey = preparedDocument.getDocumentKey();
            attrs.addAttribute("", "documentId", "documentId", "CDATA", documentKey.getDocumentId());
            attrs.addAttribute("", "branchId", "branchId", "CDATA", String.valueOf(documentKey.getBranchId()));
            attrs.addAttribute("", "languageId", "languageId", "CDATA", String.valueOf(documentKey.getLanguageId()));

            contentHandler.startElement(PublisherImpl.NAMESPACE, "preparedDocument", "p:preparedDocument", attrs);
            preparedDocument.getSaxBuffer().toSAX(contentHandler);
            contentHandler.endElement(PublisherImpl.NAMESPACE, "preparedDocument", "p:preparedDocument");
        }

        contentHandler.endElement(PublisherImpl.NAMESPACE, "preparedDocuments", "p:preparedDocuments");
    }

    public VariantKey getNavigationDoc() {
        return navigationDoc;
    }

    public String getPubReqSet() {
        return pubReqSet;
    }
    
    public boolean isDoDiff() {
		return doDiff;
	}

	public String getDiffList() {
		return diffList;
	}
}
