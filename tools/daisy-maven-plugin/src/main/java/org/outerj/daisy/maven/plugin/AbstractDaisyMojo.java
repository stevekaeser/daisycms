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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathException;

import org.apache.cocoon.maven.deployer.monolithic.XPatchDeployer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;
import org.outerj.daisy.maven.plugin.executor.ExecutionResult;
import org.outerj.daisy.maven.plugin.executor.Executor;
import org.outerj.daisy.maven.plugin.executor.LoggingExecutionResult;
import org.outerj.daisy.maven.plugin.os.OsUtils;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceManager;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.RoleNotFoundException;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.UserNotFoundException;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.workflow.WfPoolManager;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.clientimpl.RemoteWorkflowManagerProvider;
import org.w3c.dom.Document;

import sun.security.action.GetBooleanAction;

/**
 * Base class providing several utilities to use when working with Daisy.
 *
 * @author Jan Hoskens
 * @requiresDependencyResolution compile
 */
public abstract class AbstractDaisyMojo extends AbstractMojo {

    /** @component */
    protected org.apache.maven.artifact.factory.ArtifactFactory artifactFactory;

    /** @component */
    protected org.apache.maven.artifact.resolver.ArtifactResolver resolver;

    /**@parameter default-value="${localRepository}" */
    protected ArtifactRepository localRepository;

    /** @parameter default-value="${project.remoteArtifactRepositories}" */
    protected java.util.List remoteRepositories;

    /** @component */
    protected MavenProjectBuilder mavenProjectBuilder;
    
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
    
    protected ArtifactRepositoryLayout repositoryLayout = new DefaultRepositoryLayout();
    
    /** @component */
    protected ArtifactMetadataSource artifactMetadataSource;
    
    /** This is the name for the wiki service installation used in windows. */
    protected static final String DAISY_WIKI_SERVICE_NAME = "Daisy wiki";

    /**
     * This is the name for the repository service installation used in windows.
     */
    protected static final String DAISY_REPOSITORY_SERVICE_NAME = "Daisy repository";

    private Repository repository;

    private WorkflowManager workflowManager;

    private WfPoolManager wfPoolManager;

    private CollectionManager collectionManager;

    private UserManager userManager;

    private AccessManager accessManager;

    private NamespaceManager namespaceManager;

    private VariantManager variantManager;

    /**
     * @parameter expression="${maxNumberOfAttempts}" default-value="10"
     */
    protected int maxNumberOfAttempts;

    /**
     * @parameter expression="${attemptInterval}" default-value="1000"
     */
    protected long attemptInterval;

    /**
     * If defined, this version can be used to detect version conflicts.
     *
     * @parameter expression="${daisy.version}"
     */
    protected String daisyVersion;

    /**
     * Common daisy configuration object.
     *
     * @parameter
     */
    protected DaisyConfig daisyConfig;
    
    /**
     * @parameter expression="${bootstrapUser}" default-value="root"
     */
    protected String bootstrapUser;
    
    /**
     * @parameter expression="${bootstrapPassword}"
     */
    protected String bootstrapPassword;

    /**
     * @parameter expression="${daisy.home}"
     */
    protected File daisyHome;
    
    /**
     * @parameter expression="${daisy.data}"
     */
    protected File repoDataDir;
    
    /**
     * @parameter expression="${daisy.wiki}"
     */
    protected File wikiDataDir;

    /**
     * Plugins that should be loaded before the repository
     *
     * @parameter
     */
    protected FilterConfig loadBefore;
    
    /**
     * Plugins that should be loaded after the repository
     *
     * @parameter
     */
    protected FilterConfig loadAfter;
    
    /**
     * Workflows that should be deployed to the wiki
     *
     * @parameter
     */
    protected FilterConfig workflow;
    
    /**
     * Wiki extensions
     *
     * @parameter
     */
    protected FilterConfig wikiExtensions;
    
    /**
     * Filter which lets you limit which transitive dependencies of the wiki extensions are put in WEB-INF/lib 
     *
     * @parameter
     */
    protected FilterConfig wikiTransitiveDeps;
    
    /**
     * Side-effect: calls {@link #setEnvironment()} method to make sure the correct daisy server
     * location is used in the environment variables.
     *
     * @param daisyConfig
     * @throws MojoExecutionException
     */
    public void setDaisyHome(File daisyHome) throws MojoExecutionException {
        this.daisyHome = daisyHome;
        String runtimePath = this.daisyHome.toURI().getPath();
        if (runtimePath.endsWith("/"))
            runtimePath = runtimePath.substring(0, runtimePath.lastIndexOf("/"));
        if (System.getProperty("daisy.home") == null)
            System.setProperty("daisy.home", runtimePath);
        setEnvironment();
    }

