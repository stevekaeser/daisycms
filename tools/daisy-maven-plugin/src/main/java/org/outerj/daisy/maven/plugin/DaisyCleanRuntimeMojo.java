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
import java.io.IOException;
import java.util.Collection;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.outerj.daisy.maven.plugin.os.OsUtils;

/**
 * Delete the runtime by individually stopping and removing the services and
 * then removing the daisywiki/daisydata directories.
 *
 * <p>
 * Usage:
 *
 * <pre>
 * mvn daisy:clean-runtime
 * </pre>
 *
 * cleans both repository services, wiki services and the data directories for
 * the wiki and repository.
 * </p>
 * <p>
 * Parameters are available to disable wiki and/or repository cleaning:
 * <ul>
 * <li>-DdeleteWiki=false : don't delete the wiki</li>
 * <li>-DdeleteRepo=false : don't delete the repository</li>
 * </ul>
 * </p>
 *
 *
 * @author Jan Hoskens
 * @goal clean-runtime
 * @aggregator
 * @description Delete the wiki and/or repository services and their data
 *              directories.
 */
public class DaisyCleanRuntimeMojo extends AbstractDaisyMojo {

    /**
     * @parameter name="${deleteWiki}" default-value="true"
     */
    private boolean deleteWiki;

    /**
     * @parameter name="${deleteRepo}" default-value="true"
     */
    private boolean deleteRepo;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (deleteWiki)
            deleteWiki();

        if (deleteRepo)
            deleteRepo();
    }

    private void deleteWiki() throws MojoExecutionException {
        stopWikiService();

        uninstallWikiService();

        try {
            Collection<File> deletedFiles = OsUtils.deleteFile(wikiDataDir);
            if (getLog().isDebugEnabled()) {
                for (File deletedFile : deletedFiles) {
                    getLog().debug("Deleted: " + deletedFile.getAbsolutePath());
                }
            }
            getLog().info("Remove wiki dir: [OK]");
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot delete wikidata directory.", e);
        }
    }

    private void deleteRepo() throws MojoExecutionException {
        stopRepositoryService();

        uninstallRepositoryService();

        try {
            Collection<File> deletedFiles = OsUtils.deleteFile(repoDataDir);
            if (getLog().isDebugEnabled()) {
                for (File deletedFile : deletedFiles) {
                    getLog().debug("Deleted: " + deletedFile.getAbsolutePath());
                }
            }
            getLog().info("Remove repository data directory: [OK]");
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot delete daisydata directory.", e);
        }
    }
}
