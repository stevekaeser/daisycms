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
package org.outerj.daisy.books.store.source;

import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceNotFoundException;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.TimeStampValidity;
import org.outerj.daisy.books.store.BookInstance;
import org.outerx.daisy.x10Bookstoremeta.ResourcePropertiesDocument;

import java.io.InputStream;
import java.io.IOException;
import java.net.URLConnection;

public class BookStoreSource implements Source {
    private BookInstance bookInstance;
    private String resource;

    public BookStoreSource(BookInstance bookInstance, String resource) {
        this.bookInstance = bookInstance;
        this.resource = resource;
    }

    public boolean exists() {
        return bookInstance.exists(resource);
    }

    public InputStream getInputStream() throws IOException, SourceNotFoundException {
        return bookInstance.getResource(resource);
    }

    public String getURI() {
        return "bookstore:" + bookInstance.getName() + "/" + resource;
    }

    public String getScheme() {
        return "bookstore";
    }

    public SourceValidity getValidity() {
        return new TimeStampValidity(getLastModified());
    }

    public void refresh() {
    }

    public String getMimeType() {
        ResourcePropertiesDocument propertiesDocument = bookInstance.getResourceProperties(resource);
        String mimeType = (propertiesDocument != null && propertiesDocument.getResourceProperties() != null) ? propertiesDocument.getResourceProperties().getMimeType() : null;
        if (mimeType == null)
            mimeType = URLConnection.getFileNameMap().getContentTypeFor(resource);
        return mimeType;
    }

    public long getContentLength() {
        return bookInstance.getContentLength(resource);
    }

    public long getLastModified() {
        return bookInstance.getLastModified(resource);
    }
}
