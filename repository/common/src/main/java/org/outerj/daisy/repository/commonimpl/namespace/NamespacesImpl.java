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
package org.outerj.daisy.repository.commonimpl.namespace;

import org.outerj.daisy.repository.namespace.Namespaces;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerx.daisy.x10.NamespacesDocument;
import org.outerx.daisy.x10.NamespaceDocument;

public class NamespacesImpl implements Namespaces {
    private final Namespace[] namespaces;
    private final String repositoryNamespace;

    public NamespacesImpl(Namespace[] namespaces, String repositoryNamespace) {
        this.namespaces = namespaces;
        this.repositoryNamespace = repositoryNamespace;
    }

    public Namespace[] getArray() {
        return namespaces;
    }

    public NamespacesDocument getXml() {
        NamespaceDocument.Namespace[] namespacesListXml = new NamespaceDocument.Namespace[namespaces.length];
        for (int i = 0; i < namespaces.length; i++) {
            namespacesListXml[i] = namespaces[i].getXml().getNamespace();
        }

        NamespacesDocument namespacesDocument = NamespacesDocument.Factory.newInstance();
        NamespacesDocument.Namespaces namespacesXml = namespacesDocument.addNewNamespaces();
        namespacesXml.setNamespaceArray(namespacesListXml);
        namespacesXml.setRepositoryNamespace(repositoryNamespace);

        return namespacesDocument;
    }
}
