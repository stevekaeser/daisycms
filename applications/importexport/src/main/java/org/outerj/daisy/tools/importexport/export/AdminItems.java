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
package org.outerj.daisy.tools.importexport.export;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;

class AdminItems {
    private Map<String, Boolean> documentTypes = new HashMap<String, Boolean>();
    private Map<String, Boolean> fieldTypes = new HashMap<String, Boolean>();
    private Map<String, Boolean> partTypes = new HashMap<String, Boolean>();
    private Map<String, Boolean> collections = new HashMap<String, Boolean>();

    private void updateItem(Map<String, Boolean> map, String name, boolean required) {
        Boolean current = map.get(name);
        if (current != null && current && !required)
            return;
        map.put(name, required);
    }

    public void addDocumentType(String name, boolean required) {
        updateItem(documentTypes, name, required);
    }

    public void addPartType(String name, boolean required) {
        updateItem(partTypes, name, required);
    }

    public void addFieldType(String name, boolean required) {
        updateItem(fieldTypes, name, required);
    }

    public void addCollection(String name, boolean required) {
        updateItem(collections, name, required);
    }

    public Set<String> getDocumentTypes() {
        return documentTypes.keySet();
    }

    public Set<String> getFieldTypes() {
        return fieldTypes.keySet();
    }

    public Set<String> getPartTypes() {
        return partTypes.keySet();
    }

    public Set<String> getCollections() {
        return collections.keySet();
    }

    public boolean isDocumentTypeRequired(String name) {
        return documentTypes.get(name);
    }

    public boolean isPartTypeRequired(String name) {
        return partTypes.get(name);
    }

    public boolean isFieldTypeRequired(String name) {
        return fieldTypes.get(name);
    }

    public boolean isCollectionRequired(String name) {
        return collections.get(name);
    }
}
