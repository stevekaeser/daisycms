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

import org.outerj.daisy.tools.importexport.import_.fs.ImportFileEntry;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileNotFoundException;
import org.outerj.daisy.tools.importexport.import_.fs.FsUtil;

import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.ZipEntry;

class ZipImportFileEntry implements ImportFileEntry {
    private final String name;
    private final boolean isDirectory;
    private final long size;
    private Map<String, ImportFileEntry> children;
    private ZipEntry zipEntry;
    private ZipImportFile owner;
    private ZipImportFileEntry parent;

    public ZipImportFileEntry(String name, boolean isDirectory, long size, ZipImportFile owner, ZipEntry zipEntry,
            ZipImportFileEntry parent) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.size = size;
        this.owner = owner;
        if (!isDirectory && zipEntry == null)
            throw new RuntimeException("zipEntry argument is mandatory for non-directory entries");
        this.zipEntry = zipEntry;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return FsUtil.getPath(this);
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public long getSize() {
        return size;
    }

    public ImportFileEntry[] getChildren() {
        if (children == null)
            return new ImportFileEntry[0];
        else
            return children.values().toArray(new ImportFileEntry[0]);
    }

    public boolean hasChild(String name) {
        if (children == null)
            return false;
        else
            return children.containsKey(name);
    }

    public ImportFileEntry getChild(String name) throws ImportFileNotFoundException {
        if (children == null || !children.containsKey(name))
            throw new ImportFileNotFoundException(name);

        return children.get(name);
    }

    public InputStream getInputStream() throws IOException {
        if (isDirectory)
            throw new RuntimeException("Can't get an InputStream for a directory.");
        return owner.getZipFile().getInputStream(zipEntry);
    }

    public ImportFileEntry getParent() {
        return parent;
    }

    protected ZipImportFileEntry getOrCreateChild(String name, boolean directory, long size, ZipEntry zipEntry) {
        if (children == null)
            children = new HashMap<String, ImportFileEntry>();

        if (children.containsKey(name))
            return (ZipImportFileEntry)children.get(name);

        ZipImportFileEntry entry = new ZipImportFileEntry(name, directory, size, owner, zipEntry, this);
        children.put(name, entry);
        return entry;
    }
}
