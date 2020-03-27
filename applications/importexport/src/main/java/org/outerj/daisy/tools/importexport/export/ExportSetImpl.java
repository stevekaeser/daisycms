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
package org.outerj.daisy.tools.importexport.export;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.tools.importexport.ImportExportException;

import java.util.Set;
import java.util.Collections;

public class ExportSetImpl implements ExportSet {
    private Set<VariantKey> variantKeys;
    private Set<String> documentTypes;
    private Set<String> fieldTypes;
    private Set<String> partTypes;
    private Set<String> collections;
    private Set<String> namespaces;

    public ExportSetImpl(Set<VariantKey> variantKeys,
            Set<String> documentTypes,
            Set<String> fieldTypes,
            Set<String> partTypes,
            Set<String> collections,
            Set<String> namespaces) {
        this.variantKeys = Collections.unmodifiableSet(variantKeys);
        this.documentTypes = Collections.unmodifiableSet(documentTypes);
        this.fieldTypes = Collections.unmodifiableSet(fieldTypes);
        this.partTypes = Collections.unmodifiableSet(partTypes);
        this.collections = Collections.unmodifiableSet(collections);
        this.namespaces = Collections.unmodifiableSet(namespaces);
    }

    public Set<String> getDocumentTypes() {
        return documentTypes;
    }

    public Set<String> getFieldTypes() {
        return fieldTypes;
    }

    public Set<String> getPartTypes() {
        return partTypes;
    }

    public Set<String> getCollections() {
        return collections;
    }

    public Set<VariantKey> getDocuments() throws ImportExportException {
        return variantKeys;
    }

    public Set<String> getNamespaces() {
        return namespaces;
    }
}