    /**
     * Adds the DAISY_HOME environment variable to your already existing
     * configuration to make sure the variable is set to the correct location.
     * Note that this variable is appended at the end in order to overwrite any
     * existing one.
     */
    protected void setEnvironment() throws MojoExecutionException {
        Map<String, String> daisyMap = new HashMap<String, String>(System.getenv());

        // add JAVA_HOME if it's missing (JAVA_HOME must be set for daisy scripts)
        if (!daisyMap.containsKey("JAVA_HOME")) { 
            daisyMap.put("JAVA_HOME", System.getProperty("java.home"));
        }

        // add DAISY_HOME
        String daisyPath;
        try {
            daisyPath = daisyHome.getCanonicalPath();
        } catch (IOException e) {
            throw new MojoExecutionException("Could not get canonical path to DAISY_HOME", e);
        }
        daisyPath = daisyPath.endsWith("/") ? daisyPath.substring(0, daisyPath.length() - 1) : daisyPath;
        daisyMap.put("DAISY_HOME", daisyPath);
        
        String[] newEnv = new String[daisyMap.size()];
        int i = 0;
        for (String key: daisyMap.keySet()) {
            newEnv[i++] = key.concat("=".concat(daisyMap.get(key)));
        }

        Executor.env = newEnv;
        if (getLog().isDebugEnabled())
            getLog().debug(ArrayUtils.toString(newEnv));
    }

    /**
     * Check if an extracted Daisy package is available and that there are no version conflicts.
     *
     * @throws MojoExecutionException
     */
    protected void versionCheck() throws MojoExecutionException {
        String artifact = "daisy-repository-api-";
        File daisyLib = new File(daisyHome, "lib");
        if (!daisyLib.exists()) {
            throw new MojoExecutionException("No extracted Daisy package found in "
                    + daisyHome.getAbsolutePath()
                    + ".\n\tPlease download and extract Daisy version " + daisyVersion
                    + " from http://www.daisycms.org at the previous mentioned location.");
        }
        IOFileFilter fileFilter = new RegexFileFilter("^" + artifact + ".*\\.jar$");
        Collection files = FileUtils.listFiles(daisyLib, fileFilter, TrueFileFilter.INSTANCE);
        if (files == null || files.size() == 0) {
            throw new MojoExecutionException("Version check failed: No jar found at "
                    + daisyLib.getAbsolutePath());
        }
        if (files.size() != 1) {
            throw new MojoExecutionException("Version check failed: Found multiple jar versions. "
                    + ArrayUtils.toString(files));
        }
        String versionedDaisyApi = artifact + daisyVersion + ".jar";
        File artifactFile = (File)files.iterator().next();
        if (!versionedDaisyApi.equals(artifactFile.getName())) {
            String fileName = artifactFile.getName();
            String conflictVersion = fileName.substring(artifact.length(), fileName.length() - 4);
            throw new MojoExecutionException("Version check failed: Conflicting versions found "
                    + conflictVersion + " but should be " + daisyVersion);
        }
    }

    /**
     * Get the daisy repository, create one if needed.
     *
     * NOTE: we're setting the administrator role as actions may require full
     * access (should the above be configurable?)
     *
     * @return a Daisy Repository.
     * @throws Exception
     */
    protected Repository getRepository() throws Exception {
        if (repository == null) {
            Credentials credentials = new Credentials(bootstrapUser, bootstrapPassword);
            RemoteRepositoryManager repositoryManager = new RemoteRepositoryManager(daisyConfig
                    .getRepositoryURL(), credentials);
            repository = repositoryManager.getRepository(credentials);
            repository.setActiveRoleIds(new long[] { Role.ADMINISTRATOR });
        }
        return repository;
    }

    /**
     * Get the workflow manager.
     *
     * @return the {@link WorkflowManager}.
     */
    protected WorkflowManager getWorkflowManager() throws Exception {
        if (workflowManager == null)
            workflowManager = (WorkflowManager) new RemoteWorkflowManagerProvider()
                    .createExtension(getRepository());

        return workflowManager;
    }

    /**
     * Get the workflow manager.
     *
     * @return the {@link WorkflowManager}.
     */
    protected WfPoolManager getWfPoolManager() throws Exception {
        if (wfPoolManager == null)
            wfPoolManager = getWorkflowManager().getPoolManager();

        return wfPoolManager;
    }

    protected CollectionManager getCollectionManager() throws Exception {
        if (collectionManager == null)
            collectionManager = getRepository().getCollectionManager();

        return collectionManager;
    }

    protected UserManager getUserManager() throws Exception {
        if (userManager == null)
            userManager = getRepository().getUserManager();

        return userManager;
    }

    public AccessManager getAccessManager() throws Exception {
        if (accessManager == null)
            accessManager = getRepository().getAccessManager();

        return accessManager;
    }

    public NamespaceManager getNamespaceManager() throws Exception {
        if (namespaceManager == null)
            namespaceManager = getRepository().getNamespaceManager();

        return namespaceManager;
    }

