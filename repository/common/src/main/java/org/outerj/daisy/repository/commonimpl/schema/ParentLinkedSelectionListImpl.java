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

import org.outerj.daisy.repository.schema.ParentLinkedSelectionList;
import org.outerj.daisy.repository.schema.ListItem;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.query.ResultSet;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerx.daisy.x10.SelectionListDocument;
import org.outerx.daisy.x10.ExpSelectionListDocument;
import org.outerx.daisy.x10.ParentLinkedSelectionListDocument;

import java.util.*;

public class ParentLinkedSelectionListImpl implements ParentLinkedSelectionList {
    private String whereClause;
    private String parentLinkField;
    private boolean filterVariants;
    private FieldTypeImpl owner;

    public ParentLinkedSelectionListImpl(String whereClause, String parentLinkField, boolean filterVariants,
            FieldTypeImpl owner) {
        if (whereClause == null)
            throw new IllegalArgumentException("Null argument: whereClause");
        if (parentLinkField == null)
            throw new IllegalArgumentException("Null argument: parentLinkField");
        if (owner == null)
            throw new IllegalArgumentException("Null argument: owner");

        this.whereClause = whereClause;
        this.parentLinkField = parentLinkField;
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

    public String getParentLinkField() {
        return parentLinkField;
    }

    public void setParentLinkField(String parentLinkField) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);

        if (parentLinkField == null)
            throw new IllegalArgumentException("Null argument: parentLinkField");

        this.parentLinkField = parentLinkField;
    }

    public List<? extends ListItem> getItems() {
        throw new RuntimeException("This method should be called through ParentLinkedSelectionListWrapper");
    }

    public List<? extends ListItem> getItems(Repository repository, SchemaStrategy schemaStrategy, AuthenticatedUser user) {
        if (filterVariants)
            throw new RepositoryRuntimeException("This selection list needs to filter on variants, so use getItems(branchId, languageId) instead.");
        return getItems(-1, -1, Locale.getDefault(), repository, schemaStrategy, user);

    }

    public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale) {
        throw new RuntimeException("This method should be called through ParentLinkedSelectionListWrapper");
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

        // Some checks on the field
        FieldType fieldType;
        try {
            fieldType = repository.getRepositorySchema().getFieldType(parentLinkField, false);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Error getting info on parent link field in parent-linked selection list.", e);
        }
        if (fieldType.getValueType() != ValueType.LINK) {
            throw new RepositoryRuntimeException("Parent-linked selection list field is not of type link: " + parentLinkField);
        }
        if (!fieldType.isPrimitive()) {
            throw new RepositoryRuntimeException("Parent-linked selection list field is not primitive (= non-multivalue and non-hierarchical): " + parentLinkField);
        }

        String query = "select name, $" + parentLinkField + " where " + whereClause;
        ResultSet rs;
        try {
            String extraCond = filterVariants ? "branchId = " + branchId + " and languageId = " + languageId : null;
            rs = repository.getQueryManager().performQueryReturnResultSet(query, extraCond, null, locale, null);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Error executing parent-linked selection list query: " + query, e);
        }

        Stack<VariantKey> parentStack = new Stack<VariantKey>(); // used to detect recursions
        List<LinkListItem> roots = new ArrayList<LinkListItem>();
        for (int i = 0; i < rs.getSize(); i++) {
            rs.absolute(i);
            if (rs.getValue(1) == null) {
                VariantKey variantKey = rs.getVariantKey();
                String documentName = (String)rs.getValue(0);
                parentStack.push(variantKey);
                List<LinkListItem> children = getChildren(rs.getVariantKey(), parentStack, rs);
                roots.add(new LinkListItem(variantKey, documentName, children));
                parentStack.pop();
            }
        }

        return Collections.unmodifiableList(roots);
    }

    private List<LinkListItem> getChildren(VariantKey key, Stack<VariantKey> parentStack, ResultSet rs) {
        List<LinkListItem> children = new ArrayList<LinkListItem>();
        for (int i = 0; i < rs.getSize(); i++) {
            rs.absolute(i);
            VariantKey childKey = rs.getVariantKey();
            VariantKey parentKey = (VariantKey)rs.getValue(1);
            if (parentKey != null && expandVariantKey(parentKey, childKey).equals(key)) {
                if (parentStack.contains(childKey)) {
                    throw new RepositoryRuntimeException("There is a recursion in the hierarchical tree of the field " + owner.getName());
                }
                parentStack.push(childKey);
                children.add(new LinkListItem(childKey, (String)rs.getValue(0), getChildren(childKey, parentStack, rs)));
                parentStack.pop();
            }
        }
        return children;
    }
    
    private VariantKey expandVariantKey(VariantKey variantKey, VariantKey refVariantKey) {
        if (variantKey.getBranchId() == -1 || variantKey.getLanguageId() == -1) {
            long branchId = variantKey.getBranchId() == -1 ? refVariantKey.getBranchId() : variantKey.getBranchId();
            long languageId = variantKey.getLanguageId() == -1 ? refVariantKey.getLanguageId() : variantKey.getLanguageId();
            return new VariantKey(variantKey.getDocumentId(), branchId, languageId);
        }
        return variantKey;
    }

    public String getLabel(Object value, Locale locale) {
        return null;
    }

    public String getItemLabel(Object value, Locale locale) {
        return null;
    }

    public void addToFieldTypeXml(SelectionListDocument.SelectionList selectionListXml) {
        ParentLinkedSelectionListDocument.ParentLinkedSelectionList listXml = selectionListXml.addNewParentLinkedSelectionList();
        listXml.setWhereClause(whereClause);
        listXml.setFilterVariants(filterVariants);
        listXml.setParentLinkField(parentLinkField);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ParentLinkedSelectionListImpl))
            return false;

        ParentLinkedSelectionListImpl other = (ParentLinkedSelectionListImpl)obj;

        if (!whereClause.equals(other.whereClause))
            return false;

        if (filterVariants != other.filterVariants)
            return false;

        return parentLinkField.equals(other.parentLinkField);
    }
}
