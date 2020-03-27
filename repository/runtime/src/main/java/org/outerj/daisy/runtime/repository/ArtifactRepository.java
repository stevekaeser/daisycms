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

import java.net.URL;
import java.net.MalformedURLException;

/**
 * A Apache Maven-style repository of artifacts.
 */
public interface ArtifactRepository {
    URL resolve(ArtifactRef artifactRef) throws MalformedURLException, ArtifactNotFoundException;

    URL resolve(String groupId, String artifactId, String version) throws MalformedURLException, ArtifactNotFoundException;

    ResolvedArtifact tryResolve(String groupId, String artifactId, String version) throws MalformedURLException, ArtifactNotFoundException;
}
