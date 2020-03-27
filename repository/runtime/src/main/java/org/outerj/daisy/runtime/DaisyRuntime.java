/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.runtime;

import org.outerj.daisy.runtime.repository.ArtifactRepository;
import org.outerj.daisy.runtime.repository.ArtifactRef;
import org.outerj.daisy.runtime.repository.ArtifactNotFoundException;
import org.outerj.daisy.runtime.component.ContainerBuilder;
import org.outerj.daisy.runtime.component.Container;
import org.outerj.daisy.runtime.component.ContainerConfig;
import org.outerj.daisy.runtime.cli.Logging;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;

public class DaisyRuntime {
    private final List<ContainerEntry> imports;
    private ArtifactRepository repository;
    private DaisyRuntimeConfig config;
    private ClassLoader rootClassLoader;
    private List<Container> containers;
    private Map<Class, Object> sharedServices = new HashMap<Class, Object>();
    protected final Log infolog = LogFactory.getLog(Logging.INFO_LOG_CATEGORY);

    public DaisyRuntime(List<ContainerEntry> imports, Properties configProps, ArtifactRepository repository) {
        this(new DaisyRuntimeConfig(imports, configProps, repository));
    }

    public DaisyRuntime(DaisyRuntimeConfig config) {
        this.imports = config.getImports();
        this.repository = config.getRepository();
        this.config = config;
    }

    public void init() throws DaisyRTException, MalformedURLException, ArtifactNotFoundException {
        // Check there are no containers with duplicate IDs
        Set<String> idSet = new HashSet<String>();
        for (ContainerEntry entry : imports) {
            if (idSet.contains(entry.getId()))
                throw new DaisyRTException("Duplicate container ID: " + entry.getId());
            idSet.add(entry.getId());
        }

        List<ContainerConfig> containerConfigs = new ArrayList<ContainerConfig>();

        // First read the configuration of each container, and do some classpath checks
        if (infolog.isInfoEnabled())
            infolog.info("Reading container configurations of " + imports.size() + " containers.");
        for (ContainerEntry entry : imports) {
            if (infolog.isInfoEnabled())
                infolog.debug("Reading container config " + entry.getId() + " - " + entry.getFile().getAbsolutePath());
            ContainerConfig containerConf = ContainerBuilder.build(entry, config.getConfigProperties(), this);
            containerConfigs.add(containerConf);
        }

        // Check / build class path configurations
        List<ArtifactRef> sharedArtifacts = ClassLoaderConfigurer.configureClassPaths(containerConfigs, config.getEnableArtifactSharing());

        // Construct the classloader
        infolog.debug("Creating shared classloader");

        List<URL> sharedClassPath = new ArrayList<URL>();
        for (ArtifactRef artifact : sharedArtifacts) {
            sharedClassPath.add(repository.resolve(artifact));
        }

        rootClassLoader = new URLClassLoader(sharedClassPath.toArray(new URL[0]), this.getClass().getClassLoader());


        // Create the containers
        infolog.info("Instantiating the component containers.");
        containers = new ArrayList<Container>(imports.size());
        for (ContainerConfig config : containerConfigs) {
            containers.add(config.build());
        }

        infolog.info("Runtime initialisation finished.");
    }


    public ArtifactRepository getArtifactRepository() {
        return repository;
    }

    public ClassLoader getClassLoader() {
        return rootClassLoader;
    }

    public Object getService(Class type) {
        Object service = sharedServices.get(type);
        if (service == null) {
            throw new DaisyRTException("No component available providing the service " + type.getName());
        }
        return service;
    }

    public List<Class> getAvailableServices() {
        return new ArrayList<Class>(sharedServices.keySet());
    }

    public Map<Class, Object> getServices() {
        return Collections.unmodifiableMap(sharedServices);
    }

    public void provideService(Class type, Object object) {
        if (!type.isInterface())
            throw new DaisyRTException("The provided type should be an interface");

        if (sharedServices.containsKey(type))
            throw new DaisyRTException("There is already a component providing the service " + type.getName());

        if (!type.isAssignableFrom(object.getClass()))
            throw new DaisyRTException("The provided object does not implement the interface " + type.getName());

        sharedServices.put(type, object);
    }

    public void shutdown() {
        if (containers == null)
            return;

        infolog.info("Shutting down component containers.");

        List<Container> reversedContainers = new ArrayList<Container>(this.containers);
        Collections.reverse(reversedContainers);
        this.containers = null;
        this.sharedServices.clear();

        for (Container container : reversedContainers) {
            container.shutdown();
        }
    }

}
