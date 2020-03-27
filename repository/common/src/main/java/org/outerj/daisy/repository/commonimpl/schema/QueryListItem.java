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
package org.outerj.daisy.repository.commonimpl.schema;

import org.outerj.daisy.repository.schema.ListItem;

import java.util.Locale;
import java.util.List;
import java.util.Collections;

public class QueryListItem implements ListItem {
    private final Object value;
    private final String label;

    public QueryListItem(Object value, String label) {
        this.value = value;
        this.label = label;
    }

    public Object getValue() {
        return value;
    }

    public String getLabel(Locale locale) {
        return label;
    }

    public String getLabelExact(Locale locale) {
        return label;
    }

    public List<? extends ListItem> getItems() {
        return Collections.emptyList();
    }
}
