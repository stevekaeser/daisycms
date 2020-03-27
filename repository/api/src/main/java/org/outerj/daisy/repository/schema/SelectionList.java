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

import org.outerx.daisy.x10.SelectionListDocument;

import java.util.Locale;
import java.util.List;

/**
 * A list of values from which one can be selected by the end-user.
 * 
 * <p>This SelectionList consists of ListItems.
 *
 * <p><b>Important for implementations:</b> the equals method should be implemented.
 * Two selection lists are considered equal if their defining data is equals (e.g.
 * in case of a query-based selection list, this means e.g. the queries are the same,
 * not the actual list-items generated from them).
 */
public interface SelectionList {
    /**
     * Gets the selection list items.
     *
     * <p>By preference, you should use the method {@link #getItems(long, long, java.util.Locale)}.
     * This method will fail if the selection list implementation requires a branch
     * and language.
     *
     */
    List<? extends ListItem> getItems();

    /**
     * Gets the selection list items.
     *
     * <p>Some selection list implementations might want to filter the items
     * based on the branch and language of the context in which this selection
     * list is used (= typically the document that is being edited). Therefore,
     * it is important that the branch and language or specified.
     *
     * @param branchId the branch of the document that is being edited
     * @param languageId the language of the document that is being edited
     */
    List<? extends ListItem> getItems(long branchId, long languageId, Locale locale);

    /**
     * @deprecated use {@link #getItemLabel} instead.
     */
    String getLabel(Object value, Locale locale);

    /**
     * Returns the label defined in the selection list for the given value,
     * or null if not available.
     */
    String getItemLabel(Object value, Locale locale);

    void addToFieldTypeXml(SelectionListDocument.SelectionList selectionListXml);
}
