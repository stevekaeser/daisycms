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

import java.util.List;
import java.util.Properties;

public class DaisyRuntimeConfig {
    private List<ContainerEntry> imports;
    private Properties configProps;
    private ArtifactRepository repository;
    private boolean enableArtifactSharing = true;

    public DaisyRuntimeConfig(List<ContainerEntry> imports, Properties configProps, ArtifactRepository repository) {
        if (imports == null)
            throw new IllegalArgumentException("Null argument: imports");
        if (configProps == null)
            throw new IllegalArgumentException("Null argument: configProps");
        if (repository == null)
            throw new IllegalArgumentException("Null argument: repository");

        this.imports = imports;
        this.configProps = configProps;
        this.repository = repository;
    }

    public List<ContainerEntry> getImports() {
        return imports;
    }

    public Properties getConfigProperties() {
        return configProps;
    }

    public ArtifactRepository getRepository() {
        return repository;
    }

    public boolean getEnableArtifactSharing() {
        return enableArtifactSharing;
    }

    public void setEnableArtifactSharing(boolean enableArtifactSharing) {
        this.enableArtifactSharing = enableArtifactSharing;
    }
}
