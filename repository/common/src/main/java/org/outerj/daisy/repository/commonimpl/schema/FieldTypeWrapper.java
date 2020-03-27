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
package org.outerj.daisy.repository.commonimpl.schema;

import org.outerj.daisy.repository.schema.*;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerx.daisy.x10.*;

import java.util.Locale;
import java.util.Date;
import java.util.List;

/**
 * A wrapper around a field type, intended to customize field type behaviour
 * for the current user when a field type is retrieved from the cache.
 * This is needed to support user-specific query based selection lists
 * (otherwise the queries would be executed using the user stored
 * in the cached field type object, which is not the current user).
 */
public class FieldTypeWrapper implements FieldType {
    private final FieldTypeImpl fieldTypeImpl;
    private final AuthenticatedUser user;
    private final SchemaStrategy schemaStrategy;
    private final Repository repository;

    protected FieldTypeWrapper(FieldTypeImpl fieldTypeImpl, AuthenticatedUser user, SchemaStrategy schemaStrategy,
            Repository repository) {
        this.fieldTypeImpl = fieldTypeImpl;
        this.user = user;
        this.schemaStrategy = schemaStrategy;
        this.repository= repository;
    }

    public FieldTypeImpl getImpl() {
        return fieldTypeImpl;
    }

    public long getId() {
        return fieldTypeImpl.getId();
    }

    public ValueType getValueType() {
        return fieldTypeImpl.getValueType();
    }

    public String getName() {
        return fieldTypeImpl.getName();
    }

    public void setName(String name) {
        fieldTypeImpl.setName(name);
    }

    public String getDescription(Locale locale) {
        return fieldTypeImpl.getDescription(locale);
    }

    public String getDescriptionExact(Locale locale) {
        return fieldTypeImpl.getDescriptionExact(locale);
    }

    public void setDescription(Locale locale, String description) {
        fieldTypeImpl.setDescription(locale, description);
    }

    public void clearDescriptions() {
        fieldTypeImpl.clearDescriptions();
    }

    public Locale[] getDescriptionLocales() {
        return fieldTypeImpl.getDescriptionLocales();
    }

    public String getLabel(Locale locale) {
        return fieldTypeImpl.getLabel(locale);
    }

    public String getLabelExact(Locale locale) {
        return fieldTypeImpl.getLabelExact(locale);
    }

    public void setLabel(Locale locale, String label) {
        fieldTypeImpl.setLabel(locale, label);
    }

    public void clearLabels() {
        fieldTypeImpl.clearLabels();
    }

    public Locale[] getLabelLocales() {
        return fieldTypeImpl.getLabelLocales();
    }

    public Date getLastModified() {
        return fieldTypeImpl.getLastModified();
    }

    public long getLastModifier() {
        return fieldTypeImpl.getLastModifier();
    }

    public boolean isAclAllowed() {
        return fieldTypeImpl.isAclAllowed();
    }

    public void setAclAllowed(boolean aclAllowed) {
        fieldTypeImpl.setAclAllowed(aclAllowed);
    }

    public boolean isMultiValue() {
        return fieldTypeImpl.isMultiValue();
    }

    public boolean isHierarchical() {
        return fieldTypeImpl.isHierarchical();
    }

    public boolean isPrimitive() {
        return fieldTypeImpl.isPrimitive();
    }

    public boolean getAllowFreeEntry() {
        return fieldTypeImpl.getAllowFreeEntry();
    }

    public void setAllowFreeEntry(boolean allowFreeEntry) {
        fieldTypeImpl.setAllowFreeEntry(allowFreeEntry);
    }

    public boolean getLoadSelectionListAsync() {
        return fieldTypeImpl.getLoadSelectionListAsync();
    }

    public void setLoadSelectionListAsync(boolean loadAsync) {
        fieldTypeImpl.setLoadSelectionListAsync(loadAsync);
    }

    public FieldTypeDocument getXml() {
        return fieldTypeImpl.getXml();
    }

    public void setAllFromXml(FieldTypeDocument.FieldType fieldTypeXml) {
        fieldTypeImpl.setAllFromXml(fieldTypeXml);
    }

    public void save() throws RepositoryException {
        fieldTypeImpl.save();
    }

    public void setDeprecated(boolean deprecated) {
        fieldTypeImpl.setDeprecated(deprecated);
    }

    public boolean isDeprecated() {
        return fieldTypeImpl.isDeprecated();
    }

    public long getUpdateCount() {
        return fieldTypeImpl.getUpdateCount();
    }

