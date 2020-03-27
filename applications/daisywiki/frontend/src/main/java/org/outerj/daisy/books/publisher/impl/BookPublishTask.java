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
package org.outerj.daisy.books.publisher.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.environment.Request;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.books.publisher.PublicationSpec;
import org.outerj.daisy.books.publisher.impl.bookmodel.Book;
import org.outerj.daisy.books.publisher.impl.bookmodel.BookBuilder;
import org.outerj.daisy.books.publisher.impl.bookmodel.Section;
import org.outerj.daisy.books.publisher.impl.dataretrieval.BookDataRetrievalProcess;
import org.outerj.daisy.books.publisher.impl.publicationtype.AggregatedPartDecider;
import org.outerj.daisy.books.publisher.impl.publicationtype.PublicationType;
import org.outerj.daisy.books.publisher.impl.publicationtype.PublicationTypeBuilder;
import org.outerj.daisy.books.publisher.impl.util.PublicationLog;
import org.outerj.daisy.books.publisher.impl.util.XMLPropertiesHelper;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.books.store.BookInstanceMetaData;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;

public class BookPublishTask {
    private Repository repository;
    private ServiceManager serviceManager;
    private Context context;
    private String daisyContextPath;
    private String daisyCocoonPath;
    private BookInstance bookInstance;
    private VariantKey bookDefinition;
    private PublicationSpec[] publicationSpecs;
    private long dataBranchId;
    private long dataLanguageId;
    private Locale locale;
    private VersionMode dataVersion;
    private String[] state = new String[] {"bookstate.starting"};

    BookPublishTask(VariantKey bookDefintion, long dataBranchId, long dataLanguageId, VersionMode dataVersion, Locale locale,
                    PublicationSpec[] specs, BookInstance bookInstance, String daisyContextPath,
                    String daisyCocoonPath, Repository repository, ServiceManager serviceManager, Context context) {
        this.serviceManager = serviceManager;
        this.context = context;
        this.bookInstance = bookInstance;
        this.daisyContextPath = daisyContextPath;
        this.daisyCocoonPath = daisyCocoonPath;
        this.bookDefinition = bookDefintion;
        this.repository = repository;
        this.publicationSpecs = specs;
        this.dataBranchId = dataBranchId;
        this.dataLanguageId = dataLanguageId;
        this.locale = locale;
        this.dataVersion = dataVersion;
    }

