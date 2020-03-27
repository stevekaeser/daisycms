/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.frontend.editor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MimeTypeRegistry {
    
    private LogTextArea logArea;
    private Map<String, MimeTypeEntry> mimeTypeEntries;
    
    public MimeTypeRegistry(LogTextArea logArea) {
        this.logArea = logArea;
        mimeTypeEntries = new HashMap<String, MimeTypeEntry>();
    }
    
    @SuppressWarnings("unchecked")
    public void initialize(URL defaultsURL) {
        
        File mimeTypeRegistryFile = getMimeTypeRegistryFile();
        
        boolean initialized = false;

        if (mimeTypeRegistryFile.exists()) {
            try {
                mimeTypeEntries = loadMimeTypeEntries(new FileInputStream(mimeTypeRegistryFile));
                initialized = true;
            } catch (Exception e) {
                logArea.log("Could not load mimetype associations:", e);
            }
        }
        
        if (!initialized) {
            loadDefaults(defaultsURL);
            store();
        }
        
    }

    /**
     * Write the mimetype-registry information to the default file location
     */
    public void store() {
        File mimeTypeRegistryFile = getMimeTypeRegistryFile();
        File daisyDir = mimeTypeRegistryFile.getParentFile();
        if (!daisyDir.exists()) {
            if (!daisyDir.mkdirs()) {
                logArea.log("Could not create directory for storing mimetype registry :" + daisyDir.getAbsolutePath());
            }
        }
        
        try {
            logArea.log("Writing to registry file");
            
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(mimeTypeRegistryFile);
                store(fos);
            } finally {
                try { fos.close(); } catch (Exception e) {};
            }
            
            logArea.log("Stored mimetyperegistry data");
        } catch (IOException ioe) {
            logArea.log("Failed to store mimetyperegistry data", ioe);
        }
    }

    private File getMimeTypeRegistryFile() {
        String userHome = System.getProperty("user.home");
        File daisyDir = new File(userHome + File.separator + ".daisy");

        return new File(daisyDir.getAbsolutePath() + File.separator + "daisy_parteditor_mimetyperegistry.ini");
    }
    
    /**
     * Write the mimetype-registry information to the specified file location.
     * Closes the outputstream when finished.
     */
    public void store(OutputStream os) throws IOException {
        PrintWriter pw = new PrintWriter(os);
        try {
            for (String key: mimeTypeEntries.keySet()) {
                MimeTypeEntry entry = mimeTypeEntries.get(key);
                for (ApplicationEntry appEntry: entry.getApplicationEntries()) {
                    pw.println(String.format("[%s#%s]", key, appEntry.getTitle()));
                    pw.println(String.format("path:" + appEntry.getApplication()));
                    if (appEntry.getArguments() != null) {
                        pw.println("arguments:" + appEntry.getArguments());
                    }
                    pw.println();
                }
            }

        } finally {
            try { pw.close(); } catch (Exception e) {};
            try { os.close(); } catch (Exception e) {};
        }
    }
    
    private void cleanUp() {
        for (String type: mimeTypeEntries.keySet()) {
            mimeTypeEntries.get(type).cleanUp();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadDefaults(URL defaultsURL) {
        if (defaultsURL == null) {
            logArea.log("not loading defaults (url for loading defaults is null)");
            return;
        }

        logArea.log("loading defaults");
        
        try {
            mimeTypeEntries = loadMimeTypeEntries( defaultsURL.openConnection().getInputStream() );
        } catch (Exception e) {
            logArea.log("Failed to load defaults:", e);
        }

        cleanUp();
    }

    private Map<String, MimeTypeEntry> loadMimeTypeEntries(InputStream is) {
        Map<String, MimeTypeEntry> result = new HashMap<String, MimeTypeEntry>();
        
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        
        MimeTypeRegistry reg = new MimeTypeRegistry(null);
        
        String mimeType = null;
        String title = null;
        ApplicationEntry appEntry = null;

        String line = null;
        int linecount = 0;
        try {
            while ( null != ( line = r.readLine() ) ) {
                linecount++;
                line = line.trim();
                if ( line.length() == 0)
                    continue;
                
                if ( line.startsWith(";") )
                    continue;

                if ( line.startsWith("[") ) {
                    mimeType = line.substring(1, line.indexOf("#"));
                    title = line.substring(line.indexOf("#") + 1, line.indexOf("]"));
                    
                    appEntry = new ApplicationEntry(title, null, null);
                    MimeTypeEntry mimeTypeEntry = reg.getMimeTypeEntry(mimeType);
                    if (mimeTypeEntry.getApplicationEntries().size() == 0) {
                        mimeTypeEntry.setDefaultApplicationEntry(appEntry); // make the first entry the default application
                    } else {
                        mimeTypeEntry.addApplicationEntry(appEntry);
                    }
                } else if (line.startsWith("path")) {
                    appEntry.addApplication(line.substring(line.indexOf(":") + 1));
                } else if (line.startsWith("arguments")) {
                    appEntry.setArguments(line.substring(line.indexOf(":") + 1));
                } // else line skipped
            }
        } catch (Exception e) {
            logArea.log("Exception while parsing defaults file (line: " + linecount + ")", e);
        }

        return reg.mimeTypeEntries;
    }

    public void setApplication(String mimeType, ApplicationEntry applicationEntry) {
        MimeTypeEntry m = getMimeTypeEntry(mimeType);
        m.setDefaultApplicationEntry(applicationEntry);
    }
    
    public MimeTypeEntry getMimeTypeEntry(String mimeType) {
        if (!mimeTypeEntries.containsKey(mimeType)) {
            mimeTypeEntries.put(mimeType, new MimeTypeEntry());
        }
        return mimeTypeEntries.get(mimeType);
    }
    
    public void removeAllApplications(String mimeType) {
        mimeTypeEntries.remove(mimeType);
    }

    /**
     * set a mimeTypeEntry, overriding the previous one if it exists
     * @param mimeType
     * @param entry
     */
    public void setMimeTypeEntry(String mimeType, MimeTypeEntry entry) {
        mimeTypeEntries.put(mimeType, entry);
    }
    
}
