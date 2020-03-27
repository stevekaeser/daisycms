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

import org.outerj.daisy.tools.importexport.ImportExportException;

public class DocumentLoaderFactory {
    public static DocumentLoader getDocumentLoader(String daisyVersion) throws ImportExportException {
        // This needs work on each new Daisy version
        if (daisyVersion.startsWith("2.0") || daisyVersion.startsWith("2.1") || daisyVersion.startsWith("2.2") || daisyVersion.startsWith("2.3") || daisyVersion.startsWith("2.4")) {
            return new Daisy20DocumentLoader();
        } else {
            throw new ImportExportException("Export meta data specifies an unsupported Daisy version: " + daisyVersion);
        }
    }

    public static DocumentLoader getDocumentLoader() {
        return new Daisy20DocumentLoader();
    }
}
