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
package org.outerj.daisy.maven.plugin;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.outerj.daisy.maven.plugin.executor.Executor;
import org.outerj.daisy.maven.plugin.os.OsUtils;

/**
 * Show the current Daisy status. Displays information about the installation
 * and the services of Daisy.
 *
 * @author Jan Hoskens
 * @goal status
 * @aggregator
 */
public class DaisyServiceStatus extends AbstractDaisyMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            versionCheck();
            getLog().info("Version check (" + daisyVersion + "): [OK]");
        } catch (MojoExecutionException mee) {
            getLog().info("Version check (" + daisyVersion + "): [CONFLICT]");
            getLog().info(mee.toString());
        }

        checkFileAvailable(daisyHome, "Daisy runtime");

        checkFileAvailable(repoDataDir, "Daisy data dir");

        checkFileAvailable(wikiDataDir, "Daisy wiki dir");

        File serviceDir = new File(repoDataDir, "service");
        checkFileAvailable(serviceDir, "Daisy repository service scripts");

        serviceDir = new File(wikiDataDir, "service");
        checkFileAvailable(serviceDir, "Daisy wiki service scripts");

        try {
            getLog().info("Repository service:");
            if (OsUtils.isWindows()) {
                getLog().info(OsUtils.windowsServiceStatus(DAISY_REPOSITORY_SERVICE_NAME));
            } else {
                File serviceFile = new File(repoDataDir, "service/daisy-repository-server-service");
                if (serviceFile.exists())
                    Executor.executeCommand(new String[] { serviceFile.getAbsolutePath(), "status" },
                            getLog());
                else
                    getLog().info("Not available.");
            }

            getLog().info("Wiki service:");
            if (OsUtils.isWindows()) {
                getLog().info(OsUtils.windowsServiceStatus(DAISY_WIKI_SERVICE_NAME));
            } else {
                File serviceFile = new File(wikiDataDir, "service/daisy-wiki-service");
                if (serviceFile.exists())
                    Executor.executeCommand(new String[] { serviceFile.getAbsolutePath(), "status" },
                            getLog());
                else
                    getLog().info("Not available.");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Exception while fetching status.", e);
        }
    }

    private void checkFileAvailable(File file, String description) {
        getLog().debug("Checking directory/file: " + file.getAbsolutePath());
        if (file.exists())
            getLog().info(description + ": [OK]");
        else
            getLog().info(description + ": [NOT FOUND]");
    }
}