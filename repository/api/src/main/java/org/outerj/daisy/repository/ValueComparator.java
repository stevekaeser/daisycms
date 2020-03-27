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
package org.outerj.daisy.repository;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

/**
 * A comparator which can be used sort the values of Daisy fields.
 * Includes support for hierarchical values. String values are sorted
 * using a locale-specific collator.
 *
 * <p>This is just an utility class. 
 */
public final class ValueComparator<T> implements Comparator<T> {
    private final boolean ascending;
    private final Collator collator;

    public ValueComparator(boolean ascending, Locale locale) {
        this.ascending = ascending;
        this.collator = Collator.getInstance(locale);
    }

    public int compare(T value1, T value2) {
        int result;
        if (value1 instanceof String) {
            result = collator.compare(value1, value2);
        } else if (value1 instanceof Boolean) {
            // Before java 1.5 Boolean was not Comparable.  So a homebrewed solution was made where true < false            
            result = ((Boolean)value1).compareTo((Boolean)value2) * -1;            
        } else if (value1 instanceof HierarchyPath) {            
            //result = ((HierarchyPath)value1).compareTo((HierarchyPath)value2,this);
            result = 0;
            HierarchyPath path1 = (HierarchyPath)value1;
            HierarchyPath path2 = (HierarchyPath)value2;
            int i = 0;            
            while (i < path1.getElements().length  && i < path2.getElements().length && result == 0) {            
                result = compare((T)path1.getElements()[i], (T)path2.getElements()[i]);
                i++;
            } 
            
            if (result == 0 && path1.getElements().length != path2.getElements().length)
                result = path1.getElements().length < path2.getElements().length ? -1 : 1;
            else            
                result = ascending ? result : result * -1;
        } else if (value1 instanceof Comparable) {
            result = ((Comparable<T>)value1).compareTo(value2);
        } else {
            throw new RuntimeException("Non-comparable type of object: " + value1.getClass().getName());
        }
            
        return ascending ? result : result * -1;
    }
}