    public VariantManager getVariantManager() throws Exception {
        if (variantManager == null)
            variantManager = getRepository().getVariantManager();

        return variantManager;
    }

    /**
     * Install the service scripts to run the repository/wiki.
     *
     * @throws MojoExecutionException
     */
    protected void installServices() throws MojoExecutionException {
        installRepositoryService();
        installWikiService();
    }

    /**
     * Install the repository service by invoking the necessary scripts provided
     * in the daisy package.
     *
     * @throws MojoExecutionException
     */
    protected void installRepositoryService() throws MojoExecutionException {
        if (repositoryServiceInstalled()) {
            getLog().info("Repository service already installed.");
        } else {
            if (OsUtils.isWindows()) {
                File scriptFile = new File(daisyHome, "install/daisy-service-install.bat");
                executeCommand(scriptFile.getAbsolutePath() + " -r "
                        + repoDataDir.getAbsolutePath());
                scriptFile = new File(repoDataDir,
                        "service/install-daisy-repository-server-service.bat");
                executeCommand(scriptFile.getAbsolutePath());
            } else {
                File scriptFile = new File(daisyHome, "install/daisy-service-install");
                executeCommand(new String[] { scriptFile.getAbsolutePath(), "-r",
                        repoDataDir.getAbsolutePath() });
            }
        }
    }

    /**
     * Install the wiki service by invoking the necessary scripts provided in
     * the daisy package.
     *
     * @throws MojoExecutionException
     */
    protected void installWikiService() throws MojoExecutionException {
        if (wikiServiceInstalled()) {
            getLog().info("Wiki service already installed.");
        } else {
            if (OsUtils.isWindows()) {
                File scriptFile = new File(daisyHome, "install/daisy-service-install.bat");
                executeCommand(scriptFile.getAbsolutePath() + " -w "
                        + wikiDataDir.getAbsolutePath());
                scriptFile = new File(wikiDataDir, "service/install-daisy-wiki-service.bat");
                executeCommand(scriptFile.getAbsolutePath());
            } else {
                File scriptFile = new File(daisyHome, "install/daisy-service-install");
                executeCommand(new String[] { scriptFile.getAbsolutePath(), "-w",
                        wikiDataDir.getAbsolutePath() });
            }
        }
    }

    /**
     * Restart both the repository and wiki services.
     */
    protected void restartServices() throws MojoExecutionException {
        stopServices();
        startServices();
    }

    protected void restartWikiService() throws MojoExecutionException {
        stopWikiService();
        startWikiService();
    }

    protected void restartRepositoryService() throws MojoExecutionException {
        stopRepositoryService();
        startRepositoryService();
    }

    /**
     * Start both the repository and wiki services.
     */
    protected void startServices() throws MojoExecutionException {
        startRepositoryService();
        startWikiService();
    }

    /**
     * Stop both the repository and wiki services.
     *
     * @throws MojoExecutionException
     */
    protected void stopServices() throws MojoExecutionException {
        stopWikiService();
        stopRepositoryService();
    }

    /**
     * Start the repository service.
     *
     * @throws MojoExecutionException
     */
    protected void startRepositoryService() throws MojoExecutionException {
        if (!repositoryServiceRunning()) {
            if (!repositoryServiceInstalled())
                installRepositoryService();

            if (OsUtils.isWindows()) {
                File startScript = new File(repoDataDir,
                        "service/start-daisy-repository-server-service.bat");
                executeCommand(startScript.getAbsolutePath());
            } else {
                File startScript = new File(repoDataDir,
                        "service/daisy-repository-server-service");
                executeCommand(new String[] { startScript.getAbsolutePath(), "start" });
            }
            waitForRepository();
        } else
            getLog().info("Repository service already running.");
    }

    /**
     * Stop the repository service. Note that you probably should stop the wiki
     * first to have a clean shutdown cycle.
     *
     * @throws MojoExecutionException
     */
    protected void stopRepositoryService() throws MojoExecutionException {
        if (repositoryServiceRunning()) {
            if (OsUtils.isWindows()) {
                File startScript = new File(repoDataDir,
                        "service/stop-daisy-repository-server-service.bat");
                executeCommand(startScript.getAbsolutePath());
            } else {
                File startScript = new File(repoDataDir,
                        "service/daisy-repository-server-service");
                executeCommand(new String[] { startScript.getAbsolutePath(), "stop" });
            }
        }
        getLog().info("Stopping repository service: [OK]");
    }

    /**
     * Start the wiki service. Note that your repository should already be
     * running.
     *
     * @throws MojoExecutionException
     */
    protected void startWikiService() throws MojoExecutionException {
        if (!wikiServiceRunning()) {
            if (!wikiServiceInstalled())
                installWikiService();

            if (OsUtils.isWindows()) {
                File startScript = new File(wikiDataDir,
                        "service/start-daisy-wiki-service.bat");
                executeCommand(startScript.getAbsolutePath());
            } else {
                File startScript = new File(wikiDataDir, "service/daisy-wiki-service");
                executeCommand(new String[] { startScript.getAbsolutePath(), "start" });
            }
        } else {
            getLog().info("Wiki service already running.");
        }
    }

