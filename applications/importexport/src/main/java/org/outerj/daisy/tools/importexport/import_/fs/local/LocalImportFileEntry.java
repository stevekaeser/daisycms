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

import org.outerj.daisy.tools.importexport.import_.fs.ImportFileEntry;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileNotFoundException;
import org.outerj.daisy.tools.importexport.import_.fs.FsUtil;

import java.io.InputStream;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;

class LocalImportFileEntry implements ImportFileEntry {
    private File file;
    private ImportFileEntry parent;

    public LocalImportFileEntry(File file, ImportFileEntry parent) {
        this.file = file;
        this.parent = parent;
    }

    public String getName() {
        return file.getName();
    }

    public String getPath() {
        return FsUtil.getPath(this);
    }

    public ImportFileEntry getParent() {
        return parent;
    }

    public boolean isDirectory() {
        return file.isDirectory();
    }

    public long getSize() {
        return file.length();
    }

    public ImportFileEntry[] getChildren() {
        File children[] = file.listFiles();
        LocalImportFileEntry entries[] = new LocalImportFileEntry[children.length];
        for (int i = 0; i < children.length; i++) {
            entries[i] = new LocalImportFileEntry(children[i], this);
        }
        return entries;
    }

    public InputStream getInputStream() throws IOException {
        if (file.isDirectory())
            throw new RuntimeException("Can't get an InputStream for a directory.");
        return new FileInputStream(file);
    }

    public boolean hasChild(String name) {
        return new File(file, name).exists();
    }

    public ImportFileEntry getChild(String name) throws ImportFileNotFoundException {
        File child = new File(file, name);
        if (!child.exists())
            throw new ImportFileNotFoundException(name);

        return new LocalImportFileEntry(child, this);
    }
}
