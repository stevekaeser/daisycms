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
package org.outerj.daisy.frontend.util;

import org.apache.avalon.excalibur.monitor.StreamResource;

import java.io.*;

/**
 * A file resource for excalibur's monitor that also detects
 * file removals.
 */
public class AltFileResource extends StreamResource {
    private File file;
    private long previousLastModified = -1;

    public AltFileResource(File file) throws Exception {
        super(file.getCanonicalPath());
        this.file = file;
        this.previousLastModified = file.lastModified();
    }

    public long lastModified() {
        long lastModified = file.lastModified();
        if (lastModified == 0 && previousLastModified != 0) {
            previousLastModified = 0;
            return System.currentTimeMillis();
        } else {
            previousLastModified = lastModified;
            return lastModified;
        }
    }

    public InputStream getResourceAsStream() throws IOException {
        // we don't use this so didn't bother to implement
        return null;
    }

    public Reader getResourceAsReader() throws IOException {
        // we don't use this so didn't bother to implement
        return null;
    }

    public OutputStream setResourceAsStream() throws IOException {
        // we don't use this so didn't bother to implement
        return null;
    }

    public Writer setResourceAsWriter() throws IOException {
        // we don't use this so didn't bother to implement
        return null;
    }
}
