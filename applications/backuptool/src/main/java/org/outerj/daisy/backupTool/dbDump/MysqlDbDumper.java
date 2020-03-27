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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MysqlDbDumper extends AbstractDbDumper {

    private static String dumpExecutableName = "mysqldump";

    private static String restoreExecutableName = "mysql";

    public MysqlDbDumper(String dbName, String host, Integer port, String password, String username) {
        super(dbName, host, port, password, username);
    }

    public void dump(File dumpFile) throws Exception {
        try {
            // see DSY-640 and DSY-681
            List<String> cmdlist = new ArrayList<String>();
            cmdlist.add(dumpExecutableName);
            cmdlist.add("--result-file=" + dumpFile.getPath());
            cmdlist.add("--single-transaction");
            cmdlist.add("--databases");
            cmdlist.add("--user=" + this.username);
            cmdlist.add("--password=" + this.password);
            cmdlist.add("--host=" + this.host);
            if (port.intValue() > 0) {
                cmdlist.add("--port=" + port);
            }
            cmdlist.add(this.dbName);
            Process dumpProcess = Runtime.getRuntime().exec(cmdlist.toArray(new String[cmdlist.size()]));

            handleRuntimeProcess(dumpProcess, null, System.out);

        } catch (IOException e) {
            throw new Exception("The " + dumpExecutableName + " command was not found.  Try putting this executable in your environments path variable.");
        }
    }

    public void restore(File dumpFile) throws Exception {
        int returnCode;

        String dropCommand = "mysqladmin -f --user=" + username + " --password=" + password + " -h " + host + (port.intValue() > 0 ? " --port=" + port : "") + " drop " + dbName;
        System.out.println("Running " + dropCommand);
        Process dropProcess = Runtime.getRuntime().exec(dropCommand);
        returnCode = dropProcess.waitFor();
        if (returnCode != 0)
            throw new Exception("Database drop command return with non-zero code: " + returnCode);

        String createCommand = "mysqladmin -f --user=" + username + " --password=" + password + " -h " + host + (port.intValue() > 0 ? " --port=" + port : "") + " create " + dbName;
        System.out.println("Running " + createCommand);
        Process createProcess = Runtime.getRuntime().exec(createCommand);
        returnCode = createProcess.waitFor();
        if (returnCode != 0)
            throw new Exception("Database create command return with non-zero code: " + returnCode);

        String command = restoreExecutableName + " --user=" + this.username + " --password=" + this.password + " --host=" + this.host
                + (port.intValue() > 0 ? " --port=" + port : "") + " " + this.dbName;

        System.out.println("Running " + command);
        System.out.println("and piping database script into it.");
        Process restoreProcess = Runtime.getRuntime().exec(command);

        InputStream fis = null;

        try {
            fis = new FileInputStream(dumpFile);
            handleRuntimeProcess(restoreProcess, fis, System.out);
        } finally {
            if (fis != null)
                fis.close();
        }
    }
}