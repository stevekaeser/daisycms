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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.util.DefaultFileSet;
import org.codehaus.plexus.archiver.zip.ZipArchiver;

/**
 * Create a packaged daisy application
 *
 * @goal pack
 * @requiresDependencyResolution compile
 * @description Creates a packaged daisy applications, which is a zip file containing
 * &lt;ul>
 * &lt;/li>daisy/ (daisy home)&lt;li>
 * &lt;/li>repodata/ (repodata, excluding indexstore, blobstore, services and logs)&lt;li>
 * &lt;/li>wikidata/ (wikidata, excluding bookstore, services and logs)&lt;li>
 * &lt;/li>misc/ (workflow zips, daisy-js scripts, import data)&lt;li>
 * &lt;/li>other/ (misc files, e.g. installation or upgrade documentation)&lt;li>
 * &lt;/ul>
 */
public class DaisyPackMojo extends AbstractDaisyMojo {

    /**
     * The zip file or directory that contains the export.
     *
     * @parameter
     * @required
     */
    private File importFileSource;

    /**
     * The subset of documents to import.
     *
     * @parameter
     */
    private File importSet;

    /**
     * Additional options.
     *
     * @parameter expression="${importOptions}"
     */
    private File importOptionsFile;

    /**
     * Directory containing javascript files that are part of the application
     * @parameter expression="${jsDir}" default-value="${project.basedir}/src/main/dsy-js";
     */
    private File jsDir;
    
    /**
     * Other files that you may wish to include in the package
     * @parameter
     */
    private FileSet[] other;
    
    /**
     * Tmp directory used for assembling the package
     * @parameter expression="${daisyPackWorkDir}" default-value="${project.build.directory}/daisy-pack"
     */
    private File daisyPackWorkDir;
    
    /**
     * Package (destination) file
     * @parameter expression="${packageFile}" default-value="${project.build.directory}/${project.artifactId}-${project.version}-package.zip"
     */
    private File packageFile;
    
    /**
     * @parameter expression="${excludeDaisyHome}"
     */
    private boolean excludeDaisyHome = false;
    
    /**
     * Destination file for ACL.
     *
     * @parameter expression="${aclFile}" default-value="${project.basedir}/src/main/dsy-exp/acl.xml"
     * @required
     */
    private File aclFile;
    
    /**
     * Destination file for workflow pools.
     *
     * @parameter expression="${aclFile}" default-value="${project.basedir}/src/main/dsy-exp/wf-pools.xml"
     * @required
     */
    private File wfPoolFile;
    
