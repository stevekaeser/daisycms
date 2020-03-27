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
package org.outerj.daisy.tools.importexport.export.fs;

import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.export.ExportListener;

import java.io.File;
import java.io.IOException;

public class ExportFileFactory {
    public static ExportFile getExportFile(File file, ExportListener listener) throws ImportExportException, IOException {
        if (file.exists() && !file.isDirectory()) {
            throw new ImportExportException("The specified export file already exists.");
        } else if (file.exists()) {
            if (file.listFiles().length != 0)
                throw new ImportExportException("The specified export directory is not empty.");
        }

        if (file.getName().endsWith(".zip")) {
            return new ZipExportFile(file, listener);
        } else {
            return new LocalExportFile(file);
        }
    }
}
