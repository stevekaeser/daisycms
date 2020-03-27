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
package org.outerj.daisy.doctaskrunner.commonimpl;

import org.outerj.daisy.doctaskrunner.DocumentSelection;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;

public class EnumerationDocumentSelection implements DocumentSelection {
    private VariantKey[] variantKeys;

    public EnumerationDocumentSelection(VariantKey[] variantKeys) {
        if (variantKeys == null)
            throw new IllegalArgumentException("variantKeys parameter is null");

        this.variantKeys = variantKeys;
    }

    public VariantKey[] getKeys() {
        return variantKeys;
    }

    public VariantKey[] getKeys(Repository repository) throws RepositoryException {
        return variantKeys;
    }

    public String getDescription() {
        return "Enumeration of documents";
    }
}
