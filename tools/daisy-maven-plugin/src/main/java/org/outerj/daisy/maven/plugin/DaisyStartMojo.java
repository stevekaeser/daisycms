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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Start the Daisy wiki and/or repository.
 *
 * @author Jan Hoskens
 * @goal start
 * @aggregator
 * @description Start the daisy runtime (repository and wiki).
 */
public class DaisyStartMojo extends AbstractDaisyMojo {

    /**
     * Start repo.
     *
     * @parameter expression="${repo}" default-value="true"
     */
    private boolean repo;

    /**
     * Start wiki.
     *
     * @parameter expression="${wiki}" default-value="true"
     */
    private boolean wiki;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (repo && wiki)
            startServices();
        else if (repo)
            startRepositoryService();
        else if (wiki)
            startWikiService();
    }

}