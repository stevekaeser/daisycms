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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.List;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.Util;
import org.outerj.daisy.repository.schema.*;
import org.outerj.daisy.util.LocaleMap;
import org.outerx.daisy.x10.*;

// IMPORTANT:
//  When adding/changing properties to a field type, or any of the objects used by it
//  be sure to update the equals method if needed

public class FieldTypeImpl implements FieldType {
    private long id = -1;
    private ValueType valueType;
    private boolean multiValue = false;
    private boolean hierarchical = false;
    private String name;
    private SchemaLocaleMap description = new SchemaLocaleMap();
    private SchemaLocaleMap label = new SchemaLocaleMap();
    private SelectionList selectionList;
    private SchemaStrategy schemaStrategy;
    private Date lastModified;
    private long lastModifier=-1;
    private AuthenticatedUser currentModifier;
    private long labelId = -1;
    private long descriptionId = -1;
    private boolean deprecated = false;
    private boolean aclAllowed = false;
    private int size = 0;
    private boolean allowFreeEntry = false;
    private boolean loadSelectionListAsync = false;
    private boolean readOnly = false;
    private long updateCount = 0;
    private IntimateAccess intimateAccess = new IntimateAccess();
    protected static final String READ_ONLY_MESSAGE = "This field type is read-only.";

    public FieldTypeImpl(String name, ValueType fieldType, boolean multiValue, boolean hierarchical,
            SchemaStrategy schemaStrategy, AuthenticatedUser user) {
        Util.checkName(name);
        this.name = name;
        this.valueType = fieldType;
        this.multiValue = multiValue;
        this.hierarchical = hierarchical;
        this.schemaStrategy = schemaStrategy;
        this.currentModifier = user;
    }

    public IntimateAccess getIntimateAccess(SchemaStrategy schemaStrategy) {
        if (this.schemaStrategy == schemaStrategy)
            return intimateAccess;
        else
            return null;
    }

    public long getId() {
        return id;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        Util.checkName(name);
        this.name = name;
    }

    public String getDescription(Locale locale) {
        return (String)description.get(locale);
    }

    public String getDescriptionExact(Locale locale) {
        return (String)description.getExact(locale);
    }

