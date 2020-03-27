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
import java.net.InetAddress;
import java.net.Socket;

import org.outerj.daisy.backupTool.dbDump.DbDumper;
import org.outerj.daisy.backupTool.dbDump.DbDumperFactory;
import org.w3c.dom.Element;

public class OpenJMSEntryLoader extends AbstractEntryLoader {

    public OpenJMSEntryLoader(File openjmsConfig) throws Exception {
        super(openjmsConfig);
    }

    public void createEntries(BackupInstance buInstance) throws Exception {
        Element dbConf = BackupHelper.getElementFromDom(configDocument, "/Configuration/DatabaseConfiguration/RdbmsDatabaseConfiguration");
        DbDumper dbDumper = DbDumperFactory.createDbDumper(dbConf.getAttribute("url"), dbConf.getAttribute("user"), dbConf.getAttribute("password"));

        buInstance.addEntry(createDbEntry(buInstance, dbDumper, "openjms-dbDump.zip"));
        buInstance.addEntry(createFileEntry(buInstance, configFile.getParentFile(), configFile.getParentFile(), "openjms-config.zip"));
    }

    public void reloadEntries(BackupInstance buInstance, boolean checkHashSums) throws Exception {
        String[] entries = new String[] { "openjms-dbDump.zip", "openjms-config.zip" };
        if (areFilesBackedUp(buInstance, entries, checkHashSums)) {
            // TODO put the openjms server running check elsewhere !!
            if (isOpenjmsRunning())
                throw new Exception("OpenJMS seems to be running.  Shut it down & try again.");

            createEntries(buInstance);
        } else {
            System.out.println("Openjms backup files were not found.  Skipping restore of openjms.");
        }
    }

    private boolean isOpenjmsRunning() throws Exception {
        boolean isRunning;
        String host = BackupHelper.getStringFromDom(configDocument, "/Configuration/ServerConfiguration/@host");
        int port = Integer.parseInt(BackupHelper.getStringFromDom(configDocument, "/Configuration/TcpConfiguration/@port"));

        try {
            Socket socket = new Socket(InetAddress.getByName(host), port);
            socket.close();
            isRunning = true;
        } catch (Exception e) {
            isRunning = false;
        }
        return isRunning;
    }

}
