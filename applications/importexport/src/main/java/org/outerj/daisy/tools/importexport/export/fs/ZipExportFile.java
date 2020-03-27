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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.export.ExportListener;

/**
 * An {@link ExportFile} implementation that writes the export data
 * to a zip file. Internally, data is first written to a temporary
 * directory which is than zipped.
 */
public class ZipExportFile extends LocalExportFile {
    private File zipFile;
    private ExportListener listener;

    public ZipExportFile(File zipFile, ExportListener listener) throws IOException {
        super(createTempDir("daisy-export-tmp-", zipFile.getParentFile()));
        this.zipFile = zipFile;
        this.listener = listener;
        listener.info("Using temporary directory " + getRoot().getAbsolutePath());
    }

    private static File createTempDir(String prefix, File parent) throws IOException {
        int counter = new Random(System.currentTimeMillis()).nextInt(10000) + 1000;

        File file = new File(parent, prefix + counter);
        while (file.exists()) {
            counter++;
            file = new File(parent, prefix + counter);
        }

        if (!file.mkdir())
            throw new IOException("Failed to create temporary directory, tried " + file.getAbsolutePath());

        return file;
    }

    public void finish() throws ImportExportException {
        super.finish();

        listener.info("Zipping the export.");
        try {
            fileToZip(getRoot(), zipFile, Deflater.BEST_COMPRESSION);
        } catch (Exception e) {
            throw new ImportExportException("Error zipping export file.", e);
        }

        try {
            deleteDirectory(getRoot());
        } catch (Exception e) {
            listener.info("Error deleting temporary directory " + getRoot() + " : " + e.getMessage());
        }
        listener.info("Zipping done.");
    }

    private void deleteDirectory(File file) throws Exception {
        for (File child : file.listFiles()) {
            if (child.isDirectory()) {
                deleteDirectory(child);
            } else {
                if (!child.delete())
                    throw new Exception("Failed to delete " + child.getAbsolutePath());
            }
        }
        if (!file.delete())
            throw new Exception("Failed to delete " + file.getAbsolutePath());
    }

    private static int BUFFER_SIZE = 32768;

    public void fileToZip(File file, File zipFile, int compressionLevel) throws Exception {
        zipFile.createNewFile();
        FileOutputStream fout = new FileOutputStream(zipFile);
        ZipOutputStream zout = null;
        try {
            zout = new ZipOutputStream(new BufferedOutputStream(fout));
            zout.setLevel(compressionLevel);
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (File child : files)
                    fileToZip(child, zout, file);
            } else if (file.isFile()) {
                fileToZip(file, zout, file.getParentFile());
            }
        } finally {
            if (zout != null)
                zout.close();
        }
    }

    private void fileToZip(File file, ZipOutputStream zout, File baseDir) throws Exception {
        checkInterrupted();
        String entryName = file.getPath().substring(baseDir.getPath().length() + 1);
        if (File.separatorChar != '/')
        	entryName = entryName.replace(File.separator, "/");
        if (file.isDirectory()) {
            zout.putNextEntry(new ZipEntry(entryName + "/"));
            zout.closeEntry();
            File[] files = file.listFiles();
            for (File child : files)
                fileToZip(child, zout, baseDir);
        } else {
            FileInputStream is = null;
            try {
                is = new FileInputStream(file);
                zout.putNextEntry(new ZipEntry(entryName));
                streamCopy(is, zout);
            } finally {
                zout.closeEntry();
                if (is != null)
                    is.close();
            }
        }
    }

    private static void streamCopy(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = is.read(buffer)) > 0) {
            os.write(buffer, 0, len);
        }
    }

    private void checkInterrupted() throws ImportExportException {
        if (listener.isInterrupted()) {
            listener.info("Export interrupted during zip.");
            throw new ImportExportException("Export interrupted on user's request.");
        }
    }
}
