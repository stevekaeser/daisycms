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

import java.io.*;

/**
 * An {@link ExportFile} implementation which usages a filesystem
 * directory as target for the export data.
 */
public class LocalExportFile implements ExportFile {
    private File root;
    private boolean finished = false;
    private static final int BUFFER_SIZE = 32768;

    public LocalExportFile(File root) {
        this.root = root;
    }

    protected File getRoot() {
        return root;
    }

    public OutputStream getOutputStream(String path) throws ImportExportException {
        if (finished)
            throw new ImportExportException("Cannot write to a finished export.");

        File file = new File(root, path);
        if (!isWithin(root, file))
            throw new ImportExportException("It is not allowed to write a file outside of the export location: " + path);

        File parentDir = file.getParentFile();
        if (!parentDir.exists())
            parentDir.mkdirs();

        try {
            return new BufferedOutputStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            throw new ImportExportException("Unexpected FileNotFound exception.", e);
        }
    }

    public void store(String path, InputStream is) throws ImportExportException, IOException {
        OutputStream os = null;
        try {
            os = getOutputStream(path);
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
        } finally {
            if (os != null)
                os.close();
        }
    }

    public boolean isWithin(File parent, File child) {
        String parentPath = parent.getAbsolutePath();
        String childPath = child.getAbsolutePath();
        return childPath.startsWith(parentPath);
    }

    public void finish() throws ImportExportException {
        finished = true;
        // don't need to do anything
    }
}
