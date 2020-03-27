/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.backupTool.dbDump;

import java.io.File;
import java.io.IOException;

public class PostgresqlDbDumper extends AbstractDbDumper {
    private static String dumpExecutableName = "pg_dump";

    private static String restoreExecutableName = "psql";

    public PostgresqlDbDumper(String dbName, String host, Integer port, String password, String username) {
        super(dbName, host, port, password, username);
    }

    public void dump(File dumpFile) throws Exception {
        try {
            System.out.println("Dumping database : " + dbName);
            String command = dumpExecutableName + " -U " + this.username + " --host=" + this.host + " --file=" + dumpFile.getPath()
                    + (port.intValue() > 0 ? " --port=" + port : "") + " --create --oids --no-owner " + this.dbName;

            Process dumpProcess = Runtime.getRuntime().exec(command);
            handleRuntimeProcess(dumpProcess, null, System.out);
        } catch (IOException e) {
            throw new Exception("The " + dumpExecutableName + " command was not found.  Try putting this executable in your environments path variable.");
        }
    }

    public void restore(File dumpFile) throws Exception {
        try {
            System.out.println("Restoring database : " + dbName + "\nYou may be prompted for the password of the following database user twice : " + username);
            String dropCommand = "dropdb -U " + username + " --host " + host + (port.intValue() > 0 ? " --port=" + port : "") + " --quiet " + dbName;           
            Process dropProcess = Runtime.getRuntime().exec(dropCommand);                    
            handleRuntimeProcess(dropProcess, null, System.out);

            String command = restoreExecutableName + " --username " + this.username + " --host " + this.host + " --file " + dumpFile.getPath()
                    + (port.intValue() > 0 ? " --port " + port : "") + " --quiet  template1";            
            Process restoreProcess = Runtime.getRuntime().exec(command);
            handleRuntimeProcess(restoreProcess, null, System.out);            
        } catch (IOException e) {
            throw new Exception("The " + restoreExecutableName + " command was not found.  Try putting this executable in your environments path variable.");
        }
    }
}