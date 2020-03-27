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

import org.outerx.daisy.x10.ListItemDocument;

/**
 * A ListItem meant to be used in a StaticSelectionList
 */
public interface StaticListItem extends ListItem, StaticListItemParent, LabelEnabled {
    ListItemDocument getXml();

    /**
     * Inits this static list item from the given XML
     * (labels and children, the value of a static list
     * item is immutable after creation).
     */
    void setAllFromXml(ListItemDocument.ListItem listItemXml);
}
