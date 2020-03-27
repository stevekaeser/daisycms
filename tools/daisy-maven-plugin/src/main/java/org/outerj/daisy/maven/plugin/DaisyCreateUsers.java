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
 * Create a new Daisy User.
 *
 * <pre>
 * &lt;plugin&gt;
 *   &lt;groupId&gt;daisy&lt;/groupId&gt;
 *   &lt;artifactId&gt;daisy-maven-plugin&lt;/artifactId&gt;
 *   &lt;executions&gt;
 *     &lt;execution&gt;
 *       &lt;id&gt;create-users&lt;/id&gt;
 *       &lt;goals&gt;
 *         &lt;goal&gt;create-users&lt;/goal&gt;
 *       &lt;/goals&gt;
 *       &lt;phase&gt;install&lt;/phase&gt;
 *       &lt;configuration&gt;
 *         &lt;users&gt;
 *           &lt;user&gt;
 *             &lt;login&gt;userlogin&lt;/login&gt;
 *             &lt;password&gt;userpass&lt;/password&gt;
 *             &lt;roles&gt;&lt;role&gt;&lt;name&gt;Administrator&lt;/name&gt;&lt;/role&gt;&lt;/roles&gt;
 *             &lt;defaultRole&gt;&lt;name&gt;Administrator&lt;/name&gt;&lt;/defaultRole&gt;
 *             &lt;confirmed&gt;true&lt;/confirmed&gt;
 *           &lt;/user&gt;
 *         &lt;/users&gt;
 *       &lt;/configuration&gt;
 *     &lt;/execution&gt;
 *   &lt;executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 *
 * @author Jan Hoskens
 * @goal create-users
 * @aggregator
 * @description Create daisy users.
 */
public class DaisyCreateUsers extends AbstractDaisyMojo {

    /**
     * @parameter expression="${users}"
     */
    private User[] users;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            createUsers();
        } catch (Exception e) {
            throw new MojoExecutionException("User creation failed.", e);
        }
    }

    private void createUsers() throws Exception {
        if (users == null)
            return;

        for (User user : users) {
            createUser(user);
        }
    }
}