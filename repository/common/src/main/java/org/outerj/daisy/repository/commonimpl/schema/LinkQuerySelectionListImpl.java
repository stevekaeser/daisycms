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

import org.outerj.daisy.repository.schema.LinkQuerySelectionList;
import org.outerj.daisy.repository.schema.ListItem;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryRuntimeException;
import org.outerj.daisy.repository.Repository;
import org.outerx.daisy.x10.LinkQuerySelectionListDocument;
import org.outerx.daisy.x10.SearchResultDocument;
import org.outerx.daisy.x10.SelectionListDocument;

import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class LinkQuerySelectionListImpl implements LinkQuerySelectionList {
    private String whereClause;
    private boolean filterVariants;
    private FieldTypeImpl owner;

    public LinkQuerySelectionListImpl(String whereClause, boolean filterVariants, FieldTypeImpl owner) {
        checkWhereClause(whereClause);
        this.whereClause = whereClause;
        this.filterVariants = filterVariants;
        this.owner = owner;
    }

    public String getWhereClause() {
        return whereClause;
    }

    private void checkWhereClause(String whereClause) {
        if (whereClause == null || whereClause.trim().length() == 0)
            throw new IllegalArgumentException("whereClause argument cannot be null, empty or whitespace");
    }

    public void setWhereClause(String whereClause) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);

        checkWhereClause(whereClause);
        this.whereClause = whereClause;
    }

    public boolean getFilterVariants() {
        return filterVariants;
    }

    public void setFilterVariants(boolean filterVariants) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);
        this.filterVariants = filterVariants;
    }

    public LinkQuerySelectionListDocument getXml() {
        LinkQuerySelectionListDocument listDocument = LinkQuerySelectionListDocument.Factory.newInstance();
        LinkQuerySelectionListDocument.LinkQuerySelectionList selectionListXml = listDocument.addNewLinkQuerySelectionList();
        selectionListXml.setWhereClause(whereClause);
        selectionListXml.setFilterVariants(filterVariants);
        return listDocument;
    }

    public List<? extends ListItem> getItems() {
        throw new RuntimeException("This method should only be called through LinkQuerySelectionListWrapper");
    }

    public List<? extends ListItem> getItems(Repository repository) {
        if (filterVariants)
            throw new RepositoryRuntimeException("This selection list needs to filter on variants, so use getItems(branchId, languageId) instead.");
        return getItems(-1, -1, Locale.getDefault(), repository);
    }

    public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale) {
        throw new RuntimeException("This method should only be called through LinkQuerySelectionListWrapper");
    }

    public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale, Repository repository) {
        String query = "select name where " + whereClause;
        SearchResultDocument searchResultDocument;
        try {
            if (filterVariants) {
                searchResultDocument = repository.getQueryManager().performQuery(query, "branchId = " + branchId + " and languageId = " + languageId, locale);
            } else {
                searchResultDocument = repository.getQueryManager().performQuery(query, locale);
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("Error executing link-query selection list query: " + query, e);
        }
        List<SearchResultDocument.SearchResult.Rows.Row> rows = searchResultDocument.getSearchResult().getRows().getRowList();

        List<LinkListItem> items = new ArrayList<LinkListItem>(rows.size());
        for (SearchResultDocument.SearchResult.Rows.Row row : rows) {
            items.add(new LinkListItem(new VariantKey(row.getDocumentId(), row.getBranchId(), row.getLanguageId()), row.getValueArray(0)));
        }
        return Collections.unmodifiableList(items);
    }

    public String getLabel(Object value, Locale locale) {
        return getItemLabel(value, locale);
    }

    public String getItemLabel(Object value, Locale locale) {
        // value is here a VariantKey. The label is the document name, so this would require
        // looking up the document. However, the VariantKey object might contain -1 for branch
        // and language (meaning these are the same as the document they are embedded it), in
        // which case it is impossible to know what document to retrieve.
        return null;
    }

    public void addToFieldTypeXml(SelectionListDocument.SelectionList selectionListXml) {
        selectionListXml.setLinkQuerySelectionList(getXml().getLinkQuerySelectionList());
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof LinkQuerySelectionListImpl))
            return false;

        LinkQuerySelectionListImpl other = (LinkQuerySelectionListImpl)obj;

        if (filterVariants != other.filterVariants)
            return false;

        return whereClause.equals(other.whereClause);
    }
}
