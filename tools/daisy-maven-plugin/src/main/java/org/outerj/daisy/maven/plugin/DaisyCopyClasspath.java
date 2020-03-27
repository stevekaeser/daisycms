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
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;

/**
 * Copies the classpath of the current pom to the daisy instance. This can be
 * either a copy to the server lib directory or the wiki webapp lib directory.
 * Use the copyStyle to switch between both.
 *
 * @author Jan Hoskens
 * @goal copy-cp
 * @requiresDependencyResolution runtime
 * @description Copies the classpath of the current pom to the daisy instance.
 *              This can be either a copy to the server lib directory or the
 *              wiki webapp lib directory. Use the copyStyle to switch between
 *              both.
 */
public class DaisyCopyClasspath extends AbstractDaisyMojo {

    private static final String DAISYWIKI_LIB_DIR = "/daisywiki/webapp/WEB-INF/lib/";

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Indicates whether the project artifact itself should also be included
     *
     * @parameter expression="${includeSelf}" default-value="false"
     * @optional
     */
    protected boolean includeSelf;

    /**
     * If we should exclude transitive dependencies
     *
     * @since 2.0
     * @optional
     * @parameter expression="${excludeTransitive}" default-value="false"
     */
    protected boolean excludeTransitive;

    /**
     * Comma Separated list of Types to include. Empty String indicates include
     * everything (default).
     *
     * @since 2.0
     * @parameter expression="${includeTypes}" default-value=""
     * @optional
     */
    protected String includeTypes;

    /**
     * Comma Separated list of Types to exclude. Empty String indicates don't
     * exclude anything (default). Ignored if includeTypes is used.
     *
     * @since 2.0
     * @parameter expression="${excludeTypes}" default-value=""
     * @optional
     */
    protected String excludeTypes;

    /**
     * Scope to include. An Empty string indicates all scopes (default).
     *
     * @since 2.0
     * @parameter expression="${includeScope}" default-value="runtime"
     * @optional
     */
    protected String includeScope;

    /**
     * Scope to exclude. An Empty string indicates no scopes (default). Ignored
     * if includeScope is used.
     *
     * @since 2.0
     * @parameter expression="${excludeScope}" default-value=""
     * @optional
     */
    protected String excludeScope;

    /**
     * Comma Separated list of Classifiers to include. Empty String indicates
     * include everything (default).
     *
     * @since 2.0
     * @parameter expression="${includeClassifiers}" default-value=""
     * @optional
     */
    protected String includeClassifiers;

    /**
     * Comma Separated list of Classifiers to exclude. Empty String indicates
     * don't exclude anything (default). Ignored if includeClassifiers is used.
     *
     * @since 2.0
     * @parameter expression="${excludeClassifiers}" default-value=""
     * @optional
     */
    protected String excludeClassifiers;

    /**
     * Comma Seperated list of Artifact names too exclude. Ignored if
     * includeArtifacts is used.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${excludeArtifactIds}" default-value=""
     */
    protected String excludeArtifactIds;

    /**
     * Comma Seperated list of Artifact names to include.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${includeArtifactIds}" default-value=""
     */
    protected String includeArtifactIds;

    /**
     * Comma Seperated list of GroupId Names to exclude. Ignored if
     * includeGroupsIds is used.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${excludeGroupIds}" default-value=""
     */
    protected String excludeGroupIds;

    /**
     * Comma Seperated list of GroupIds to include.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${includeGroupIds}" default-value=""
     */
    protected String includeGroupIds;

    /**
     * The dependencies to copy to Daisy Lib folder.
     */
    private Collection<Artifact> dependenciesToCopy;

    /**
     * Copy the artifacts to the server repository.
     *
     * @parameter expression="${copyServer}" default-value="true"
     */
    protected boolean copyServer;

    /**
     * Copy the artifacts to the wiki WEB-INF/lib directory.
     *
     * @parameter expression="${copyWiki}" default-value="false"
     */
    protected boolean copyWiki;

    /**
     * Cleanup previous versions of the artifacts.
     *
     * @parameter expression="${cleanup}" default-value="true"
     */
    protected boolean cleanup;

    private File daisyRuntime;

    private ArtifactRepositoryLayout repositoryLayout = new DefaultRepositoryLayout();

    public void execute() throws MojoExecutionException, MojoFailureException {
        versionCheck();
        copyDependencies();
    }

    /**
     * This method uses a Filtering technique as showed by the
     * maven-dependency-plugin. It allows for including/excluding artifacts in a
     * number of ways.
     *
     * @throws MojoExecutionException
     */
    @SuppressWarnings("unchecked")
    private void doDependencyResolution() throws ArtifactFilterException {
        FilterArtifacts filter = new FilterArtifacts();

        filter.addFilter(new ProjectTransitivityFilter(project.getDependencyArtifacts(),
                this.excludeTransitive));
        filter.addFilter(new ScopeFilter(this.includeScope, this.excludeScope));
        filter.addFilter(new TypeFilter(this.includeTypes, this.excludeTypes));
        filter.addFilter(new ClassifierFilter(this.includeClassifiers, this.excludeClassifiers));
        filter.addFilter(new GroupIdFilter(this.includeGroupIds, this.excludeGroupIds));
        filter.addFilter(new ArtifactIdFilter(this.includeArtifactIds, this.excludeArtifactIds));

        // start with all artifacts.
        Set<Artifact> artifacts = project.getArtifacts();

        if (includeSelf)
            artifacts.add(project.getArtifact());

        // perform filtering
        dependenciesToCopy = new TreeSet<Artifact>(filter.filter(artifacts));
    }

    private void copyDependencies() throws MojoExecutionException {
        try {
            doDependencyResolution();
            daisyRuntime = daisyHome;
            File source;
            for (Artifact artifact : dependenciesToCopy) {
                source = artifact.getFile();
                if (copyWiki) {
                    if (cleanup)
                        cleanUpWiki(artifact);
                    copyFile(source, getWikiFile(artifact), artifact.isSnapshot());
                }
                if (copyServer) {
                    copyFile(source, getServerFile(artifact), artifact.isSnapshot());
                }
            }
        } catch (ArtifactFilterException e) {
            throw new MojoExecutionException("Could not copy dependencies to Daisy lib.", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not copy dependencies to Daisy lib.", e);
        }
    }

    private File getServerFile(Artifact artifact) {
        return new File(daisyRuntime, "lib/" + repositoryLayout.pathOf(artifact));
    }

    private File getWikiFile(Artifact artifact) {
        return new File(daisyRuntime, DAISYWIKI_LIB_DIR + artifact.getFile().getName());
    }

    private void copyFile(final File source, final File dest, final boolean force) throws IOException {
        if (force || !dest.exists()) {
            FileUtils.copyFile(source, dest);
            getLog().info("Copied artifact: " + dest.getAbsolutePath());
        } else {
            getLog().info("Already present: " + dest.getAbsolutePath());
        }
    }

    private void cleanUpWiki(Artifact artifact) {
        RegexFileFilter fileFilter = new RegexFileFilter(artifact.getArtifactId() + "-[\\d\\.]*(-SNAPSHOT)?.jar");
        Collection<File> oldArtifacts = FileUtils.listFiles(new File(daisyRuntime, DAISYWIKI_LIB_DIR), fileFilter, null);
        for (File oldArtifact : oldArtifacts) {
            if (oldArtifact.delete())
                getLog().info("Cleanup removed: " + oldArtifact.getAbsolutePath());
            else
                getLog().warn("Cleanup failed for: " + oldArtifact.getAbsolutePath());
        }
    }
}
