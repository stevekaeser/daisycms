/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.httpconnector.spi;

import java.io.InputStream;
import java.io.IOException;

public interface UploadItem {
    /**
     * Returns the original filename in the client's filesystem, as
     * provided by the browser (or other client software).
     */
    String getName();

    /**
     * Returns the name of the field in the multipart form corresponding
     * to this file item.
     */
    String getFieldName();

    long getSize();

    InputStream getInputStream() throws IOException;

    String getContentType();
}
