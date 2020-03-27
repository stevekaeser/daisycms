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
package org.outerj.daisy.publisher;

import org.outerj.daisy.repository.RepositoryException;

import java.util.Date;
import java.io.InputStream;
import java.io.IOException;

public interface BlobInfo {
    Date getLastModified();

    String getMimeType();
    
    String getFilename();

    long getSize();

    /**
     * Note: it is the responsibility of the caller to close the input stream to avoid resource leakage.
     */
    InputStream getInputStream() throws RepositoryException, IOException;

    /**
     * This method MUST be called by the user of the BlobInfo object when it doesn't need it any
     * longer, in order to avoid resource leakage.
     */
    void dispose();
}
