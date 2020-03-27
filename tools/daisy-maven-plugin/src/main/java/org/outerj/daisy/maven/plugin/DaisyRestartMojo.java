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
 * Restart the Daisy repository and/or wiki.
 *
 * @author Jan Hoskens
 *
 * @goal restart
 * @requiresDependencyResolution runtime
 * @aggregator
 * @description Restart the daisy runtime.
 */
public class DaisyRestartMojo extends AbstractDaisyMojo {


    /**
     * Restart repo.
     *
     * @parameter expression="${repo}" default-value="true"
     */
    private boolean repo;

    /**
     * Restart wiki.
     *
     * @parameter expression="${wiki}" default-value="true"
     */
    private boolean wiki;

    /**
     * Workaround for daisy repo FULL startup.
     *
     * @parameter expression="${daisySleep}"
     */
    private boolean daisySleep = false;

    /**
     * The millis to sleep if {@link #daisySleep} is <code>true</code>.
     *
     * @parameter expression="${daisySleepInterval}" default-value="5000"
     */
    private int daisySleepInterval;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (repo && wiki)
            restartServices();
        else if (repo)
            restartRepositoryService();
        else if (wiki)
            restartWikiService();

        // workaround: there's no way to know when Daisy is completely up and running, therefore
        // you might want to hold on a few msecs to hope that everything is indeed there on the other side.
        if (daisySleep)
            try {
                Thread.sleep(daisySleepInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
    }

}
