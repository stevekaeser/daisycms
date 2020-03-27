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

import org.outerx.daisy.x10.StaticSelectionListDocument;


/**
 * A static selection list. Can be hierarchical if it belongs to
 * a field type which is hierarchical.
 *
 * <p>This selection list is created on a field type through
 * {@link FieldType#createStaticSelectionList}.
 */
public interface StaticSelectionList extends SelectionList, StaticListItemParent {
    /**
     * @deprecated Has been renamed to {@link #createItem}.
     */
    StaticListItem createStaticListItem(Object value);

    StaticSelectionListDocument getXml();

    void setAllFromXml(org.outerx.daisy.x10.StaticSelectionListDocument.StaticSelectionList list);
}
