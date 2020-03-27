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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Copies the one or more jars and their transitive dependencies to a destination directory.
 * If other versions of a jar are already present they will be removed first.
 *
 * @author karel
 * @goal install-wiki-ext
 * @requiresDependencyResolution runtime
 * @description Installs a wiki extension by deploying the artifact and its dependencies to WEB-INF/lib.
 *   This plugin also tries to avoid having multiple versions of the same artifact in the WEB-INF/lib dir.
 *           <lt;wikiExtensions>
 *             <lt;includeGroupIds>com.example</includeGroupIds>
 *             <lt;includeArtifactIds>myproject-daisywiki-extensions</includeArtifactIds>
 *           <lt;/wikiExtensions>
 *           <lt;!-- This is used to filter out dependencies that should be copied to WEB-INF/lib -->
 *           <lt;wikiTransitiveDeps>
 *             <lt;includeGroupIds>org.springframework,org.hibernate,jaxen<includeGroupIds>
 *           <lt;/wikiTransitiveDeps>
 */
public class DaisyInstallWikiExtension extends AbstractDaisyMojo {

    /**
     * @parameter expression="${wikiExtDestination}" default-value="${daisy.home}/daisywiki/webapp/WEB-INF/lib"
     */
    private File wikiExtDestination;

    /**
     * Replace other version of the artifacts to avoid two different jars of the same artifact
     * in the WEB-INF/lib dir.
     * @parameter expression="${removeOtherVersions}" default-value="true"
     */
    private boolean removeOtherVersions;

    public void execute() throws MojoExecutionException, MojoFailureException {
        versionCheck();
        
        // start with only the wiki extensions
        Set<Artifact> artifacts = filterArtifacts(wikiExtensions, project.getArtifacts());

        Set<Artifact> filteredTransitiveArtifacts = null;
        // for each extension, get the transitive dependencies
        Set<Artifact> transitiveArtifacts = new HashSet<Artifact>();
        try {
            for (Artifact wikiExt: artifacts) {
                transitiveArtifacts.addAll(getDependencies(wikiExt));
            }
            if (wikiTransitiveDeps != null) {
                filteredTransitiveArtifacts = filterArtifacts(wikiTransitiveDeps, transitiveArtifacts);
            } else {
                filteredTransitiveArtifacts = transitiveArtifacts;
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to collect the transitive dependencies for wiki extensions", e);
        }
        
        // now install everything
        for (Artifact a: artifacts) {
            cleanAndCopy(a);
        }
        if (filteredTransitiveArtifacts != null) {
            // note: here we get rid of the duplicates - but we also get rid of artifacts with multiple versions.
            for (Artifact a: (Collection<Artifact>)ArtifactUtils.artifactMapByVersionlessId(filteredTransitiveArtifacts).values()) {
                cleanAndCopy(a);
            }
        }
    }

    private void cleanAndCopy(Artifact artifact) throws MojoExecutionException {
        RegexFileFilter fileFilter = new RegexFileFilter(artifact.getArtifactId() + "-[\\d\\.]*(-SNAPSHOT)?.jar");
        Collection<File> oldArtifacts = FileUtils.listFiles(wikiExtDestination, fileFilter, null);
        for (File oldArtifact: oldArtifacts) {
            if (!oldArtifact.getName().equals(artifact.getFile().getName())) {
                if (removeOtherVersions) {
                    if (oldArtifact.delete())
                        getLog().info("Removed: " + oldArtifact.getAbsolutePath());
                    else
                        getLog().warn("Could not remove: " + oldArtifact.getAbsolutePath());
                }
            }
        }
        File dest = new File(wikiExtDestination, artifact.getFile().getName());
        try {
            copyIfNewer(artifact.getFile(), dest);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy artifact " + artifact.getFile() + " -> " + dest);
        }
    }
}
