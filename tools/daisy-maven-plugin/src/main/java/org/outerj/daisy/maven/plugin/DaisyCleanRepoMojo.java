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
import java.io.FileFilter;
import java.util.Arrays;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Removes the links or copied files from the daisy data directory that
 * correspond to the files found in the given directory
 * <code>repoSourceDir</code>.
 *
 * @author Jan Hoskens
 * @goal clean-repo
 */
public class DaisyCleanRepoMojo extends AbstractDaisyMojo {

    /**
     * The directory that contains the configuration files that have been copied
     * to the daisy data directory.
     *
     * @parameter expression="${repoSourceDir}"
     */
    private File repoSourceDir;

    /**
     * The directories that contain configuration files that have been copied
     * to the daisy data directory.
     *
     * @parameter
     */
    private File[] repoSourceDirs;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        FileFilter svnAwareFilter = FileFilterUtils.makeSVNAware(null);
        if (repoSourceDir != null)
            deleteDataDirEquivalent(repoSourceDir, repoDataDir, svnAwareFilter);

        if (repoSourceDirs != null)
            deleteDataDirEquivalent(Arrays.asList(repoSourceDirs), repoDataDir, svnAwareFilter);
    }
}