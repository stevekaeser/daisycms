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
package org.outerj.daisy.tools.recode;

import java.io.File;
import java.util.List;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * recode maven plugin
 * @goal recode
 */
public class RecodeMojo extends AbstractMojo {
    
    /**
     * @parameter 
     * @required
     */
    private FileSet[] filesets;
    
    /**
     * @parameter default-value="UTF-8"
     */
    private String inputEncoding;
    
    /**
     * @parameter default-value="UTF-8"
     */
    private String outputEncoding;
    
    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        File lastFile = null;
        try {
            for (FileSet fileset: filesets) {
                String dir = fileset.getDirectory();
                if (dir == null) {
                    throw new MojoExecutionException("Fileset is missing a <directory> element.");
                }
                List files = FileUtils.getFiles(new File(dir), StringUtils.join(fileset.getIncludes().toArray(),","), StringUtils.join(fileset.getExcludes().toArray(),","));
                for (File target: (List<File>)files) {
                    lastFile = target;
                    getLog().info("Recoding " + target.getAbsolutePath());
                    RecodeUtil.recode(target, inputEncoding, outputEncoding);
                }
            }
        } catch (Exception e) {
            if (lastFile == null) {
                throw new MojoExecutionException("Unexpected error - rerun with -e flag for more details", e);
            }
            throw new MojoExecutionException("Failed to recode " + lastFile.getAbsolutePath(), e);
        }
    }
}
