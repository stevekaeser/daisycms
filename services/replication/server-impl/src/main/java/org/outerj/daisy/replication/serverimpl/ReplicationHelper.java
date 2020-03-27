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
package org.outerj.daisy.replication.serverimpl;

import java.util.Arrays;
import java.util.Map;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.Field;
import org.outerj.daisy.repository.Link;
import org.outerj.daisy.repository.LiveStrategy;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.util.ObjectUtils;

public class ReplicationHelper {
    
    public static void copyNonVersionedData(Document srcDoc, Document destDoc) throws RepositoryException {
        //
        // Document-level properties
        //
        if (srcDoc.isPrivate() != destDoc.isPrivate()) {
            destDoc.setPrivate(srcDoc.isPrivate());
        }

        if (srcDoc.getReferenceLanguageId() != destDoc.getReferenceLanguageId()) {
            destDoc.setReferenceLanguageId(srcDoc.getReferenceLanguageId());
        }

        //
        // Non-versioned variant properties
        //

        // document type is not changed as it 's already guaranteed to be correct

        // Custom fields
        if (!srcDoc.getCustomFields().equals(destDoc.getCustomFields())) {
            destDoc.clearCustomFields();
            for (Map.Entry<String, String> customField : srcDoc.getCustomFields().entrySet()) {
                destDoc.setCustomField(customField.getKey(), customField.getValue());
            }
        }

        // Collections
        if (collectionsChanged(srcDoc, destDoc)) {
            destDoc.clearCollections();
            for (DocumentCollection collection : srcDoc.getCollections().getArray()) {
                destDoc.addToCollection(collection);
            }
        }

        if (srcDoc.isRetired() != destDoc.isRetired()) {
            destDoc.setRetired(srcDoc.isRetired());
        }


    }

    /**
     * Copied from WriteAccessDetailHelper
     * @param doc1
     * @param doc2
     * @return
     */
    private static boolean collectionsChanged(Document doc1, Document doc2) {
        DocumentCollection[] doc1Collections = doc1.getCollections().getArray();
        DocumentCollection[] doc2Collections = doc2.getCollections().getArray();

        boolean changed = false;
        if (doc1Collections.length == doc2Collections.length) {
            for (int i = 0; i < doc1Collections.length; i++) {
                if (doc1Collections[i].getId() != doc2Collections[i].getId()) {
                    changed = true;
                    break;
                }
            }
        } else {
            changed = true;
        }

        return changed;
    }

    public static void copyVersionData(Version srcDoc, Document destDoc) throws RepositoryException{
        destDoc.setNewLiveStrategy(LiveStrategy.NEVER);
        
        destDoc.setNewVersionState(srcDoc.getState());
        destDoc.setNewChangeComment(srcDoc.getChangeComment());
        destDoc.setNewChangeType(srcDoc.getChangeType());
        destDoc.setNewSyncedWith(srcDoc.getSyncedWith());

        //
        // Versioned properties
        //

        if (!srcDoc.getDocumentName().equals(destDoc.getName())) {
            destDoc.setName(srcDoc.getDocumentName());
        }

        // Links
        if (!Arrays.equals(srcDoc.getLinks().getArray(), destDoc.getLinks().getArray())) {
            destDoc.clearLinks();
            for (Link link : srcDoc.getLinks().getArray()) {
                destDoc.addLink(link.getTitle(), link.getTarget());
            }
        }

        //
        // Apply field changes
        //
        
        // All fields in srcDoc, which are writeable: update corresponding field in destDoc
        for (Field srcField : srcDoc.getFields().getArray()) {
            destDoc.setField(srcField.getTypeId(), srcField.getValue());
        }

        //
        // Apply part changes
        //
        for (Part srcPart : srcDoc.getParts().getArray()) {
            if (!destDoc.hasPart(srcPart.getTypeName()) || srcPart.getDataChangedInVersion() == srcDoc.getId()) {
                destDoc.setPart(srcPart.getTypeName(), srcPart.getMimeType(), srcPart.getData());
            }
            if (!ObjectUtils.safeEquals(destDoc.getPart(srcPart.getTypeName()).getFileName(), srcPart.getFileName())) {
                destDoc.setPartFileName(srcPart.getTypeName(), srcPart.getFileName());
            }
        }

    }

}
