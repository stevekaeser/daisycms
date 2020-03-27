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
package org.outerj.daisy.repository.schema;

/**
 * A hierarchical selection list for link-type fields. It works
 * by building up a hierarchy from documents which are connected through
 * a link-type field indicating what the parent document is. The root
 * documents are those for which this field is null.
 *
 * <p>This selection list is created on a field type through
 * {@link FieldType#createParentLinkedSelectionList}.
 */
public interface ParentLinkedSelectionList extends SelectionList {
    String getWhereClause();

    /**
     *
     * @param whereClause everything that comes after the 'where' in the query. This can
     *                    include an order by clause, the order of the items as returned
     *                    from the query will be maintained.
     */
    void setWhereClause(String whereClause);

    boolean getFilterVariants();

    void setFilterVariants(boolean filterVariants);

    String getParentLinkField();

    void setParentLinkField(String parentLinkField);
}