    /**
     * Stop the wiki service.
     *
     * @throws MojoExecutionException
     */
    protected void stopWikiService() throws MojoExecutionException {
        if (wikiServiceRunning()) {
            if (OsUtils.isWindows()) {
                File startScript = new File(wikiDataDir,
                        "service/stop-daisy-wiki-service.bat");
                executeCommand(startScript.getAbsolutePath());
            } else {
                File startScript = new File(wikiDataDir, "service/daisy-wiki-service");
                executeCommand(new String[] { startScript.getAbsolutePath(), "stop" });
            }
        }
        getLog().info("Stopping wiki service: [OK]");
    }

    protected boolean repositoryServiceRunning() throws MojoExecutionException {
        if (OsUtils.isWindows()) {
            try {
                return OsUtils.windowsServiceRunning(DAISY_REPOSITORY_SERVICE_NAME);
            } catch (Exception e) {
                throw new MojoExecutionException("Repository running check failed.", e);
            }
        }

        File serviceFile = new File(repoDataDir, "service/daisy-repository-server-service");
        if (!serviceFile.exists())
            return false;

        return executeCommand(new String[] { serviceFile.getAbsolutePath(), "status" });
    }

    /**
     * Check if the service scripts are installed.
     *
     * @return <code>true</code> if the service scripts are available.
     */
    protected boolean repositoryServiceInstalled() throws MojoExecutionException {
        if (OsUtils.isLinux()) {
            File serviceFile = new File(repoDataDir, "service/daisy-repository-server-service");
            if (serviceFile.exists())
                return true;
        } else if (OsUtils.isWindows()) {
            try {
                return OsUtils.windowsServiceInstalled(DAISY_REPOSITORY_SERVICE_NAME);
            } catch (Exception e) {
                throw new MojoExecutionException("Repository installed check failed", e);
            }
        }
        return false;
    }

    /**
     * Uninstalling services is actually only needed when they are installed in
     * a different location or registry. This is the case on Windows machines,
     * but not on Linux machines. The latter ones only have a directory
     * containing startup scripts so uninstalling isn't really applicable in
     * that case.
     *
     * @return <code>true</code> if uninstall was successful.
     * @throws MojoExecutionException
     */
    protected void uninstallRepositoryService() throws MojoExecutionException {
        if (repositoryServiceInstalled()) {
            if (OsUtils.isWindows()) {
                try {
                    OsUtils.windowsDeleteService(DAISY_REPOSITORY_SERVICE_NAME);
                } catch (Exception e) {
                    throw new MojoExecutionException("Could not delete repository service.", e);
                }
            }
        }
        getLog().info("Uninstall repository service: [OK]");
    }

    protected boolean wikiServiceRunning() throws MojoExecutionException {
        if (OsUtils.isWindows()) {
            try {
                return OsUtils.windowsServiceRunning(DAISY_WIKI_SERVICE_NAME);
            } catch (Exception e) {
                throw new MojoExecutionException("Wiki running check failed.", e);
            }
        }

        File serviceFile = new File(wikiDataDir, "service/daisy-wiki-service");
        if (!serviceFile.exists())
            return false;

        return executeCommand(new String[] { serviceFile.getAbsolutePath(), "status" });
    }

    /**
     * Check if the service scripts are installed.
     *
     * @return <code>true</code> if the service scripts are available.
     * @throws Exception
     */
    protected boolean wikiServiceInstalled() throws MojoExecutionException {
        if (OsUtils.isLinux()) {
            File serviceFile = new File(wikiDataDir, "service/daisy-wiki-service");
            if (serviceFile.exists())
                return true;
        } else if (OsUtils.isWindows()) {
            try {
                return OsUtils.windowsServiceInstalled(DAISY_WIKI_SERVICE_NAME);
            } catch (Exception e) {
                throw new MojoExecutionException("Wiki installed check failed.", e);
            }
        }
        return false;
    }

    /**
     * Uninstalling services is actually only needed when they are installed in
     * a different location or registry. This is the case on Windows machines,
     * but not on Linux machines. The latter ones only have a directory
     * containing startup scripts so uninstalling isn't really applicable in
     * that case.
     *
     * @throws MojoExecutionException
     */
    protected void uninstallWikiService() throws MojoExecutionException {
        if (wikiServiceInstalled()) {
            if (OsUtils.isWindows()) {
                try {
                    OsUtils.windowsDeleteService(DAISY_WIKI_SERVICE_NAME);
                } catch (Exception e) {
                    throw new MojoExecutionException("Could not delete wiki service.", e);
                }
            }
        }
        getLog().info("Uninstall wiki service: [OK]");
    }

