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

import org.apache.cocoon.xml.SaxBuffer;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.repository.VariantKey;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Holds a SaxBuffer for each of the prepared documents.
 *
 */
public class PreparedDocuments {
    private Map<Integer, PreparedDocument> preparedDocumentsById = new HashMap<Integer, PreparedDocument>();
    private static final String PUBLISHER_NAMESPACE = "http://outerx.org/daisy/1.0#publisher";

    public void putPreparedDocument(int id, VariantKey documentKey, SaxBuffer content) {
        preparedDocumentsById.put(new Integer(id), new PreparedDocument(content, id, documentKey));
    }

    /**
     * The buffer includes startDocument and endDocument events.
     */
    public PreparedDocument getPreparedDocument(int id) {
        return preparedDocumentsById.get(new Integer(id));
    }

    public int[] getPreparedDocumentIds() {
        int[] ids = new int[preparedDocumentsById.size()];
        Iterator it = preparedDocumentsById.keySet().iterator();
        int i = 0;
        while (it.hasNext()) {
            ids[i++] = ((Integer)it.next()).intValue();
        }
        return ids;
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

    public void generateSax(ContentHandler contentHandler) throws SAXException {
        AttributesImpl rootAttrs = new AttributesImpl();
        contentHandler.startElement(PUBLISHER_NAMESPACE, "preparedDocuments", "p:preparedDocuments", rootAttrs);

        for (PreparedDocument preparedDocument : preparedDocumentsById.values()) {
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "id", "id", "CDATA", String.valueOf(preparedDocument.getId()));
            VariantKey documentKey = preparedDocument.getDocumentKey();
            attrs.addAttribute("", "documentId", "documentId", "CDATA", String.valueOf(documentKey.getDocumentId()));
            attrs.addAttribute("", "branchId", "branchId", "CDATA", String.valueOf(documentKey.getBranchId()));
            attrs.addAttribute("", "languageId", "languageId", "CDATA", String.valueOf(documentKey.getLanguageId()));

            contentHandler.startElement(PUBLISHER_NAMESPACE, "preparedDocument", "p:preparedDocument", attrs);
            preparedDocument.getSaxBuffer().toSAX(new IncludeXMLConsumer(contentHandler));
            contentHandler.endElement(PUBLISHER_NAMESPACE, "preparedDocument", "p:preparedDocument");
        }

        contentHandler.endElement(PUBLISHER_NAMESPACE, "preparedDocuments", "p:preparedDocuments");
    }

}
