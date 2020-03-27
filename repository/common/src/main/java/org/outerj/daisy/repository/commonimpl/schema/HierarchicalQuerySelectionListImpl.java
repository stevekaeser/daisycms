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

import org.outerj.daisy.repository.schema.HierarchicalQuerySelectionList;
import org.outerj.daisy.repository.schema.ListItem;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerx.daisy.x10.*;

import java.util.*;

public class HierarchicalQuerySelectionListImpl implements HierarchicalQuerySelectionList {
    private String whereClause;
    private String[] linkFields;
    private boolean filterVariants;
    private FieldTypeImpl owner;

    public HierarchicalQuerySelectionListImpl(String whereClause, String[] linkFieldNames, boolean filterVariants,
            FieldTypeImpl owner) {
        if (whereClause == null)
            throw new IllegalArgumentException("Null argument: whereClause");
        if (linkFieldNames == null)
            throw new IllegalArgumentException("Null argument: linkFieldNames");
        if (owner == null)
            throw new IllegalArgumentException("Null argument: owner");

        this.whereClause = whereClause;
        this.linkFields = linkFieldNames;
        this.filterVariants = filterVariants;
        this.owner = owner;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public void setWhereClause(String whereClause) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);

        if (whereClause == null)
            throw new IllegalArgumentException("Null argument: whereClause");

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

