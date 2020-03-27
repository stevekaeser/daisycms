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
package org.outerj.daisy.books.publisher.impl.dataretrieval;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Repository;

/**
 * Decides whether a part should be downloaded during the book data retrieval process.
 */
public interface PartDecider {
    /**
     * Returns true if the data of this part should be downloaded and stored in the book instance.
     * The implementation can make this decission based on the supplied information. In many cases
     * the document type name and part type name will be sufficient.
     */
    boolean needsPart(long documentTypeId, String documentTypeName, long partTypeId, String partTypeName,
            String mimeType, String fileName, long size, VariantKey document, long versionId, Repository repository);
}
