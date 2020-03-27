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

import org.outerj.daisy.runtime.repository.ArtifactRef;

public class ClassPathEntry {
    private final ArtifactRef artifactRef;
    private final ArtifactSharingMode sharingMode;

    public ClassPathEntry(ArtifactRef artifactRef, ArtifactSharingMode sharingMode) {
        this.artifactRef = artifactRef;
        this.sharingMode = sharingMode;
    }
    
    public ArtifactRef getArtifactRef() {
        return artifactRef;
    }

    public ArtifactSharingMode getSharingMode() {
        return sharingMode;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ClassPathEntry))
            return false;

        ClassPathEntry other = (ClassPathEntry)obj;

        return artifactRef.equals(other.artifactRef) && sharingMode == other.sharingMode;
    }
}
