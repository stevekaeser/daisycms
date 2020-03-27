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
package org.outerj.daisy.repository.testsupport;

import junit.framework.TestCase;

import java.io.*;
import java.util.Collections;
import java.util.Set;
import java.util.Map;

import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.launcher.remoteclient.RemoteClientLauncher;
import org.outerj.daisy.launcher.repository.RuntimeLauncher;
import org.outerj.daisy.launcher.repository.RuntimeHandle;
import org.outerj.daisy.jms.JmsClient;

public abstract class AbstractDaisyTestCase extends TestCase {

    protected RuntimeHandle runtime;
    private TestSupportConfig config;

    protected void setUp() throws Exception {
        super.setUp();

        config = new TestSupportConfig();

        installRepoConfig(null);

        if (resetDataStores()) {
            performResetDataStores();
        }

        launchRuntime();
    }

    private void installRepoConfig(String namespace) throws Exception {
        if (namespace == null)
            namespace = "DSYTEST";

        PrintWriter writer = new PrintWriter(new File("target/myconfig.xml"));
        BufferedReader reader = new BufferedReader(new FileReader("myconfig.xml.template"));

        String line;
        while ((line = reader.readLine()) != null) {
            // This is not nice code
            for (Map.Entry entry : config.entrySet()) {
                String key = (String)entry.getKey();
                String value = (String)entry.getValue();
                line = line.replaceAll("@" + key + "@", value);
            }

            line = line.replaceAll("@namespace@", namespace);

            writer.println(line);
        }

        writer.close();
        reader.close();
    }

    private void performResetDataStores() throws Exception {
        System.out.println("Reinstalling database...");
        DatabaseHelper dbHelper = new DatabaseHelper(config);
        dbHelper.resetDatabase("testuser", "testuser");

        String blobstore = config.getRequiredProperty("testsupport.blobstore");
        String indexstore = config.getRequiredProperty("testsupport.fulltextindexstore");

        File blobstoreFile = new File(blobstore);
        if (!blobstoreFile.exists() || !blobstoreFile.isDirectory())
            throw new Exception("Blobstore directory does not exist or is not a directory: " + blobstoreFile.getAbsolutePath());
        System.out.println("Deleting contents of blobstore directory (" + blobstoreFile.getAbsolutePath() + ")...");
        emptyDir(blobstoreFile);

        File indexstoreFile = new File(indexstore);
        if (!indexstoreFile.exists() || !indexstoreFile.isDirectory())
            throw new Exception("Indexstore directory does not exist or is not a directory: " + indexstoreFile.getAbsolutePath());
        System.out.println("Deleting contents of indexstore directory (" + indexstoreFile.getAbsolutePath() + ")...");
        emptyDir(indexstoreFile);
    }

    private void launchRuntime() {
        runtime = RuntimeLauncher.launch("../server/src/conf/runtime-config.xml",
                getRepositoryLocation().getAbsolutePath(),
                "target/myconfig.xml",
                getDisabledContainerIds());
    }

    private void emptyDir(File dir) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory())
                emptyDir(file);
            file.delete();
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        runtime.shutdown();
    }

    protected abstract boolean resetDataStores();

    protected Set<String> getDisabledContainerIds() {
        // workflow takes long to load and is only need by a few tests, so disable it by default
        return Collections.singleton("workflow");
    }

    protected Object getComponent(Class type) throws Exception {
        return runtime.getService(type);
    }

    protected RepositoryManager getRemoteRepositoryManager() throws Exception {
        JmsClient jmsClient = (JmsClient)getComponent(JmsClient.class);
        return (RepositoryManager)RemoteClientLauncher.getRepositoryManager("http://localhost:9263", new Credentials("testuser", "testuser"), jmsClient, config.getRequiredProperty("testsupport.jmstopic"), getRepositoryLocation());
    }

    protected RepositoryManager getLocalRepositoryManager() throws Exception {
        return (RepositoryManager)getComponent(RepositoryManager.class);
    }

    private File getRepositoryLocation() {
        return new File(getMavenHome() + File.separator + "repository");
    }

    private File getMavenHome() {
        try {
            String local = System.getProperty("maven.home.local", System.getenv("MAVEN_HOME_LOCAL"));
            if (local != null) return new File(local).getCanonicalFile();
            return new File(System.getProperty("user.home") + File.separator + ".m2").getCanonicalFile();
        } catch (IOException e) {
            throw new RuntimeException("Error trying to determine maven home.", e);
        }
    }

    protected void restartRepository(boolean cleanData, String namespace) throws Exception {
        runtime.shutdown();

        if (cleanData) {
            installRepoConfig(namespace);
            performResetDataStores();
        }

        launchRuntime();
    }
}
