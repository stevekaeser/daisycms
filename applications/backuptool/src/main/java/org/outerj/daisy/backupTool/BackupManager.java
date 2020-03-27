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
import java.util.ArrayList;
import java.util.List;

public class BackupManager {
    private static BackupManager instance;

    private static JMXRepositoryLocker locker;

    private static File backupLocation;

    private List<BackupInstance> backupInstances;

    private List<EntryLoader> entryLoaders;

    private boolean isRepositoryLocked;

    private BackupManager(File backupLocation, JMXRepositoryLocker locker) throws Exception {
        if (backupLocation != null && backupLocation.exists())
            BackupManager.backupLocation = backupLocation;
        else
            throw new Exception("Backup location is not set or does not exist");

        if (locker != null)
            BackupManager.locker = locker;
        else
            throw new Exception("Repository locker is not set");

        backupInstances = new ArrayList<BackupInstance>();
        entryLoaders = new ArrayList<EntryLoader>();
    }

    public static BackupManager getInstance() throws Exception {
        if (instance == null) {
            instance = new BackupManager(BackupManager.backupLocation, BackupManager.locker);
        }
        return instance;
    }

    public BackupInstance createBackupInstance() throws Exception {
        BackupInstance buInstance = new BackupInstance(backupLocation);
        for (EntryLoader entryLoader : entryLoaders) {
            entryLoader.createEntries(buInstance);
        }
        backupInstances.add(buInstance);
        return buInstance;
    }

    public BackupInstance loadBackupInstance(String backupName, boolean checkHashSums) throws Exception {
        BackupInstance buInstance = new BackupInstance(backupLocation, backupName);

        for (EntryLoader entryLoader : entryLoaders) {
            entryLoader.reloadEntries(buInstance, checkHashSums);
        }       

        backupInstances.add(buInstance);
        return buInstance;
    }
    
    public void rehash(BackupInstance buInstance) throws Exception {        
        buInstance.invalidate();
        BackupHelper.saveDocument(buInstance.getInstanceFile(), buInstance.getInstanceDocument());
    }

    public void registerEntryLoader(EntryLoader el) {
        entryLoaders.add(el);
    }

    public static File getBackupLocation() {
        return backupLocation;
    }

    public static void setBackupLocation(File backupLocation) {
        BackupManager.backupLocation = backupLocation;
    }

    public static JMXRepositoryLocker getLocker() {
        return locker;
    }

    public static void setLocker(JMXRepositoryLocker locker) {
        BackupManager.locker = locker;
    }

    public void backup(BackupInstance buInstance) throws Exception {
        for (int i = 0; i < buInstance.entryCount(); i++) {
            buInstance.getEntry(i).backupInit();
        }
        try {
            if (isRepositoryOn()) {
                System.out.println("Locking repository ...");
                locker.lock();
                isRepositoryLocked = locker.isLocked();
            } else {
                throw new Exception("Could not connect to repository.  Please turn on the repository server and try backing up again");                
            }

            System.out.println("Copying data to backup destination...");
            for (int i = 0; i < buInstance.entryCount(); i++) {
                buInstance.getEntry(i).backup();
            }

        } finally {
            if (isRepositoryLocked) {
                System.out.println("Unlocking repository ...");
                locker.unlock();
            }
        }

        System.out.println("Post-processing backup...");
        for (int i = 0; i < buInstance.entryCount(); i++) {
            buInstance.getEntry(i).backupCleanup();
        }

        BackupHelper.saveDocument(buInstance.getInstanceFile(), buInstance.getInstanceDocument());
        System.out.println("Done.");
    }

    public void restore(BackupInstance buInstance) throws Exception {
        if (!isRepositoryOn()) {
            System.out.println("Restoring backup-" + buInstance.getName() + " ...");
            for (int i = 0; i < buInstance.entryCount(); i++) {
                buInstance.getEntry(i).restore();
            }
            System.out.println("Restored backup-" + buInstance.getName() + ".");
        } else {
            throw new Exception("Repository server is running.  Please shut it down before restoring a backup.");
        }
    }

    private boolean isRepositoryOn() throws Exception {
        boolean isOn;
        try {
            Socket socket = new Socket(InetAddress.getByName(locker.getRepositoryHost()), locker.getRepositoryPort());
            socket.close();
            isOn = true;
        } catch (Exception e) {
            isOn = false;
        }
        return isOn;
    }
}
