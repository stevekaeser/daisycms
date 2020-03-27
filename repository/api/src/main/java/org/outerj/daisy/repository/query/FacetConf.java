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
package org.outerj.daisy.repository.query;

import java.util.HashMap;
import java.util.Map;

public final class FacetConf {
    private FacetType type;
    private boolean isFacet;
    private int maxValues;
    private boolean sortOnValue;
    private boolean sortAscending;
    private Map<String, String> properties = new HashMap<String, String>();

    public FacetConf(boolean isFacet) {
        this();
        this.isFacet = isFacet;
    }
    
    public FacetConf(boolean isFacet, String type) {
        this(isFacet);
        this.type = FacetType.valueOf(type);
    }
    
    public FacetConf(int maxValues, boolean sortOnValue, boolean sortAscending) {
        this(true, maxValues, sortOnValue, sortAscending, FacetType.DEFAULT.name());
    }

    public FacetConf( boolean isFacet, int maxValues, boolean sortOnValue, boolean sortAscending, String type) {
        this.isFacet = isFacet;
        this.maxValues = maxValues;
        this.sortOnValue = sortOnValue;
        this.sortAscending = sortAscending;
        this.type = FacetType.valueOf(type);
    }

    public FacetConf() {
        this(true, 10, true, true, FacetType.DEFAULT.name());
    }

    public boolean isFacet() {
        return isFacet;
    }

    public void setIsFacet(boolean isFacet) {
        this.isFacet = isFacet;
    }

    /**
     * The maximum number of distinct values to be returned, -1 for all.
     */
    public int getMaxValues() {
        return maxValues;
    }

    public void setMaxValues(int maxValues) {
        this.maxValues = maxValues;
    }

    /**
     * If true, the distinct values for this facet should be sorted on value,
     * otherwise on count.
     */
    public boolean getSortOnValue() {
        return sortOnValue;
    }

    public void setSortOnValue(boolean sortOnValue) {
        this.sortOnValue = sortOnValue;
    }

    /**
     * If true, sorting happens ascending, otherwise descending.
     */
    public boolean getSortAscending() {
        return sortAscending;
    }

    public void setSortAscending(boolean sortAscending) {
        this.sortAscending = sortAscending;
    }

    public FacetType getType() {
        return type;
    }

    public void setType(FacetType type) {
        this.type = type;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }
    
    public enum FacetType {
        DEFAULT, 
        DATE,
        STRING,
        NUMBER
    }
}