    public SelectionList getSelectionList() {
        SelectionList list = fieldTypeImpl.getSelectionList();

        if (list instanceof HierarchicalQuerySelectionList) {
            return new HierarchicalQuerySelectionListWrapper((HierarchicalQuerySelectionListImpl)list);
        } else if (list instanceof LinkQuerySelectionList) {
            return new LinkQuerySelectionListWrapper((LinkQuerySelectionListImpl)list);
        } else if (list instanceof QuerySelectionList) {
            return new QuerySelectionListWrapper((QuerySelectionListImpl)list);
        } else if (list instanceof ParentLinkedSelectionListImpl) {
            return new ParentLinkedSelectionListWrapper((ParentLinkedSelectionListImpl)list);
        }

        return list;
    }

    public boolean hasSelectionList() {
        return fieldTypeImpl.hasSelectionList();
    }

    public ExpSelectionListDocument getExpandedSelectionListXml(long branchId, long languageId, Locale locale) {
        return fieldTypeImpl.getExpandedSelectionListXml(branchId, languageId, locale, getSelectionList(), repository);
    }

    public void clearSelectionList() {
        fieldTypeImpl.clearSelectionList();
    }

    public StaticSelectionList createStaticSelectionList() {
        return fieldTypeImpl.createStaticSelectionList();
    }

    public LinkQuerySelectionList createLinkQuerySelectionList(String whereClause, boolean filterVariants) {
        return new LinkQuerySelectionListWrapper((LinkQuerySelectionListImpl)fieldTypeImpl.createLinkQuerySelectionList(whereClause, filterVariants));
    }

    public QuerySelectionList createQuerySelectionList(String query, boolean filterVariants, SortOrder sortOrder) {
        return new QuerySelectionListWrapper((QuerySelectionListImpl)fieldTypeImpl.createQuerySelectionList(query, filterVariants, sortOrder));
    }

    public HierarchicalQuerySelectionList createHierarchicalQuerySelectionList(String whereClause, String[] fieldTypeNames, boolean filterVariants) {
        return new HierarchicalQuerySelectionListWrapper((HierarchicalQuerySelectionListImpl)fieldTypeImpl.createHierarchicalQuerySelectionList(whereClause, fieldTypeNames, filterVariants));
    }

    public ParentLinkedSelectionList createParentLinkedSelectionList(String whereClause, String linkFieldName, boolean filterVariants) {
        return new ParentLinkedSelectionListWrapper((ParentLinkedSelectionListImpl)fieldTypeImpl.createParentLinkedSelectionList(whereClause, linkFieldName, filterVariants));
    }

    public int getSize() {
        return fieldTypeImpl.getSize();
    }

    public void setSize(int size) {
        fieldTypeImpl.setSize(size);
    }

    public boolean equals(Object obj) {
        return fieldTypeImpl.equals(obj);
    }

    private class HierarchicalQuerySelectionListWrapper implements HierarchicalQuerySelectionList {
        private HierarchicalQuerySelectionListImpl listImpl;

        private HierarchicalQuerySelectionListWrapper(HierarchicalQuerySelectionListImpl listImpl) {
            this.listImpl = listImpl;
        }

        public String getWhereClause() {
            return listImpl.getWhereClause();
        }

        public void setWhereClause(String whereClause) {
            listImpl.setWhereClause(whereClause);
        }

        public boolean getFilterVariants() {
            return listImpl.getFilterVariants();
        }

        public void setFilterVariants(boolean filterVariants) {
            listImpl.setFilterVariants(filterVariants);
        }

        public void setLinkFields(String[] linkFieldNames) {
            listImpl.setLinkFields(linkFieldNames);
        }

        public String[] getLinkFields() {
            return listImpl.getLinkFields();
        }

        public List<? extends ListItem> getItems() {
            return listImpl.getItems(repository, schemaStrategy, user);
        }

        public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale) {
            return listImpl.getItems(branchId, languageId, locale, repository, schemaStrategy, user);
        }

        public String getLabel(Object value, Locale locale) {
            return listImpl.getLabel(value, locale);
        }

        public String getItemLabel(Object value, Locale locale) {
            return listImpl.getItemLabel(value, locale);
        }

        public void addToFieldTypeXml(SelectionListDocument.SelectionList selectionListXml) {
            listImpl.addToFieldTypeXml(selectionListXml);
        }