    public void run() {
        PublicationLog publicationLog = null;
        try {
            publicationLog = new PublicationLog(bookInstance);
            publicationLog.info("Starting");

            state = new String[] {"bookstate.initializing"};

            // Make daisyContextPath available, needed for wikidata source
            Request request = ContextHelper.getRequest(context);
            request.setAttribute("daisyContextPath", daisyContextPath);

            Document bookDefDoc = repository.getDocument(bookDefinition, false);
            Version bookDefDocVersion = dataVersion.equals("last") ? bookDefDoc.getLastVersion() : bookDefDoc.getLiveVersion();
            if (bookDefDocVersion == null)
                throw new Exception("Book definition document does not have a live version.");

            Map<String, String> bookMetaData = getBookMetadata(bookDefDocVersion);

            Part bookDefinitionPart = bookDefDocVersion.getPart("BookDefinitionDescription");
            BookBuilder bookBuilder = new BookBuilder(repository, dataBranchId, dataLanguageId, bookMetaData,
                    dataVersion, locale);

            Book book;
            InputStream is = bookDefinitionPart.getDataStream();
            try {
                book = bookBuilder.buildBook(is);
            } finally {
                is.close();
            }

            String bookPath = bookDefDoc.hasField("BookPath") ? (String)bookDefDoc.getField("BookPath").getValue() : "";
            BookInstanceMetaData metaData = bookInstance.getMetaData();
            metaData.setBookPath(bookPath + "/" + bookMetaData.get("title"));
            bookInstance.setMetaData(metaData);

            PublicationType[] publicationTypes = new PublicationType[publicationSpecs.length];
            for (int i = 0; i < publicationSpecs.length; i++) {
                PublicationSpec spec = publicationSpecs[i];
                publicationTypes[i] = PublicationTypeBuilder.build(spec.getPublicationTypeName(), serviceManager);
            }
            
            completeSections(book, dataVersion);
            
            state = new String[] {"bookstate.retrieving-data"};
            BookDataRetrievalProcess bookDataRetrievalProcess = new BookDataRetrievalProcess(repository, dataBranchId, dataLanguageId, locale, book, bookInstance, bookMetaData, new AggregatedPartDecider(publicationTypes), publicationLog);
            bookDataRetrievalProcess.run();

            // Save a copy of the publication specs
            OutputStream pubSpecsStream = bookInstance.getResourceOutputStream(BookInstanceLayout.getPublicationSpecsPath());
            try {
                XmlOptions xmlOptions = new XmlOptions();
                xmlOptions.setSavePrettyPrint();
                PublicationSpec.getXml(publicationSpecs).save(pubSpecsStream);
            } finally {
                pubSpecsStream.close();
            }

            for (int i = 0; i < publicationTypes.length; i++) {
                PublicationSpec spec = publicationSpecs[i];
                state = new String[] {"bookstate.publishing", spec.getPublicationTypeName(), spec.getPublicationOutputName()};
                publicationTypes[i].publishBook(book, bookInstance, spec.getPublicationOutputName(), spec.getPublicationOutputLabel(),
                        spec.getPublicationProperties(), bookMetaData, repository, serviceManager, context, daisyCocoonPath, locale, publicationLog);
            }

            publicationLog.info("Finished");
            state = new String[] {"bookstate.finished"};
        } catch (Throwable e) {
            publicationLog.error("Error producing book.", e);
        } finally {
            if (publicationLog != null)
                try { publicationLog.dispose(); } catch (Throwable e) { /* ignore */ }
        }
    }

    private void completeSections(Book book, VersionMode dataVersion) throws Exception {
        completeSections(book.getSections(), dataVersion);
    }
    
    private void completeSections(Section[] sections, VersionMode dataVersion) throws Exception {
        for (Section section: sections) {
            if (section.getDocumentId() != null) {
                long branchId = section.getBranchId();
                if (branchId == -1) {
                    branchId = dataBranchId;
                    section.setBranchId(dataBranchId); 
                }
                
                long languageId = section.getLanguageId();
                if (languageId == -1) {
                    languageId = dataLanguageId;
                    section.setLanguageId(languageId);
                }
                
                String version = section.getVersion();
                if (version == null) {
                } else if (version.equals("-2")) {
                    dataVersion = VersionMode.LAST;
                } else if (version.equals("-1")) {
                    dataVersion = VersionMode.LIVE;
                } else {
                    dataVersion = VersionMode.get(version);
                }
                
            }
            completeSections(section.getSections(), dataVersion);
        }
        
    }

    public String[] getState() {
        return state;
    }

    public Repository getRepository() {
        return repository;
    }

    private Map<String, String> getBookMetadata(Version bookDefDocVersion) throws Exception {
        Map<String, String> bookMetaData;

        if (bookDefDocVersion.hasPart("BookMetadata")) {
            Part bookMetadataPart = bookDefDocVersion.getPart("BookMetadata");
            InputStream is = bookMetadataPart.getDataStream();
            try {
                bookMetaData = XMLPropertiesHelper.load(is, "metadata");
            } finally {
                is.close();
            }
        } else {
            bookMetaData = new HashMap<String, String>();
        }

        if (!bookMetaData.containsKey("title")) {
            bookMetaData.put("title", "Book Without Title");
        }

        return bookMetaData;
    }

    public BookInstance getBookInstance() {
        return bookInstance;
    }
}
