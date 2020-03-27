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
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Install a daisy repository plugin. Default configuration will check for
 * already installed plugins with the same artifactId and will remove them
 * prior to installing to avoid plugin clashes.
 *
 * <pre>
 * &lt;plugin&gt;
 *   &lt;groupId&gt;daisy&lt;/groupId&gt;
 *   &lt;artifactId&gt;daisy-maven-plugin&lt;/artifactId&gt;
 *   &lt;executions&gt;
 *     &lt;execution&gt;
 *       &lt;id&gt;install-plugin&lt;/id&gt;
 *       &lt;goals&gt;
 *         &lt;goal&gt;install-plugin&lt;/goal&gt;
 *       &lt;/goals&gt;
 *       &lt;phase&gt;install&lt;/phase&gt;
 *       &lt;configuration&gt;
 *         &lt;loadBefore&gt;
 *           &lt;includeGroupIds&gt;mygroup&lt;/includeGroupIds&gt;&lt;includeArtifactIds&gt;myartifact-before&lt;/includeArtifactIds&gt;
 *         &lt;/loadBefore&gt;
 *         &lt;loadAfter&gt;
 *           &lt;includeGroupIds&gt;mygroup&lt;/includeGroupIds&gt;&lt;includeArtifactIds&gt;myartifact-after&lt;/includeArtifactIds&gt;
 *         &lt;/loadAfter&gt;
 *         &lt;removeOldVersions&gt;true&lt;/removeOldVersions&gt;
 *       &lt;/configuration&gt;
 *     &lt;/execution&gt;
 *   &lt;executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 *
 * @author Jan Hoskens
 * @goal install-plugin
 */
public class DaisyInstallPluginMojo extends AbstractDaisyMojo {

    /**
     * Remove any old versions that are installed. (default: true)
     *
     * @parameter expression="${removeOldVersions}" default-value="true"
     */
    private boolean removeOldVersions;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        File loadBeforeDir = new File(repoDataDir, "/plugins/load-before-repository");
        File loadAfterDir = new File(repoDataDir, "/plugins/load-after-repository");

        if (loadBefore != null) {
            installPlugins(loadBefore, loadBeforeDir);
        }
        
        if (loadAfter != null) {
            installPlugins(loadAfter, loadAfterDir);
        }
        
        if (loadBefore == null && loadAfter == null) {
            throw new MojoFailureException("install-plugin goal called, but no plugin configurations are present");
        }
    }

    private void installPlugins(FilterConfig plugins, File dir) throws MojoExecutionException,
            MojoFailureException {

        Set<Artifact> pluginArtifacts = filterArtifacts(plugins, project.getArtifacts());
        
        for (Artifact pluginArtifact: pluginArtifacts) {
            try {
                installDependencies(getDependencies(pluginArtifact));
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to install dependencies for plugin " + pluginArtifact, e);
            }
            installPlugin(pluginArtifact, dir);
        }
        
    }

    /**
     * Copies all plugins and dependencies to the specified dir.
     */
    private void installPlugin(Artifact plugin, File dir) throws MojoFailureException {
        if (removeOldVersions) {
            doRemoveOldVersions(dir, plugin.getArtifactId());
            try {
                copyIfNewer(plugin.getFile(), new File(dir, plugin.getFile().getName()));
            } catch (IOException e) {
                throw new MojoFailureException("plugin installation failed while copying " + plugin.getFile(), e);
            }
        }
    }
    
    private void installDependencies(Collection<Artifact> dependencyArtifacts) throws MojoFailureException {
        for (Artifact artifact: dependencyArtifacts) {
            try {
                if (!artifact.getGroupId().equals("daisy")) // never copy artifacts with groupId='daisy' because that will confuse the versionCheck.
                    copyIfNewer(artifact.getFile(), getServerFile(artifact));
            } catch (IOException e) {
                throw new MojoFailureException("plugin installation failed while copying " + artifact.getFile(), e);
            }
        }
    }        
    
    private File getServerFile(Artifact artifact) {
        return new File(daisyHome, "lib/" + repositoryLayout.pathOf(artifact));
    }

    private void doRemoveOldVersions(File dir, String artifactName) {
        getLog().info("checking old versions: " + dir.getAbsolutePath() + "/" + "^" + artifactName + ".*\\.jar$");
        FileFilter fileFilter = new RegexFileFilter("^" + artifactName + ".*\\.jar$");
        File[] files = dir.listFiles(fileFilter);
        boolean success = false;
        
        if (files == null) // nothing to delete
            return;
        
        for (File fileToDelete : files) {
            success = fileToDelete.delete();
            if (success)
                getLog().info("Removed old plugin: " + fileToDelete.getAbsolutePath());
            else
                getLog().warn("Could not remove old plugin: " + fileToDelete.getAbsolutePath());
        }
    }
}