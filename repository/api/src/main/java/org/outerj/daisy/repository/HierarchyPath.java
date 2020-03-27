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
package org.outerj.daisy.repository;

import java.util.Arrays;

/**
 * A HierarchyPath is the value assigned to a hierarchical field.
 * In case of a multivalue hierarchical field, the value of the
 * field is an array of HierarchyPath objects.
 *
 * <p>A HierarchyPath object is immutable.
 */
public final class HierarchyPath {
    private final Object[] elements;

    /**
     *
     * @param elements the elements of the hierarchy path. The object types in the list
     *                 should correspond to the datatype of the field.
     */
    public HierarchyPath(Object[] elements) {
        this.elements = elements.clone();
    }

    public Object[] getElements() {
        return elements.clone();
    }

    public int length() {
        return elements.length;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof HierarchyPath))
            return false;

        HierarchyPath other = (HierarchyPath)obj;
        return Arrays.equals(elements, other.elements);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Object element : elements) {
            builder.append("/");
            builder.append(element.toString());
        }
        return builder.toString();
    }
}
