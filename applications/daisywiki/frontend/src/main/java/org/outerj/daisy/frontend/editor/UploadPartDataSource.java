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
package org.outerj.daisy.frontend.editor;

import org.outerj.daisy.repository.PartDataSource;

import java.io.InputStream;
import java.io.IOException;

public class UploadPartDataSource implements PartDataSource {
    private final org.apache.cocoon.servlet.multipart.Part part;

    public UploadPartDataSource(org.apache.cocoon.servlet.multipart.Part part) {
        this.part = part;
    }

    public InputStream createInputStream() throws IOException {
        // Here we make the assumption that part.getInputStream() will always
        // return a new input stream, which is the case in Cocoon's PartOnDisk
        // implementation, but not in the PartInMemory implementation.
        try {
            return part.getInputStream();
        } catch (Exception e) {
            throw new RuntimeException("Error getting upload input stream.", e);
        }
    }

    public long getSize() {
        return part.getSize();
    }

}
