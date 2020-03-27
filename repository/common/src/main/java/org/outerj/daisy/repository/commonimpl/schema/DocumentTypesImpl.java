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
package org.outerj.daisy.repository.commonimpl.schema;

import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.DocumentTypes;
import org.outerx.daisy.x10.DocumentTypesDocument;
import org.outerx.daisy.x10.DocumentTypeDocument;

public class DocumentTypesImpl implements DocumentTypes {
    private DocumentType[] documentTypes;

    public DocumentTypesImpl(DocumentType[] documentTypes) {
        this.documentTypes = documentTypes;
    }

    public DocumentType[] getArray() {
        return documentTypes;
    }

    public DocumentTypesDocument getXml() {
        DocumentTypeDocument.DocumentType[] documentTypeXml = new DocumentTypeDocument.DocumentType[documentTypes.length];
        for (int i = 0; i < documentTypes.length; i++) {
            documentTypeXml[i] = documentTypes[i].getXml().getDocumentType();
        }

        DocumentTypesDocument documentTypesDocument = DocumentTypesDocument.Factory.newInstance();
        DocumentTypesDocument.DocumentTypes documentTypesXml = documentTypesDocument.addNewDocumentTypes();
        documentTypesXml.setDocumentTypeArray(documentTypeXml);
        return documentTypesDocument;
    }

    public int size() {
        return documentTypes.length;
    }
}
