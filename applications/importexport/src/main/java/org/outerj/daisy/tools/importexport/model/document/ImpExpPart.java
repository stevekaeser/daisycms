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
package org.outerj.daisy.tools.importexport.model.document;

import org.outerj.daisy.repository.schema.PartType;

import java.io.InputStream;

public class ImpExpPart {
    private PartType type;
    private String mimeType;
    private String fileName;
    private PartDataAccess dataAccess;

    /**
     *
     * @param fileName optional, can be null
     */
    public ImpExpPart(PartType type, String mimeType, String fileName, PartDataAccess dataAccess) {
        if (type == null)
            throw new IllegalArgumentException("Null argument: type");
        if (mimeType == null)
            throw new IllegalArgumentException("Null argument: mimeType");
        if (dataAccess == null)
            throw new IllegalArgumentException("Null argument: dataAccess");
        // Note: fileName is allowed to be null

        this.type = type;
        this.mimeType = mimeType;
        this.fileName = fileName;
        this.dataAccess = dataAccess;
    }

    public PartType getType() {
        return type;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        if (mimeType == null)
            throw new IllegalArgumentException("Null argument: mimeType");
        this.mimeType = mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public PartDataAccess getDataAccess() {
        return dataAccess;
    }

    public void setDataAccess(PartDataAccess dataAccess) {
        if (dataAccess == null)
            throw new IllegalArgumentException("Null argument: dataAccess");
        this.dataAccess = dataAccess;
    }


    public interface PartDataAccess {
        InputStream getInputStream() throws Exception;

        long getSize() throws Exception;
    }
}
