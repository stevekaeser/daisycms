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
import java.io.FileNotFoundException;
import java.security.DigestException;

import org.outerj.daisy.backupTool.dbDump.DbDumper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class AbstractEntryLoader implements EntryLoader {
    protected File configFile;

    protected Document configDocument;

    public AbstractEntryLoader(File configFile) throws Exception {
        this.configFile = configFile;
        configDocument = BackupHelper.parseFile(configFile);
    }

    protected boolean isFileBackedUp(BackupInstance buInstance, String backupFileName, boolean checkHashSums) throws Exception {
        boolean entryOk = false;
        Element entryElement = BackupHelper.getElementFromDom(buInstance.getInstanceDocument(), "/backupInstance/entry[@filename = '" + backupFileName + "']", false);
        if (entryElement != null) {
            File backupFile = new File(buInstance.getDirectory(), entryElement.getAttribute("filename"));
            String hash = entryElement.getAttribute("checksum");
            if (!backupFile.exists()) {
                throw new FileNotFoundException("Backupfile " + backupFileName + " was not found");
            } else if (checkHashSums && !hash.equals(BackupHelper.generateMD5Hash(backupFile)))
                throw new DigestException(backupFileName + " has been tampered with");
            else
                entryOk = true;
        } else {
            // File was not backed up
            entryOk = false;
        }

        return entryOk;
    }

    protected boolean areFilesBackedUp(BackupInstance buInstance, String[] backupFiles, boolean checkHashSums) throws Exception {
        boolean entriesOk = true;
        for (int i = 0; i < backupFiles.length; i++)
            entriesOk = entriesOk & isFileBackedUp(buInstance, backupFiles[i], checkHashSums);

        return entriesOk;
    }

    protected DbBackupEntry createDbEntry(BackupInstance buInstance, DbDumper dumper, String backupFileName) {
        return new DbBackupEntry(new File(buInstance.getDirectory(), backupFileName), dumper, buInstance);
    }

    protected FileBackupEntry createFileEntry(BackupInstance buInstance, File toBackup, File baseDir, String backupFileName) throws Exception {
        return new FileBackupEntry(toBackup, new File(buInstance.getDirectory(), backupFileName), baseDir, buInstance);
    }
}
