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
package org.outerj.daisy.repository;

import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerx.daisy.x10.AvailableVariantDocument;

/**
 * Holds information about a variant of a document.
 */
public interface AvailableVariant {
    Branch getBranch() throws RepositoryException;

    Language getLanguage() throws RepositoryException;

    long getBranchId();

    long getLanguageId();

    boolean isRetired();

    /**
     * Returns the ID of the live version of this document variant, or -1
     * if this variant has no live version.
     */
    long getLiveVersionId();

    AvailableVariantDocument getXml();

    AvailableVariantDocument getXml(boolean includeVariantNames) throws RepositoryException;
}
