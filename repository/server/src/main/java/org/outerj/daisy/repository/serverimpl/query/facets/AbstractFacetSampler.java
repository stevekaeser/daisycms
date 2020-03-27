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
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueComparator;
import org.outerj.daisy.repository.query.FacetConf;
import org.outerj.daisy.repository.serverimpl.query.Counter;
import org.outerj.daisy.repository.serverimpl.query.Formatter;
import org.outerx.daisy.x10.FacetedQueryResultDocument;
import org.outerx.daisy.x10.FacetedQueryResultDocument.FacetedQueryResult.Facets.Facet;

public abstract class AbstractFacetSampler implements FacetSampler {
    protected FacetConf facetConf;

    protected Comparator<Object> comparator;

    protected Locale locale;

    protected int valueCount = 0;
    
    protected Log logger;
    
    protected int threshold = Integer.MAX_VALUE;

    protected AbstractFacetSampler(FacetConf facetConf, Locale locale, Log logger) {
        this.facetConf = facetConf;
        this.locale = locale;
        this.comparator = new ValueComparator<Object>(facetConf.getSortAscending(), locale);
        this.logger = logger;
        Map<String,String> facetProperties = facetConf.getProperties();
        if (facetProperties != null && facetProperties.containsKey("threshold"))
            threshold = Integer.parseInt(facetProperties.get("threshold"));
    }

    public static FacetSampler createFacetSampler(FacetConf facetConf, Locale locale, Log logger) throws RepositoryException {
        FacetSampler facetSampler;
        switch (facetConf.getType()) {
        case DATE : 
            facetSampler = new DateFacetSampler(facetConf, locale, logger);
            break;
        case NUMBER : 
            facetSampler = new NumberFacetSampler(facetConf, locale, logger);
            break;
        case STRING : 
            facetSampler = new StringFacetSampler(facetConf, locale, logger);
            break;
        default :
            facetSampler = new DummyFacetSampler(facetConf, locale, logger);
        }
        
        return facetSampler;
    }

    protected SortedMap<Object, CountGroup> makeGroupCounts(FacetValues facetValues, ValueExpr valueExpr, Map<String, Formatter> formatters) {
        SortedMap<Object, CountGroup> countGroups = new TreeMap<Object, CountGroup>(comparator);
        CountGroup countsObj;

        for (Map.Entry<Object, Counter> entry : facetValues.getValues().entrySet()) {
            Object value = entry.getKey();
            long counter = entry.getValue().getValue();
            countsObj = countGroups.get(value);

            if (countsObj == null) {
                countGroups.put(value, new CountGroup(this, value, counter, valueExpr, formatters));
            } else {
                countsObj.addValue(value, counter);
            }
        }

        if (facetValues.getValues().size() > threshold
                || (facetValues.getValues().size() > 1 && facetValues.getValues().firstKey() instanceof HierarchyPath))
            regroup(countGroups);

        return countGroups;
    }

    public void valuesToXml(FacetValues facetValues, Facet facetXml, ValueExpr valueExpr, Map<String, Formatter> formatters) {
        Map<Object, CountGroup> countGroups = makeGroupCounts(facetValues, valueExpr, formatters);
        valueCount = 0;
        Queue<CountGroup> sortedGroups = reorderGroups(countGroups);
        facetXml.setAvailableValues(sortedGroups.size());
        
        CountGroup cg;
        while ((cg = sortedGroups.poll()) != null && (facetConf.getMaxValues() < 0 || valueCount < facetConf.getMaxValues())) {
            valueCount++;
            FacetedQueryResultDocument.FacetedQueryResult.Facets.Facet.Value valueXml = facetXml.addNewValue();
            
            if (cg.getMin().equals(cg.getMax()) || cg.getMin() instanceof HierarchyPath)
                valueXml.setIsDiscrete(false);
            else
                valueXml.setIsDiscrete(true);

            valueXml.setUserFormat(cg.getUserValue());
            valueXml.setQueryFormat(cg.getQueryValue());
            valueXml.setCount(cg.getCount());
        }
    }

    protected abstract String createUserFormat(CountGroup group, ValueExpr valueExpr, Formatter valueFormatter);

    protected abstract String createQueryFormat(CountGroup g, ValueExpr valueExpr, Formatter queryFormatter);

    protected abstract void regroup(SortedMap<Object, CountGroup> countGroups);

    public Comparator<Object> getComparator() {
        return comparator;
    }

    public int getValueCount() {
        return valueCount;
    }
    
    public Locale getLocale() {
        return this.locale;
    }
    
    private Queue<CountGroup> reorderGroups (Map<Object, CountGroup> countGroups) {
        Queue<CountGroup> sortedGroups;
        if (countGroups.size() > 0) {
            if (facetConf.getSortOnValue()) {
                if (facetConf.getProperties().containsKey("sortValueOn") && facetConf.getProperties().get("sortValueOn").equals("label")) {
                    sortedGroups = new PriorityQueue<CountGroup>(countGroups.size(), new GroupUserValueComparator(facetConf.getSortAscending(), locale));
                    sortedGroups.addAll(countGroups.values());
                } else {
                    sortedGroups = new LinkedList<CountGroup>(countGroups.values());
                }
            } else {
                sortedGroups = new PriorityQueue<CountGroup>(countGroups.size(), new GroupCountComparator(facetConf.getSortAscending()));
                sortedGroups.addAll(countGroups.values());
            }
        } else {
            sortedGroups = new LinkedList<CountGroup>();
        }
        
        return sortedGroups;
    }
    
    private class GroupCountComparator implements Comparator<CountGroup> {
        private boolean sortAscending;
        
        public GroupCountComparator(boolean sortAscending) {
            this.sortAscending = sortAscending;
        }

        public int compare(CountGroup o1, CountGroup o2) {
            int result = (int) (o1.getCount() - o2.getCount());
            return sortAscending ? result : result  * -1;
        }
    }
    
    private class GroupUserValueComparator implements Comparator<CountGroup> {
        private Comparator<Object> delegate;
        public GroupUserValueComparator(boolean sortAscending, Locale locale) {
            this.delegate = new ValueComparator<Object>(sortAscending, locale);
        }

        public int compare(CountGroup o1, CountGroup o2) {            
            return delegate.compare(o1.getUserValue(), o2.getUserValue());
        }
        
    }

}