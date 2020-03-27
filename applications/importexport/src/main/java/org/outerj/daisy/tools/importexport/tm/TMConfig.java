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
package org.outerj.daisy.tools.importexport.tm;

import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

/**
 * Translation management specific configuration.
 */
public class TMConfig {
    private Set<String> globalFields = new HashSet<String>();
    private Set<String> globalParts = new HashSet<String>();

    private Map<String, Set<String>> fieldsByDocType = new HashMap<String, Set<String>>();
    private Map<String, Set<String>> partsByDocType = new HashMap<String, Set<String>>();

    private boolean normalized = false;

    public void addLangIndepField(String documentType, String fieldType) {
        if (documentType != null) {
            Set<String> fields = fieldsByDocType.get(documentType);
            if (fields == null) {
                fields = new HashSet<String>();
                fieldsByDocType.put(documentType, fields);
            }
            fields.add(fieldType);
        } else {
            addLangIndepField(fieldType);
        }
        normalized = false;
    }

    public void addLangIndepPart(String documentType, String partType) {
        if (documentType != null) {
            Set<String> parts = partsByDocType.get(documentType);
            if (parts == null) {
                parts = new HashSet<String>();
                partsByDocType.put(documentType, parts);
            }
            parts.add(partType);
        } else {
            addLangIndepPart(partType);
        }
        normalized = false;
    }

    public void addLangIndepField(String fieldType) {
        globalFields.add(fieldType);
        normalized = false;
    }

    public void addLangIndepPart(String partType) {
        globalParts.add(partType);
        normalized = false;
    }

    private void normalize() {
        if (!normalized) {
            for (Set<String> set : fieldsByDocType.values()) {
                set.addAll(globalFields);
            }

            for (Set<String> set : partsByDocType.values()) {
                set.addAll(globalParts);
            }
            normalized  = true;
        }
    }

    public Set<String> getLanguageIndependentFields(String documentType) {
        normalize();
        Set<String> set = fieldsByDocType.get(documentType);
        return set != null ? set : globalFields;
    }

    public Set<String> getLanguageIndependentParts(String documentType) {
        normalize();
        Set<String> set = partsByDocType.get(documentType);
        return set != null ? set : globalParts;
    }

    public boolean isLanguageIndependentField(String documentType, String fieldType) {
        normalize();
        Set<String> set = fieldsByDocType.get(documentType);
        if (set != null) {
            return set.contains(fieldType);
        } else {
            return globalFields.contains(fieldType);
        }
    }

    public boolean isLanguageIndependentPart(String documentType, String partType) {
        normalize();
        Set<String> set = partsByDocType.get(documentType);
        if (set != null) {
            return set.contains(partType);
        } else {
            return globalParts.contains(partType);
        }
    }
}
