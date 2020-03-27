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
package org.outerj.daisy.repository.namespace;

import org.outerx.daisy.x10.NamespaceDocument;

import java.util.Date;

public interface Namespace {
    /**
     * Internal namespace ID. This is a repository-specific value, i.e. the same namespace
     * in two different repositories might have different internal IDs. This value should
     * normally not be used for anything but operations on a specific repository instance.
     */
    long getId();

    /**
     * The name of the namespace. Each namespace has a unique name.
     */
    String getName();

    /**
     * The fingerprint of the namespace. The fingerprint is some random key which is used
     * to check that two namespaces with the same name are really the same between
     * different repositories. This is used to detect collisions in the namespace-names
     * selected by different non-coordinated parties.
     */
    String getFingerprint();

    Date getRegisteredOn();

    long getRegisteredBy();

    NamespaceDocument getXml();
    
    boolean isManaged();
    
    void setManaged(boolean isManaged);
    
    long getDocumentCount();
    
    void setDocumentCount(long documentCount);    
    
}
