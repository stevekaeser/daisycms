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
package org.outerj.daisy.tools.importexport.docset;

import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.repository.VariantKey;

import java.util.Set;

/**
 * A set of documents.
 *
 * <p>This is used to specify the set of documents to export to the
 * export tool ({@link org.outerj.daisy.tools.importexport.export.Exporter#run})
 * or to limit the set of documents to import to the import tool
 * ({@link org.outerj.daisy.tools.importexport.import_.Importer#run}).
 */
public interface DocumentSet {
    Set<VariantKey> getDocuments() throws ImportExportException;
}
