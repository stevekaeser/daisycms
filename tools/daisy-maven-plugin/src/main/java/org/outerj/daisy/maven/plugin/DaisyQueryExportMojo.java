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
import java.util.Locale;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.outerx.daisy.x10.SearchResultDocument;

/**
 * This mojo allows to export a given daisy query to a file.
 *
 * @goal export-query
 * @requiresDependencyResolution runtime
 * @description Export a daisy query.
 */
public class DaisyQueryExportMojo extends AbstractDaisyMojo {

    /**
     * File destination for the export.
     *
     * @parameter expression="${exportQueryFile}"
     * @required
     */
    private File exportQueryFile;

    /**
     * The query to perform and export.
     *
     * @parameter expression="${exportQuery}"
     */
    private String exportQuery;

    /**
     * Force the export by deleting any existing directory/archive already
     * exported.
     *
     * @parameter expression="${force}" default-value="false"
     */
    private boolean force;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (exportQueryFile.exists()) {
                if (force) {
                    if (!exportQueryFile.delete())
                        throw new MojoExecutionException("Could not delete current export file: "
                                + exportQueryFile.getAbsolutePath());
                } else
                    throw new MojoExecutionException(
                            "Export file already present, specify -Dforce to delete.");
            }
            SearchResultDocument srd;
            getLog().info("Exporting query: " + exportQuery);
            srd = getRepository().getQueryManager().performQuery(exportQuery, Locale.getDefault());
            srd.save(exportQueryFile);
        } catch (Exception ioe) {
            throw new MojoExecutionException("something went wrong", ioe);
        }
    }

}
