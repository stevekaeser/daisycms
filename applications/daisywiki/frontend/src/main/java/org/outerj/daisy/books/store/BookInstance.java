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

import org.outerx.daisy.x10Bookstoremeta.ResourcePropertiesDocument;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.URI;

public interface BookInstance {
    String getName();

    /**
     * Returns a stream for the given resource. The stream will already be buffered if needed.
     *
     * <p>Throws a {@link BookResourceNotFoundException} if the path does not exist.
     *
     * <p><b style="color: red">It is the callers' responsibility to close this stream!</b>
     */
    InputStream getResource(String path);

    /**
     * Returns null if not available.
     */
    ResourcePropertiesDocument getResourceProperties(String path);

    /**
     * Stores a resource under the given path.
     *
     * <p>The implementation of this method is required to close the input stream, even
     * if an exception occurs.
     */
    void storeResource(String path, InputStream is);

    void storeResourceProperties(String path, ResourcePropertiesDocument resourcePropertiesDocument);

    /**
     * Get an output stream to store a resource in the book instance.
     *
     * <p> This will return a buffered stream if necessary, no need to wrap it into a BufferedOutputStream
     * yourself.
     *
     * <p><b style="color: red">It is the callers' responsibility to close this stream!</b>
     */
    OutputStream getResourceOutputStream(String path) throws IOException;

    boolean rename(String path, String newName);

    boolean exists(String path);

    long getLastModified(String path);

    long getContentLength(String path);

    void lock();

    void unlock();

    BookAcl getAcl();

    void setAcl(BookAcl bookAcl);

    /**
     * Returns true if the user can perform management operations on this book instance.
     */
    boolean canManage();

    PublicationsInfo getPublicationsInfo();

    void addPublication(PublicationInfo publicationInfo);

    void setPublications(PublicationsInfo publicationsInfo);

    /**
     * Returns a Java URI that can be used to retrieve the resource. Bypasses the BookInstance abstraction.
     */
    URI getResourceURI(String path);

    /**
     * Note: while this returns a mutable object, it is a clone of the original
     * and any changed performed on it will not have effect.
     */
    BookInstanceMetaData getMetaData();

    void setMetaData(BookInstanceMetaData metaData);

    String[] getDescendantPaths(String path);
}
