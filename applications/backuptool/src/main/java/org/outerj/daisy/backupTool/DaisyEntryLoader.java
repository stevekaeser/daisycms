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
import java.util.Properties;

import org.outerj.daisy.backupTool.dbDump.DbDumper;
import org.outerj.daisy.backupTool.dbDump.DbDumperFactory;
import org.outerj.daisy.configutil.PropertyResolver;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DaisyEntryLoader extends AbstractEntryLoader {
    private static String XPATH_INDEX_DIR = "/targets/target[@path = '/daisy/repository/fullTextIndex']/configuration/indexDirectory";
    private static String XPATH_BLOB_DIR = "/targets/target[@path = '/daisy/repository/blobstore']/configuration/directory";
    private static String XPATH_PUBREQ_DIR = "/targets/target[@path = '/daisy/extensions/publisher/publisher']/configuration/publisherRequestDirectory";
    private Properties resolveProps;

    public DaisyEntryLoader(File myConfigFile, File datadir) throws Exception {
        super(myConfigFile);
        resolveProps = PropertyResolver.getBaseProperties();
        resolveProps.setProperty("daisy.datadir", datadir.getAbsolutePath());
    }

    public void createEntries(BackupInstance buInstance) throws Exception {

        Element dbConfigElement = BackupHelper.getElementFromDom(configDocument, "/targets/target[@path = '/daisy/datasource/datasource']/configuration");

        DbDumper dbDumper = DbDumperFactory.createDbDumper(BackupHelper.getStringFromDom(dbConfigElement, "url"), BackupHelper.getStringFromDom(
                dbConfigElement, "username"), BackupHelper.getStringFromDom(dbConfigElement, "password"));

        File indexStoreDir = new File(resolve(BackupHelper.getStringFromDom(this.configDocument, XPATH_INDEX_DIR)));
        File blobStoreDir = new File(resolve(BackupHelper.getStringFromDom(this.configDocument, XPATH_BLOB_DIR)));
        File pubReqDir = new File(resolve(BackupHelper.getStringFromDom(this.configDocument, XPATH_PUBREQ_DIR)));
        File confDir = new File(blobStoreDir.getParentFile(), "conf");
        File serviceDir = new File(blobStoreDir.getParentFile(), "service");
        

        buInstance.addEntry(createDbEntry(buInstance, dbDumper, "daisy-dbDump.zip"));
        buInstance.addEntry(createFileEntry(buInstance, indexStoreDir, indexStoreDir, "daisy-indexstore.zip"));
        buInstance.addEntry(createFileEntry(buInstance, blobStoreDir, blobStoreDir, "daisy-blobstore.zip"));
        buInstance.addEntry(createFileEntry(buInstance, pubReqDir, pubReqDir, "daisy-pubreq.zip"));
        buInstance.addEntry(createFileEntry(buInstance, confDir, confDir, "daisy-conf.zip"));
        buInstance.addEntry(createFileEntry(buInstance, serviceDir, serviceDir, "daisy-service.zip"));
    }

    private String resolve(String value) {
        // to resolve property references such as ${daisy.datadir}
        return PropertyResolver.resolveProperties(value, resolveProps);
    }

    public void reloadEntries(BackupInstance buInstance, boolean checkHashSums) throws Exception {
        String[] entries = new String[] { "daisy-dbDump.zip", "daisy-conf.zip" };
        if (areFilesBackedUp(buInstance, entries, checkHashSums)) {
            File oldConfFile = configFile;
            File confTemp = new File(buInstance.getDirectory(), "confTemp");
            Document oldConfDocument = configDocument;

            BackupHelper.unzipToDirectory(new File(buInstance.getDirectory(), "daisy-conf.zip"), confTemp);
            configFile = new File(confTemp, "myconfig.xml");
            configDocument = BackupHelper.parseFile(configFile);

            createEntries(buInstance);
            BackupHelper.deleteFile(confTemp);
            configFile = oldConfFile;
            configDocument = oldConfDocument;
        } else {
            System.out.println("Daisy backup files were not found.  Skipping restore of daisy");
        }
    }
}
