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
package org.outerj.daisy.repository.schema;

import java.util.Locale;

/**
 * Methods shared by objects that contain child static list items.
 */
public interface StaticListItemParent {
    /**
     * Adds a child item.
     *
     * @deprecated This method does exactly the same as {@link #createItem(Object)},
     *             except that the created list item is not returned.
     */
    void addItem(Object value) throws Exception;

    /**
     * Removes all child items.
     */
    void clear();

    /**
     * Creates a StaticListItem instance, adds it to this selection
     * list and returns it.
     *
     * <p>Calling this on an list item is only possible if the selection list
     * belongs to a hierarchical field type.
     */
    StaticListItem createItem(Object value);

    StaticListItem getItem(Object value);

    String getItemLabel(Object value, Locale locale);
}
