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
package org.outerj.daisy.tools.importexport.import_.fs.local;

import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileEntry;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileNotFoundException;
import org.outerj.daisy.tools.importexport.import_.fs.FsUtil;

import java.io.File;

public class LocalImportFile implements ImportFile {
    private ImportFileEntry root;

    public LocalImportFile(File root) {
        this.root = new LocalImportFileEntry(root, null);
    }

    public ImportFileEntry getPath(String path) throws ImportFileNotFoundException {
        String nameParts[] = FsUtil.parsePath(path);

        ImportFileEntry current = root;

        for (String namePart : nameParts) {
            if (!current.hasChild(namePart))
                throw new ImportFileNotFoundException(path);
            current = current.getChild(namePart);
        }

        return current;
    }

    public boolean exists(String path) {
        String nameParts[] = FsUtil.parsePath(path);

        ImportFileEntry current = root;

        for (String namePart : nameParts) {
            if (!current.hasChild(namePart))
                return false;
            try {
                current = current.getChild(namePart);
            } catch (ImportFileNotFoundException e) {
                // should not occur as we just tested for it
                return false;
            }
        }

        return true;
    }

    public ImportFileEntry getRoot() {
        return root;
    }
}
