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
package org.outerj.daisy.publisher.clientimpl;

import org.outerj.daisy.publisher.BlobInfo;
import org.outerj.daisy.repository.RepositoryException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.DateParser;
import org.apache.commons.httpclient.util.DateParseException;

import java.util.Date;
import java.io.InputStream;
import java.io.IOException;

public class BlobInfoImpl implements BlobInfo {
    private final GetMethod method;
    private final Date lastModified;
    private final String mimeType;
    private final long size;
    private String filename = null;
    private boolean disposed = false;
    private static final String DISPOSED_MESSAGE = "This BlobInfo object has been disposed.";

    public BlobInfoImpl(GetMethod method) throws RepositoryException {
        this.method = method;
        try {
            this.lastModified = DateParser.parseDate(method.getResponseHeader("Last-Modified").getValue());
        } catch (DateParseException e) {
            throw new RepositoryException(e);
        }
        this.mimeType = method.getResponseHeader("Content-Type").getValue();
        this.size = Integer.parseInt(method.getResponseHeader("Content-Length").getValue());
        if (method.getResponseHeader("X-Daisy-Filename") != null)
            this.filename = method.getResponseHeader("X-Daisy-Filename").getValue();
    }

    public Date getLastModified() {
        if (disposed)
            throw new RuntimeException(DISPOSED_MESSAGE);
        return lastModified;
    }

    public String getMimeType() {
        if (disposed)
            throw new RuntimeException(DISPOSED_MESSAGE);
        return mimeType;
    }

    public long getSize() {
        if (disposed)
            throw new RuntimeException(DISPOSED_MESSAGE);
        return size;
    }
    
    public String getFilename() {
        if (disposed)
            throw new RuntimeException(DISPOSED_MESSAGE);
        return filename;      
    }

    public InputStream getInputStream() throws RepositoryException, IOException {
        if (disposed)
            throw new RuntimeException(DISPOSED_MESSAGE);
        return method.getResponseBodyAsStream();
    }

    public void dispose() {
        method.releaseConnection();
        this.disposed = true;
    }
}
