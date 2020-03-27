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
package org.outerj.daisy.backupTool.dbDump;

import java.io.InputStream;
import java.io.OutputStream;

import org.outerj.daisy.backupTool.BackupHelper;

public abstract class AbstractDbDumper implements DbDumper {

    protected String dbName;

    protected String host;
    
    protected Integer port;

    protected String password;

    protected String username;
    
    public AbstractDbDumper (String dbName, String host, Integer port, String password, String username) {
        this.dbName = dbName;
        this.host = host;
        this.port = port;
        this.password = password;
        this.username = username;
    }

    protected static void handleRuntimeProcess(Process process, InputStream is,
            OutputStream os) throws Exception {
        InputStream pris = process.getInputStream();
        InputStream pres = process.getErrorStream();
        OutputStream pros = process.getOutputStream();
        Thread writeToProcess = null;
        Thread readFromProcess = null;
        Thread readErrors;
        try {
            if (is != null) {
                writeToProcess = new Thread(
                        new BackupHelper.RunnableStreamCopy(is, pros));
                writeToProcess.start();
            }
            if (os != null) {
                readFromProcess = new Thread(
                        new BackupHelper.RunnableStreamCopy(pris, os));
                readFromProcess.start();
            }
            readErrors = new Thread(new BackupHelper.RunnableStreamCopy(pres,
                    System.err));
            readErrors.start();

            if (writeToProcess != null)
                writeToProcess.join();
            pros.close();

            if (readFromProcess != null)
                readFromProcess.join();
            pris.close();

            readErrors.join();
            pres.close();

            if (process.waitFor() != 0)
                throw new RuntimeException(process + " ran with errors.");
        } finally {
            pris.close();
            pres.close();
            pros.close();
        }
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}