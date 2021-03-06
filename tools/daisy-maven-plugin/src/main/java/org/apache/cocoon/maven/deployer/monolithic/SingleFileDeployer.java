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
package org.apache.cocoon.maven.deployer.monolithic;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

import org.apache.cocoon.maven.deployer.utils.FileUtils;
import org.apache.commons.lang.Validate;
import org.apache.maven.plugin.logging.Log;

/**
 * Deploy a single file.
 * 
 * @version $Id: SingleFileDeployer.java 588009 2007-10-24 20:39:12Z vgritsenko $
 */
public class SingleFileDeployer implements FileDeployer {
    private File basedir;
    private Log logger;
    private String outputDir;
    private boolean skipRootDirectory;
    private Set alreadyDeployedFilesSet;

    public SingleFileDeployer(final String outputDir, final boolean skipRootDirectory) {
        Validate.notNull(outputDir, "An outputDir has to be set.");
        this.outputDir = outputDir;
        this.skipRootDirectory = skipRootDirectory;
    }

    public SingleFileDeployer(final String outputDir) {
        this(outputDir, false);
    }

    public void setBasedir(final File basedir) {
        this.basedir = basedir;
    }

    protected File getBasedir() {
        return this.basedir;
    }

    public void setLogger(final Log logger) {
        this.logger = logger;
    }

    protected Log getLogger() {
        return this.logger;
    }

    public void setAlreadyDeployedFilesSet(Set alreadyDeployedFilesSet) {
        this.alreadyDeployedFilesSet = alreadyDeployedFilesSet;
    }

    protected String getFileName(final String documentName) {
        return documentName.substring(documentName.lastIndexOf('/') + 1);
    }

    protected String getOutputDir() {
        return this.outputDir;
    }

    public OutputStream writeResource(final String documentName) throws IOException {
        File outDir = new File(this.getBasedir(), getOutputDir());
        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        String outputDocumentName = documentName;
        // if the root directory is has to be skipped, the remaining path will
        // be used
        if (this.skipRootDirectory) {
            outputDocumentName = removeRootDirectory(documentName);
            this.logger.debug("Changing output document name from '" + documentName + "', to '" + outputDocumentName
                    + "'.");
        }
        // only take the filename
        else {
            outputDocumentName = this.getFileName(documentName);
        }

        File targetFile = FileUtils.createPath(new File(outDir, outputDocumentName));
        if (this.alreadyDeployedFilesSet.contains(targetFile.getCanonicalFile())) {
            throw new FileAlreadyDeployedException("File '" + targetFile + "' already exists!");
        }

        this.alreadyDeployedFilesSet.add(targetFile.getCanonicalFile());
        this.logger.debug("Deploying block resource to " + getOutputDir() + "/" + outputDocumentName);

        return new FileOutputStream(FileUtils.createPath(targetFile));
    }

    protected String removeRootDirectory(final String documentName) {
        return documentName.substring(documentName.indexOf('/') + 1);
    }
}
