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
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.logging.Log;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.repository.ValueComparator;
import org.outerj.daisy.repository.query.FacetConf;
import org.outerj.daisy.repository.serverimpl.query.Formatter;

class StringFacetSampler extends AbstractFacetSampler {
    public StringFacetSampler(FacetConf facetConf, Locale locale, Log logger) {
        super(facetConf, locale, logger);
        comparator = new CaseInsensitiveStringComparator(facetConf.getSortAscending(), locale);
        Map<String,String> facetProperties = facetConf.getProperties();
        if (facetProperties.containsKey("threshold"))
            threshold = Integer.parseInt(facetProperties.get("threshold"));
        else {
            logger.warn("Could not find facet property with name 'threshold'. Falling back to default value MAX_VALUE");
            threshold = Integer.MAX_VALUE;
        }
    }

    public String createUserFormat(CountGroup group, ValueExpr valueExpr, Formatter valueFormatter) {
        String format;
        if (group.getMin().equals(group.getMax())) {
            format = valueFormatter.format(valueExpr, group.getMin());
        } else {
            String s1 = valueFormatter.format(valueExpr, group.getMin());
            String s2 = valueFormatter.format(valueExpr, group.getMax());
            int end = s1.length() > s2.length() ? s2.length() : s1.length();
            int endPos = 0;
            String si1 = s1.toLowerCase(locale);
            String si2 = s2.toLowerCase(locale);
            while (si1.startsWith(si2.substring(0, endPos)) && endPos < end)
                endPos++;

            format = s1.substring(0, endPos) + " - " + s2.substring(0, endPos);
        }
        return format;
    }

    public void regroup(SortedMap<Object, CountGroup> groups) {
        int minsize = threshold / 2 + 1;
        int maxsize = threshold;
        while (groups.size() < minsize || groups.size() > maxsize) {
            if (groups.size() < minsize) {
                // we shouldn't be able to go in here
            } else if (groups.size() > maxsize) {
                int mergeCount = groups.size() / minsize;
                CountGroup g = null;
                if (mergeCount > 1) {
                    Map.Entry<Object, CountGroup>[] entries = (Map.Entry<Object, CountGroup>[])groups.entrySet().toArray(new Map.Entry[groups.size()]);
                    for (int i = 0; i < entries.length; i++) {
                        if (i % mergeCount == 0) {
                            g = entries[i].getValue();
                        } else {
                            g.merge(entries[i].getValue());
                            groups.remove(entries[i].getKey());
                        }
                    }
                } else {
                    Object secondToLastKey = groups.headMap(groups.lastKey()).lastKey();
                    g = groups.get(secondToLastKey);
                    g.merge(groups.get(groups.lastKey()));
                    groups.remove(groups.lastKey());
                }
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

    private class CaseInsensitiveStringComparator<T extends Object> implements Comparator<String> {
        private final ValueComparator<String> delegate;

        private final Locale locale;

        public CaseInsensitiveStringComparator(boolean ascending, Locale locale) {
            delegate = new ValueComparator<String>(ascending, locale);
            this.locale = locale;
        }

        public int compare(String o1, String o2) {
            return delegate.compare(o1.toUpperCase(locale), o2.toUpperCase(locale));
        }
    }
}