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

import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10.DocumentTypeDocument;

import java.util.Date;

/**
 * Describes a type of document in the repository.
 *
 * <p>Instances are retrieved from {@link RepositorySchema}.
 *
 * <p>A document type has some general properties like a name (which must
 * be unique), and a locale-sensitive label and description. Next to these,
 * a document type is associated with a number of {@link FieldType}s
 * and {@link PartType}s.
 *
 * <p>The part types and field types belonging to a document type are
 * ordered collections: the order in which you add them matters. To
 * reorder them, first remove them all and re-add them.
 *
 * <p>A document type object can be read-only, in which case all state-modifying
 * methods (i.e. all setters and the save method) will throw a RuntimeException.
 * Whether a document type object is read-only or not depends on where you
 * retrieved it from. The purpose of read-only document type objects is for
 * caching, i.e. the same object can be used by multiple users who only which
 * to consult the document type information, but not modify it.
 *
 * <p>The equals method for DocumentType is supported, two document types are
 * equal if all their defining data is equal, with exception of the ID.
 */
public interface DocumentType extends LabelEnabled, DescriptionEnabled {
    long getId();

    /**
     * Returns the part types contained by this document type. This is an ordered
     * collection. The returned array is a newly created copy, thus modifying
     * the order of the part types in the array, or putting other ones in it, won't
     * modify this document type.
     */
    PartTypeUse[] getPartTypeUses();

    /**
     * Adds a new association with a part type to this document type.
     *
     * <p>The supplied part type should already exist in the repository, i.e.
     * it should have an id != -1.
     *
     * <p>The same part type can be added only once.
     *
     * <p>A part type is always added to the end, after the already existing part types.
     *
     * @throws IllegalArgumentException if the part type's id is -1, or if
     * it is already contained by this document type.
     */
    PartTypeUse addPartType(PartType partType, boolean required);

    /**
     * Removes all associations with part types.
     */
    void clearPartTypeUses();

    /**
     * Checks if this document type contains the part type with the given ID.
     */
    boolean hasPartType(long id);

    /**
     * Gets a 'part type use' by part type ID.
     *
     * @param id ID of the part type.
     * @return null if the part type is not associated with this document type
     */
    PartTypeUse getPartTypeUse(long id);

    /**
     * Gets a 'part type use' by part type name.
     *
     * @return null if the part type is not associated with this document type
     */
    PartTypeUse getPartTypeUse(String partTypeName);

    FieldTypeUse[] getFieldTypeUses();

    boolean hasFieldType(long id);

    /**
     * Gets a 'field type use' by field type ID.
     *
     * @param id ID of the field type.
     * @return null if the field type is not associated with this document type
     */
    FieldTypeUse getFieldTypeUse(long id);

    /**
     * Gets a 'field type use' by field type name.
     *
     * @return null if the field type is not associated with this document type
     */
    FieldTypeUse getFieldTypeUse(String fieldTypeName);

    /**
     * The suplied field type should already exist in the repository, i.e.
     * it should have an id != -1.
     *
     * <p>A field type is always added to the end, after the already existing field types.
     *
     * <p>The same field type can only be added once.
     */
    FieldTypeUse addFieldType(FieldType type, boolean required);

    void clearFieldTypeUses();

    String getName();

    void setName(String name);

    boolean isDeprecated();

    void setDeprecated(boolean deprecated);

    /**
     * When was this document type last changed (persistently). Returns null on newly
     * created, not-yet-saved, document types.
     */
    Date getLastModified();

    /**
     * Who (which user) last changed this document type (persistently). Returns -1 on
     * newly created, not-yet-saved, document types.
     */
    long getLastModifier();

    DocumentTypeDocument getXml();

    /**
     * Same as {@link #getXml()} but includes the full XML description of the associated
     * part types and field types in the generated XML.
     */
    DocumentTypeDocument getExtendedXml();

    void setAllFromXml(DocumentTypeDocument.DocumentType documentTypeXml);

    void save() throws RepositoryException;

    long getUpdateCount();
}
