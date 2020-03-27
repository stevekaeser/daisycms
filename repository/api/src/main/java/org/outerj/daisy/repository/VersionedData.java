/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository;

/**
 * Read-access to the versioned data of a Daisy document.
 *
 * <p>Implemented by both {@link Document} and {@link Version}.
 *
 */
public interface VersionedData {
    /**
     * Returns the name of the document as it was on the time this version was created.
     */
    String getDocumentName();

    /**
     * Returns the parts, in unspecified order.
     */
    Parts getParts();

    /**
     * Returns the parts, in the order as they are defined in
     * the document type of the document. Any parts that are not present
     * in the current document type are returned in no specific order,
     * but after the ordered parts.
     */
    Parts getPartsInOrder();

    /**
     * Get a part by id.
     *
     * @throws PartNotFoundException when the part is not available.
     *            Use {@link #hasPart(long)} to check it is available.
     */
    Part getPart(long typeId);

    /**
     * Checks if the specified part is available, either because the
     * document/version does not contain it, or because the user is
     * not allowed to read it.
     */
    boolean hasPart(long typeId);

    /**
     * Checks if the specified part is available.
     */
    boolean hasPart(String typeName);

    /**
     * Get a part by name.
     *
     * @throws PartNotFoundException when the part is not available.
     *            Use {@link #hasPart(String)} to check it is available.
     */
    Part getPart(String typeName);

    /**
     * Returns the fields, in unspecified order.
     */
    Fields getFields();

    /**
     * Returns the fields, in the order as they are defined in the
     * document type of the document. Any fields that are not present
     * in the current document type are returned in no specific order,
     * but after the ordered fields.
     */
    Fields getFieldsInOrder();

    /**
     * Get a field by field type id.
     *
     * @throws FieldNotFoundException when the field is not available.
     *            Use {@link #hasField(long)} to check if it is available.
     */
    Field getField(long fieldTypeId) throws FieldNotFoundException;

    /**
     * Gets a field by field type name. Throws a FieldNotFoundException if there is no
     * such field in the document, use {@link #hasField(String)} to check if the
     * document has the field.
     *
     * <p>This is a variant-level method.
     *
     * @throws FieldNotFoundException when the field is not available.
     *            Use {@link #hasField(String)} to check if it is available.
     */
    Field getField(String fieldTypeName) throws FieldNotFoundException;

    /**
     * Checks if the specified field is available, either because the
     * document/version does not contain it, or because the user is
     * not allowed to read it.
     */
    boolean hasField(long fieldTypeId);

    /**
     * Checks if the specified field is available.
     */
    boolean hasField(String fieldTypeName);

    /**
     * Get the out-of-line links.
     */
    Links getLinks();
    
    /**
     * Get the summary
     */
    String getSummary();
}