        public boolean equals(Object obj) {
            return listImpl.equals(obj);
        }
    }

    private class ParentLinkedSelectionListWrapper implements ParentLinkedSelectionList {
        private ParentLinkedSelectionListImpl listImpl;

        private ParentLinkedSelectionListWrapper(ParentLinkedSelectionListImpl listImpl) {
            this.listImpl = listImpl;
        }

        public String getWhereClause() {
            return listImpl.getWhereClause();
        }

        public void setWhereClause(String whereClause) {
            listImpl.setWhereClause(whereClause);
        }

        public boolean getFilterVariants() {
            return listImpl.getFilterVariants();
        }

        public void setFilterVariants(boolean filterVariants) {
            listImpl.setFilterVariants(filterVariants);
        }

        public String getParentLinkField() {
            return listImpl.getParentLinkField();
        }

        public void setParentLinkField(String parentLinkField) {
            listImpl.setParentLinkField(parentLinkField);
        }

        public List<? extends ListItem> getItems() {
            return listImpl.getItems(repository, schemaStrategy, user);
        }

        public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale) {
            return listImpl.getItems(branchId, languageId, locale, repository, schemaStrategy, user);
        }

        public String getLabel(Object value, Locale locale) {
            return listImpl.getLabel(value, locale);
        }

        public String getItemLabel(Object value, Locale locale) {
            return listImpl.getItemLabel(value, locale);
        }

        public void addToFieldTypeXml(SelectionListDocument.SelectionList selectionListXml) {
            listImpl.addToFieldTypeXml(selectionListXml);
        }

        public boolean equals(Object obj) {
            return listImpl.equals(obj);
        }
    }

    private class LinkQuerySelectionListWrapper implements LinkQuerySelectionList {
        private LinkQuerySelectionListImpl listImpl;

        private LinkQuerySelectionListWrapper(LinkQuerySelectionListImpl listImpl) {
            this.listImpl = listImpl;
        }

        public String getWhereClause() {
            return listImpl.getWhereClause();
        }

        public void setWhereClause(String whereClause) {
            listImpl.setWhereClause(whereClause);
        }

        public boolean getFilterVariants() {
            return listImpl.getFilterVariants();
        }

        public void setFilterVariants(boolean filterVariants) {
            listImpl.setFilterVariants(filterVariants);
        }

        public LinkQuerySelectionListDocument getXml() {
            return listImpl.getXml();
        }

        public List<? extends ListItem> getItems() {
            return listImpl.getItems(repository);
        }

        public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale) {
            return listImpl.getItems(branchId, languageId, locale, repository);
        }

        public String getLabel(Object value, Locale locale) {
            return listImpl.getLabel(value, locale);
        }

        public String getItemLabel(Object value, Locale locale) {
            return listImpl.getItemLabel(value, locale);
        }

        public void addToFieldTypeXml(SelectionListDocument.SelectionList selectionListXml) {
            listImpl.addToFieldTypeXml(selectionListXml);
        }

        public boolean equals(Object obj) {
            return listImpl.equals(obj);
        }
    }

    private class QuerySelectionListWrapper implements QuerySelectionList {
        private QuerySelectionListImpl listImpl;

        private QuerySelectionListWrapper(QuerySelectionListImpl listImpl) {
            this.listImpl = listImpl;
        }

        public String getQuery() {
            return listImpl.getQuery();
        }

        public SortOrder getSortOrder() {
            return listImpl.getSortOrder();
        }

        public void setQuery(String query) {
            listImpl.setQuery(query);
        }

        public void setSortOrder(SortOrder sortOrder) {
            listImpl.setSortOrder(sortOrder);
        }

        public boolean getFilterVariants() {
            return listImpl.getFilterVariants();
        }

        public void setFilterVariants(boolean filterVariants) {
            listImpl.setFilterVariants(filterVariants);
        }

        public QuerySelectionListDocument getXml() {
            return listImpl.getXml();
        }

        public List<? extends ListItem> getItems() {
            return listImpl.getItems(repository);
        }

        public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale) {
            return listImpl.getItems(branchId, languageId, locale, repository);
        }

        public String getLabel(Object value, Locale locale) {
            return listImpl.getLabel(value, locale);
        }

        public String getItemLabel(Object value, Locale locale) {
            return listImpl.getItemLabel(value, locale);
        }

        public void addToFieldTypeXml(SelectionListDocument.SelectionList selectionListXml) {
            listImpl.addToFieldTypeXml(selectionListXml);
        }

        public boolean equals(Object obj) {
            return listImpl.equals(obj);
        }
    }
}
