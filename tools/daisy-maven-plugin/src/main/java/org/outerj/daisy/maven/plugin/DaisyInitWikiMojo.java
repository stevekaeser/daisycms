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
 * Copies/links files to the wiki data directory and adds/replaces properties in the wiki service configuration file.
 *
 * @author Jan Hoskens
 * @goal init-wiki
 * @description Initialise the wiki by copying or linking all custom configuration from the source directory.
 */
public class DaisyInitWikiMojo extends AbstractDaisyMojo {

    /**
     * Directory containing wikidata resources needed for this project
     * @parameter expression="${wikiSourceDir}" default-value="src/main/dsy-wiki"
     */
    private File wikiResource;

    /**
     * Additional directories containing wikidata resources needed for this project
     * @parameter
     */
    private File[] wikiResources;

    /**
     * This parameter won't allow the creation of links instead of making
     * copies. As a default, a Linux machine will get symbolic links to ease the
     * development setup.
     *
     * @parameter expression="${noLink}" default-value="false"
     */
    private boolean noLink;

    /**
     * Add/replace the following properties in the wiki service configuration file.
     *
     * @parameter expression="${wikiServiceConfiguration}"
     */
    private Properties wikiServiceConfiguration;

    /**
     * Only include the given names.
     *
     * @parameter
     */
    private String[] includes;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (wikiResource != null)
                copyOrLink(wikiResource, wikiDataDir, noLink, includes != null ? new NameFileFilter(Arrays.asList(includes)) : null);

            if (wikiResources != null)
                copyOrLink(Arrays.asList(wikiResources), wikiDataDir, noLink);

            if (wikiServiceConfiguration != null && wikiServiceConfiguration.size() > 0) {
                File serviceConfFile = new File(wikiDataDir, "service/daisy-wiki-service.conf");
                addOrReplaceProperties(serviceConfFile, wikiServiceConfiguration);
            }
        } catch (IOException e) {
            getLog().error(e);
        }
    }

}