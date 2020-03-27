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

import org.outerj.daisy.repository.ValueType;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

public class ImpExpFieldType implements ImpExpLabelEnabled, ImpExpDescriptionEnabled, Comparable {
    private String name;
    private ValueType valueType;
    private Map<Locale, String> labels = new HashMap<Locale, String>();
    private Map<Locale, String> descriptions = new HashMap<Locale, String>();
    private boolean deprecated = false;
    private boolean aclAllowed = false;
    private boolean loadSelectionListAsync = false;
    private boolean multiValue = false;
    private boolean hierarchical = false;
    private int size = 0;
    private boolean allowFreeEntry = false;
    private Object selectionList;

    protected ImpExpFieldType(String name, ValueType valueType) {
        if (name == null || name.trim().length() == 0)
            throw new IllegalArgumentException("Null or empty argument: name");
        if (valueType == null)
            throw new IllegalArgumentException("Null argument: valueType");
        this.name = name;
        this.valueType = valueType;
    }

    public String getName() {
        return name;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public boolean isAclAllowed() {
        return aclAllowed;
    }

    public void setAclAllowed(boolean aclAllowed) {
        this.aclAllowed = aclAllowed;
    }

    public boolean isMultiValue() {
        return multiValue;
    }

    public void setMultiValue(boolean multiValue) {
        this.multiValue = multiValue;
    }

    public boolean isHierarchical() {
        return hierarchical;
    }

    public void setHierarchical(boolean hierarchical) {
        this.hierarchical = hierarchical;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean getAllowFreeEntry() {
        return allowFreeEntry;
    }

    public void setAllowFreeEntry(boolean allowFreeEntry) {
        this.allowFreeEntry = allowFreeEntry;
    }

    public boolean getLoadSelectionListAsync() {
        return loadSelectionListAsync;
    }

    public void setLoadSelectionListAsync(boolean loadAsync) {
        this.loadSelectionListAsync = loadAsync;
    }

    public Object getSelectionList() {
        return selectionList;
    }

    public void setSelectionList(Object selectionList) {
        if (selectionList != null && selectionList instanceof ImpExpStaticSelectionList) {
            ValueType selListValueType = ((ImpExpStaticSelectionList)selectionList).getValueType();
            if (selListValueType != valueType) {
                throw new IllegalArgumentException("Value type of static selection list does not match: got " + selListValueType + ", expected " + valueType);
            }
        }
        this.selectionList = selectionList;
    }

    public void addLabel(Locale locale, String label) {
        labels.put(locale, label);
    }

    public void clearLabels() {
        labels.clear();
    }

    public Map<Locale, String> getLabels() {
        return new HashMap<Locale, String>(labels);
    }

    public void addDescription(Locale locale, String description) {
        descriptions.put(locale, description);
    }

    public void clearDescriptions() {
        descriptions.clear();
    }

    public Map<Locale, String> getDescriptions() {
        return new HashMap<Locale, String>(descriptions);
    }

    public int compareTo(Object o) {
        return name.compareTo(((ImpExpFieldType)o).name);
    }
}
