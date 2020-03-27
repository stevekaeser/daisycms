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
import org.outerj.daisy.runtime.repository.ChainedMaven1StyleArtifactRepository;

import java.io.File;
import java.util.Properties;
import java.util.Set;
import java.util.Collections;

/**
 * Helper class for creating a DaisyRuntime. This is intended to be the most
 * high-level interface for embedded creation of the repository.
 */
public class DaisyRuntimeHelper {
    public static DaisyRuntime createRuntime(String runtimeConfigLocation, Properties configProps, String mavenRepoLocation) throws Exception {
        Set<String> disabledContainerIds = Collections.emptySet();
        return createRuntime(runtimeConfigLocation, configProps, mavenRepoLocation, disabledContainerIds);
    }

    public static DaisyRuntime createRuntime(String runtimeConfigLocation, Properties configProps, String mavenRepoLocation, Set<String> disabledContainerIds) throws Exception {
        ArtifactRepository artifactRepository = new ChainedMaven1StyleArtifactRepository(mavenRepoLocation);
        DaisyRuntimeConfig runtimeConfig = XmlDaisyRuntimeConfigBuilder.build(new File(runtimeConfigLocation), disabledContainerIds, artifactRepository, configProps);
        DaisyRuntime runtime = new DaisyRuntime(runtimeConfig);
        runtime.init();
        return runtime;
    }
}
