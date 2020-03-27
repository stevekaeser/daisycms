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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

import org.outerj.daisy.books.publisher.impl.bookmodel.Book;
import org.outerj.daisy.books.publisher.impl.util.PublicationLog;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.repository.Repository;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.context.Context;
import org.apache.cocoon.i18n.Bundle;

import java.util.Locale;
import java.util.Map;

public class PublicationContext {
    private final Book book;
    private final BookInstance bookInstance;
    private final String publicationTypeName;
    private final String publicationOutputName;
    private final Repository repository;
    private final ServiceManager serviceManager;
    private final Context avalonContext;
    private final String daisyCocoonPath;
    private final Locale locale;
    private final Map<String, String> properties;
    private final Map<String, String> bookMetadata;
    private final Bundle bundle;
    private final PublicationLog publicationLog;

    public PublicationContext(Book book, BookInstance bookInstance, String publicationTypeName,
                              String publicationOutputName, Repository repository, ServiceManager serviceManager,
                              Context avalonContext, String daisyCocoonPath,
                              Locale locale, Map<String, String> properties, Map<String, String> bookMetadata, Bundle bundle,
                              PublicationLog publicationLog) {
        this.book = book;
        this.bookInstance = bookInstance;
        this.publicationTypeName = publicationTypeName;
        this.publicationOutputName = publicationOutputName;
        this.repository = repository;
        this.serviceManager = serviceManager;
        this.avalonContext = avalonContext;
        this.daisyCocoonPath = daisyCocoonPath;
        this.locale = locale;
        this.properties = properties;
        this.bookMetadata = bookMetadata;
        this.bundle = bundle;
        this.publicationLog = publicationLog;
    }

    public Book getBook() {
        return book;
    }

    public BookInstance getBookInstance() {
        return bookInstance;
    }

    public String getPublicationTypeName() {
        return publicationTypeName;
    }

    public String getPublicationOutputName() {
        return publicationOutputName;
    }

    public Repository getRepository() {
        return repository;
    }

    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    public org.apache.avalon.framework.context.Context getAvalonContext() {
        return avalonContext;
    }

    public String getDaisyCocoonPath() {
        return daisyCocoonPath;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Locale getLocale() {
        return locale;
    }

    public Bundle getI18nBundle() {
        return bundle;
    }

    public PublicationLog getPublicationLog() {
        return publicationLog;
    }

    public Map<String, String> getBookMetadata() {
        return bookMetadata;
    }
}