    /**
     * By default, a script to upgrade between packaged application versions is included.
     * Set this to true if you do not want to include it.
     * @parameter expression="${excludeUpgradeScript}" default-value="false" 
     */
    private boolean excludeUpgradeScript = false;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!aclFile.exists()) {
            throw new MojoExecutionException("The specified acl file " + aclFile + " does not exist. Call daisy:export-acl before calling daisy:pack");
        }
        if (!wfPoolFile.exists()) {
            throw new MojoExecutionException("The specified wf pool file " + wfPoolFile + " does not exist. Call daisy:export-acl before calling daisy:pack");
        }
        if (!importFileSource.exists()) {
            throw new MojoExecutionException(importFileSource + " does not exist. Call daisy:export before calling daisy:pack.");
        }
        try {
            ZipArchiver archiver = new ZipArchiver();
            archiver.setDestFile(packageFile);
            
            File versionFile = new File(daisyPackWorkDir, "package.version");
            FileUtils.writeStringToFile(versionFile, project.getVersion());
            if (!excludeDaisyHome) {
                DefaultFileSet daisyHomeFileSet = new DefaultFileSet();
                daisyHomeFileSet.setDirectory(daisyHome);
                daisyHomeFileSet.setPrefix("daisy/");
                archiver.addFileSet(daisyHomeFileSet);
            }

            archiver.addFile(versionFile, versionFile.getName());
            DefaultFileSet repoDataFileSet = new DefaultFileSet();
            repoDataFileSet.setDirectory(repoDataDir);
            repoDataFileSet.setPrefix("repodata/");
            repoDataFileSet.setExcludes(new String[] { "activemq-data/", "indexstore/", "blobstore/", "service/", "logs/" });
            archiver.addFileSet(repoDataFileSet);

            DefaultFileSet wikiDataFileSet = new DefaultFileSet();
            wikiDataFileSet.setDirectory(wikiDataDir);
            wikiDataFileSet.setPrefix("wikidata/");
            wikiDataFileSet.setExcludes(new String[] { "tmp/", "bookstore/", "service/", "logs/" });
            archiver.addFileSet(wikiDataFileSet);

            if (workflow != null) {
                Set<Artifact> workflowArtifacts = filterArtifacts(workflow, project.getArtifacts());
                if (workflowArtifacts.size() > 0) {
                    addClasspathResource(archiver, "org/outerj/daisy/maven/plugin/scripts/wf-upload.js", "workflow/wf-upload.js");
                    for (Artifact wf: workflowArtifacts) {
                        File wfFile = wf.getFile();
                        archiver.addFile(wfFile, "workflow/" + wfFile.getName());
                    }
                }
            }
            
            if (importFileSource.isFile()) {
                archiver.addFile(importFileSource, "import/" + importFileSource.getName());
            } else {
                DefaultFileSet ifs = new DefaultFileSet();
                ifs.setDirectory(importFileSource.getAbsoluteFile());
                ifs.setPrefix("import/" + importFileSource.getName() + "/");
                archiver.addFileSet(ifs);
            }
            if (importSet != null) {
                archiver.addFile(importSet, "import/" + importSet.getName());
            }
            if (importOptionsFile != null) {
                archiver.addFile(importOptionsFile, "import/" + importOptionsFile.getName());
            }
            if (aclFile != null) {
                archiver.addFile(aclFile, "import/" + aclFile.getName());
                addClasspathResource(archiver, "org/outerj/daisy/maven/plugin/scripts/acltool.js", "import/acltool.js");
            }
            if (wfPoolFile != null) {
                archiver.addFile(wfPoolFile, "import/" + wfPoolFile.getName());
            }
            
            if (other != null) {
                for (FileSet o: other) {
                    DefaultFileSet oFileSet = new DefaultFileSet(); // todo: create helper function for creating filesets
                    List<String> includes = o.getIncludes();
                    List<String> excludes = o.getExcludes();
                    oFileSet.setDirectory(new File(o.getDirectory()));
                    oFileSet.setPrefix("other/");
                    oFileSet.setIncludes(includes.toArray(new String[includes.size()]));
                    oFileSet.setExcludes(excludes.toArray(new String[excludes.size()]));
                    archiver.addFileSet(oFileSet);
                }
            } else if (new File(project.getBasedir(), "src/main/dsy-other").exists()) {
                DefaultFileSet oFileSet = new DefaultFileSet();
                oFileSet.setPrefix("other/");
                oFileSet.setDirectory(new File(project.getBasedir(), "src/main/dsy-other"));
                archiver.addFileSet(oFileSet);
            }
            if (!excludeUpgradeScript) {
                addClasspathResource(archiver, "org/outerj/daisy/maven/plugin/scripts/upgrade-app.sh", "other/upgrade-app.sh");
            }
            
            archiver.createArchive();
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to build package", e);
        } catch (ArchiverException e) {
            throw new MojoExecutionException("Failed to build package", e);
        }
    }

    /**
     * Add a classpath resource to an archive
     * 
     * It should be possible to add classpath resources directly via archiver.addResource(...) but I couldn't figure out how, hence this method
     * 
     * @param archiver
     * @param resource path to the classpath resource
     * @param destPath destination path in the archive
     * @throws IOException
     * @throws FileNotFoundException
     * @throws ArchiverException
     */
    private void addClasspathResource(ZipArchiver archiver, String resource, String destPath) throws IOException,
            FileNotFoundException, ArchiverException {
        
        File file = new File(daisyPackWorkDir, destPath);
        file.getParentFile().mkdirs();
        IOUtils.copy(getClass().getClassLoader().getResourceAsStream(resource), new FileOutputStream(file));
        archiver.addFile(file, destPath);
    }
    
}