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

import org.outerx.daisy.x10.QuerySelectionListDocument;
import org.outerj.daisy.repository.query.SortOrder;

/**
 * A query-based selection list implementation. It works by executing the
 * query and taking the distinct set of values of the first
 * selected value.
 *
 * <p>This selection list is created on a field type through
 * {@link FieldType#createQuerySelectionList}.
 */
public interface QuerySelectionList extends SelectionList {
    String getQuery();

    SortOrder getSortOrder();

    void setQuery(String query);

    void setSortOrder(SortOrder sortOrder);

    boolean getFilterVariants();

    void setFilterVariants(boolean filterVariants);

    QuerySelectionListDocument getXml();
}
