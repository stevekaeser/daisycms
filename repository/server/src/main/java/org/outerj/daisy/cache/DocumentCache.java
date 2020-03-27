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
package org.outerj.daisy.cache;

import org.outerj.daisy.repository.commonimpl.DocumentImpl;
import org.outerj.daisy.repository.AvailableVariants;

/**
 * The service that caches Documents.
 */
public interface DocumentCache {
    public void put(String documentId, long branchId, long languageId, DocumentImpl document);

    /**
     * Returns the cached Document, or null if it is not in the cache.
     */
    public DocumentImpl get(String documentId, long branchId, long languageId);

    /**
     * Removes a cached Document, or does nothing if it is not in the cache.
     */
    public void remove(String documentId, long branchId, long languageId);

    /**
     * Removes all cached variants of the document.
     */
    public void remove(String documentId);

    /**
     * Clears the entire cache. Should only be used exceptionally to avoid performance drops.
     */
    public void clear();

    public void put(String documentId, AvailableVariants availableVariants);

    public AvailableVariants getAvailableVariants(String documentId);

    public void removeAvailableVariants(String documentId);
}
