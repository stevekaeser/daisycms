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
package org.outerj.daisy.publisher.serverimpl;

import org.outerj.daisy.publisher.BlobInfo;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.RepositoryException;

import java.util.Date;
import java.io.InputStream;

public class BlobInfoImpl implements BlobInfo {
    private Part part;
    private Date lastModified;
    private boolean disposed = false;
    private static final String DISPOSED_MESSAGE = "This BlobInfo object has been disposed.";

    public BlobInfoImpl(Part part, Date lastModified) {
        this.part = part;        
        this.lastModified = lastModified;
    }

    public Date getLastModified() {
        if (disposed)
            throw new RuntimeException(DISPOSED_MESSAGE);
        return lastModified;
    }

    public String getMimeType() {
        if (disposed)
            throw new RuntimeException(DISPOSED_MESSAGE);
        return part.getMimeType();
    }

    public long getSize() {
        if (disposed)
            throw new RuntimeException(DISPOSED_MESSAGE);
        return part.getSize();
    }
    
    public String getFilename() {
        if (disposed)
            throw new RuntimeException(DISPOSED_MESSAGE);
        return part.getFileName();
    }

    public InputStream getInputStream() throws RepositoryException {
        if (disposed)
            throw new RuntimeException(DISPOSED_MESSAGE);
        return part.getDataStream();
    }

    public void dispose() {
        this.disposed = true;
        this.part = null;
        this.lastModified = null;
    }
}
