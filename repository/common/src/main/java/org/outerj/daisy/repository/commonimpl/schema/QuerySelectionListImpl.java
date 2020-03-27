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
package org.outerj.daisy.repository.commonimpl.schema;

import org.outerj.daisy.repository.schema.QuerySelectionList;
import org.outerj.daisy.repository.schema.ListItem;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.repository.*;
import org.outerx.daisy.x10.*;

import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class QuerySelectionListImpl implements QuerySelectionList {
    private String query;
    private boolean filterVariants;
    private SortOrder sortOrder;
    private FieldTypeImpl owner;

    public QuerySelectionListImpl(String query, boolean filterVariants, SortOrder sortOrder, FieldTypeImpl owner) {
        checkQueryParam(query);
        this.query = query;
        this.filterVariants = filterVariants;
        this.sortOrder = sortOrder;
        if (this.sortOrder == null)
            this.sortOrder = SortOrder.ASCENDING;
        this.owner = owner;
    }

    public String getQuery() {
        return query;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setQuery(String query) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);

        checkQueryParam(query);
        this.query = query;
    }

    public void setSortOrder(SortOrder sortOrder) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);

        if (sortOrder == null)
            throw new IllegalArgumentException("sortOrder arg cannot be null");
        this.sortOrder = sortOrder;
    }

    public boolean getFilterVariants() {
        return filterVariants;
    }

    public void setFilterVariants(boolean filterVariants) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);
        this.filterVariants = filterVariants;
    }

    private void checkQueryParam(String query) {
        if (query == null || query.trim().length() == 0)
            throw new IllegalArgumentException("query argument cannot be null, empty or whitespace");
    }

    public QuerySelectionListDocument getXml() {
        QuerySelectionListDocument listDocument = QuerySelectionListDocument.Factory.newInstance();
        QuerySelectionListDocument.QuerySelectionList listXml = listDocument.addNewQuerySelectionList();
        listXml.setQuery(query);
        listXml.setFilterVariants(filterVariants);
        listXml.setSortOrder(sortOrder.toString());
        return listDocument;
    }

    public List<? extends ListItem> getItems() {
        throw new RuntimeException("This method should only be called through QuerySelectionListWrapper");
    }

    public List<? extends ListItem> getItems(Repository repository) {
        if (filterVariants)
            throw new RepositoryRuntimeException("This selection list needs to filter on variants, so use getItems(branchId, languageId) instead.");
        return getItems(-1, -1, Locale.getDefault(), repository);
    }

    public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale) {
        throw new RuntimeException("This method should only be called through QuerySelectionListWrapper");
    }

    public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale, Repository repository) {
        DistinctSearchResultDocument searchResultDocument;
        try {
            if (filterVariants) {
                searchResultDocument = repository.getQueryManager().performDistinctQuery(query, "branchId = " + branchId + " and languageId = " + languageId, sortOrder, locale);
            } else {
                searchResultDocument = repository.getQueryManager().performDistinctQuery(query, sortOrder, locale);
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("Error executing query selection list query: " + query, e);
        }
        String resultValueTypeString = searchResultDocument.getDistinctSearchResult().getValues().getValueType();
        ValueType resultValueType = ValueType.fromString(resultValueTypeString);

        if (resultValueType != owner.getValueType())
            throw new RuntimeException("Values returned from selection list query are of a different type (" + resultValueType.toString() + ") then the field type (" + owner.getValueType().toString() + ").");

        List<DistinctSearchResultDocument.DistinctSearchResult.Values.Value> valuesXml = searchResultDocument.getDistinctSearchResult().getValues().getValueList();
        List<QueryListItem> items = new ArrayList<QueryListItem>(valuesXml.size());
        FieldHelper.XmlFieldValueGetter valueGetter = FieldHelper.getXmlFieldValueGetter(resultValueType);
        for (DistinctSearchResultDocument.DistinctSearchResult.Values.Value valueXml : valuesXml) {
            items.add(new QueryListItem(valueGetter.getValue(valueXml, false), valueXml.getLabel()));
        }
        return Collections.unmodifiableList(items);
    }

    public String getLabel(Object value, Locale locale) {
        return getItemLabel(value, locale);
    }

    public String getItemLabel(Object value, Locale locale) {
        return null;
    }

    public void addToFieldTypeXml(SelectionListDocument.SelectionList selectionListXml) {
        selectionListXml.setQuerySelectionList(getXml().getQuerySelectionList());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof QuerySelectionListImpl))
            return false;

        QuerySelectionListImpl other = (QuerySelectionListImpl)obj;

        if (filterVariants != other.filterVariants)
            return false;

        if (sortOrder != other.sortOrder)
            return false;

        return query.equals(other.query);
    }
}
