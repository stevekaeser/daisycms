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
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceManager;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;

/**
 * <p>
 * Create a namespace with a specific fingerprint. If it already exists and the
 * fingerprint matches, update it. If a namespace is already registered with a
 * different namespace, an exception is thrown.
 * </p>
 *
 * <pre>
 * mvn daisy:create-ns -Dns=NAMESPACE -Duri=http://company.org/ns/my-namespace -Dmanaged=true
 * </pre>
 *
 * <p>
 * If needed, multiple namespaces can be created at once by configuring the mojo
 * in you pom as follows:
 * </p>
 *
 * <pre>
 * &lt;plugin&gt;
 *   &lt;groupId&gt;daisy&lt;/groupId&gt;
 *   &lt;artifactId&gt;daisy-maven-plugin&lt;/artifactId&gt;
 *   &lt;executions&gt;
 *     &lt;execution&gt;
 *       &lt;id&gt;create-ns&lt;/id&gt;
 *       &lt;goals&gt;
 *         &lt;goal&gt;create-ns&lt;/goal&gt;
 *       &lt;/goals&gt;
 *       &lt;phase&gt;install&lt;/phase&gt;
 *       &lt;configuration&gt;
 *         &lt;namespaces&gt;
 *           &lt;namespace&gt;&lt;ns&gt;NAMESPACE&lt;/ns&gt;&lt;uri&gt;http://company.com/namespace&lt;/uri&gt;&lt;managed&gt;true&lt;/managed&gt;&lt;/namespace&gt;
 *         &lt;/namespaces&gt;
 *       &lt;/configuration&gt;
 *     &lt;/execution&gt;
 *   &lt;executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 *
 * <p>
 * Additional configuration can be found in {@link AbstractDaisyMojo}.
 * </p>
 *
 * @author Jan Hoskens
 * @goal create-ns
 * @aggregator
 * @description Create a daisy namespace.
 */
public class DaisyCreateNamespaceMojo extends AbstractDaisyMojo {

    private List<org.outerj.daisy.maven.plugin.Namespace> namespaces;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        createNamespaces(namespaces);
    }

}
