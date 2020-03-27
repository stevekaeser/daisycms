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
package org.outerj.daisy.httpconnector;

import org.outerj.daisy.httpconnector.spi.UploadItem;
import org.apache.commons.fileupload.FileItem;

import java.io.InputStream;
import java.io.IOException;

public class UploadItemImpl implements UploadItem {
    private FileItem fileItem;

    public UploadItemImpl(FileItem fileItem) {
        this.fileItem = fileItem;
    }

    public String getName() {
        return fileItem.getName();
    }

    public String getFieldName() {
        return fileItem.getFieldName();
    }

    public long getSize() {
        return fileItem.getSize();
    }

    public InputStream getInputStream() throws IOException {
        return fileItem.getInputStream();
    }

    public String getContentType() {
        return fileItem.getContentType();
    }
}
