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

import org.outerj.daisy.repository.query.SortOrder;

public class ImpExpQuerySelectionList {
    private String query;
    private SortOrder sortOrder;
    private boolean filterVariants;

    public ImpExpQuerySelectionList(String query, SortOrder sortOrder, boolean filterVariants) {
        setQuery(query);
        setSortOrder(sortOrder);
        setFilterVariants(filterVariants);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        if (query == null || query.trim().equals(""))
            throw new IllegalArgumentException("Null or empty argument: query");
        this.query = query;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        if (sortOrder == null)
            throw new IllegalArgumentException("Null argument: sortOrder");
        this.sortOrder = sortOrder;
    }

    public boolean getFilterVariants() {
        return filterVariants;
    }

    public void setFilterVariants(boolean filterVariants) {
        this.filterVariants = filterVariants;
    }
}
