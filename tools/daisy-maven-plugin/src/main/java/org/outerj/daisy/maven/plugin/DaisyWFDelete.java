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

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.outerj.daisy.workflow.WfProcessDefinition;

/**
 * Delete a workflow in Daisy by defining its process definition name.
 *
 * @author Jan Hoskens
 * @goal wf-delete
 * @aggregator
 * @requiresDependencyResolution runtime
 * @description Delete workflow(s).
 */
public class DaisyWFDelete extends AbstractDaisyMojo {

    /**
     * List of process definition names that correspond to workflows which have
     * to be deleted.
     *
     * @parameter
     * @required
     */
    private List<String> processDefinitionNames;

    /**
     * We need to request all process definitions and see if any name matches.
     *
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        waitForWorkflow();
        try {
            List<WfProcessDefinition> definitions = getWorkflowManager().getAllProcessDefinitions(
                    daisyConfig.getLocale());
            for (WfProcessDefinition definition : definitions) {
                if (processDefinitionNames.contains(definition.getName())) {
                    getWorkflowManager().deleteProcessDefinition(definition.getId());
                    getLog().info("Workflow \"" + definition.getName() + "\" deleted.");
                }
            }
        } catch (Exception e) {
            getLog().error("Workflow definition delete failed.", e);
        }
    }
}