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
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Copies/links files to the repository data directory and adds/replaces properties in the repository service configuration file.
 *
 * @author Jan Hoskens
 * @goal init-repo
 * @description Copies or links the repository data structure to the daisy data instance.
 */
public class DaisyInitRepoMojo extends AbstractDaisyMojo {

    /**
     * Directory containing repodata resources needed for this project
     * @parameter expression="${repoSourceDir}" default-value="src/main/dsy-data"
     */
    private File repoResource;

    /**
     * Additional directories containing repodata resources needed for this project
     * @parameter
     */
    private File[] repoResources;

    /**
     * This parameter won't allow the creation of links instead of making
     * copies. As a default, a Linux machine will get symbolic links to ease the
     * development setup.
     *
     * @parameter expression="${noLink}" default-value="false"
     */
    private boolean noLink;

    /**
     * Add/replace the following properties in the repo service configuration file.
     *
     * @parameter expression="${repoServiceConfiguration}"
     */
    private Properties repoServiceConfiguration;

    /**
     * Only include the given names.
     *
     * @parameter
     */
    private String[] includes;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (repoResource != null)
                copyOrLink(repoResource, repoDataDir, noLink, includes != null ? new NameFileFilter(Arrays.asList(includes)) : null);

            if (repoResources != null)
                copyOrLink(Arrays.asList(repoResources), repoDataDir, noLink);

            if (repoServiceConfiguration != null && repoServiceConfiguration.size() > 0) {
                File serviceConfFile = new File(repoDataDir, "service/daisy-repository-server-service.conf");
                addOrReplaceProperties(serviceConfFile, repoServiceConfiguration);
            }
        } catch (IOException e) {
            getLog().error(e);
        }
    }

}