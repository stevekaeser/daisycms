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
package org.outerj.daisy.frontend.components.daisysource;

import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceNotFoundException;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.TimeStampValidity;
import org.outerj.daisy.publisher.BlobInfo;
import org.outerj.daisy.repository.RepositoryException;

import java.io.InputStream;
import java.io.IOException;

/**
 * See {@link DaisySourceFactory}.
 */
public class DaisySource implements Source {
    private BlobInfo blobInfo;
    private String url;

    public DaisySource(BlobInfo blobInfo, String url) {
        this.blobInfo = blobInfo;
        this.url = url;
    }

    public boolean exists() {
        return true;
    }

    public InputStream getInputStream() throws IOException, SourceNotFoundException {
        try {
            return blobInfo.getInputStream();
        } catch (RepositoryException e) {
            throw new IOException("Error in daisy source: " + e.toString());
        }
    }

    public String getURI() {
        return url;
    }

    public String getScheme() {
        return "daisy";
    }

    public SourceValidity getValidity() {
        return new TimeStampValidity(getLastModified());
    }

    public void refresh() {
    }

    public String getMimeType() {
        return blobInfo.getMimeType();
    }

    public long getContentLength() {
        return blobInfo.getSize();
    }

    public long getLastModified() {
        return blobInfo.getLastModified().getTime();
    }

    public void dispose() {
        this.blobInfo.dispose();
    }
}
