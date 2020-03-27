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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Create Daisy roles.
 *
 * <pre>
 * &lt;plugin&gt;
 *   &lt;groupId&gt;daisy&lt;/groupId&gt;
 *   &lt;artifactId&gt;daisy-maven-plugin&lt;/artifactId&gt;
 *   &lt;executions&gt;
 *     &lt;execution&gt;
 *       &lt;id&gt;create-roles&lt;/id&gt;
 *       &lt;goals&gt;
 *         &lt;goal&gt;create-roles&lt;/goal&gt;
 *       &lt;/goals&gt;
 *       &lt;phase&gt;install&lt;/phase&gt;
 *       &lt;configuration&gt;
 *         &lt;roles&gt;
 *           &lt;role&gt;&lt;name&gt;AdminRole&lt;/name&gt;&lt;/role&gt;
 *           &lt;role&gt;&lt;name&gt;MyRole&lt;/name&gt;&lt;description&gt;A specific role&lt;/description&gt;&lt;/role&gt;
 *         &lt;/roles&gt;
 *       &lt;/configuration&gt;
 *     &lt;/execution&gt;
 *   &lt;executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 *
 * @author Jan Hoskens
 * @goal create-roles
 * @aggregator
 * @description Create daisy roles.
 */
public class DaisyCreateRoles extends AbstractDaisyMojo {

    /**
     * @parameter expression="${roles}"
     */
    private Role[] roles;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            createRoles();
        } catch (Exception e) {
            throw new MojoExecutionException("Role creation failed.", e);
        }
    }

    private void createRoles() throws Exception {
        if (roles == null)
            return;

        for (Role role : roles) {
            createRole(role);
        }
    }
}