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

import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerx.daisy.x10.FieldTypeDocument;
import org.outerx.daisy.x10.ExpSelectionListDocument;

import java.util.Date;
import java.util.Locale;

/**
 * Describes a type of field in the repository.
 *
 * <p>Instances are retrieved from {@link RepositorySchema}.
 *
 * <p>The equals method for FieldType is supported, two field types are
 * equal if all their defining data is equal, with exception of the ID.
 */
public interface FieldType extends DescriptionEnabled, LabelEnabled {
    long getId();

    ValueType getValueType();

    String getName();

    void setName(String name);

    boolean isDeprecated();

    void setDeprecated(boolean deprecated);

    int getSize();

    /**
     * Sets the size of this FieldType. If this method isn't called,
     * the size will be 0. The size can be used for presentational purposes,
     * and doesn't cause any validation to happen.
     */
    void setSize(int size);

    /**
     * Indicates whether fields of this type can be used in object expressions of ACL rules.
     * When fields can be used in ACL object expressions, this means that changing the
     * value of such as field can influence the result of the ACL.
     */
    boolean isAclAllowed();

    void setAclAllowed(boolean aclAllowed);

    boolean isMultiValue();

    boolean isHierarchical();

    /**
     * Returns true if the values of fields of this field type are simple objects,
     * rather than more complex objects, which is the case for multivalue and/or
     * hierarchical fields.
     *
     * <p>So this is basically the same as: !isMultiValue() && !isHierarhical(),
     * but is safe for the case other dimensions would be added in the future.
     */
    boolean isPrimitive();

    /**
     * When was this PartType last changed (persistently). Returns null on newly
     * created FieldTypes.
     */
    Date getLastModified();

    /**
     * Who (which user) last changed this PartType (persistently). Returns -1 on
     * newly created FieldTypes.
     */
    long getLastModifier();

    /**
     * Clears(removes) the SelectionList for this FieldType.
     */
    void clearSelectionList();

    /**
     * Returns the SelectionList for this FieldType if one has been defined,
     * null if no SelectionList had been defined (i.e. not set for this FieldType
     * using the setSelectionList(SelectionList selectionList) method).
     */
    SelectionList getSelectionList();

    boolean hasSelectionList();

    /**
     * Creates and returns a StaticSelectionList. The selection list automatically
     * becomse the selection list of this field type.
     */
    StaticSelectionList createStaticSelectionList();

    /**
     * Creates and returns a LinkQuerySelectionList. The selection list automatically
     * becomse the selection list of this field type.
     *
     * <p>This only works for link-type fields.
     */
    LinkQuerySelectionList createLinkQuerySelectionList(String whereClause, boolean filterVariants);

    /**
     * Creates and returns a QuerySelectionList. The selection list automatically
     * becomse the selection list of this field type.
     */
    QuerySelectionList createQuerySelectionList(String query, boolean filterVariants, SortOrder sortOrder);

    HierarchicalQuerySelectionList createHierarchicalQuerySelectionList(String whereClause, String[] fieldTypeNames, boolean filterVariants);

    ParentLinkedSelectionList createParentLinkedSelectionList(String whereClause, String linkFieldName, boolean filterVariants);

    boolean getAllowFreeEntry();

    /**
     * When a field type has a selection list, should the user also be able to
     * enter other values then those available in the selection list?
     *
     * <p>Note that in either case, it is not checked whether the fields' value
     * occurs in the selection list. This is only a hint towards the editing GUI.
     */
    void setAllowFreeEntry(boolean allowFreeEntry);

    boolean getLoadSelectionListAsync();

    void setLoadSelectionListAsync(boolean loadAsync);

    /**
     * Returns the content of the selection list as XML. Returns null if there is
     * no selection list.
     */
    ExpSelectionListDocument getExpandedSelectionListXml(long branchId, long languageId, Locale locale);

    FieldTypeDocument getXml();

    void setAllFromXml(FieldTypeDocument.FieldType fieldTypeXml);

    void save() throws RepositoryException;

    long getUpdateCount();
}
