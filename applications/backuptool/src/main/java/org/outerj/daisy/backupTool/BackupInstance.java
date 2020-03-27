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
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class BackupInstance {
    private final DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy", Locale.getDefault());
    private String name;
    private List<BackupEntry> entries;
    private File directory;
    private Date createDate;
    private String serverVersion;
    private File instanceFile;
    private Document instanceDocument;
    private boolean changed;
    
    public BackupInstance(File backupBaseDir) throws Exception{
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dirName = dateFormat.format(new Date());
        
        long seq = 0;
        DecimalFormat df = new DecimalFormat("000");
        do {
            String dirNameSeq = dirName + "_" + df.format(seq);
            directory = new File(backupBaseDir, dirNameSeq);
            seq++;
        } while (!directory.mkdir() && seq < 1000);
        if (seq > 999)
            throw new Exception("Could not create directory with prefix "
                    + dirName);
        
        entries =  new ArrayList<BackupEntry>();
        name = directory.getName();
        createDate = new Date();
        serverVersion = BackupManager.getLocker().getServerVersion();
    }
    
    public BackupInstance(File backupBaseDir, String backupName) throws Exception {
        this.name = backupName;
        directory = new File(backupBaseDir, this.name);       
        entries =  new ArrayList<BackupEntry>();
        if (!directory.exists())
            throw new Exception ("Backup " + name + " does not exist at " + directory.getPath());        

        instanceDocument = BackupHelper.parseFile(getInstanceFile());       
        
        createDate = dateFormat.parse(BackupHelper.getStringFromDom(instanceDocument, "/backupInstance/@createDate"));
        serverVersion = BackupHelper.getStringFromDom(instanceDocument, "/backupInstance/@serverVersion");
    }
    
    public void addEntry(BackupEntry entry) {
        entries.add(entry);
    }
    
    public BackupEntry getEntry(int index ) {
        return entries.get(index);
    }
    
    public int entryCount() {
        return entries.size();
    }
    
    public void backup() {
        
    }
    public void restore() {
        
    }
    public File getDirectory() {
        return directory;
    }
    public void setDirectory(File directory) {
        changed = true;
        this.directory = directory;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        changed = true;
        this.name = name;
    }

    public String getServerVersion() {
        return serverVersion;
    }
    
    public Document getInstanceDocument() throws Exception {
        if (changed || instanceDocument == null)
            instanceDocument = backupToDocument();
        return instanceDocument;
    }

    public File getInstanceFile() {
        if (instanceFile == null || instanceFile.getParentFile().equals(getDirectory()))
            instanceFile = new File(getDirectory(), "instance.xml");
        return instanceFile;
    }
    
    public void invalidate() {
        changed = true;
    }

    private Document backupToDocument() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.newDocument();        

        Element backupElement = document.createElement("backupInstance");
        document.appendChild(backupElement);
        backupElement.setAttribute("name", this.getName());
        backupElement.setAttribute("createDate", dateFormat.format(this.getCreateDate()));
        backupElement.setAttribute("serverVersion", serverVersion);

        for (int i = 0; i < this.entryCount(); i++) {
            AbstractBackupEntry buEntry = (AbstractBackupEntry) this.getEntry(i);
            if (buEntry.backupFile.exists()) {
                Element entry = document.createElement("entry");
                backupElement.appendChild(entry);
                entry.setAttribute("checksum", buEntry.generateDigest());
                entry.setAttribute("filename", buEntry.backupFile.getName());
            }
        }

        return document;
    }
}
