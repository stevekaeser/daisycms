/*
 * Copyright 2008 Outerthought bvba and Schaubroeck nv
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
import java.io.Writer;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.WriterFactory;

/**
 * This mojo can be used to re-deploy a number of dependencies to another
 * repository with a default generated pom. Select your dependencies by using
 * filters (as in the maven dependency plugin) then select your deployment
 * repository. As we're working with dependencies, the artifacts are already in
 * your local repository. Each artifact will be temporarily be copied, a default
 * pom will be generated and then both are uploaded to your local repository and
 * the specified deployment repository.
 *
 * Usage:
 *
 * <pre>
 * mvn daisy:generate-poms -DincludeGroupIds=daisy -DrepositoryId=<repositoryId> -DrepositoryLayout=default -Durl=<url>
 * </pre>
 *
 * @goal generate-poms
 * @requiresDependencyResolution runtime
 * @aggregator
 * @description Re-deploy dependencies with a default pom.
 */
public class DaisyGeneratePomsMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Indicates whether the project artifact itself should also be included
     *
     * @parameter default-value="false"
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
     * The dependencies to list in file.
     */
    private Collection<Artifact> dependenciesToList;

    /**
     * @parameter expression=
     *            "${component.org.apache.maven.artifact.deployer.ArtifactDeployer}"
     * @required
     * @readonly
     */
    private ArtifactDeployer deployer;

    /**
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Server Id to map on the &lt;id&gt; under &lt;server&gt; section of
     * settings.xml In most cases, this parameter will be required for
     * authentication.
     *
     * @parameter expression="${repositoryId}" default-value="remote-repository"
     * @required
     */
    private String repositoryId;

    /**
     * The type of remote repository layout to deploy to. Try <i>legacy</i> for
     * a Maven 1.x-style repository layout.
     *
     * @parameter expression="${repositoryLayout}" default-value="default"
     * @required
     */
    private String repositoryLayout;

    /**
     * Map that contains the layouts
     *
     * @component role=
     *            "org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout"
     */
    private Map repositoryLayouts;

    /**
     * URL where the artifact will be deployed. <br/>
     * ie ( file://C:\m2-repo or scp://host.com/path/to/repo )
     *
     * @parameter expression="${url}"
     * @required
     */
    private String url;

    /**
     * Component used to create a repository
     *
     * @component
     */
    private ArtifactRepositoryFactory repositoryFactory;

    /**
     * Whether to deploy snapshots with a unique version or not.
     *
     * @parameter expression="${uniqueVersion}" default-value="true"
     */
    private boolean uniqueVersion;

    public ArtifactDeployer getDeployer() {
        return deployer;
    }

    public void setDeployer(ArtifactDeployer deployer) {
        this.deployer = deployer;
    }

    public ArtifactRepository getLocalRepository() {
        return localRepository;
    }

    public void setLocalRepository(ArtifactRepository localRepository) {
        this.localRepository = localRepository;
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info("Creating Dependency List...");
            doDependencyResolution();
            if (dependenciesToList.size() > 0) {
                ArtifactRepositoryLayout layout;

                layout = (ArtifactRepositoryLayout) repositoryLayouts.get(repositoryLayout);
                ArtifactRepository deploymentRepository = repositoryFactory
                        .createDeploymentArtifactRepository(repositoryId, url, layout, uniqueVersion);
                for (Artifact artifact : dependenciesToList) {
                    getLog().info(
                            "Check artifact: " + artifact.getGroupId() + "::" + artifact.getArtifactId()
                                    + "::" + artifact.getVersion());
                    ArtifactMetadata metadata = new ProjectArtifactMetadata(artifact,
                            generatePomFile(artifact));
                    artifact.addMetadata(metadata);
                    File tempFile = File.createTempFile("mvninstall", ".jar");
                    FileUtils.copyFile(artifact.getFile(), tempFile);
                    getLog().info(
                            "deploy file: " + artifact.getFile() + " artifact " + artifact + " to "
                                    + deploymentRepository + " local " + getLocalRepository());
                    getDeployer().deploy(tempFile, artifact, deploymentRepository, getLocalRepository());
                }
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Error while deploying.", ex);
        }
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

        filter.addFilter(new ProjectTransitivityFilter(project.getDependencyArtifacts(), this.excludeTransitive));
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
        dependenciesToList = filter.filter(artifacts);
    }

    private File generatePomFile(Artifact artifact) throws MojoExecutionException {
        Writer fw = null;
        try {
            File tempFile = File.createTempFile("mvninstall", ".pom");
            tempFile.deleteOnExit();

            Model model = new Model();
            model.setModelVersion("4.0.0");
            model.setGroupId(artifact.getGroupId());
            model.setArtifactId(artifact.getArtifactId());
            model.setVersion(artifact.getVersion());
            model.setPackaging("jar");

            fw = WriterFactory.newXmlWriter(tempFile);
            new MavenXpp3Writer().write(fw, model);

            return tempFile;
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing temporary pom file: " + e.getMessage(), e);
        } finally {
            IOUtil.close(fw);
        }
    }
}
