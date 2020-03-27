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

import org.outerj.daisy.backupTool.dbDump.DbDumper;

public class DbBackupEntry extends AbstractBackupEntry {
    private DbDumper dbDumper;
    private File tmpFile;

   public DbBackupEntry(File backupFile, DbDumper dbDumper, BackupInstance buInstance) {
       super(backupFile, buInstance);
       String fileName = backupFile.getName();
       fileName = fileName.substring(0, fileName.lastIndexOf(".")+1) + "sql";
       tmpFile = new File(backupFile.getParentFile(), fileName );
       this.dbDumper = dbDumper;
   }   

    public void backupInit() throws Exception {
        this.backupFile.createNewFile();    
        tmpFile.createNewFile();
    }
    
    public void backup() throws Exception {        
        dbDumper.dump(tmpFile);
    }

    protected File getTempPath() {
        return tmpFile;
    }

    public void restore() throws Exception {
        tmpFile.createNewFile();
        BackupHelper.unzipToFile(backupFile, tmpFile);
        dbDumper.restore(tmpFile);
        BackupHelper.deleteFile(tmpFile);
    }

    protected boolean isNothing() {
        return false;
    }
}
