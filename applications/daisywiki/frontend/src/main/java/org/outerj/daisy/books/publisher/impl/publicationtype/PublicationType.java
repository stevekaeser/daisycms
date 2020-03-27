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
package org.outerj.daisy.books.publisher.impl.publicationtype;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.i18n.Bundle;
import org.apache.cocoon.i18n.BundleFactory;
import org.outerj.daisy.books.publisher.impl.bookmodel.Book;
import org.outerj.daisy.books.publisher.impl.dataretrieval.PartDecider;
import org.outerj.daisy.books.publisher.impl.publicationprocess.PublicationContext;
import org.outerj.daisy.books.publisher.impl.publicationprocess.PublicationProcess;
import org.outerj.daisy.books.publisher.impl.util.PublicationLog;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.books.store.PublicationInfo;
import org.outerj.daisy.repository.Repository;

public class PublicationType {
    private final String name;
    private final String label;
    private final String startResource;

	private final Map<String, String> defaultProperties;
    private final PartDecider partDecider;
    private final PublicationProcess publicationProcess;
    
    PublicationType(String name, String label, String startResource, PublicationProcess publicationProcess,
            PartDecider partDecider, Map<String, String> defaultProperties) {
        this.name = name;
        this.label = label;
        this.startResource = startResource;
        this.publicationProcess = publicationProcess;
        this.partDecider = partDecider;
        this.defaultProperties = defaultProperties;
    }

    public PartDecider getPartDecider() {
        return partDecider;
    }

    public void publishBook(Book book, BookInstance bookInstance, String publicationOutputName, String publicationOutputLabel,
                            Map<String, String> customProperties, Map<String, String> bookMetadata, Repository repository, ServiceManager serviceManager,
                            Context avalonContext, String daisyCocoonPath, Locale locale,
                            PublicationLog publicationLog) throws Exception {
        publicationLog.info("Starting publication for publication type \"" + name + "\" to publication output \"" + publicationOutputName + "\".");
        Date now = new Date();

        Map<String, String> properties = new HashMap<String, String>();
        properties.putAll(defaultProperties);
        properties.putAll(customProperties);
        // Note: publication tasks may modify the properties

        if (properties.containsKey("publishDate")) {
            String pattern = properties.get("publishDate");
            SimpleDateFormat dateFormat;
            if (pattern.trim().length() == 0) {
                // do nothing
            } else {
                if (pattern.equals("date")) {
                    dateFormat = (SimpleDateFormat)DateFormat.getDateInstance(DateFormat.LONG, locale);
                } else if (pattern.equals("dateTime")) {
                    dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, locale);
                } else {
                    dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
                    dateFormat.applyPattern(pattern);
                }
                properties.put("publishDate", dateFormat.format(now));
            }
        }

        Bundle bundle;
        BundleFactory bundleFactory = null;
        try {
            bundleFactory = (BundleFactory)serviceManager.lookup(BundleFactory.ROLE);
            String[] locations = new String[] { "wikidata:/books/publicationtypes/common/i18n", "wikidata:/books/publicationtypes/" + name + "/i18n" };
            bundle = bundleFactory.select(locations, "messages", locale);
        } finally {
            if (bundleFactory != null)
                serviceManager.release(bundleFactory);
        }

        PublicationContext publicationContext = new PublicationContext(book, bookInstance, name, publicationOutputName,
                repository, serviceManager, avalonContext, daisyCocoonPath, locale, properties,
                bookMetadata, bundle, publicationLog);
        publicationProcess.run(publicationContext);

        String bookPackage = publicationProcess.buildsPackage() ? "output/" + bookInstance.getName() + "-" + publicationOutputName + ".zip" : null;

        bookInstance.addPublication(new PublicationInfo(publicationOutputName, publicationOutputLabel, startResource,
                bookPackage, repository.getUserId(), now));
    }

}
