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

import org.outerj.daisy.books.publisher.impl.bookmodel.Book;
import org.outerj.daisy.books.publisher.impl.bookmodel.SectionContainer;
import org.outerj.daisy.books.publisher.impl.bookmodel.Section;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.books.publisher.impl.util.PublicationLog;
import org.outerj.daisy.books.publisher.impl.util.BookVariablesConfigBuilder;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.repository.Repository;
import org.xml.sax.helpers.DefaultHandler;
import org.outerx.daisy.x10Publisher.*;

import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

/**
 * The process that retrieves all the data needed for the book (documents & images).
 */
public class BookDataRetrievalProcess {
    private Repository repository;
    private Publisher publisher;
    private long dataBranchId;
    private long dataLanguageId;
    private Locale locale;
    private BookInstance bookInstance;
    private Book book;
    private Map<String, String> bookMetaData;
    private VariablesConfigType variablesConfig;
    private PartDecider partDecider;
    private BookDependencies bookDependencies = new BookDependencies();
    private PublicationLog publicationLog;

    public BookDataRetrievalProcess(Repository repository, long dataBranchId, long dataLanguageId, Locale locale,
            Book book, BookInstance bookInstance, Map<String, String> bookMetaData, PartDecider partDecider,
            PublicationLog publicationLog) {
        this.repository = repository;
        this.publisher = (Publisher)repository.getExtension("Publisher");
        this.dataBranchId = dataBranchId;
        this.dataLanguageId = dataLanguageId;
        this.book = book;
        this.bookMetaData = bookMetaData;
        this.bookInstance = bookInstance;
        this.locale = locale;
        this.publicationLog = publicationLog;
        this.partDecider = partDecider;
    }

    public void run() throws Exception {
        publicationLog.info("Starting retrieval of book data from repository");
        variablesConfig = BookVariablesConfigBuilder.buildVariablesConfig(bookMetaData, String.valueOf(dataBranchId), String.valueOf(dataLanguageId));
        fetchResources();

        // Store dependencies
        publicationLog.info("Storing book dependency list");
        OutputStream dependenciesStream = bookInstance.getResourceOutputStream(BookInstanceLayout.getDependenciesPath());
        try {
            bookDependencies.store(dependenciesStream);
        } finally {
            dependenciesStream.close();
        }

        // Store processed book definition
        publicationLog.info("Storing processed book definition");
        OutputStream bookDefProcessedStream = bookInstance.getResourceOutputStream(BookInstanceLayout.getProcessedBookDefinitionPath());
        try {
            book.store(bookDefProcessedStream);
        } finally {
            bookDefProcessedStream.close();
        }

        publicationLog.info("Ending data retrieval");
    }

    private void fetchResources() throws Exception {
        ImageHandler imageHandler = new ImageHandler(repository, bookInstance, bookDependencies, new DefaultHandler());
        fetchSectionDocs(book, imageHandler);
    }

    private void fetchSectionDocs(SectionContainer sectionContainer, ImageHandler imageHandler) throws Exception {
        Section[] sections = sectionContainer.getSections();
        for (Section section : sections) {
            if (section.getDocumentId() != null) {
                String documentId = section.getDocumentId();
                long branchId = section.getBranchId() == -1 ? dataBranchId : section.getBranchId();
                long languageId = section.getLanguageId() == -1 ? dataLanguageId : section.getLanguageId();
                String version = section.getVersion();

                PublisherRequestDocument publisherRequestDocument = PublisherRequestDocument.Factory.newInstance();
                PublisherRequestDocument.PublisherRequest publisherRequestXml = publisherRequestDocument.addNewPublisherRequest();
                publisherRequestXml.setLocale(locale.toString());
                publisherRequestXml.setVariablesConfig(variablesConfig);
                DocumentDocument.Document documentRequestXml = publisherRequestXml.addNewDocument();
                documentRequestXml.setId(documentId);
                documentRequestXml.setBranch(String.valueOf(branchId));
                documentRequestXml.setLanguage(String.valueOf(languageId));
                documentRequestXml.setVersion(version);
                PreparedDocumentsDocument.PreparedDocuments preparedDocsRequestXml = documentRequestXml.addNewPreparedDocuments();
                PreparedDocumentsDocument.PreparedDocuments.Context preparedDocsContextXml = preparedDocsRequestXml.addNewContext();
                preparedDocsContextXml.setBranch(String.valueOf(dataBranchId));
                preparedDocsContextXml.setLanguage(String.valueOf(dataLanguageId));
                if (bookMetaData.containsKey("publisherRequestSet")) {
                    String publisherRequestSet = bookMetaData.get("publisherRequestSet"); 
                    publicationLog.info("Using publisherRequestSet " + publisherRequestSet);
                    preparedDocsRequestXml.setPublisherRequestSet(publisherRequestSet);
                }

                String storePath = BookInstanceLayout.getDocumentStorePath(bookInstance, documentId, branchId, languageId);
                OutputStream os = bookInstance.getResourceOutputStream(storePath);
                try {
                    // Push parsing result through a pipeline like this:
                    // 1. leave only through the p:preparedDocuments element
                    // 2. collect IDs of all the prepared documents for the dependency information
                    // 3. download the parts specified in the downloadParts set
                    // 4. download referenced images
                    // 5. serialize result
                    XmlSerializer serializer = new XmlSerializer(os);
                    imageHandler.setConsumer(serializer);
                    PartDownloadHandler partDownloadHandler = new PartDownloadHandler(partDecider, bookInstance, repository, imageHandler);
                    DependencyCollector dependencyCollector = new DependencyCollector(bookDependencies, partDownloadHandler);
                    PreparedDocumentsExtractor preparedDocumentsExtractor = new PreparedDocumentsExtractor(dependencyCollector);
                    publisher.processRequest(publisherRequestDocument, preparedDocumentsExtractor);
                } finally {
                    os.close();
                }

                section.setBookStorePath(storePath);
            }
            fetchSectionDocs(section, imageHandler);
        }
    }

}
