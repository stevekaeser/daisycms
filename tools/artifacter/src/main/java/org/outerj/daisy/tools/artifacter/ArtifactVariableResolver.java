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
package org.outerj.daisy.tools.artifacter;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathVariableResolver;

import org.outerj.daisy.tools.artifacter.Artifacter.Artifact;

public class ArtifactVariableResolver implements XPathVariableResolver {
    
    private String groupId;
    private String artifactId;
    private String version;
    
    public ArtifactVariableResolver(Artifact artifact) {
        this(artifact.groupId, artifact.artifactId, null);
    }

    public ArtifactVariableResolver(String groupId) {
        this(groupId, null, null);
    }

    public ArtifactVariableResolver(String groupId, String artifactId) {
        this(groupId, artifactId, null);
    }

    public ArtifactVariableResolver(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public Object resolveVariable(QName name) {
        String lname = name.getLocalPart();
        if (lname.equals("groupId")) 
            return groupId;
        if (lname.equals("artifactId"))
            return artifactId;
        if (lname.equals("version"))
            return version;
        return null;
    }

}
