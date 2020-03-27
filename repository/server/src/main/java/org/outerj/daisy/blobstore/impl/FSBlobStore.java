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
package org.outerj.daisy.blobstore.impl;

import org.outerj.daisy.blobstore.BlobStore;
import org.outerj.daisy.blobstore.NonExistingBlobException;
import org.outerj.daisy.blobstore.BlobIOException;
import org.outerj.daisy.configutil.PropertyResolver;
import org.outerj.daisy.backuplock.spi.SuspendableProcess;
import org.outerj.daisy.plugin.PluginRegistry;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

import javax.annotation.PreDestroy;
import java.io.*;
import java.security.SecureRandom;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link BlobStore} that stores blobs in a directory
 * on the filesystem.
 *
 */
public class FSBlobStore implements BlobStore, SuspendableProcess {
    private static final int DIRECTORY_DEPTH = 4;
    private static final int DIRECTORY_NAME_LENGTH = 2;
    private File directory;
    private SecureRandom random = null;
    private int KEYLENGTH = 20;
    private ReadWriteLock suspendWritesLock = new ReentrantReadWriteLock(true);
    private PluginRegistry pluginRegistry;
    private static final String SUSPEND_PROCESS_NAME = "Blobstore";

    public FSBlobStore(Configuration configuration, PluginRegistry pluginRegistry) throws Exception {
        this.pluginRegistry = pluginRegistry;
        this.configure(configuration);
        this.initialize();
    }

    @PreDestroy
    public void destroy() {
        pluginRegistry.removePlugin(SuspendableProcess.class, SUSPEND_PROCESS_NAME, this);        
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        String directoryName = PropertyResolver.resolveProperties(configuration.getChild("directory").getValue());
        directory = new File(directoryName);
        if (!directory.exists())
            throw new ConfigurationException("The specified directory does not exist: " + directoryName);
        if (!directory.isDirectory())
            throw new ConfigurationException("The specified directory is not a directory: " + directoryName);
    }

    private void initialize() throws Exception {
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        }
        catch(java.security.NoSuchAlgorithmException nsae) {
            // maybe we are on IBM's SDK
            random = SecureRandom.getInstance("IBMSecureRandom");
        }
        random.setSeed(System.currentTimeMillis());

        pluginRegistry.addPlugin(SuspendableProcess.class, SUSPEND_PROCESS_NAME, this);
    }

    private void checkSuspendWrites() {
        boolean hasSuspendLock = false;
        try {
            hasSuspendLock = suspendWritesLock.readLock().tryLock(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
        if (!hasSuspendLock)
            throw new RuntimeException("Write access to the blobstore is currently disabled. Try again later.");
    }

    public void delete(String name) throws NonExistingBlobException {
        checkSuspendWrites();
        try {
            File file = nameToFile(name);
            if (!file.exists())
                throw new NonExistingBlobException(name);

            file.delete();
        } finally {
            suspendWritesLock.readLock().unlock();
        }
    }

    public String store(byte[] data) throws BlobIOException {
        File file = null;
        FileOutputStream fos = null;
        checkSuspendWrites();
        try {
            try {
                file = createFile();
                fos = new FileOutputStream(file);
                fos.write(data);
                fos.getFD().sync();
            } finally {
                if (fos != null)
                    fos.close();
            }
        } catch (IOException e) {
            throw new BlobIOException("Error storing blob.", e);
        } finally {
            suspendWritesLock.readLock().unlock();
        }
        return fileToName(file);
    }

    private static final int BUFFER_SIZE = 32768;

    public String store(InputStream is) throws BlobIOException {
        checkSuspendWrites();
        try {
            File file = null;
            FileOutputStream fos = null;
            byte[] buffer = new byte[BUFFER_SIZE];
            try {
                try {
                    file = createFile();
                    fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        bos.write(buffer, 0, read);
                    }
                    bos.flush();
                    fos.getFD().sync();
                } finally {
                    if (fos != null)
                        fos.close();
                }
            } catch (IOException e) {
                throw new BlobIOException("Error storing blob.", e);
            }
            return fileToName(file);
        } finally {
            suspendWritesLock.readLock().unlock();
            try {
                is.close();
            } catch (IOException e) {
                throw new BlobIOException("Error closing input stream.", e);
            }
        }
    }

    public InputStream retrieve(String name) throws BlobIOException, NonExistingBlobException {
        File file = nameToFile(name);
        if (!file.exists())
            throw new NonExistingBlobException(name);

        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new BlobIOException("Error retrieving the blob named \"" + name + "\".", e);
        }
    }

    /**
     * Generates a filename and creates the file.
     *
     * <p>The implementation uses a secure random number generator, which is strictly
     * speaking not necessary, but it won't hurt either. This code is an adjustment of
     * the code that generates Web Continuation ID's in Cocoon.
     */
    private File createFile() throws IOException {
        byte[] bytes = new byte[KEYLENGTH];
        char[] result = new char[KEYLENGTH * 2];

        while (true) {
            random.nextBytes(bytes);

            for (int i = 0; i < KEYLENGTH; i++) {
                byte ch = bytes[i];
                result[2 * i] = Character.forDigit(Math.abs(ch >> 4), 16);
                result[2 * i + 1] = Character.forDigit(Math.abs(ch & 0x0f), 16);
            }

            String id = new String(result);
            synchronized (this) {
                File file = nameToFile(id);
                if (file.createNewFile()) {
                    return file;
                }                
            }
        }
    }

    public boolean suspendWrites(long msecs) throws InterruptedException {
        return suspendWritesLock.writeLock().tryLock(msecs, TimeUnit.MILLISECONDS);
    }

    public void resumeWrites() {
        suspendWritesLock.writeLock().unlock();
    }

    public Lock getAvoidSuspendLock() {
        return suspendWritesLock.readLock();
    }

    public boolean suspendExecution(long msecs) throws InterruptedException {
        return suspendWrites(msecs);
    }

    public void resumeExecution() {
        resumeWrites();
    }

    private File nameToFile(String name) {
        StringBuilder subdirName = new StringBuilder((DIRECTORY_NAME_LENGTH+1) * DIRECTORY_DEPTH);
        int position = 0;
        for (int i = 0; i < DIRECTORY_DEPTH; i++)
            subdirName.append(name.substring(position, position+=DIRECTORY_NAME_LENGTH)).append(File.separator);
        
        File subdir = new File(directory, subdirName.toString());
        subdir.mkdirs();
                   
        return new File(subdir, name.substring(position));
    }
    
    private String fileToName(final File file) throws BlobIOException{
        File workFile = new File(file, "");
        String name = file.getName();
        for (int i = 0; i < DIRECTORY_DEPTH; i++) {
            workFile = workFile.getParentFile();
            name = workFile.getName() + name;
        }
        
        if (name.length() != KEYLENGTH*2)
            throw new BlobIOException("Blob '" + file.getPath() + "' does not have the correct id '" + name + "'");
            
        return name;
    }
}
