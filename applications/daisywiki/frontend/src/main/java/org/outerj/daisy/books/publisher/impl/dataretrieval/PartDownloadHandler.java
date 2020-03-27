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
package org.outerj.daisy.books.publisher.impl.dataretrieval;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.xmlutil.ForwardingContentHandler;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.publisher.BlobInfo;
import org.outerx.daisy.x10Bookstoremeta.ResourcePropertiesDocument;
import org.apache.cocoon.xml.AttributesImpl;

import java.io.InputStream;

/**
 * Downloads the data of certain parts and stores it in the book instance. The storage
 * path is left as an attribute on the part element.
 */
public class PartDownloadHandler extends ForwardingContentHandler {
    private String currentDocumentId = null;
    private long currentDocBranchId = -1;
    private long currentDocLanguageId = -1;
    private long currentDataVersionId;
    private long currentDocumentTypeId;
    private String currentDocumentTypeName;
    private Repository repository;
    private PartDecider partDecider;
    private BookInstance bookInstance;

    public PartDownloadHandler(PartDecider partDecider, BookInstance bookInstance, Repository repository, ContentHandler consumer) {
        super(consumer);
        this.repository = repository;
        this.partDecider = partDecider;
        this.bookInstance = bookInstance;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        if (namespaceURI.equals(Constants.DAISY_NAMESPACE)) {
            if (localName.equals("document")) {
                currentDocumentId = attributes.getValue("id");
                currentDocBranchId = Long.parseLong(attributes.getValue("branchId"));
                currentDocLanguageId = Long.parseLong(attributes.getValue("languageId"));
                currentDataVersionId = Long.parseLong(attributes.getValue("dataVersionId"));
                currentDocumentTypeId = Long.parseLong(attributes.getValue("typeId"));
                currentDocumentTypeName = attributes.getValue("typeName");
            } else if (localName.equals("part")) {
                if (currentDocumentId == null)
                    throw new SAXException("PartDownloadHandler: encountered a part outside the context of a document.");

                long partTypeId = Long.parseLong(attributes.getValue("typeId"));
                String partTypeName = attributes.getValue("name");
                boolean needsPart = partDecider.needsPart(currentDocumentTypeId, currentDocumentTypeName, partTypeId, partTypeName,
                        attributes.getValue("mimeType"), attributes.getValue("fileName"),
                        Long.parseLong(attributes.getValue("size")), new VariantKey(currentDocumentId, currentDocBranchId,
                        currentDocLanguageId), currentDataVersionId, repository);

                if (needsPart) {
                    // download the part
                    String storePath;
                    try {
                        storePath = fetchResource(new VersionKey(currentDocumentId, currentDocBranchId, currentDocLanguageId, currentDataVersionId), partTypeId);
                    } catch (Exception e) {
                        throw new SAXException("PartDownloadHandler: error trying to download and store part data.", e);
                    }
                    // Note: since this is a part within a requested document, we don't need to add a new
                    // book dependency
                    AttributesImpl newAttrs = new AttributesImpl(attributes);
                    newAttrs.addAttribute("", "bookStorePath", "bookStorePath", "CDATA", storePath);
                    attributes = newAttrs;
                }
            }
        }
        super.startElement(namespaceURI, localName, qName, attributes);
    }

    private String fetchResource(VersionKey resourceKey, long partTypeId) throws Exception {
        Publisher publisher = (Publisher)repository.getExtension("Publisher");
        VariantKey imageVariantKey = new VariantKey(resourceKey.getDocumentId(), resourceKey.getBranchId(), resourceKey.getLanguageId());
        String storePath = BookInstanceLayout.getResourceStorePath(resourceKey.getDocumentId(), resourceKey.getBranchId(), resourceKey.getLanguageId(), resourceKey.getVersionId(), partTypeId);
        if (!bookInstance.exists(storePath)) {
            BlobInfo blobInfo = publisher.getBlobInfo(imageVariantKey, String.valueOf(resourceKey.getVersionId()), String.valueOf(partTypeId));
            try {
                InputStream is = blobInfo.getInputStream();
                // InputStream gets closed by storeResource method
                bookInstance.storeResource(storePath, is);

                ResourcePropertiesDocument propertiesDocument = ResourcePropertiesDocument.Factory.newInstance();
                propertiesDocument.addNewResourceProperties().setMimeType(blobInfo.getMimeType());
                bookInstance.storeResourceProperties(storePath, propertiesDocument);
            } finally {
                blobInfo.dispose();
            }
        }
        return storePath;
    }
}
