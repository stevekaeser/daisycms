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
import org.outerj.daisy.backupTool.dbDump.DbDumperFactory;
import org.w3c.dom.Element;

public class ActiveMQEntryLoader extends AbstractEntryLoader {
    
    public ActiveMQEntryLoader(File amqConfig) throws Exception{
        super(amqConfig);
    }

    public void createEntries(BackupInstance buInstance) throws Exception {
        Element dbConf = BackupHelper.getElementFromDom(configDocument, "/beans/bean[@id='dataSource']");
        DbDumper dbDumper = DbDumperFactory.createDbDumper(
                BackupHelper.getStringFromDom(dbConf, "property[@name='url']/@value"),
                BackupHelper.getStringFromDom(dbConf, "property[@name='username']/@value"),
                BackupHelper.getStringFromDom(dbConf, "property[@name='password']/@value"));
        buInstance.addEntry(createDbEntry(buInstance, dbDumper, "activemq-dbDump.zip"));
    }

    public void reloadEntries(BackupInstance buInstance, boolean checkHashSums) throws Exception {
        if (areFilesBackedUp(buInstance, new String[] {"activemq-dbDump.zip"}, checkHashSums))
            createEntries(buInstance);
        else 
            System.out.println("ActiveMQ backup files were not found.  Skipping restore of ActiveMQ.");
    }

}
