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
package org.outerj.daisy.tools.importexport.export.config;

import org.outerj.daisy.tools.importexport.model.document.ImpExpDocument;
import org.outerj.daisy.tools.importexport.model.document.ImpExpPart;

/**
 * A document export customizer can alter the content of a document
 * before it gets exported. Useful to remove e.g. fields which contain
 * internal data that you don't want to be part of the export.
 */
public interface DocumentExportCustomizer {
    /**
     * Called before exporting this document. At this stage you can
     * manipulate the document, e.g. add or remove fields, parts, ...
     */
    void preExportFilter(ImpExpDocument impExpDocument);
    
    /**
     * Called to determine which filename to use for exporting this document.
     * If null, the default filename will be used (data1, data2, ...)
     */
    String exportFilename(String suggestedFilename, ImpExpPart impExpPart);

    /**
     * Returns the fragment of XML to configure this customizer
     * in the export options XML. Return empty string if not
     * interested in having this.
     */
    String getXml();
}
