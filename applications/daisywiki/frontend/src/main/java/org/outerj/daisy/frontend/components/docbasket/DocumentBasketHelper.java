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
package org.outerj.daisy.frontend.components.docbasket;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Session;
import org.outerj.daisy.frontend.WikiHelper;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.outerx.daisy.x10Publisher.ResolveDocumentIdsDocument;
import org.outerx.daisy.x10Publisher.ResolveDocumentIdsDocument.ResolveDocumentIds;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class DocumentBasketHelper {
    public static final String DOCBASKET_SESSION_ATTR = "DaisyDocumentBasket";

    /**
     *
     * @param create create the basket in the session if it does not exist. When
     *               false, no session will be created if it did not yet exist.
     */
    public static DocumentBasket getDocumentBasket(Request request, boolean create) {
        Session session = request.getSession(create);
        if (session == null)
            return null;

        DocumentBasket documentBasket = (DocumentBasket)session.getAttribute(DOCBASKET_SESSION_ATTR);
        if (documentBasket == null) {
            if (!create)
                return null;

            documentBasket = new DocumentBasket();
            session.setAttribute(DOCBASKET_SESSION_ATTR, documentBasket);
        }
        return documentBasket;
    }

    public static void updateDocumentNames(DocumentBasketEntry entry, Request request, Repository repository)
            throws SAXException, RepositoryException {
        updateDocumentNames(new DocumentBasketEntry[] { entry }, request, repository);
    }

    public static void updateDocumentNames(DocumentBasketEntry[] entries, Request request, Repository repository)
            throws RepositoryException, SAXException {

        if (entries.length == 0)
            return;

        PublisherRequestDocument pubReqDoc = PublisherRequestDocument.Factory.newInstance();
        PublisherRequestDocument.PublisherRequest pubReq = pubReqDoc.addNewPublisherRequest();
        ResolveDocumentIdsDocument.ResolveDocumentIds resolveDocIds = pubReq.addNewResolveDocumentIds();

        ResolveDocumentIds.Document documents[] = new ResolveDocumentIds.Document[entries.length];

        for (int i = 0; i < entries.length; i++) {
            DocumentBasketEntry entry = entries[i];
            ResolveDocumentIds.Document doc = ResolveDocumentIds.Document.Factory.newInstance();
            doc.setId(entry.getDocumentId());
            doc.setBranch(entry.getBranch());
            doc.setLanguage(entry.getLanguage());

            String version;
            long versionId = entry.getVersionId();
            if (versionId == -3) {
                version = WikiHelper.getVersionMode(request).toString();
            } else if (versionId == -2) {
                version = "last";
            } else if (versionId == -1) {
                version = "live";
            } else {
                version = String.valueOf(versionId);
            }

            doc.setVersion(version);
            documents[i] = doc;
        }

        resolveDocIds.setDocumentArray(documents);


        Publisher publisher = (Publisher)repository.getExtension("Publisher");
        publisher.processRequest(pubReqDoc, new DocNamesReceiver(entries));
    }

    static class DocNamesReceiver extends DefaultHandler {
        private DocumentBasketEntry[] entries;
        private int pos = 0;

        public DocNamesReceiver(DocumentBasketEntry[] entries) {
            this.entries = entries;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (uri.equals("http://outerx.org/daisy/1.0#publisher") && localName.equals("document")) {
                entries[pos++].setDocumentName(attributes.getValue("name"));
            }
        }
    }

}
