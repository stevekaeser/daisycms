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

import java.util.*;

import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.FieldHelper;
import org.outerj.daisy.repository.schema.ListItem;
import org.outerj.daisy.repository.schema.StaticListItem;
import org.outerj.daisy.repository.schema.StaticSelectionList;
import org.outerx.daisy.x10.ListItemDocument;
import org.outerx.daisy.x10.SelectionListDocument;
import org.outerx.daisy.x10.StaticSelectionListDocument;

/**
 * A static selection list, manually created 
 * by a daisy administrator for a specific FieldType.
 */
public class StaticSelectionListImpl implements StaticSelectionList {
    private StaticListItemImpl root;
    private SchemaStrategy schemaStrategy = null;
    // marks whether or not the object was changed since last storage
    private boolean hasChanges = false;
    private IntimateAccess intimateAccess = new IntimateAccess();
    private ValueType valueType;
    private FieldTypeImpl owner;

    public StaticSelectionListImpl(SchemaStrategy creatingStrategy, ValueType valueType, FieldTypeImpl owner) {
        this.schemaStrategy = creatingStrategy;
        this.valueType = valueType;
        this.owner = owner;
        this.root = new StaticListItemImpl("<root, should never be visible>", valueType, creatingStrategy, this);
    }

    protected FieldTypeImpl getOwner() {
        return owner;
    }

    protected boolean isReadOnly() {
        return owner.isReadOnly();
    }

    protected void markChanged() {
        this.hasChanges = true;
    }

    public List<? extends ListItem> getItems() {
        return root.getItems();
    }

    public List<? extends ListItem> getItems(long branchId, long languageId, Locale locale) {
        return getItems();
    }

    public void addItem(Object value) throws InvalidValueTypeException {
        root.createItem(value);
    }

    public void clear() {
        root.clear();
    }

    public StaticListItem createItem(Object value) {
        return root.createItem(value);
    }

    public StaticListItem createStaticListItem(Object value) {
        return createItem(value);
    }

    public String getLabel(Object value, Locale locale) {
        return getItemLabel(value, locale);
    }

    public StaticListItem getItem(Object value) {
        return root.getItem(value);
    }

    public String getItemLabel(Object value, Locale locale) {
        return root.getItemLabel(value, locale);
    }

    public StaticSelectionListDocument getXml() {
        StaticSelectionListDocument selListDoc = StaticSelectionListDocument.Factory.newInstance();
        StaticSelectionListDocument.StaticSelectionList selListXml = selListDoc.addNewStaticSelectionList();

        List<StaticListItem> children = (List<StaticListItem>)root.getItems();
        ListItemDocument.ListItem[] childrenXml = new ListItemDocument.ListItem[children.size()];
        int c = 0;
        for (StaticListItem child : children) {
            childrenXml[c++] = child.getXml().getListItem();
        }
        selListXml.setListItemArray(childrenXml);

        return selListDoc;
    }

    public void addToFieldTypeXml(SelectionListDocument.SelectionList selectionListXml) {
        selectionListXml.setStaticSelectionList(getXml().getStaticSelectionList());
    }

    public void setAllFromXml(StaticSelectionListDocument.StaticSelectionList listXml) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);

        for (ListItemDocument.ListItem childXml : listXml.getListItemList()) {
            Object value;

            value = FieldHelper.getFieldValueFromXml(valueType, false, childXml);
            StaticListItemImpl li = (StaticListItemImpl) createItem(value);
            li.setAllFromXml(childXml);
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof StaticSelectionListImpl))
            return false;

        StaticSelectionListImpl other = (StaticSelectionListImpl)obj;
        if (valueType != other.valueType)
            return false;

        return root.equals(other.root);
    }

    public IntimateAccess getIntimateAccess(SchemaStrategy strategy) {
        return strategy == schemaStrategy ? intimateAccess : null;
    }

    public class IntimateAccess {
        private IntimateAccess() {}

        /**
         * Method to be called by the strategy after the object
         * has been persisted.
         */
        public void saved() {
            hasChanges = false;
        }

        /**
         * Marks whether items have been added and/or cleared after
         * the last time the object was persisted.
         */
        public boolean hasChanges() {
            return hasChanges;
        }

        /**
         * Adds listitem without marking the fact that the selection list
         * has been changed.
         */
        public void addItem(StaticListItemImpl item) {
            root.getIntimateAccess(schemaStrategy).addItem(item);
        }

        public StaticListItemImpl getRoot() {
            return root;
        }
    }
}
