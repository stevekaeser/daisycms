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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.query.FacetConf;
import org.outerj.daisy.repository.serverimpl.LocalRepositoryManager;
import org.outerj.daisy.repository.serverimpl.query.Counter;
import org.outerj.daisy.repository.serverimpl.query.FacetValueFormatter;
import org.outerj.daisy.repository.serverimpl.query.Formatter;
import org.outerj.daisy.repository.serverimpl.query.QueryFormatter;
import org.outerx.daisy.x10.FacetedQueryResultDocument;

public class FacetValues {    
    private SortedMap<Object, Counter> values;

    private FacetSampler facetSampler;
    
    private Locale locale;
    
    private Map<String, Formatter> formatters = new HashMap<String, Formatter>();

    public FacetValues(FacetSampler facetSampler, AuthenticatedUser user, LocalRepositoryManager.Context context) {
        this.facetSampler = facetSampler;
        this.values = new TreeMap<Object, Counter>(this.facetSampler.getComparator());
        this.locale = facetSampler.getLocale();
        formatters.put("value", new FacetValueFormatter(locale, context, user));
        formatters.put("query", new QueryFormatter(locale));
    }

    public void addValue(Object value) {
        Counter counter = values.get(value);
        if (counter != null) {
            counter.increment();
        } else {
            counter = new Counter();
            counter.increment();
            values.put(value, counter);
        }
    }

    public void addXml(FacetedQueryResultDocument.FacetedQueryResult.Facets facetsXml, FacetConf facetConf, ValueExpr valueExpr) {
        FacetedQueryResultDocument.FacetedQueryResult.Facets.Facet facetXml = facetsXml.addNewFacet();
        
        facetXml.setLabel(valueExpr.getTitle(locale));
        facetXml.setExpression(valueExpr.getExpression());
        facetXml.setMultiValue(valueExpr.isMultiValue());
        boolean isHierarchical = false;
        if (this.values != null && this.values.size() > 0)
            isHierarchical = this.values.firstKey() instanceof HierarchyPath;
        facetXml.setHierarchical(isHierarchical);
        
        facetSampler.valuesToXml(this, facetXml, valueExpr, formatters);
    }

    public SortedMap<Object, Counter> getValues() {
        return values;
    }
}