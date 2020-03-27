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
 * Removes the installed files from the wiki data dir.
 *
 * @author Jan Hoskens
 * @goal clean-wiki
 */
public class DaisyCleanWikiMojo extends AbstractDaisyMojo {

    /**
     * The source directory from which files/directories have to be
     * copied/linked.
     *
     * @parameter expression="${wikiSourceDir}"
     */
    private File wikiSourceDir;

    /**
     * Copy the contents of all given dirs to the wikiDataDir.
     *
     * @parameter
     */
    private File[] wikiSourceDirs;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        FileFilter svnAwareFilter = FileFilterUtils.makeSVNAware(null);
        if (wikiSourceDir != null)
            deleteDataDirEquivalent(wikiSourceDir, wikiDataDir, svnAwareFilter);

        if (wikiSourceDirs != null)
            deleteDataDirEquivalent(Arrays.asList(wikiSourceDirs), wikiDataDir, svnAwareFilter);

    }
}