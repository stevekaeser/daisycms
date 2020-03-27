/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.textextraction;

import java.io.InputStream;

public interface TextExtractorManager {
    /**
     * @param mimeType indicates the type of data
     * @param is the input stream to be read, will be closed and buffered for you
     *
     * @return the extracted text, or null if the mimeType is not supported.
     */
    String getText(String mimeType, InputStream is) throws Exception;

    boolean supportsMimeType(String mimeType);
}
