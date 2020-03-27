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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;

import org.apache.commons.logging.Log;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.query.FacetConf;
import org.outerj.daisy.repository.serverimpl.query.Formatter;

class DummyFacetSampler extends AbstractFacetSampler {
    public DummyFacetSampler(FacetConf facetConf, Locale locale, Log logger) {
        super(facetConf, locale, logger);
    }

    // This is O(nm) where n is the amount of entries and (m) is the depth of
    // the hierarchy
    public void regroup(SortedMap<Object, CountGroup> countGroups) {
        if (countGroups.firstKey() instanceof HierarchyPath) {
            int minDepth = Integer.MAX_VALUE;
            int maxDepth = 1;
            for (Object key : countGroups.keySet()) {
                minDepth = Math.min(minDepth, ((HierarchyPath)key).length());
                maxDepth = Math.max(maxDepth, ((HierarchyPath)key).length());
            }

            hierarchyRegroup(countGroups, maxDepth, minDepth);
        }
    }

    private void hierarchyRegroup(SortedMap<Object, CountGroup> groups, int maxDepth, int minDepth) {
        int diffDepth = -1;
        if (maxDepth > minDepth) {
            diffDepth = minDepth - 1;
        } else if (maxDepth == minDepth) {
            boolean foundDiff = false;
            diffDepth = 0;
            Object[] elements1 = ((HierarchyPath)groups.firstKey()).getElements();
            Object[] elements2 = ((HierarchyPath)groups.lastKey()).getElements();
            while (diffDepth < maxDepth && !foundDiff && diffDepth < elements1.length && diffDepth < elements2.length) {                
                foundDiff = !(elements1[diffDepth].equals(elements2[diffDepth]));
                diffDepth++;
            }
            diffDepth--;
        }

        if (diffDepth >= 0 && groups.size() > 1) {
            if (!((HierarchyPath)groups.firstKey()).getElements()[diffDepth].equals(((HierarchyPath)groups.lastKey()).getElements()[diffDepth])) {

                // create a list of subMaps. Each subMap will then be merged into one element.
                List<SortedMap<Object, CountGroup>> subMaps = new ArrayList<SortedMap<Object, CountGroup>>();
                HierarchyPath previousPath = (HierarchyPath)groups.firstKey();
                for (Object key : groups.keySet()) {
                    HierarchyPath path = (HierarchyPath)key;
                    if (!path.getElements()[diffDepth].equals(previousPath.getElements()[diffDepth])) {
                        if (subMaps.size() < 1)
                            subMaps.add(groups.headMap(key));
                        else
                            subMaps.add(groups.subMap(previousPath, key));
                        previousPath = path;
                    }
                }
                // add the last subMap
                subMaps.add(groups.tailMap(previousPath));

                for (SortedMap<Object, CountGroup> subMap : subMaps) {
                    CountGroup.merge(subMap);
                }
            } else {
                // find the shortest(there should be only one) & 2nd shortest path
                HierarchyPath shorty = null;
                HierarchyPath shortysNeighbour = null;
                int newMin = maxDepth;
                for (Object key : groups.keySet()) {
                    HierarchyPath path = (HierarchyPath) key;
                    if (shorty != null && shortysNeighbour == null)
                        shortysNeighbour = path;
                    
                    if (path.length() == minDepth)
                        shorty = path;
                    else if (path.length() > minDepth)
                        newMin = Math.min(path.length(), newMin);
                }
                
                if (shorty != null) {
                    if (shortysNeighbour != null)
                        hierarchyRegroup(groups.tailMap(shortysNeighbour), maxDepth, newMin);
                    else
                        hierarchyRegroup(groups.headMap(shorty), maxDepth, newMin);
                }
            }
        }
    }

    public String createUserFormat(CountGroup g, ValueExpr valueExpr, Formatter valueFormatter) {
        String format;
        if (!g.getMin().equals(g.getMax()) && g.getMin() instanceof HierarchyPath)
            format = createUserPath((HierarchyPath)g.getMin(), (HierarchyPath)g.getMax(), valueExpr, valueFormatter);
        else
            format = valueFormatter.format(valueExpr, g.getMin());

        return format;
    }

    @Override
    protected String createQueryFormat(CountGroup g, ValueExpr valueExpr, Formatter queryFormatter) {
        String format;
        if (!g.getMin().equals(g.getMax()) && g.getMin() instanceof HierarchyPath) {
            HierarchyPath shorty = findShortestPath(g);
            format = queryFormatter.format(valueExpr, shorty);
            format += " OR " + valueExpr.getExpression() + " matchesPath('"
                    + createPath((HierarchyPath)g.getMin(), (HierarchyPath)g.getMax(), valueExpr, queryFormatter) + "')";
        } else {
            format = queryFormatter.format(valueExpr, g.getMin());
        }
        return format;
    }

    private String createPath(HierarchyPath path1, HierarchyPath path2, ValueExpr valueExpr, Formatter formatter) {
        StringBuilder path = new StringBuilder();
        Object[] elements1 = path1.getElements();
        Object[] elements2 = path2.getElements();
        int end = Math.min(elements1.length, elements2.length);
        int i = 0;
        while (i < end && elements1[i].equals(elements2[i])) {
            path.append("/");
            String formatted = formatter.format(valueExpr, elements1[i]);
            path.append(formatted.replaceAll("^'|'$", ""));
            i++;
        }
        path.append("/**");

        return path.toString();
    }

    private String createUserPath(HierarchyPath path1, HierarchyPath path2, ValueExpr valueExpr, Formatter formatter) {
        StringBuilder path = new StringBuilder();
        Object[] elements1 = path1.getElements();
        Object[] elements2 = path2.getElements();
        int end = Math.min(elements1.length, elements2.length);
        int i = 0;
        while (i < end && elements1[i].equals(elements2[i])) {
            path.append("/");
            String formatted = formatter.format(valueExpr, elements1[i]);
            if (formatted != null)
                path.append(formatted.replaceAll("'", ""));
            i++;
        }
        path.append("/...");

        return path.toString();
    }
    
    private HierarchyPath findShortestPath(CountGroup g) {
        HierarchyPath path1 = (HierarchyPath)g.getMin();
        HierarchyPath path2 = (HierarchyPath)g.getMax();
        return path1.length() <= path2.length() ? path1 : path2;
    }
}