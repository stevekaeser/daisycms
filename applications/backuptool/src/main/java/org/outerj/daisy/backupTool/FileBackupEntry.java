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
package org.outerj.daisy.backupTool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileBackupEntry extends AbstractBackupEntry {

    protected List<File> filesToBackup;

    protected File baseDirectory;

    private File tempDir;

    public FileBackupEntry(File fileToBackup, File backupFile, File baseDirectory, BackupInstance buInstance) throws Exception {
        super(backupFile, buInstance);
        filesToBackup = new ArrayList<File>();
        this.baseDirectory = baseDirectory;
        addFileToBackup(fileToBackup);
    }

    public FileBackupEntry(File backupFile, File baseDirectory, BackupInstance buInstance) {
        super(backupFile, buInstance);
        filesToBackup = new ArrayList<File>();
        this.baseDirectory = baseDirectory;
    }

    public FileBackupEntry(File backupFile, File[] filesToBackup, File baseDirectory, BackupInstance buInstance) {
        super(backupFile, buInstance);
        for (File file : filesToBackup)
            this.addFileToBackup(file);        
        this.baseDirectory = baseDirectory;
    }

    public void backup() throws Exception {        
        for (File tobackup : filesToBackup) {
            String pathSuffix = tobackup.getPath().substring(baseDirectory.getPath().length());

            File from = tobackup;
            File to = new File (tempDir, pathSuffix);
            try {
                BackupHelper.copyFile(from, to);
            } catch (Exception e) {
                throw new Exception("Error copying file from " + from.getAbsolutePath() + " to " + to.getAbsolutePath());
            }
        } 
    }

    protected File getTempPath() {
        return tempDir;
    }

    public void backupInit() throws Exception {
        tempDir = new File(buInstance.getDirectory(), this.backupFile.getName() + "-tmp");
        tempDir.mkdir();
    }

    public void restore() throws Exception {
        for (int i = 0; i < filesToBackup.size(); i++)
            BackupHelper.deleteFile(getFileToBackup(i), true);

        // make sure the base directory gets created, also when the
        // backupFile is not present (e.g. to recreate an empty indexstore or pubreq dir)
        baseDirectory.mkdirs();
        if (backupFile.exists()) {
            BackupHelper.unzipToDirectory(backupFile, baseDirectory);
        } else {
            System.out.println("Backup does not contain file " + backupFile.getName() + ", skipping its restore.");
        }
    }

    public void addFileToBackup(File file) {
        if (file.exists())
            filesToBackup.add(file);
    }

    public void removeFileFromBackup(File file) {
        filesToBackup.remove(file);
    }

    public void clearFilesFromBackup() {
        filesToBackup.clear();
    }

    public File getFileToBackup(int index) {
        return filesToBackup.get(index);
    }

    protected boolean isNothing() {
        return isEmpty(tempDir);
    }

    private boolean isEmpty(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            for (int i = 0; i < children.length; i++) {
                if (children[i].isFile()) {
                    return false;
                } else {
                    boolean dirEmpty = isEmpty(children[i]);
                    if (!dirEmpty)
                        return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }
}
