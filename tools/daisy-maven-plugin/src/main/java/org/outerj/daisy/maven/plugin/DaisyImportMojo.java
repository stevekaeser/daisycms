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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.docset.DocumentSet;
import org.outerj.daisy.tools.importexport.docset.DocumentSetFactory;
import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.import_.Importer;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptions;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptionsFactory;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileFactory;

/**
 * Import a set of documents, based upon an export file/directory and a number
 * of {@link ImportOptions}.
 *
 * @goal import
 * @description Import daisy documents.
 */
public class DaisyImportMojo extends AbstractDaisyMojo {

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

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            ImportFile importFile = ImportFileFactory.getImportFile(importFileSource);

            DocumentSet documentSet = null;
            if (importSet != null) {
                InputStream is = null;
                try {
                    is = new FileInputStream(importSet);
                    documentSet = DocumentSetFactory.parseFromXml(is, getRepository());
                } catch (Throwable e) {
                    throw new ImportExportException("Error reading import document subset.", e);
                } finally {
                    if (is != null)
                        is.close();
                }
            }

            ImportOptions options;
            if (importOptionsFile != null) {
                InputStream is = null;
                try {
                    is = new FileInputStream(importOptionsFile);
                    options = ImportOptionsFactory.parseFromXml(is, getRepository());
                } finally {
                    if (is != null)
                        try {
                            is.close();
                        } catch (Exception e) { /* ignore */
                        }
                }
            } else {
                options = ImportOptionsFactory.getDefaultImportOptions();
            }

            ImportListener importListener = new LoggingImportListener(getLog());

            Importer.run(importFile, documentSet, getRepository(), options, importListener);
        } catch (Exception e) {
            throw new MojoExecutionException("Exception during document import.", e);
        }
    }

}