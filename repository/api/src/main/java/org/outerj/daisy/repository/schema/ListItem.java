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
package org.outerj.daisy.repository.schema;

import java.util.Locale;
import java.util.List;

/**
 * An item from a SelectionList
 */
public interface ListItem {
    /**
     * Returns the actual value of the ListItem.
     */
    Object getValue();

    /**
     * Returns a label which is shown to the end-user instead of the
     * actual value of the ListItem.
     * 
     * @param locale the locale to use for the presentation of the label
     * @return the desired label
     */
    String getLabel(Locale locale);

    /**
     * Get child items, in case this selection list is hierarchical.
     * Returns an empty list (not null) if there are no children.
     */
    List<? extends ListItem> getItems();
}
