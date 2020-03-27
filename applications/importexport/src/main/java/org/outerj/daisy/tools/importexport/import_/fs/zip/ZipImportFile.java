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
package org.outerj.daisy.tools.importexport.import_.fs.zip;

import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileEntry;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileNotFoundException;
import org.outerj.daisy.tools.importexport.import_.fs.FsUtil;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.*;

public class ZipImportFile implements ImportFile {
    private ZipImportFileEntry root = new ZipImportFileEntry("root", true, 0, this, null, null);
    private ZipFile zipFile;

    public ZipImportFile(File file) throws IOException {
        zipFile = new ZipFile(file);
        Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry)entries.nextElement();
            String nameParts[] = FsUtil.parsePath(entry.getName());
            getPathCreateIfNotExists(nameParts, entry);
        }
    }

    protected ZipFile getZipFile() {
        return zipFile;
    }

    private ZipImportFileEntry getPathCreateIfNotExists(String[] pathParts, ZipEntry zipEntry) {
        ZipImportFileEntry current = root;

        for (int i = 0; i < pathParts.length; i++) {
            if (i == pathParts.length - 1) {
                if (zipEntry.isDirectory()) {
                    current = current.getOrCreateChild(pathParts[i], true, 0, null);
                } else {
                    current = current.getOrCreateChild(pathParts[i], false, zipEntry.getSize(), zipEntry);
                }
            } else {
                current = current.getOrCreateChild(pathParts[i], true, 0, null);
            }
        }

        return current;
    }

    public ImportFileEntry getRoot() {
        return root;
    }

    public ImportFileEntry getPath(String path) throws ImportFileNotFoundException {
        String nameParts[] = FsUtil.parsePath(path);

        ZipImportFileEntry current = root;

        for (String namePart : nameParts) {
            if (!current.hasChild(namePart))
                throw new ImportFileNotFoundException(path);
            current = (ZipImportFileEntry)current.getChild(namePart);
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
}
