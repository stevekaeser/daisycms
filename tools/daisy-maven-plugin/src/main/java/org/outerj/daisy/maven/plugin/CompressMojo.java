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
package org.outerj.daisy.maven.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Compress/decompress files to/from an archive.
 *
 * Maven has an antRun plugin that allows packing/unpacking but for some reason the obvious 'unpack in-place' doesn't exist.
 * Therefore an own implementation is provided based upon the commons-compress library from Apache.
 *
 * Note: starting with unpacking ability as this is the one we currently need, implement packing when needed.
 *
 * @author Jan Hoskens
 * @goal compress
 * @description Compress tools..
 */
public class CompressMojo extends AbstractMojo {

    /**
     * Recurse into subdirectories.
     *
     * @parameter expression="${recursive}" default-value="true"
     */
    private boolean recursive;

    /**
     * Source directory containing archives.
     *
     * @parameter expression="${archiveSourceDir}"
     * @required
     */
    private File archiveSourceDir;

    /**
     * Delete the archive after extracting.
     *
     * @parameter expression="${deleteArchive}" default-value="false"
     */
    private boolean deleteArchive;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!archiveSourceDir.exists() || !archiveSourceDir.isDirectory()) { 
            getLog().warn("Skipping, file does not exist or is not a directory: " + archiveSourceDir.getAbsolutePath()); 
            return; 
        }     
    
        Collection<File> files = FileUtils.listFiles(archiveSourceDir, new String[] {"zip"}, recursive);
        Iterator<File> fileIterator = files.iterator();
        while (fileIterator.hasNext()) {
            File file = fileIterator.next();
            if (file.getName().endsWith("zip")) {
                extractZipArchive(file);
            } else {
                getLog().info("Extraction not yet implemented for: " + file.getAbsolutePath());
            }
        }
    }

    private void extractZipArchive(File file) throws MojoFailureException {
        ZipFile zipArchiveFile;
        try {
            zipArchiveFile = new ZipFile(file);
            Enumeration<ZipArchiveEntry> entries = zipArchiveFile.getEntries();
            ZipArchiveEntry entry;
            File destination;
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                destination = new File(file.getParent(), entry.getName());
                if (entry.isDirectory())
                    destination.mkdirs();
                else {
                    getLog().info("Extracting file: " + destination.getAbsolutePath());
                    destination.getParentFile().mkdirs();
                    destination.createNewFile();
                    FileOutputStream fos = new FileOutputStream(destination);
                    InputStream zis = zipArchiveFile.getInputStream(entry);
                    try {
                        IOUtils.copy(zis, fos);
                    } finally {
                        IOUtils.closeQuietly(zis);
                        IOUtils.closeQuietly(fos);
                    }
                }
            }
            if (deleteArchive)
                file.deleteOnExit();
        } catch (IOException ioe) {
            throw new MojoFailureException("Could not read zip file.",ioe);
        }
    }
}
