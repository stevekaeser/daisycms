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
package org.outerj.daisy.runtime.classloading;

import org.outerj.daisy.runtime.repository.ArtifactRepository;
import org.outerj.daisy.runtime.repository.ArtifactNotFoundException;
import org.outerj.daisy.runtime.repository.ArtifactRef;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.MalformedURLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class ClassLoadingConfigImpl implements ClassLoadingConfig {
    private List<ClassPathEntry> entries;
    private List<ClassPathEntry> sharedEntries = new ArrayList<ClassPathEntry>();
    private List<URL> initialEntries;
    private ArtifactRepository repository;

    public ClassLoadingConfigImpl(List<ClassPathEntry> classPathEntries, List<URL> initialEntries, ArtifactRepository repository) {
        this.entries = classPathEntries;
        this.initialEntries = initialEntries;
        this.repository = repository;
    }

    public List<ClassPathEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public void enableSharing(ArtifactRef artifact) {
        for (ClassPathEntry entry : entries) {
            if (entry.getArtifactRef().equalsIgnoreVersion(artifact)) {
                sharedEntries.add(entry);
            }
        }
    }

    public ClassLoader getClassLoader(ClassLoader parentClassLoader) throws MalformedURLException, ArtifactNotFoundException {
        List<URL> classPath = new ArrayList<URL>();
        classPath.addAll(initialEntries);

        for (ArtifactRef artifact : getUsedArtifacts()) {
            classPath.add(repository.resolve(artifact));
        }

        return new URLClassLoader(classPath.toArray(new URL[0]), parentClassLoader);
    }

    public List<ArtifactRef> getUsedArtifacts() {
        List<ArtifactRef> artifacts = new ArrayList<ArtifactRef>();
        for (ClassPathEntry entry : entries) {
            if (!sharedEntries.contains(entry))
                artifacts.add(entry.getArtifactRef());
        }
        return artifacts;
    }
}
