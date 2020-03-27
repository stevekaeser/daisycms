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
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.outerj.daisy.tools.importexport.export.ExportListener;
import org.outerj.daisy.tools.importexport.export.ExportSet;
import org.outerj.daisy.tools.importexport.export.ExportSetFactory;
import org.outerj.daisy.tools.importexport.export.Exporter;
import org.outerj.daisy.tools.importexport.export.config.ExportOptions;
import org.outerj.daisy.tools.importexport.export.config.ExportOptionsFactory;
import org.outerj.daisy.tools.importexport.export.fs.ExportFile;
import org.outerj.daisy.tools.importexport.export.fs.ExportFileFactory;

/**
 * Export a set of Daisy documents to a directory or a zip file.
 *
 * @goal export
 * @description Export daisy documents.
 */
public class DaisyExportMojo extends AbstractDaisyMojo {

    /**
     * A directory or '.zip' file as destination for the export.
     *
     * @parameter expression="${exportFile}"
     * @required
     */
    private File exportFileDestination;

    /**
     * A file with the set of documents to export.
     *
     * @parameter expression="${exportSet}"
     * @required
     */
    private File exportSetFile;

    /**
     * Force the export by deleting any existing directory/archive already
     * exported.
     *
     * @parameter expression="${force}" default-value="false"
     */
    private boolean force;

    /**
     * Additional export options file.
     *
     * @parameter expression="${exportOptions}"
     */
    private File exportOptionsFile;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (exportFileDestination.exists()) {
                if (force) {
                    if (!exportFileDestination.delete())
                        throw new MojoExecutionException("Could not delete current export file/archive: "
                                + exportFileDestination.getAbsolutePath());
                } else
                    throw new MojoExecutionException(
                            "Export file/archive already present, specify -Dforce to delete.");
            }
            Date exportTime = new Date();
            ExportSet exportSet;
            InputStream is = null;
            try {
                is = new FileInputStream(exportSetFile);
                exportSet = ExportSetFactory.parseFromXml(is, getRepository());
            } catch (Throwable e) {
                throw new MojoExecutionException("Error reading export document set.", e);
            } finally {
                if (is != null)
                    is.close();
            }

            ExportListener exportListener = new LoggingExportListener(getLog());

            ExportFile exportFile = ExportFileFactory.getExportFile(exportFileDestination, exportListener);

            ExportOptions options;
            if (exportOptionsFile != null) {
                is = null;
                try {
                    is = new FileInputStream(exportOptionsFile);
                    options = ExportOptionsFactory.parseFromXml(is, getRepository());
                } finally {
                    if (is != null)
                        try {
                            is.close();
                        } catch (Exception e) { /* ignore */
                        }
                }
            } else {
                options = ExportOptionsFactory.getDefaultExportOptions();
            }

            Exporter.run(exportSet, exportFile, exportTime, getRepository(), options, exportListener);
            exportFile.finish();
        } catch (Exception ioe) {
            throw new MojoExecutionException("something went wrong", ioe);
        }
    }

}