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

import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.logging.Log;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.repository.query.FacetConf;
import org.outerj.daisy.repository.serverimpl.query.Formatter;

class NumberFacetSampler extends AbstractFacetSampler {
    private double spreadFactor;
    
    public NumberFacetSampler(FacetConf facetConf, Locale locale, Log logger) {
        super(facetConf, locale, logger);
        Map<String,String> facetProperties = facetConf.getProperties();
        if (facetProperties.containsKey("threshold"))
            threshold = Integer.parseInt(facetProperties.get("threshold"));
        else {
            logger.warn("Could not find facet property with name 'threshold'. Falling back to default value MAX_VALUE");
            threshold = Integer.MAX_VALUE;
        }
        if (facetProperties.containsKey("spreadFactor"))
            spreadFactor = Double.parseDouble(facetProperties.get("spreadFactor"));
        else {
            logger.warn("Could not find facet property with name 'spreadFactor'. Falling back to default value '1'");
            spreadFactor = 1;
        }
    }

    public String createUserFormat(CountGroup group, ValueExpr valueExpr, Formatter valueFormatter) {
        String s1 = valueFormatter.format(valueExpr, group.getMin());
        String s2 = valueFormatter.format(valueExpr, group.getMax());
        return s1 + " - " + s2;
    }

    public void regroup(SortedMap<Object, CountGroup> groups) {
        Map.Entry<Object, CountGroup>[] entries = (Map.Entry<Object, CountGroup>[])groups.entrySet().toArray(new Map.Entry[groups.size()]);

        Number min = (Number)(entries[0].getValue()).getMin();
        Number max = (Number)(entries[entries.length - 1].getValue()).getMin();
        double diff = Math.abs(max.doubleValue() - min.doubleValue());
        int ascFactor = facetConf.getSortAscending() ? 1 : -1;
        
        double z = Math.pow(diff, 1.0 / spreadFactor) / threshold;
        int groupCount = 1;
        Double absoluteThreshold = new Double( min.doubleValue() + Math.pow(groupCount * z, spreadFactor) * ascFactor ); 

        CountGroup currentGroup = entries[0].getValue();
        for (int i = 1; i < entries.length; i++) {
            CountGroup group = entries[i].getValue();
            boolean changedGroup = false;
            while (comparator.compare(absoluteThreshold, ((Number)group.getMin()).doubleValue()) < 0 || comparator.compare(absoluteThreshold, ((Number)group.getMax()).doubleValue()) < 0) {
                groupCount++;
                absoluteThreshold = new Double( min.doubleValue() + Math.pow(groupCount * z, spreadFactor) * ascFactor );
                changedGroup = true;
            }
            if (changedGroup) {
                currentGroup = group;
            } else {
                currentGroup.merge(group);
                groups.remove(entries[i].getKey());
            }
        }
    }
    
    @Override
    protected String createQueryFormat(CountGroup g, ValueExpr valueExpr, Formatter formatter) {
        if (g.getMin().equals(g.getMax())) {
            return formatter.format(valueExpr, g.getMin());
        } else {
            Object realmin = g.getMin(), realmax = g.getMax();
            if (!facetConf.getSortAscending()) {
                realmin = g.getMax();
                realmax = g.getMin();
            }
            return formatter.format(valueExpr, realmin) + " AND " + formatter.format(valueExpr, realmax);
        }    
    }
}