    /**
     * This method is a straight forward implementation of a 'is-alive' check to
     * make sure the repository is up and running before you connect. It tries
     * to access the repository for a number of times, waiting a fixed amount of
     * milliseconds in between and either returns an instance or throws an
     * exception.
     *
     * @throws MojoExecutionException
     */
    protected void waitForRepository() throws MojoExecutionException {
        int attempt = 0;
        boolean success = false;
        try {
            while (attempt <= maxNumberOfAttempts && success == false) {
                try {
                    getRepository();
                    success = true;
                } catch (Exception e) {
                    attempt++;
                    if (attempt <= maxNumberOfAttempts) {
                        getLog().info(
                                "Repository server not online, retrying in " + attemptInterval
                                        + " milliseconds, attempt " + attempt + " of " + maxNumberOfAttempts);
                        Thread.sleep(attemptInterval);
                    } else {
                        throw new MojoExecutionException("Repository server is not online!");
                    }
                }
            }
        } catch (InterruptedException e1) {
            throw new MojoExecutionException(
                    "Interuption while waiting for repository server to come online.");
        }
    }

    /**
     * This method is a straight forward implementation of a 'is-alive' check to
     * make sure the repository is up and running before you connect. It tries
     * to access the repository for a number of times, waiting a fixed amount of
     * milliseconds in between and either returns an instance or throws an
     * exception.
     *
     * @throws MojoExecutionException
     */
    protected void waitForWorkflow() throws MojoExecutionException {
        int attempt = 0;
        boolean success = false;
        try {
            while (attempt <= maxNumberOfAttempts && success == false) {
                try {
                    getWorkflowManager().getAllLatestProcessDefinitions(Locale.getDefault());
                    success = true;
                } catch (Exception e) {
                    attempt++;
                    if (attempt <= maxNumberOfAttempts) {
                        getLog().info(
                                "Workflow manager not online, retrying in " + attemptInterval
                                        + " milliseconds, attempt " + attempt + " of " + maxNumberOfAttempts);
                        Thread.sleep(attemptInterval);
                    } else {
                        throw new MojoExecutionException("Workflow manager is not online!");
                    }
                }
            }
        } catch (InterruptedException e1) {
            throw new MojoExecutionException("Interuption while waiting for workflow manager to come online.");
        }
    }

    /**
     * Execute the given command in the shell. Be aware that a single string
     * containing the command and the arguments might not produce the desired
     * result as they are seen as a whole. Use the other variant
     * {@link #executeCommand(String[])} to work around this.
     *
     * @param cmd
     *            Command to execute in the shell.
     * @return <code>true</code> if the command terminated with success
     * @throws MojoExecutionException
     */
    protected boolean executeCommand(String cmd) throws MojoExecutionException {
        return executeCommand(new String[] { cmd });
    }

    /**
     * Execute the given command in the shell. Uses the {@link Log} of the mojo
     * to display output.
     *
     * @param cmd
     *            Command(s) and parameter(s) to execute
     * @return <code>true</code> if the command terminated with success
     * @throws MojoExecutionException
     */
    protected boolean executeCommand(String[] cmd) throws MojoExecutionException {
        return executeCommand(cmd, new LoggingExecutionResult(getLog()));
    }

    /**
     * Execute the given command in the shell. Use the {@link ExecutionResult}
     * object to communicate output/errors and exit value.
     *
     * @param cmd
     *            Command(s) and parameter(s) to execute
     * @param executionResult
     *            the object that will hold the error/output and exit value.
     * @return <code>true</code> if the command terminated with success
     * @throws MojoExecutionException
     */
    protected boolean executeCommand(String[] cmd, ExecutionResult executionResult)
            throws MojoExecutionException {
        try {
            return Executor.executeCommand(cmd, executionResult);
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing command.", e);
        }

    }

