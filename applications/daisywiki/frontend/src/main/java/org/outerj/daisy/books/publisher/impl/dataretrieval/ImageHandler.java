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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.publisher.BlobInfo;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.apache.cocoon.xml.AttributesImpl;
import org.outerx.daisy.x10Bookstoremeta.ResourcePropertiesDocument;

import java.util.regex.Matcher;
import java.io.InputStream;

public class ImageHandler implements ContentHandler {
    private String currentDocumentId = null;
    private long currentDocBranchId = -1;
    private long currentDocLanguageId = -1;
    private VariantManager variantManager;
    private Repository repository;
    private ContentHandler consumer;
    private BookInstance bookInstance;
    private BookDependencies bookDependencies;

    public ImageHandler(Repository repository, BookInstance bookInstance, BookDependencies bookDependencies, ContentHandler consumer) {
        this.repository = repository;
        this.variantManager = repository.getVariantManager();
        this.consumer = consumer;
        this.bookInstance = bookInstance;
        this.bookDependencies = bookDependencies;
    }

    public void setConsumer(ContentHandler consumer) {
        this.consumer = consumer;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (uri.equals(Constants.DAISY_NAMESPACE) && localName.equals("document")) {
            currentDocumentId = attributes.getValue("id");
            currentDocBranchId = Long.parseLong(attributes.getValue("branchId"));
            currentDocLanguageId = Long.parseLong(attributes.getValue("languageId"));
        } else if (uri.equals("") && localName.equals("img")) {
            if (currentDocumentId == null)
                throw new SAXException("ImageHandler: encountered img tag outside the context of a document.");

            String src = attributes.getValue("src");
            if (src != null && !src.equals("")) {
                Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(src);
                if (matcher.matches()) {
                    String documentId = matcher.group(1);

                    String branchString = matcher.group(2);
                    String languageString = matcher.group(3);
                    String versionString = matcher.group(4);

                    long branchId;
                    long languageId;
                    long versionId = -1; // -1 == live version

                    if (branchString == null || branchString.length() == 0) {
                        branchId = currentDocBranchId;
                    } else {
                        if (Character.isDigit(branchString.charAt(0))) {
                            branchId = Long.parseLong(branchString);
                        } else {
                            Branch branch;
                            try {
                                branch = variantManager.getBranchByName(branchString, false);
                            } catch (RepositoryException e) {
                                throw new RuntimeException(e);
                            }
                            branchId = branch.getId();
                        }
                    }

                    if (languageString == null || languageString.length() == 0) {
                        languageId = currentDocLanguageId;
                    } else {
                        if (Character.isDigit(languageString.charAt(0))) {
                            languageId = Long.parseLong(languageString);
                        } else {
                            Language language;
                            try {
                                language = variantManager.getLanguageByName(languageString, false);
                            } catch (RepositoryException e) {
                                throw new RuntimeException(e);
                            }
                            languageId = language.getId();
                        }
                    }

                    if (versionString != null) {
                        if (versionString.equals("LAST")) {
                            versionId = -2; // -2 == last version
                        } else {
                            versionId = Long.parseLong(versionString);
                        }
                    }

                    try {
                        Document document = repository.getDocument(documentId, branchId, languageId, false);
                        if (versionId == -1)
                            versionId = document.getLiveVersionId();
                        else if (versionId == -2)
                            versionId = document.getLastVersionId();

                        VersionKey imageKey = new VersionKey(documentId, branchId, languageId, versionId);
                        String storePath = fetchImage(imageKey);
                        AttributesImpl newAttrs = new AttributesImpl(attributes);
                        newAttrs.addAttribute("", "bookStorePath", "bookStorePath", "CDATA", storePath);
                        attributes = newAttrs;
                        bookDependencies.addDependency(imageKey);
                    } catch (Exception e) {
                        throw new SAXException("ImageHandler: error trying to download and store image data."
                                + " Image document: " + documentId + ", branch " + branchId + ", language " + languageId
                                + ". Containing document: " + currentDocumentId + ", branch " + currentDocBranchId
                                + ", language " + currentDocLanguageId, e);
                    }
                }
            }
        }
        consumer.startElement(uri, localName, qName, attributes);
    }

    private String fetchImage(VersionKey imageKey) throws Exception {
        Publisher publisher = (Publisher)repository.getExtension("Publisher");
        VariantKey imageVariantKey = new VariantKey(imageKey.getDocumentId(), imageKey.getBranchId(), imageKey.getLanguageId());
        String storePath = BookInstanceLayout.getImageStorePath(imageKey.getDocumentId(), imageKey.getBranchId(), imageKey.getLanguageId(), imageKey.getVersionId());
        if (!bookInstance.exists(storePath)) {
            BlobInfo blobInfo = publisher.getBlobInfo(imageVariantKey, String.valueOf(imageKey.getVersionId()), "ImageData");
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

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (uri.equals(Constants.DAISY_NAMESPACE) && localName.equals("document")) {
            currentDocumentId = null;
        }
        consumer.endElement(uri, localName, qName);
    }

    public void endDocument() throws SAXException {
        consumer.endDocument();
    }

    public void startDocument() throws SAXException {
        consumer.startDocument();
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        consumer.characters(ch, start, length);
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        consumer.ignorableWhitespace(ch, start, length);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        consumer.endPrefixMapping(prefix);
    }

    public void skippedEntity(String name) throws SAXException {
        consumer.skippedEntity(name);
    }

    public void setDocumentLocator(Locator locator) {
        consumer.setDocumentLocator(locator);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        consumer.processingInstruction(target, data);
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        consumer.startPrefixMapping(prefix, uri);
    }
}
