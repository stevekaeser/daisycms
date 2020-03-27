/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.tools.importexport.import_.collections;

import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.model.collection.ImpExpCollectionsDexmlizer;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.user.Role;

import java.io.InputStream;
import java.util.Set;

public class CollectionImporter {
    private ImportFile importFile;
    private ImportListener listener;
    private Repository repository;
    private static final String COLLECTION_FILE_PATH = "info/collections.xml";

    public static void run(ImportFile importFile, ImportListener listener, Repository repository) throws ImportExportException {
        new CollectionImporter(importFile, listener, repository).run();
    }

    private CollectionImporter(ImportFile importFile, ImportListener listener, Repository repository) {
        this.importFile = importFile;
        this.listener = listener;
        this.repository = repository;
    }

    private void run() throws ImportExportException {
        // collections file is optional
        if (!importFile.exists(COLLECTION_FILE_PATH))
            return;
        
        listener.startActivity("Will check collections and create if necessary.");
        InputStream is = null;
        Set<String> collections;
        try {
            is = importFile.getPath(COLLECTION_FILE_PATH).getInputStream();
            collections = ImpExpCollectionsDexmlizer.fromXml(is);
        } catch (Throwable e) {
            throw new ImportExportException("Failed reading " + COLLECTION_FILE_PATH, e);
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) { /* ignore */ }
        }


        CollectionManager collectionManager = repository.getCollectionManager();
        for (String collectionName : collections) {
            boolean found = true;
            try {
                collectionManager.getCollectionByName(collectionName, false);
                listener.info("Collection " + collectionName + " available.");
            } catch (CollectionNotFoundException e) {
                found = false;
            } catch (RepositoryException e) {
                throw new ImportExportException("Unexpected error getting collection " + collectionName, e);
            }

            if (!found) {
                if (!repository.isInRole(Role.ADMINISTRATOR)) {
                    throw new ImportExportException("Need to create collection " + collectionName + " but the user is not in the Administrator role.");
                } else if (repository.isInRole(Role.ADMINISTRATOR)) {
                    try {
                        DocumentCollection collection = collectionManager.createCollection(collectionName);
                        collection.save();
                        listener.info("Created collection " + collectionName);
                    } catch (RepositoryException e) {
                        throw new ImportExportException("Error creating collection " + collectionName, e);
                    }
                }
            }
        }
        listener.info("Collection checking and creation done.");
    }

}
