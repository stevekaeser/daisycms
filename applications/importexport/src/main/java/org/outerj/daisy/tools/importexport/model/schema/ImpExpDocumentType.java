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

import java.util.*;

public class ImpExpDocumentType implements ImpExpLabelEnabled, ImpExpDescriptionEnabled, Comparable {
    private String name;
    private boolean deprecated;
    private List<ImpExpFieldTypeUse> fieldTypeUses = new ArrayList<ImpExpFieldTypeUse>();
    private List<ImpExpPartTypeUse> partTypeUses = new ArrayList<ImpExpPartTypeUse>();
    private Map<Locale, String> labels = new HashMap<Locale, String>();
    private Map<Locale, String> descriptions = new HashMap<Locale, String>();

    protected ImpExpDocumentType(String name) {
        if (name == null || name.trim().length() == 0)
            throw new IllegalArgumentException("Null or empty argument: name");
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addFieldTypeUse(ImpExpFieldTypeUse fieldTypeUse) {
        for (ImpExpFieldTypeUse existingFieldTypeUse : fieldTypeUses) {
            if (existingFieldTypeUse.getFieldTypeName().equals(fieldTypeUse.getFieldTypeName()))
                throw new RuntimeException("Document type " + name + " already contains the field " + fieldTypeUse.getFieldTypeName());
        }
        fieldTypeUses.add(fieldTypeUse);
    }

    public ImpExpFieldTypeUse[] getFieldTypeUses() {
        return fieldTypeUses.toArray(new ImpExpFieldTypeUse[0]);
    }

    public void removeFieldTypeUse(String fieldTypeName) {
        for (int i = 0; i < fieldTypeUses.size(); i++) {
            ImpExpFieldTypeUse use = fieldTypeUses.get(i);
            if (use.getFieldTypeName().equals(fieldTypeName)) {
                fieldTypeUses.remove(i);
                break;
            }
        }
    }

    public void clearFieldTypeUses() {
        fieldTypeUses.clear();
    }

    public void addPartTypeUse(ImpExpPartTypeUse partTypeUse) {
        for (ImpExpPartTypeUse existingPartTypeUse : partTypeUses) {
            if (existingPartTypeUse.getPartTypeName().equals(partTypeUse.getPartTypeName()))
                throw new RuntimeException("Document type " + name + " already contains the part " + partTypeUse.getPartTypeName());
        }
        partTypeUses.add(partTypeUse);
    }

    public ImpExpPartTypeUse[] getPartTypeUses() {
        return partTypeUses.toArray(new ImpExpPartTypeUse[0]);
    }

    public void removePartTypeUse(String partTypeName) {
        for (int i = 0; i < partTypeUses.size(); i++) {
            ImpExpPartTypeUse use = partTypeUses.get(i);
            if (use.getPartTypeName().equals(partTypeName)) {
                partTypeUses.remove(i);
                break;
            }
        }
    }

    public void clearPartTypeUses() {
        partTypeUses.clear();
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

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public int compareTo(Object o) {
        return name.compareTo(((ImpExpDocumentType)o).name);
    }
}
