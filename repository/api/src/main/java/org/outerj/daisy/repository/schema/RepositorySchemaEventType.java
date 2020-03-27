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
package org.outerj.daisy.repository.schema;

public enum RepositorySchemaEventType {
    DOCUMENT_TYPE_UPDATED("DocumentTypeUpdated"),
    DOCUMENT_TYPE_CREATED("DocumentTypeCreated"),
    DOCUMENT_TYPE_DELETED("DocumentTypeDeleted"),
    PART_TYPE_UPDATED("PartTypeUpdated"),
    PART_TYPE_CREATED("PartTypeCreated"),
    PART_TYPE_DELETED("PartTypeDeleted"),
    FIELD_TYPE_UPDATED("FieldTypeUpdated"),
    FIELD_TYPE_CREATED("FieldTypeCreated"),
    FIELD_TYPE_DELETED("FieldTypeDeleted");

    private String name;

    private RepositorySchemaEventType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
