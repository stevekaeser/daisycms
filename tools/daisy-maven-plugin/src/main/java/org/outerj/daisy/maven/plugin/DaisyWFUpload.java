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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.outerj.daisy.workflow.WfProcessDefinition;

/**
 * Upload the given set of worfklow packages in Daisy.
 *
 * @author Jan Hoskens
 * @goal wf-upload
 * @aggregator
 * @requiresDependencyResolution runtime
 * @description Upload workflow(s).
 */
public class DaisyWFUpload extends AbstractDaisyMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        waitForWorkflow();
        String mimeType;
        BufferedInputStream bufferedInputStream;
        WfProcessDefinition processDefinition;

        if (workflow == null) {
            throw new MojoFailureException("workflow configuration is missing");
        }
        
        Set<Artifact> wfArtifacts = filterArtifacts(workflow, project.getArtifacts());
        for (Artifact workflow : wfArtifacts) {
            File wfFile = workflow.getFile();
            try {
                if (wfFile.getName().endsWith(".zip"))
                    mimeType = "application/zip";
                else
                    mimeType = "text/xml";

                bufferedInputStream = new BufferedInputStream(new FileInputStream(wfFile));

                processDefinition = getWorkflowManager().deployProcessDefinition(bufferedInputStream,
                        mimeType, daisyConfig.getLocale());
                getLog().info(
                        "Workflow \"" + processDefinition.getName() + "\" deployed with version "
                                + processDefinition.getVersion());
            } catch (Exception e) {
                getLog().error("Failed to upload workflow file: " + wfFile.getAbsolutePath(), e);
            }
        }
    }
}