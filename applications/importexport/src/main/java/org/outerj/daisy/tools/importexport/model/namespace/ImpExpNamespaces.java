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
package org.outerj.daisy.tools.importexport.model.namespace;

import java.util.List;
import java.util.ArrayList;

public class ImpExpNamespaces {
    private List<ImpExpNamespace> namespaces = new ArrayList<ImpExpNamespace>();

    public void addNamespace(String name, String fingerprint, boolean required) {
        // check if namespace is already present, upgrade required setting if needed
        for (ImpExpNamespace namespace : namespaces) {
            if (namespace.getName().equals(name)) {
                if (!namespace.getFingerprint().equals(fingerprint))
                    throw new RuntimeException("Namespace " + name + " already present with another fingerprint.");
                if (required && !namespace.isRequired())
                    namespace.setRequired(true);
                return;
            }
        }

        // not yet present, add
        namespaces.add(new ImpExpNamespace(name, fingerprint, required));
    }

    public ImpExpNamespace[] getNamespaces() {
        return namespaces.toArray(new ImpExpNamespace[0]);
    }
}
