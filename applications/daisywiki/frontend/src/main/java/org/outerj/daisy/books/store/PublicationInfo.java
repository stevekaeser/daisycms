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
package org.outerj.daisy.books.store;

import org.outerx.daisy.x10Bookstoremeta.PublicationsInfoDocument;

import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Holds information about a publication in the book instance.
 * This is an immutable object.
 */
public final class PublicationInfo {
    private final String name;
    private final String label;
    private final String startResource;
    private final long publishedBy;
    private final Date publishedOn;
    private String bookPackage;

    public PublicationInfo(String name, String label, String startResource, String bookPackage, long publishedBy, Date publishedOn) {
        if (name == null)
            throw new IllegalArgumentException("name argument can not be null");
        if (label == null)
            throw new IllegalArgumentException("label argument can not be null");
        if (startResource == null)
            throw new IllegalArgumentException("startResource argument can not be null");
        if (publishedOn == null)
            throw new IllegalArgumentException("publishedOn argument can not be null");

        this.name = name;
        this.label = label;
        this.startResource = startResource;
        this.bookPackage = bookPackage;
        this.publishedBy = publishedBy;
        this.publishedOn = publishedOn;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getStartResource() {
        return startResource;
    }

    public String getBookPackage() {
        return bookPackage;
    }

    public long getPublishedBy() {
        return publishedBy;
    }

    public Date getPublishedOn() {
        return (Date)publishedOn.clone();
    }

    public PublicationsInfoDocument.PublicationsInfo.PublicationInfo getXml() {
        PublicationsInfoDocument.PublicationsInfo.PublicationInfo infoXml = PublicationsInfoDocument.PublicationsInfo.PublicationInfo.Factory.newInstance();
        infoXml.setName(name);
        infoXml.setLabel(label);
        infoXml.setStartResource(startResource);
        if (bookPackage != null)
            infoXml.setPackage(bookPackage);
        infoXml.setPublishedBy(publishedBy);
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(publishedOn);
        infoXml.setPublishedOn(calendar);
        return infoXml;
    }
}
