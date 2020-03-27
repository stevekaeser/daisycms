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

/**
 * A reference to an artifact (= an item in an {@link ArtifactRepository}, typically a jar).
 */
public final class ArtifactRef {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final int hashCode;

    public ArtifactRef(String groupId, String artifactId, String version) {
        if (groupId == null)
            throw new IllegalArgumentException("Null argument: groupId");
        if (artifactId == null)
            throw new IllegalArgumentException("Null argument: artifactId");
        if (version == null)
            throw new IllegalArgumentException("Null argument: version");
        
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.hashCode = (groupId + artifactId + version).hashCode();
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public boolean equalsIgnoreVersion(ArtifactRef artifact) {
        return groupId.equals(artifact.getGroupId()) && artifactId.equals(artifact.getArtifactId());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ArtifactRef))
            return false;

        ArtifactRef other = (ArtifactRef)obj;
        return groupId.equals(other.getGroupId())
                && artifactId.equals(other.getArtifactId())
                && version.equals(other.getVersion());
    }

    public int hashCode() {
        return hashCode;
    }

    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }

    public String toStringWithoutVersion() {
        return groupId + ":" + artifactId;
    }
}
