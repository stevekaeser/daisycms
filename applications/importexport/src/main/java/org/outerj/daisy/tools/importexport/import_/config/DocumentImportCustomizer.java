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
package org.outerj.daisy.tools.importexport.import_.config;

import org.outerj.daisy.tools.importexport.model.document.ImpExpDocument;
import org.outerj.daisy.repository.Document;

/**
 * A document import customizer can alter the content of a document
 * before it gets imported.
 */
public interface DocumentImportCustomizer {
    /**
     * Called before copying data from the import document to the
     * target document. Allows to modify the import document.
     */
    void preImportFilter(ImpExpDocument impExpDocument);

    boolean storePart(ImpExpDocument impExpDocument, Document targetDocument, String partTypeName);

    boolean removePartWhenMissing(ImpExpDocument impExpDocument, Document targetDocument, String partTypeName);

    boolean storeField(ImpExpDocument impExpDocument, Document targetDocument, String fieldTypeName);

    boolean removeFieldWhenMissing(ImpExpDocument impExpDocument, Document targetDocument, String fieldTypeName);

    boolean storeLinks(ImpExpDocument impExpDocument, Document targetDocument);

    boolean storeCustomFields(ImpExpDocument impExpDocument, Document targetDocument);

    /**
     * Called before saving the document (only if the document was changed).
     */
    void beforeSaveFilter(Document document);

    /**
     * Returns the fragment of XML to configure this customizer
     * in the import options XML. Return empty string if not
     * interested in having this.
     */
    String getXml();
}
