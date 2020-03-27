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
import org.outerj.daisy.repository.schema.StaticListItem;
import org.outerj.daisy.repository.schema.ListItem;
import org.outerj.daisy.util.LocaleMap;
import org.outerx.daisy.x10.ListItemDocument;

/**
 * Implementation of StaticListItem
 */
public class StaticListItemImpl implements StaticListItem {
    private final Object value;
    private List<StaticListItemImpl> children;
    private Map<Object, StaticListItemImpl> childrenByValue;
    private SchemaLocaleMap labelMap = new SchemaLocaleMap();
    private IntimateAccess intimateAccess = new IntimateAccess();
    private SchemaStrategy schemaStrategy = null;
    private StaticSelectionListImpl owner;
    private ValueType valueType;

    public StaticListItemImpl(Object value, ValueType valueType, SchemaStrategy schemaStrategy,
            StaticSelectionListImpl owner) {
        this.value = value;
        this.schemaStrategy = schemaStrategy;
        this.valueType = valueType;
        this.owner = owner;
    }

    public Object getValue() {
        return value;
    }

    public void setLabel(Locale locale, String label) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);

        labelMap.put(locale, label);
    }

    public String getLabel(Locale locale) {
        return (String) labelMap.get(locale);
    }

    public String getLabelExact(Locale locale) {
        return (String) labelMap.getExact(locale);
    }

    public void clearLabels() {
        labelMap.clear();
    }

    public Locale[] getLabelLocales() {
        return labelMap.getLocales();
    }

    public void addItem(Object value) {
        createItem(value);
    }

    public void clear() {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);

        if (children != null) {
            children.clear();
            childrenByValue.clear();
            owner.markChanged();
        }
    }

    public StaticListItem createItem(Object value) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);

        if (!valueType.getTypeClass().isAssignableFrom(value.getClass()))
            throw new InvalidValueTypeException();

        if (children == null)
            children = new ArrayList<StaticListItemImpl>();

        StaticListItemImpl listItem = new StaticListItemImpl(value, valueType, this.schemaStrategy, owner);
        children.add(listItem);
        index(listItem);
        owner.markChanged();
        return listItem;
    }

    private void index(StaticListItemImpl listItem) {
        if (childrenByValue == null)
            childrenByValue = new HashMap<Object, StaticListItemImpl>();
        childrenByValue.put(listItem.getValue(), listItem);
    }

    public List<? extends ListItem> getItems() {
        if (this.children == null)
            return Collections.emptyList();

        return Collections.unmodifiableList(children);
    }

    public StaticListItem getItem(Object value) {
        if (children == null)
            return null;

        return childrenByValue.get(value);
    }

    public String getItemLabel(Object value, Locale locale) {
        if (childrenByValue == null)
            return null;
        ListItem listItem = childrenByValue.get(value);
        if (listItem != null) {
            String label = listItem.getLabel(locale);
            if (label != null)
                return label;
        }

        return null;
    }

    public ListItemDocument getXml() {
        ListItemDocument listItemDocument = ListItemDocument.Factory.newInstance();
        ListItemDocument.ListItem listItem = listItemDocument.addNewListItem();

        // add value
        FieldHelper.getXmlFieldValueSetter(valueType).addValue(value, listItem);

        // add children
        if (children != null) {
            ListItemDocument.ListItem[] childrenXml = new ListItemDocument.ListItem[children.size()];
            int c = 0;
            for (StaticListItem child : children) {
                childrenXml[c++] = child.getXml().getListItem();
            }
            listItem.setListItemArray(childrenXml);
        }

        // add labels
        if (!labelMap.isEmpty())
            listItem.setLabels(labelMap.getAsLabelsXml());

        return listItemDocument;
    }

    public void setAllFromXml(ListItemDocument.ListItem listItemXml) {
        if (owner.isReadOnly())
            throw new RuntimeException(FieldTypeImpl.READ_ONLY_MESSAGE);

        // Value is not set on purpose, the value of a static list item
        // is immutable after creation (otherwise, this would require
        // updating indexes in parent static list items).

        if (listItemXml.isSetLabels())
            labelMap.readFromLabelsXml(listItemXml.getLabels());

        clear();
        for (ListItemDocument.ListItem childXml : listItemXml.getListItemList()) {
            Object value = FieldHelper.getFieldValueFromXml(valueType, false, false, childXml);
            StaticListItem child = createItem(value);
            child.setAllFromXml(childXml);
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof StaticListItemImpl))
            return false;

        StaticListItemImpl other = (StaticListItemImpl)obj;

        if (valueType != other.valueType)
            return false;

        if (!value.equals(other.value))
            return false;

        if (!(children == null && other.children == null) && (children == null || other.children == null))
            return false;

        if (children != null && !(children.equals(other.children)))
            return false;

        return labelMap.equals(other.labelMap);
    }

    public IntimateAccess getIntimateAccess(SchemaStrategy strategy) {
        if (strategy == schemaStrategy) {
            return intimateAccess;
        } else {
            return null;
        }
    }

    public class IntimateAccess {
        private IntimateAccess() {
        }

        public LocaleMap getLabels() {
            return labelMap;
        }

        /**
         * Adds listitem without marking the fact that the selection list
         * has been changed.
         */
        public void addItem(StaticListItemImpl element) {
            children.add(element);
            index(element);
        }
    }
}
