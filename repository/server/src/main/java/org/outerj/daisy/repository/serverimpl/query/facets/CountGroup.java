/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.repository.serverimpl.query.facets;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;

import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.repository.serverimpl.query.Formatter;

public class CountGroup {
    private Object min;

    private Object max;

    private long count;
    
    private Comparator<Object> comparator;
    
    private AbstractFacetSampler facetSampler;
    
    private ValueExpr valueExpr;
    
    private Map<String, Formatter> formatters;
    
    private String userValue;
    
    private String queryValue;

    public CountGroup(AbstractFacetSampler facetSampler, Object o, long count, ValueExpr valueExpr,  Map<String, Formatter> formatters) {
        this.count = count;
        min = o;
        max = o;
        comparator = facetSampler.getComparator();
        this.facetSampler = facetSampler;
        this.valueExpr = valueExpr;
        this.formatters = formatters;
    }

    public void addValue(Object s, long count) {
        min = comparator.compare(s, min) < 0 ? s : min;
        max = comparator.compare(s, max) > 0 ? s : max;
        queryValue = null;
        userValue = null;
        this.count += count;
    }

    public void merge(CountGroup g1) {
        min = comparator.compare(min, g1.min) < 0 ? min : g1.min;
        max = comparator.compare(max, g1.max) > 0 ? max : g1.max;
        queryValue = null;
        userValue = null;
        count += g1.count;
    }
    
    public static void merge(SortedMap<Object, CountGroup> map) {
        if (map.size() > 1) {
            CountGroup destination = map.get(map.lastKey());
            SortedMap<Object, CountGroup> toBeMerged = map.headMap(map.lastKey());
            for (Object key : toBeMerged.keySet()) 
                destination.merge(toBeMerged.get(key));            
            
            toBeMerged.clear();
        }
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public Object getMax() {
        return max;
    }

    public void setMax(Object max) {
        this.max = max;
    }

    public Object getMin() {
        return min;
    }

    public void setMin(Object min) {
        this.min = min;
    }
    
    public String getQueryValue() {
        if (queryValue == null) {
            queryValue = facetSampler.createQueryFormat(this, valueExpr, formatters.get("query"));
        }
        return queryValue;
    }

    public String getUserValue() {
        if (userValue == null) {
            userValue = facetSampler.createUserFormat(this, valueExpr, formatters.get("value"));
        }
        return userValue;
    }
}