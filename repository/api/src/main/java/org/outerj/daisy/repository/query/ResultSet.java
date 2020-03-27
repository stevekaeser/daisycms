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
package org.outerj.daisy.repository.query;

import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;

/**
 * A result set for Daisy queries, loosely modelled after the JDBC ResultSet class,
 * however unlike the JDBC ResultSet class, <b>row and column index are 0-based</b>.
 */
public interface ResultSet {
    int getColumnCount();

    ValueType getValueType(int column);

    boolean isMultiValue(int column);

    boolean isHierarchical(int column);

    String getTitle(int column);

    String getStyleHint();

    void beforeFirst();

    boolean next();

    boolean first();

    boolean last();

    /**
     * Returns the current row index, or -1 if there is no current row.
     */
    int getRow();

    /**
     * Moves to the specified row. Row indexes or 0-based. It is possible
     * to move to row index -1, which will locate the row cursor before the first row.
     */
    boolean absolute(int index);

    /**
     * Returns the total number of rows in this ResultSet.
     */
    int getSize();

    /**
     * Returns the VariantKey of the current row (each row in a Daisy ResultSet
     * corresponds to a document variant).
     */
    VariantKey getVariantKey();

    /**
     * Returns the value of the specified column in the current row.
     * Column indexes are 0-based.
     */
    Object getValue(int column);
}
