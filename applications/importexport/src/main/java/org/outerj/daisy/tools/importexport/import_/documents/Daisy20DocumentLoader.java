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
package org.outerj.daisy.tools.importexport.import_.documents;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptions;
import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileEntry;

public class Daisy20DocumentLoader implements DocumentLoader {
    public DocumentImportResult run(String documentId, String branch, String language, ImportFileEntry dir,
            Repository repository, ImportOptions options, ImportListener listener) throws Exception {
        BaseDocumentLoader loader = BaseDocumentLoader.load(documentId, branch, language, dir, repository, options, listener);
        if (loader.getDontSave())
            return loader.getResult();

        loader.unretire();
        loader.storeName();
        loader.storeParts();
        loader.storeFields();
        loader.storeLinks();
        loader.storeCustomFields();
        loader.storeCollections();
        loader.storeOwner();
        loader.storeReferenceLanguage();
        loader.save();

        return loader.getResult();
    }
}
