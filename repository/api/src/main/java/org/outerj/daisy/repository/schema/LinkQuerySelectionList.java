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

import org.outerx.daisy.x10.LinkQuerySelectionListDocument;

/**
 * A selection list to be used only with link-type fields. The selection list
 * is defined by a query.
 *
 * <p>This selection list is created on a field type through
 * {@link FieldType#createLinkQuerySelectionList}.
 */
public interface LinkQuerySelectionList extends SelectionList {
    String getWhereClause();

    /**
     *
     * @param whereClause everything that comes after the 'where' in the query. The select
     *           part will always be "select name where ...". 
     */
    void setWhereClause(String whereClause);

    boolean getFilterVariants();

    void setFilterVariants(boolean filterVariants);

    LinkQuerySelectionListDocument getXml();
}
