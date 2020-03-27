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
package org.outerj.daisy.repository;

import org.outerx.daisy.x10.FieldDocument;

/**
 * Fields belong to documents (or versions of documents), and can
 * be used for various purposes.
 *
 * <p>Often, fields will be used for meta-data about the document, though
 * you can use them for whatever data that you want to have addressable
 * on a finer level then that contained in the {@link Parts}.
 *
 * <p>A field is always based upon a {@link org.outerj.daisy.repository.schema.FieldType},
 * which defines the kind of data that the field can contain.
 *
 * <p>Note that a field has no setters methods, modifications can only
 * be done through the containing {@link Document}. This is because
 * fields can also be obtained from {@link Version}s, which are not
 * modifiable.
 */
public abstract interface Field {

    /**
     * The id of the field type of this field. More information on the field type
     * can then be retrieved from the {@link org.outerj.daisy.repository.schema.RepositorySchema}.
     */
    long getTypeId();

    /**
     * The name of the field type (for convenience, this is retrieved from the RepositorySchema).
     */
    String getTypeName();

    /**
     * The ValueType of the field, which defines the kind of object you
     * will get from {@link #getValue()}. This method is here for convenience,
     * the information is retrieved from the RepositorySchema.
     */
    ValueType getValueType();

    /**
     * Indicates if this field is a multivalue field. This method is here
     * for convenience, the information is retrieved from the RepositorySchema.
     */
    boolean isMultiValue();

    /**
     * Indicates if this field is a hierarchical field. This method is here
     * for convenience, the information is retrieved from the RepositorySchema.
     */
    boolean isHierarchical();

    /**
     * The value of the field. This will never be null (otherwise the document
     * wouldn't have the field in the first place). The kind of object returned
     * is dependent of the {@link ValueType}, and of whether it concerns a multi-value
     * and/or hierarchical field type. For multi-value fields, an array (Object[]) is returned.
     * For hierarchical fields, a {@link HierarchyPath} object is returned.
     * It hence follows that for hierarchical-and-multivalue fields,
     * an array of HierarchyPath objects is returned.
     * 
     * For link fields: When branch and language are unspecified, the variantKey will 
     * return -1 for getBranchId() or getLanguageId().  You should then refer to the document
     * containing this field for the correct branch and language:
     *   VariantKey value = (VariantKey)document.getField(...).getValue(); 
     *   long branchId = value.getBranchId() == -1 ? document.getBranchId() : value.getBranchId();
     *   long languageId = value.getLanguageId() == -1 ? document.getLanguageId() : value.getLanguageId();
     */
    Object getValue();

    /**
     * Get an XML document describing this field.
     */ 
    FieldDocument getXml();
}
