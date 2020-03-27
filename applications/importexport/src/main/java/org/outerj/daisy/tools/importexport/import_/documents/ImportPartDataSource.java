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
package org.outerj.daisy.tools.importexport.import_.documents;

import org.outerj.daisy.repository.PartDataSource;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.tools.importexport.model.document.ImpExpPart;

import java.io.InputStream;
import java.io.IOException;

public class ImportPartDataSource implements PartDataSource {
    private ImpExpPart.PartDataAccess dataAccess;

    public ImportPartDataSource(ImpExpPart.PartDataAccess dataAccess) {
        this.dataAccess = dataAccess;
    }

    public InputStream createInputStream() throws IOException, RepositoryException {
        try {
            return dataAccess.getInputStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long getSize() {
        try {
            return dataAccess.getSize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
