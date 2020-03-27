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
package org.outerj.daisy.tools.importexport.model.schema;

import org.outerj.daisy.repository.ValueType;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class ImpExpStaticSelectionList {
    private ValueType valueType;
    private List<ImpExpStaticListItem> items = new ArrayList<ImpExpStaticListItem>();

    public ImpExpStaticSelectionList(ValueType valueType) {
        if (valueType == null)
            throw new IllegalArgumentException("Null argument: " + valueType);
        this.valueType = valueType;
    }

    public void addItem(ImpExpStaticListItem item) {
        if (item.getValueType() != valueType)
            throw new IllegalArgumentException("The supplied list item is of a different value type, expected: " + valueType + ", got " + item.getValueType());

        items.add(item);
    }

    public void clear() {
        items.clear();
    }

    public List<ImpExpStaticListItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public ValueType getValueType() {
        return valueType;
    }
}
