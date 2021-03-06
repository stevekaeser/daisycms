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
package org.outerj.daisy.runtime.repository;

import java.net.MalformedURLException;
import java.io.File;
import java.util.Collections;

public class Maven1StyleArtifactRepository extends BaseArtifactRepository {
    private final File repositoryLocation;
    private final String sep = System.getProperty("file.separator");

    public Maven1StyleArtifactRepository(File repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }

    public ResolvedArtifact tryResolve(String groupId, String artifactId, String version) throws MalformedURLException, ArtifactNotFoundException {
        File artifactFile = new File(repositoryLocation, groupId + sep + "jars" + sep + artifactId + "-" + version + ".jar");
        return new ResolvedArtifact(artifactFile.toURI().toURL(), Collections.singletonList(artifactFile.getAbsolutePath()), artifactFile.exists());
    }
}
