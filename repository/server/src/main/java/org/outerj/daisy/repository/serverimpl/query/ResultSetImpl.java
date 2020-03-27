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
package org.outerj.daisy.repository.serverimpl.query;

import org.outerj.daisy.repository.query.ResultSet;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;

import java.util.List;

public class ResultSetImpl implements ResultSet {
    private ValueType[] valueTypes;
    private boolean[] multiValue;
    private boolean[] hierarchical;
    private String[] titles;
    private List<Row> rows;
    private int pos = 0;
    private String styleHint;

    public ResultSetImpl(ValueType[] valueTypes, boolean[] multiValue, boolean[] hierarchical,
            String[] titles, String styleHint, List<Row> rows) {
        this.valueTypes = valueTypes;
        this.multiValue = multiValue;
        this.hierarchical = hierarchical;
        this.titles = titles;
        this.styleHint = styleHint;
        this.rows = rows;
    }

    public int getColumnCount() {
        return valueTypes.length;
    }

    public ValueType getValueType(int column) {
        return valueTypes[column];
    }

    public boolean isMultiValue(int column) {
        return multiValue[column];
    }

    public boolean isHierarchical(int column) {
        return hierarchical[column];
    }

    public String getTitle(int column) {
        return titles[column];
    }

    public String getStyleHint() {
        return styleHint;
    }

    public boolean next() {
        if (pos <= rows.size()) {
            pos++;
            return true;
        } else {
            return false;
        }
    }

    public boolean first() {
        if (rows.size() == 0) {
            return false;
        } else {
            pos = 0;
            return true;
        }
    }

    public boolean last() {
        if (rows.size() == 0) {
            return false;
        } else {
            pos = rows.size() - 1;
            return true;
        }
    }

    public void beforeFirst() {
        pos = -1;
    }

    public boolean absolute(int index) {
        if (index < -1 || index >= rows.size()) {
            return false;
        } else {
            pos = index;
            return true;
        }
    }

    public int getSize() {
        return rows.size();
    }

    public int getRow() {
        return pos;
    }

    public VariantKey getVariantKey() {
        return rows.get(pos).getVariantKey();
    }

    public Object getValue(int column) {
        return rows.get(pos).getValue(column);
    }

    static class Row {
        private VariantKey variantKey;
        private Object[] values;

        public Row(VariantKey variantKey, Object[] values) {
            this.variantKey = variantKey;
            this.values = values;
        }

        public VariantKey getVariantKey() {
            return variantKey;
        }

        public Object getValue(int column) {
            return values[column];
        }
    }
}
