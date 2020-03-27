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
package org.outerj.daisy.tools.importexport.model.document;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.variant.VariantManager;

import java.util.Map;
import java.io.InputStream;

public class ImpExpDocumentFactory {
    public static ImpExpDocument fromDocument(Document document, Version version, Repository repository) throws RepositoryException {
        VariantManager variantManager = repository.getVariantManager();
        RepositorySchema schema = repository.getRepositorySchema();
        UserManager userManager = repository.getUserManager();

        String branch = variantManager.getBranch(document.getBranchId(), false).getName();
        String language = variantManager.getLanguage(document.getLanguageId(), false).getName();
        String documentType = schema.getDocumentTypeById(document.getDocumentTypeId(), false).getName();

        ImpExpDocument impexpDoc = new ImpExpDocument(document.getId(), branch, language, documentType, version.getDocumentName());

        // version state
        impexpDoc.setVersionState(version.getState());

        // custom fields
        Map<String, String> customFields = document.getCustomFields();
        for (Map.Entry<String, String> entry : customFields.entrySet()) {
            impexpDoc.addCustomField(new ImpExpCustomField(entry.getKey(), entry.getValue()));
        }

        // links
        Link[] links = version.getLinks().getArray();
        for (Link link : links) {
            impexpDoc.addLink(new ImpExpLink(link.getTitle(), link.getTarget()));
        }

        // fields
        Field[] fields = version.getFields().getArray();
        for (Field field : fields) {
            FieldType fieldType = schema.getFieldTypeById(field.getTypeId(), false);
            impexpDoc.addField(new ImpExpField(fieldType, field.getValue()));
        }

        // parts
        Part[] parts = version.getParts().getArray();
        for (Part part : parts) {
            PartType partType = schema.getPartTypeById(part.getTypeId(), false);
            impexpDoc.addPart(new ImpExpPart(partType, part.getMimeType(), part.getFileName(), new RepoPartDataAccess(part)));
        }

        // owner
        String owner = userManager.getUserLogin(document.getOwner());
        impexpDoc.setOwner(owner);

        // collections
        DocumentCollection[] collections = document.getCollections().getArray();
        for (DocumentCollection collection : collections) {
            impexpDoc.addCollection(collection.getName());
        }

        // reference language
        long refLangId = document.getReferenceLanguageId();
        if (refLangId != -1)
            impexpDoc.setReferenceLanguage(variantManager.getLanguage(refLangId, false).getName());

        return impexpDoc;
    }

    static class RepoPartDataAccess implements ImpExpPart.PartDataAccess {
        private Part part;

        public RepoPartDataAccess(Part part) {
            this.part = part;
        }

        public InputStream getInputStream() throws Exception {
            return part.getDataStream();
        }

        public long getSize() throws Exception {
            return part.getSize();
        }
    }
}
