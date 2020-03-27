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
package org.outerj.daisy.tools.importexport.model.schema;

import java.util.*;

public class ImpExpSchema {
    private Map<String, ImpExpFieldType> fieldTypes = new HashMap<String, ImpExpFieldType>();
    private Map<String, ImpExpPartType> partTypes = new HashMap<String, ImpExpPartType>();
    private Map<String, ImpExpDocumentType> documentTypes = new HashMap<String, ImpExpDocumentType>();

    public void addFieldType(ImpExpFieldType fieldType) {
        fieldTypes.put(fieldType.getName(), fieldType);
    }

    public void addPartType(ImpExpPartType partType) {
        partTypes.put(partType.getName(), partType);
    }

    public void addDocumentType(ImpExpDocumentType documentType) {
        documentTypes.put(documentType.getName(), documentType);
    }

    public void removeFieldType(String fieldTypeName) {
        fieldTypes.remove(fieldTypeName);
    }

    public void removePartType(String partTypeName) {
        partTypes.remove(partTypeName);
    }

    public void removeDocumentType(String documentTypeName) {
        documentTypes.remove(documentTypeName);
    }

    public ImpExpFieldType[] getFieldTypes() {
        return fieldTypes.values().toArray(new ImpExpFieldType[0]);
    }

    public ImpExpPartType[] getPartTypes() {
        return partTypes.values().toArray(new ImpExpPartType[0]);
    }

    public ImpExpDocumentType[] getDocumentTypes() {
        return documentTypes.values().toArray(new ImpExpDocumentType[0]);
    }

    public void clearFieldTypes() {
        fieldTypes.clear();
    }

    public void clearPartTypes() {
        partTypes.clear();
    }

    public void clearDocumentTypes() {
        documentTypes.clear();
    }

    public boolean hasFieldType(String fieldTypeName) {
        return fieldTypes.containsKey(fieldTypeName);
    }

    public boolean hasPartType(String partTypeName) {
        return partTypes.containsKey(partTypeName);
    }

    public boolean hasDocumentType(String documentTypeName) {
        return documentTypes.containsKey(documentTypeName);
    }

    /**
     * Checks that all field types and part types used by document types
     * are present in the schema. Throws an exception if this is not the case.
     */
    public void checkConsistency() throws Exception {
        for (ImpExpDocumentType documentType : documentTypes.values()) {
            ImpExpFieldTypeUse[] fieldTypeUses = documentType.getFieldTypeUses();
            for (ImpExpFieldTypeUse fieldTypeUse : fieldTypeUses) {
                if (!fieldTypes.containsKey(fieldTypeUse.getFieldTypeName()))
                    throw new Exception("Field type " + fieldTypeUse.getFieldTypeName() + " is used by document type " + documentType.getName() + " but not present in the schema.");
            }

            ImpExpPartTypeUse[] partTypeUses = documentType.getPartTypeUses();
            for (ImpExpPartTypeUse partTypeUse : partTypeUses) {
                if (!partTypes.containsKey(partTypeUse.getPartTypeName()))
                    throw new Exception("Part type " + partTypeUse.getPartTypeName() + " is used by document type " + documentType.getName() + " but not present in the schema.");
            }
        }
    }
}