    public void setLinkFields(String[] linkFieldNames) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);

        if (linkFieldNames == null)
            throw new IllegalArgumentException("Null argument: linkFieldNames");

        for (String linkFieldName : linkFieldNames) {
            if (linkFieldName == null)
                throw new IllegalArgumentException("linkFieldNames contains a null entry.");
        }

        this.linkFields = linkFieldNames;
    }

    public String[] getLinkFields() {
        return linkFields.clone();
    }

    public List<? extends ListItem> getItems() {
        throw new RuntimeException("This method should be called through HierarchicalQuerySelectionListWrapper");
    }

    public List<? extends ListItem> getItems(Repository repository, SchemaStrategy schemaStrategy, AuthenticatedUser user) {
        if (filterVariants)
            throw new RepositoryRuntimeException("This selection list needs to filter on variants, so use getItems(branchId, languageId) instead.");
        return getItems(-1, -1, Locale.getDefault(), repository, schemaStrategy, user);
    }

    public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale) {
        throw new RuntimeException("This method should be called through HierarchicalQuerySelectionListWrapper");
    }

    public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale,
            Repository repository, SchemaStrategy schemaStrategy, AuthenticatedUser user) {

        ExpSelectionListDocument listDocument;
        try {
            listDocument = schemaStrategy.getExpandedSelectionListData(owner.getId(), branchId, languageId, locale, user);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Error fetching hierarchical query selection list data.", e);
        }
        if (listDocument != null)
            return SelListUtil.buildListFromXml(listDocument);

        List<Long> fieldTypeIds = new ArrayList<Long>(linkFields.length);
        for (String linkField : linkFields) {
            FieldType fieldType;
            try {
                fieldType = repository.getRepositorySchema().getFieldTypeByName(linkField, false);
            } catch (RepositoryException e) {
                throw new RepositoryRuntimeException("Error getting field info in hierarchical query selection list for: " + linkField, e);
            }
            if (fieldType.getValueType() != ValueType.LINK) {
                throw new RepositoryRuntimeException("Hierarchical query selection list references a field that is not of type link: " + linkField);
            }
            if (fieldType.isHierarchical()) {
                throw new RepositoryRuntimeException("Hierarchical query selection list references a field that is hierarchical: " + linkField);
            }
            fieldTypeIds.add(fieldType.getId());
        }

        String query = "select name where " + whereClause;
        VariantKey[] searchResults;
        try {
            if (filterVariants) {
                searchResults = repository.getQueryManager().performQueryReturnKeys(query, "branchId = " + branchId + " and languageId = " + languageId, locale);
            } else {
                searchResults = repository.getQueryManager().performQueryReturnKeys(query, locale);
            }
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Error executing hierarchical-query selection list query: " + query, e);
        }

        List<LinkListItem> items = new ArrayList<LinkListItem>(searchResults.length);
        Stack<VariantKey> parents = new Stack<VariantKey>();
        for (VariantKey variantKey : searchResults) {
            parents.push(variantKey);
            Document document = getDocument(variantKey, repository);
            if (document == null)
                continue;
            List<LinkListItem> children;
            if (fieldTypeIds.size() > 0)
                children = buildChildren(document, fieldTypeIds, 0, parents, repository);
            else
                children = Collections.emptyList();
            items.add(new LinkListItem(variantKey, document.getName(), children));
            parents.pop();
        }

        return Collections.unmodifiableList(items);
    }

    private List<LinkListItem> buildChildren(Document document, List<Long>fieldTypeIds, int level,
            Stack<VariantKey> parents, Repository repository) {
        List<LinkListItem> items;
        if (document.hasField(fieldTypeIds.get(level))) {
            Field field = document.getField(fieldTypeIds.get(level));
            Object value = field.getValue();
            Object[] values = value instanceof Object[] ? (Object[])value : new Object[] { value };
            items = new ArrayList<LinkListItem>(values.length);
            for (Object linkValue : values) {
                VariantKey variantKey = expandVariantKey((VariantKey)linkValue, document);
                if (parents.contains(variantKey))
                    throw new RepositoryRuntimeException("Loop in hierarchical-query selection list on " + variantKey);
                parents.push(variantKey);
                Document childDocument = getDocument(variantKey, repository);
                if (childDocument == null)
                    continue;
                List<LinkListItem> children;
                LinkListItem childItem;
                if (level < fieldTypeIds.size() - 1) {
                    children = buildChildren(childDocument, fieldTypeIds, level + 1, parents, repository);
                    childItem = new LinkListItem(variantKey, childDocument.getName(), children);
                } else {
                    childItem = new LinkListItem(variantKey, childDocument.getName());
                }
                items.add(childItem);
                parents.pop();
            }
        } else {
            items = Collections.emptyList();
        }
        return items;
    }

    private VariantKey expandVariantKey(VariantKey variantKey, Document document) {
        if (variantKey.getBranchId() == -1 || variantKey.getLanguageId() == -1) {
            long branchId = variantKey.getBranchId() == -1 ? document.getBranchId() : variantKey.getBranchId();
            long languageId = variantKey.getLanguageId() == -1 ? document.getLanguageId() : variantKey.getLanguageId();
            return new VariantKey(variantKey.getDocumentId(), branchId, languageId);
        }
        return variantKey;
    }

    private Document getDocument(VariantKey variantKey, Repository repository) {
        Document document = null;
        try {
            document = repository.getDocument(variantKey, false);
        } catch (DocumentNotFoundException e) {
            // ignore
        } catch (DocumentVariantNotFoundException e) {
            // ignore
        } catch (AccessException e) {
            // ignore
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Error during building hierarchical selection list with document " + variantKey, e);
        }
        return document;
    }

    public String getLabel(Object value, Locale locale) {
        return null;
    }

    public String getItemLabel(Object value, Locale locale) {
        return null;
    }

    public void addToFieldTypeXml(SelectionListDocument.SelectionList selectionListXml) {
        HierarchicalQuerySelectionListDocument.HierarchicalQuerySelectionList listXml = selectionListXml.addNewHierarchicalQuerySelectionList();
        listXml.setWhereClause(whereClause);
        listXml.setFilterVariants(filterVariants);
        HierarchicalQuerySelectionListDocument.HierarchicalQuerySelectionList.LinkFieldNames linkFieldNamesXml = listXml.addNewLinkFieldNames();
        for (String linkFieldName : linkFields)
            linkFieldNamesXml.addLinkFieldName(linkFieldName);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof HierarchicalQuerySelectionListImpl))
            return false;

        HierarchicalQuerySelectionListImpl other = (HierarchicalQuerySelectionListImpl)obj;

        if (!whereClause.equals(other.whereClause))
            return false;

        if (filterVariants != other.filterVariants)
            return false;

        return Arrays.equals(linkFields, other.linkFields);
    }
}