    /**
     * Check if the given namespace exists. Daisy has a fail fast nature which
     * forces to deal with exceptions. This method avoids the exception
     * handling.
     *
     * @param namespace
     *            the namespace to check.
     * @return <code>true</code> if the namespace exists.
     * @throws Exception
     */
    protected boolean namespaceExists(String namespace) throws Exception {
        try {
            getRepository().getNamespaceManager().getNamespace(namespace);
        } catch (NamespaceNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * Determines whether or not you can have links instead of having to copy
     * your files. This can be useful in a development setup on Linux machines.
     *
     * @param src
     *            source file.
     * @param dest
     *            destination file.
     * @param filter
     *            a filter to apply if it is a directory and it needs to be
     *            traversed.
     * @throws IOException
     * @throws MojoExecutionException
     */
    protected void copyOrLink(File src, File dest, FileFilter filter) throws IOException,
            MojoExecutionException {
        copyOrLink(src, dest, filter, false);
    }

    /**
     * Determines whether or not you can have links instead of having to copy
     * your files. This can be useful in a development setup on Linux machines.
     * Strange as it may seem, this method allows to define a parameter to force
     * copying instead of linking. The reason this method exists as addition to
     * {@link #copyOrLink(File, File, FileFilter)} is to avoid multiple switches
     * in your calling code to decide whether or not to allow links. Just pass
     * the switch to this method instead.
     *
     * @param src
     *            source file.
     * @param dest
     *            destination file.
     * @param filter
     *            a filter to apply if it is a directory and it needs to be
     *            traversed.
     * @param noLink
     *            <code>true</code> if no links may be created.
     * @throws IOException
     * @throws MojoExecutionException
     */
    protected void copyOrLink(File src, File dest, FileFilter filter, boolean noLink) throws IOException,
            MojoExecutionException {
        if (noLink || !OsUtils.isLinux()) {
            copy(src, dest, filter);
        } else {
            linkFiles(src, dest, filter);
        }
    }

    /**
     * @param src Source file
     * @param dest Destination file. If this is an existing directory, the file name from the src file will be used.
     */
    protected void copyIfNewer(File src, File dest) throws IOException {
        if (dest.isDirectory()) {
            dest = new File(dest, src.getName());
        }
        
        if (!dest.exists() || src.lastModified() > dest.lastModified()) {
            getLog().info("Copying " + src + " to " + dest);
            FileUtils.copyFile(src, dest, true);
        } else {
            getLog().info("Not copying " + src + " to " + dest);
        }
    }

    /**
     * Copy a file to its destination. A slightly different approach is taken
     * when the source is a directory. If its destination already exists, a
     * directory may merge its contents with the existing one.
     *
     * @param src
     *            source file.
     * @param dest
     *            destination file.
     * @param filter
     *            filter to apply if it is a directory and it needs to be
     *            traversed.
     * @throws IOException
     */
    protected void copy(File src, File dest, FileFilter filter) throws IOException {
        if (src.isDirectory()) {
            FileUtils.copyDirectory(src, dest, filter);
            getLog().info("Copied " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
        } else if (src.isFile()) {
            FileUtils.copyFile(src, dest);
            getLog().info("Copied " + src.getAbsolutePath() + " to " + dest.getAbsolutePath());
        }
    }

    /**
     * Create links on a Unix type OS. Directories are simply created, files are linked.
     *
     * @param target
     *            the target to link to.
     * @param linkNameFile
     *            the file that contains the full path to the name of the link.
     * @param filter
     *            the filter to apply to list the files if the target is a
     *            directory and it already exists.
     * @throws MojoExecutionException
     */
    protected void linkFiles(File target, File linkNameFile, FileFilter filter) throws MojoExecutionException {
        if (linkNameFile.exists() && OsUtils.isLink(linkNameFile)) { // avoid going through existing links
            return;
        }
        if (target.isDirectory()) {
            if (!linkNameFile.exists()) {
                linkNameFile.mkdirs();
            }

            File[] childFiles = target.listFiles(filter);
            for (File childFile : childFiles) {
                linkFiles(childFile, new File(linkNameFile, childFile.getName()), filter);
            }
        } else if (target.isFile()) {
            if (linkNameFile.exists()) // removing file to create link to ours
                linkNameFile.delete();

            createLink(target, linkNameFile);
        }
    }

    /**
     * Create a symbolic link on a Unix type OS.
     *
     * @param target
     *            the target to link to.
     * @param linkNameFile
     *            the file that contains the full path to the name of the link.
     * @throws MojoExecutionException
     * @see #createLink(File, File, boolean)
     */
    protected void createLink(File target, File linkNameFile) throws MojoExecutionException {
        createLink(target, linkNameFile, false);
    }

    /**
     * Create a hard or symbolic link on a Unix type OS.
     *
     * @param target
     *            the target to link to.
     * @param linkNameFile
     *            the file that contains the full path to the name of the link.
     * @param hardLink
     *            <code>true</code> if you want to create a hard link. As a
     *            default, symbolic links are created.
     * @throws MojoExecutionException
     */
    protected void createLink(File target, File linkNameFile, boolean hardLink) throws MojoExecutionException {
        if (hardLink)
            executeCommand(new String[] { "ln", target.getAbsolutePath(), linkNameFile.getAbsolutePath() });
        else
            executeCommand(new String[] { "ln", "-s", target.getAbsolutePath(),
                    linkNameFile.getAbsolutePath() });
    }

    protected void patchFile(File target, File patchFile) throws MojoExecutionException, IOException,
            XPathException {
        File tmpDir = File.createTempFile("dsy", "tmp");
        tmpDir.delete();
        tmpDir.mkdir();
        XPatchDeployer xPatchDeployer = new XPatchDeployer(tmpDir.getAbsolutePath());
        xPatchDeployer.setBasedir(tmpDir);
        xPatchDeployer.setLogger(getLog());
        xPatchDeployer.addPatch(patchFile);

        xPatchDeployer.applyPatches(new FileInputStream(target), target.getName());

        FileUtils.copyFile(new File(tmpDir, target.getName()), target);
    }

    protected org.outerj.daisy.repository.user.User createUser(User user) throws Exception {
        org.outerj.daisy.repository.user.User newUser;
        try {
            newUser = getUserManager().getUser(user.getLogin(), true);
            getLog().info("User already exists: " + user.getLogin());
        } catch (UserNotFoundException e) {
            newUser = getUserManager().createUser(user.getLogin());
            newUser.setPassword(user.getPassword());
            newUser.setConfirmed(user.isConfirmed());
            newUser.setUpdateableByUser(user.isUpdateableByUser());
        }

        for (org.outerj.daisy.maven.plugin.Role role : user.getRoles()) {
            newUser.addToRole(createRole(role));
        }
        if (user.getDefaultRole() != null) {
            newUser.setDefaultRole(createRole(user.getDefaultRole()));
        }
        if (user.getFirstName() != null)
            newUser.setFirstName(user.getFirstName());
        if (user.getLastName() != null)
            newUser.setLastName(user.getLastName());
        if (user.getEmail() != null)
            newUser.setEmail(user.getEmail());
        newUser.save();
        getLog().info("User created/updated: id = " + newUser.getId() + ", login = " + newUser.getLogin());
        return newUser;
    }

    protected Role createRole(org.outerj.daisy.maven.plugin.Role role) throws Exception {
        org.outerj.daisy.repository.user.Role newRole;
        try {
            newRole = getUserManager().getRole(role.getName(), false);
            getLog().info("Role already exists: " + role.getName());
        } catch (RoleNotFoundException rnfe) {
            newRole = getUserManager().createRole(role.getName());
            newRole.setDescription(role.getDescription());
            newRole.save();
            getLog().info("Role created: id = " + newRole.getId() + ", name = " + newRole.getName());
        }
        return newRole;
    }

    public static void saveDOMDocument(Document document, File output) throws Exception {
        // set up a transformer
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // create string from xml tree
        FileWriter sw = new FileWriter(output);
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(document);
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(source, result);
    }

    /**
     * Replace/add key-value pairs in a Property file.
     *
     * @param fileName the configuration file to patch
     * @param properties the properties that should be replaced or added
     * @throws IOException
     */
    public void addOrReplaceProperties(File fileName, Properties properties) throws IOException {
        getLog().info("Adding/replacing properties in file " + fileName.getAbsolutePath() + "(" + properties.size() + " properties)");
        BufferedReader input = null;
        StringBuffer contents;
        try {
            input = new BufferedReader(new FileReader(fileName));
            String line;
            contents = new StringBuffer();
            Properties propertiesToHandle = new Properties();
            propertiesToHandle.putAll(properties);
            Properties propertiesHandled = new Properties();
            while ((line = input.readLine()) != null) {
                int equalsIndex = line.indexOf('=');
                String key = equalsIndex > 0 ? line.substring(0, equalsIndex) : null;
                if (key != null) {
                    int commentIndex = key.lastIndexOf('#');
                    key = commentIndex >= 0 ? key.substring(commentIndex + 1) : key;
                    key = key.trim();
                    if (propertiesToHandle.containsKey(key)) {
                        String value = propertiesToHandle.getProperty(key);
                        line = key + "=" + value;
                        propertiesHandled.setProperty(key, value);
                        propertiesToHandle.remove(key);
                        getLog().info("Replaced property: " + key + "=" + value + "(was: " + line.substring(equalsIndex + 1) + ")");
                    }
                }
                contents.append(line);
                contents.append(System.getProperty("line.separator"));
            }
            if (propertiesToHandle.size() > 0) {
                for (Entry<Object,Object> property : propertiesToHandle.entrySet()) {
                    contents.append(property.getKey() + "=" + property.getValue());
                    contents.append(System.getProperty("line.separator"));
                    getLog().info("Added property: " + property.getKey() + "=" + property.getValue());
                }
            }
        }
        finally {
            if (input!= null) {
                input.close();
            }
        }
        Writer output = null;
        try {
            output = new BufferedWriter( new FileWriter(fileName) );
            output.write( contents.toString() );
        }
        finally {
            if (output != null) {
                output.close();
            }
        }
    }

    public void copyOrLink(Collection<File> sourceDirs, File destinationDir, boolean noLink)
            throws MojoExecutionException, IOException {
        for (File sourceDir : sourceDirs) {
            copyOrLink(sourceDir, destinationDir, noLink);
        }
    }

    public void copyOrLink(File sourceDir, File destinationDir, boolean noLink)
            throws MojoExecutionException, IOException {
        copyOrLink(sourceDir, destinationDir, noLink, null);
    }

    public void copyOrLink(File sourceDir, File destinationDir, boolean noLink, IOFileFilter filter)
            throws MojoExecutionException, IOException {
        FileFilter svnAwareFilter = FileFilterUtils.makeSVNAware(null);
        File[] files = sourceDir.listFiles(filter != null ? filter : svnAwareFilter);
        if (files != null) {
            for (File file : files) {
                String localName = file.getName();
                getLog().info("Copying or linking: " + localName);
                copyOrLink(new File(sourceDir, localName), new File(destinationDir, localName), svnAwareFilter, noLink);
            }
        }
    }

    protected void deleteDataDirEquivalent(Collection<File> sourceDirs, File targetDir, FileFilter fileFilter) {
        for (File file : sourceDirs) {
            deleteDataDirEquivalent(file, targetDir, fileFilter);
        }
    }

    protected void deleteDataDirEquivalent(File sourceDir, File targetDir, FileFilter fileFilter) {
        for (File sourceFile : sourceDir.listFiles(fileFilter)) {
            File targetFile = new File(targetDir, sourceFile.getName());
            if (targetFile.exists()) {
                if (targetFile.isFile() || OsUtils.isLink(targetFile)) {
                    getLog().info("Deleting file: " + targetFile.getAbsolutePath());
                    targetFile.delete();
                } else if (targetFile.isDirectory()) {
                    deleteDataDirEquivalent(sourceFile, targetFile, fileFilter);
                    if (targetFile.list() == null) {
                        getLog().info("Deleting empty directory: " + targetFile.getAbsolutePath());
                        targetFile.delete();
                    }
                }
            }
        }
    }

    protected void createNamespaces(List<org.outerj.daisy.maven.plugin.Namespace> namespaces) throws MojoExecutionException {
        if (namespaces != null) {
            for (org.outerj.daisy.maven.plugin.Namespace namespace : namespaces) {
                createNamespace(namespace.getNs(), namespace.getUri(), namespace.isManaged());
            }
        }
    }

    protected void createNamespace(String ns, String uri, boolean managed) throws MojoExecutionException {
        try {
            final NamespaceManager nsm = getNamespaceManager();
            Namespace namespace = null;

            try {
                namespace = nsm.getNamespace(ns);
                final String fingerprint = namespace.getFingerprint();
                if (!uri.equals(fingerprint))
                    throw new MojoExecutionException("Namespace-fingerprint mismatch! " + ns
                            + " already has fingerprint: " + fingerprint);
            } catch (NamespaceNotFoundException e) {
                namespace = nsm.registerNamespace(ns, uri);
            }

            namespace.setManaged(managed);
            nsm.updateNamespace(namespace);
            getLog().info(
                    "Namespace registered: id=" + namespace.getId() + ", name=" + namespace.getName()
                            + ", fingerprint=" + namespace.getFingerprint() + ", managed="
                            + namespace.isManaged() + ", creatorId=" + namespace.getRegisteredBy()
                            + ", date=" + namespace.getRegisteredOn());

        } catch (Exception e) {
            getLog().error(e);
            throw new MojoExecutionException("Could not register namespace " + ns + " with uri " + uri);
        }
    }

    protected Set<Artifact> getDependencies(Artifact artifact)
            throws ProjectBuildingException, InvalidDependencyVersionException,
            ArtifactResolutionException, ArtifactNotFoundException {
        // get the transitive set of dependencies
        Artifact pomArtifact = artifactFactory.createArtifact(artifact.getGroupId(), artifact.getArtifactId(),artifact.getVersion(), artifact.getClassifier(), "pom");
        MavenProject pomProject = mavenProjectBuilder.buildFromRepository(pomArtifact, remoteRepositories, localRepository);

        Set<Artifact> pomArtifacts;
        pomArtifacts = pomProject.createArtifacts(artifactFactory, null, null);
        ArtifactResolutionResult arr = resolver.resolveTransitively(pomArtifacts, pomArtifact, localRepository, remoteRepositories, artifactMetadataSource, null);
        return arr.getArtifacts();
    }

   protected Set<Artifact> filterArtifacts(FilterConfig filterConf, Set<Artifact> artifacts) throws MojoExecutionException {
       FilterArtifacts filter = new FilterArtifacts();
       
       filter.addFilter(new ProjectTransitivityFilter(project.getDependencyArtifacts(),
               filterConf.isExcludeTransitive()));
       filter.addFilter(new ScopeFilter(filterConf.getIncludeScope(), filterConf.getExcludeScope()));
       filter.addFilter(new TypeFilter(filterConf.getIncludeTypes(), filterConf.getExcludeTypes()));
       filter.addFilter(new ClassifierFilter(filterConf.getIncludeClassifiers(), filterConf.getExcludeClassifiers()));
       filter.addFilter(new GroupIdFilter(filterConf.getIncludeGroupIds(), filterConf.getExcludeGroupIds()));
       filter.addFilter(new ArtifactIdFilter(filterConf.getIncludeArtifactIds(), filterConf.getExcludeArtifactIds()));

       try {
           return filter.filter(artifacts);
       } catch (ArtifactFilterException e) {
           throw new MojoExecutionException("Failed to filter artifacts");
       }

   }

}