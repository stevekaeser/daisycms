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
package org.outerj.daisy.repository.commonimpl;

import org.outerj.daisy.repository.AvailableVariants;
import org.outerj.daisy.repository.AvailableVariant;
import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10.AvailableVariantsDocument;
import org.outerx.daisy.x10.AvailableVariantDocument;

public class AvailableVariantsImpl implements AvailableVariants {
    private final AvailableVariant[] availableVariants;

    public AvailableVariantsImpl(AvailableVariant[] availableVariants) {
        this.availableVariants = availableVariants;
    }

    public AvailableVariant[] getArray() {
        return availableVariants;
    }

    public int size() {
        return availableVariants.length;
    }

    public boolean hasVariant(long branchId, long languageId) {
        for (int i = 0; i < availableVariants.length; i++) {
            if (availableVariants[i].getBranchId() == branchId && availableVariants[i].getLanguageId() == languageId) {
                return true;
            }
        }
        return false;
    }

    public AvailableVariantsDocument getXml() {
        try {
            return getXml(false);
        } catch (RepositoryException e) {
            // getXml only throws an exception if includeVariantNames is true
            throw new RuntimeException(e);
        }
    }

    public AvailableVariantsDocument getXml(boolean includeVariantNames) throws RepositoryException {
        AvailableVariantDocument.AvailableVariant[] availableVariantXml = new AvailableVariantDocument.AvailableVariant[availableVariants.length];
        for (int i = 0; i < availableVariants.length; i++) {
            availableVariantXml[i] = availableVariants[i].getXml(includeVariantNames).getAvailableVariant();
        }

        AvailableVariantsDocument document = AvailableVariantsDocument.Factory.newInstance();
        AvailableVariantsDocument.AvailableVariants availableVariantsXml = document.addNewAvailableVariants();
        availableVariantsXml.setAvailableVariantArray(availableVariantXml);
        return document;
    }
}