    public void setDescription(Locale locale, String description) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.description.put(locale, description);
    }

    public void clearDescriptions() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        description.clear();
    }

    public Locale[] getDescriptionLocales() {
        return description.getLocales();
    }

    public String getLabel(Locale locale) {
        String result = (String)label.get(locale);
        return result == null ? getName() : result;
    }

    public String getLabelExact(Locale locale) {
        return (String)label.getExact(locale);
    }

    public void setLabel(Locale locale, String label) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.label.put(locale, label);
    }

    public void clearLabels() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        label.clear();
    }

    public Locale[] getLabelLocales() {
        return label.getLocales();
    }

    public Date getLastModified() {
        if (lastModified != null)
            return (Date)lastModified.clone();
        else
            return null;
    }

    public long getLastModifier() {
        return lastModifier;
    }

    public boolean isAclAllowed() {
        return aclAllowed;
    }

    public void setAclAllowed(boolean aclAllowed) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.aclAllowed = aclAllowed;
    }

    public boolean isMultiValue() {
        return multiValue;
    }

    public boolean isHierarchical() {
        return hierarchical;
    }

    public boolean isPrimitive() {
        return !multiValue && !hierarchical;
    }

    public boolean getAllowFreeEntry() {
        return allowFreeEntry;
    }

    public void setAllowFreeEntry(boolean allowFreeEntry) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.allowFreeEntry = allowFreeEntry;
    }

    public boolean getLoadSelectionListAsync() {
        return loadSelectionListAsync;
    }

    public void setLoadSelectionListAsync(boolean loadAsync) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.loadSelectionListAsync = loadAsync;
    }

    public FieldTypeDocument getXml() {
        FieldTypeDocument fieldTypeDocument = FieldTypeDocument.Factory.newInstance();
        FieldTypeDocument.FieldType fieldType = fieldTypeDocument.addNewFieldType();

        if (id != -1) {
            fieldType.setId(id);
            GregorianCalendar lastModified = new GregorianCalendar();
            lastModified.setTime(this.lastModified);
            fieldType.setLastModified(lastModified);
            fieldType.setLastModifier(lastModifier);
        }

        fieldType.setName(name);
        fieldType.setValueType(valueType.toString());
        fieldType.setMultiValue(multiValue);
        fieldType.setHierarchical(hierarchical);
        fieldType.setDeprecated(deprecated);
        fieldType.setAclAllowed(aclAllowed);
        fieldType.setLabels(label.getAsLabelsXml());
        fieldType.setDescriptions(description.getAsDescriptionsXml());
        fieldType.setUpdateCount(updateCount);
        fieldType.setSize(size);
        fieldType.setAllowFreeEntry(allowFreeEntry);
        fieldType.setLoadSelectionListAsync(loadSelectionListAsync);

        if (selectionList != null) {
            selectionList.addToFieldTypeXml(fieldType.addNewSelectionList());
        }

        return fieldTypeDocument;
    }

    public void setAllFromXml(FieldTypeDocument.FieldType fieldTypeXml) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.name = fieldTypeXml.getName();
        this.deprecated = fieldTypeXml.getDeprecated();
        this.aclAllowed = fieldTypeXml.getAclAllowed();
        this.label.clear();
        this.label.readFromLabelsXml(fieldTypeXml.getLabels());
        this.description.clear();
        this.description.readFromDescriptionsXml(fieldTypeXml.getDescriptions());
        this.size = fieldTypeXml.getSize();
        this.allowFreeEntry = fieldTypeXml.getAllowFreeEntry();
        this.loadSelectionListAsync = fieldTypeXml.getLoadSelectionListAsync();

        if (fieldTypeXml.getSelectionList() != null) {
            SelectionListDocument.SelectionList selectionListXml = fieldTypeXml.getSelectionList();
            if (selectionListXml.isSetStaticSelectionList()) {
                StaticSelectionList selectionList = this.createStaticSelectionList();
                selectionList.setAllFromXml(fieldTypeXml.getSelectionList().getStaticSelectionList());
                this.selectionList = selectionList;
            } else if (selectionListXml.isSetLinkQuerySelectionList()) {
                this.selectionList = createLinkQuerySelectionList(selectionListXml.getLinkQuerySelectionList().getWhereClause(),
                        selectionListXml.getLinkQuerySelectionList().getFilterVariants());
            } else if (selectionListXml.isSetQuerySelectionList()) {
                this.selectionList = createQuerySelectionList(selectionListXml.getQuerySelectionList().getQuery(),
                        selectionListXml.getQuerySelectionList().getFilterVariants(),
                        SortOrder.fromString(selectionListXml.getQuerySelectionList().getSortOrder()));
            } else if (selectionListXml.isSetHierarchicalQuerySelectionList()) {
                HierarchicalQuerySelectionListDocument.HierarchicalQuerySelectionList listXml = selectionListXml.getHierarchicalQuerySelectionList();
                this.selectionList = createHierarchicalQuerySelectionList(listXml.getWhereClause(),
                        listXml.getLinkFieldNames().getLinkFieldNameList().toArray(new String[0]), listXml.getFilterVariants());
            } else if (selectionListXml.isSetParentLinkedSelectionList()) {
                ParentLinkedSelectionListDocument.ParentLinkedSelectionList listXml = selectionListXml.getParentLinkedSelectionList();
                this.selectionList = createParentLinkedSelectionList(listXml.getWhereClause(),
                        listXml.getParentLinkField(), listXml.getFilterVariants());
            }
        } else {
           clearSelectionList();
       }
    }

    public void save() throws RepositoryException {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        schemaStrategy.store(this);
    }

    public void setDeprecated(boolean deprecated) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.deprecated = deprecated;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    /**
     * Disables all operations that can modify the state of this object.
     */
    public void makeReadOnly() {
        this.readOnly = true;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public SelectionList getSelectionList() {
        return selectionList;
    }

    public boolean hasSelectionList() {
        return selectionList != null;
    }

    public void clearSelectionList() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.selectionList = null;
    }

    public StaticSelectionList createStaticSelectionList() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        StaticSelectionList newList = new StaticSelectionListImpl(this.schemaStrategy, valueType, this);
        this.selectionList = newList;

        return newList;
    }

    public LinkQuerySelectionList createLinkQuerySelectionList(String whereClause, boolean filterVariants) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (valueType != ValueType.LINK)
            throw new RepositoryRuntimeException("A link query selection list can only be added to link-type fields.");

        LinkQuerySelectionListImpl newList = new LinkQuerySelectionListImpl(whereClause, filterVariants, this);
        this.selectionList = newList;

        return newList;
    }

    public QuerySelectionList createQuerySelectionList(String query, boolean filterVariants, SortOrder sortOrder) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        QuerySelectionListImpl newList = new QuerySelectionListImpl(query, filterVariants, sortOrder, this);
        this.selectionList = newList;

        return newList;
    }

    public HierarchicalQuerySelectionList createHierarchicalQuerySelectionList(String whereClause, String[] fieldTypeNames, boolean filterVariants) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (valueType != ValueType.LINK)
            throw new RepositoryRuntimeException("A hierarchical query selection list can only be added to link-type fields.");

        HierarchicalQuerySelectionList newList = new HierarchicalQuerySelectionListImpl(whereClause, fieldTypeNames, filterVariants, this);
        this.selectionList = newList;

        return newList;
    }

    public ParentLinkedSelectionList createParentLinkedSelectionList(String whereClause, String linkFieldName, boolean filterVariants) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (valueType != ValueType.LINK)
            throw new RepositoryRuntimeException("A parent-linked selection list can only be added to link-type fields.");

        ParentLinkedSelectionList newList = new ParentLinkedSelectionListImpl(whereClause, linkFieldName, filterVariants, this);
        this.selectionList = newList;

        return newList;
    }

    public ExpSelectionListDocument getExpandedSelectionListXml(long branchId, long languageId, Locale locale) {
        throw new RuntimeException("This method should be called through FieldTypeWrapper.");
    }

    public ExpSelectionListDocument getExpandedSelectionListXml(long branchId, long languageId, Locale locale, SelectionList list, Repository repository) {
        if (list == null)
            return null;

        ExpSelectionListDocument listDocument = ExpSelectionListDocument.Factory.newInstance();
        ExpSelectionListDocument.ExpSelectionList listXml = listDocument.addNewExpSelectionList();

        listXml.setFieldTypeId(id);
        listXml.setFieldTypeName(name);
        listXml.setValueType(valueType.toString());
        listXml.setHierarchical(hierarchical);
        listXml.setMultiValue(multiValue);
        listXml.setLabel(getLabel(locale));

        List<? extends ListItem> items = list.getItems(branchId, languageId, Locale.getDefault());
        for (ListItem item : items) {
            ExpListItemDocument.ExpListItem childItemXml = listXml.addNewExpListItem();
            buildExpListItem(childItemXml, item, locale, repository);
        }

        return listDocument;
    }

    private void buildExpListItem(ExpListItemDocument.ExpListItem itemXml, ListItem item, Locale locale, Repository repository) {
        Object value = item.getValue();
        FieldValuesType valueXml = itemXml.addNewValue();
        FieldHelper.getXmlFieldValueSetter(valueType).addValue(value, valueXml);

        String label = item.getLabel(locale);
        if (label == null)
            label = FieldHelper.getFormattedValue(value, valueType, locale, repository);
        itemXml.setLabel(label);

        List<? extends ListItem> children  = item.getItems();
        for (ListItem child : children) {
            ExpListItemDocument.ExpListItem childItemXml = itemXml.addNewExpListItem();
            buildExpListItem(childItemXml, child, locale, repository);
        }
    }

    public int getSize() {
        return this.size;
    }

    public void setSize(int size) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (size < 0)
            throw new RuntimeException("Invalid size specified");

        this.size = size;
    }

    public boolean equals(Object obj) {
        if (obj instanceof FieldTypeWrapper)
            obj = ((FieldTypeWrapper)obj).getImpl();

        if (!(obj instanceof FieldTypeImpl))
            return false;

        FieldTypeImpl other = (FieldTypeImpl)obj;

        if (size != other.size)
            return false;

        if (valueType != other.valueType)
            return false;

        if (multiValue != other.multiValue)
            return false;

        if (hierarchical != other.hierarchical)
            return false;

        if (deprecated != other.deprecated)
            return false;

        if (aclAllowed != other.aclAllowed)
            return false;

        if (!description.equals(other.description))
            return false;

        if (!label.equals(other.label))
            return false;

        if (selectionList == null ? other.selectionList != null : !selectionList.equals(other.selectionList))
            return false;

        if (allowFreeEntry != other.allowFreeEntry)
            return false;

        if (loadSelectionListAsync != other.loadSelectionListAsync)
            return false;

        return name.equals(other.name);
    }

    public class IntimateAccess {
        private IntimateAccess() {
        }

        public long getLabelId() {
            return labelId;
        }

        public void setLabelId(long labelId) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            FieldTypeImpl.this.labelId = labelId;
        }

        public long getDescriptionId() {
            return descriptionId;
        }

        public void setDescriptionId(long descriptionId) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            FieldTypeImpl.this.descriptionId = descriptionId;
        }

        public AuthenticatedUser getCurrentModifier() {
            return currentModifier;
        }

        public LocaleMap getLabels() {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            return label;
        }

        public LocaleMap getDescriptions() {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            return description;
        }

        public void setLastModified(Date lastModified) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            FieldTypeImpl.this.lastModified = lastModified;
        }

        public void setLastModifier(long lastModifier) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            FieldTypeImpl.this.lastModifier = lastModifier;
        }

        public void setId(long id) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            FieldTypeImpl.this.id = id;
        }

        public void setUpdateCount(long updateCount) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            FieldTypeImpl.this.updateCount = updateCount;
        }

        /**
         * method to be called from storestrategy after state of this object
         * has been persisted
         */
        public void saved() {
        }

        public void setSelectionList(SelectionList selList) {
            selectionList = selList;
        }
    }

}
