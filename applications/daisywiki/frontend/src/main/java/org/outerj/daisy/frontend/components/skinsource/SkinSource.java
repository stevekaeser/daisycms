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
package org.outerj.daisy.frontend.components.skinsource;

import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceNotFoundException;
import org.apache.excalibur.source.SourceValidity;

import java.io.*;
import java.net.URLConnection;

public class SkinSource implements Source {
    private File file;
    private String uri;

    public SkinSource(File file, String uri) {
        this.file = file;
        this.uri = uri;
    }

    public boolean exists() {
        return file.exists();
    }

    public InputStream getInputStream() throws IOException, SourceNotFoundException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
            throw new SourceNotFoundException(uri + " doesn't exist.", fnfe);
        }
    }

    public String getURI() {
        return uri;
    }

    public String getScheme() {
        return "daisyskin";
    }

    public SourceValidity getValidity() {
        if (file.exists()) {
            return new SkinSourceValidity(file);
        } else {
            return null;
        }
    }

    public void refresh() {
        // nothing to do
    }

    public String getMimeType() {
        return URLConnection.getFileNameMap().getContentTypeFor(file.getName());
    }

    public long getContentLength() {
        return file.length();
    }

    public long getLastModified() {
        return file.lastModified();
    }
}
