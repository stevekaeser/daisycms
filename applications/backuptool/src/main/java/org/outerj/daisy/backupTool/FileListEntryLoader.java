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
import java.util.List;

import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class FileListEntryLoader extends AbstractEntryLoader {

    public FileListEntryLoader(File listFile) throws Exception {
        super(listFile);
    }

    public void createEntries(BackupInstance buInstance) throws Exception {
        List entryNodes = new DOMXPath("/backup-entries/backup-entry").selectNodes(configDocument);
        for (int i = 0; i < entryNodes.size(); i++) {
            Element entryElement = (Element) entryNodes.get(i);
            String name = BackupHelper.getStringFromDom(entryElement, "@name");
            File basedir = new File(BackupHelper.getStringFromDom(entryElement, "@basedir"));
            File backupFile = new File(buInstance.getDirectory(), name  + ".zip");

            List elements = new DOMXPath("paths/path").selectNodes(entryElement);
            FileBackupEntry entry = new FileBackupEntry(backupFile, basedir, buInstance);
            for (int j = 0; j < elements.size(); j++) {
                Element element = (Element) elements.get(j);
                entry.addFileToBackup(new File(basedir, BackupHelper.getStringFromDom(element, ".")));
            }
            buInstance.addEntry(entry);
        }
        File backuppedList = new File (buInstance.getDirectory(), configFile.getName());
        if (!backuppedList.exists()) 
            BackupHelper.copyFile(configFile, backuppedList);
        
    }

    public void reloadEntries(BackupInstance buInstance, boolean checkHashSums) throws Exception {
        File oldConfigFile = configFile;
        Document oldConfigDocument = configDocument;
        
        configFile = new File (buInstance.getDirectory(), configFile.getName());
        configDocument = BackupHelper.parseFile(configFile);
        
        List entryNodes = new DOMXPath("/backup-entries/backup-entry").selectNodes(configDocument);
        String[] entries = new String[entryNodes.size()];
        for (int i = 0; i < entryNodes.size(); i++) {
            Element entryElement = (Element) entryNodes.get(i);
            entries[i] = BackupHelper.getStringFromDom(entryElement, "@name") + ".zip";            
        }
        
        if (areFilesBackedUp(buInstance, entries, checkHashSums))
            createEntries(buInstance);
        else
            System.out.println("Additional backup entries were not found.  Skipping restoration of these files");
        
        configFile = oldConfigFile;
        configDocument = oldConfigDocument;
    }
}
