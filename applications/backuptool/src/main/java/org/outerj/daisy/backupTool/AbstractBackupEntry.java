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

import org.outerj.daisy.util.ZipHelper;

import java.io.File;
import java.util.zip.Deflater;

public abstract class AbstractBackupEntry implements BackupEntry {
    protected File backupFile;
    protected BackupInstance buInstance;

    public AbstractBackupEntry (File backupFile, BackupInstance buInstance) {
        this.backupFile = backupFile;
        this.buInstance = buInstance;
    }

    public String generateDigest() throws Exception{
        return BackupHelper.generateMD5Hash(backupFile);
    }

    public void backupCleanup() throws Exception {
        if (!isNothing()) {
            try {
                ZipHelper.fileToZip(getTempPath(), backupFile, Deflater.BEST_COMPRESSION);
            } catch (Throwable e) {
                throw new Exception("Error zipping backup files into " + backupFile.getAbsolutePath(), e);
            }
        } else {
            System.out.println("Excluding " + backupFile.getName() + " from the backup because it is empty.");
        }

        try {
            BackupHelper.deleteFile(getTempPath());
        } catch (Throwable e) {
            throw new Exception("Error deleting temporary backup file or directory: " + getTempPath().getAbsolutePath(), e);
        }
    }

    protected abstract File getTempPath();

    /**
     * If this backup entry results in no file to be included in the backup,
     * this method returns true.
     */
    protected abstract boolean isNothing();
}
