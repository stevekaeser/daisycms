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

public class ImpExpHierarchicalQuerySelectionList {
    private String whereClause;
    private String[] linkFields;
    private boolean filterVariants;

    public ImpExpHierarchicalQuerySelectionList(String whereClause, String[] linkFields, boolean filterVariants) {
        this.whereClause = whereClause;
        this.linkFields = linkFields;
        this.filterVariants = filterVariants;
    }

    public String getWhereClause() {
        return whereClause;
    }

    public String[] getLinkFields() {
        return linkFields;
    }

    public void setLinkFields(String[] linkFields) {
        if (linkFields == null)
            throw new IllegalArgumentException("Null argument: linkFields");
        
        this.linkFields = linkFields;
    }

    public void setWhereClause(String whereClause) {
        if (whereClause == null || whereClause.trim().equals(""))
            throw new IllegalArgumentException("Null or empty argument: whereClause");
        this.whereClause = whereClause;
    }

    public boolean getFilterVariants() {
        return filterVariants;
    }

    public void setFilterVariants(boolean filterVariants) {
        this.filterVariants = filterVariants;
    }
}
