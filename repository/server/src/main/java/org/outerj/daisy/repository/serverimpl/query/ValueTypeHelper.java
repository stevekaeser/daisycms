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
package org.outerj.daisy.repository.serverimpl.query;

import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.query.model.QValueType;

public class ValueTypeHelper {
    public static ValueType queryToFieldValueType(QValueType qValueType) {
        ValueType valueType;
        switch (qValueType) {
            case STRING:
                valueType = ValueType.STRING;
                break;
            case LONG:
                valueType = ValueType.LONG;
                break;
            case DOUBLE:
                valueType = ValueType.DOUBLE;
                break;
            case DECIMAL:
                valueType = ValueType.DECIMAL;
                break;
            case DATE:
                valueType = ValueType.DATE;
                break;
            case DATETIME:
                valueType = ValueType.DATETIME;
                break;
            case LINK:
                valueType = ValueType.LINK;
                break;
            case BOOLEAN:
                valueType = ValueType.BOOLEAN;
                break;
            case VERSION_STATE:
            case XML:
            case UNDEFINED:
            case DOCID:
                // These value types are unknown in ValueType enum, fall back to string
                valueType = ValueType.STRING;
                break;
            default:
                throw new RuntimeException("Encountered unexpected value type: " + qValueType);
        }
        return valueType;
    }

    public static Object queryValueToFieldValueType(Object value, QValueType qValueType) {
        switch (qValueType) {
            case VERSION_STATE:
            case XML:
            case UNDEFINED:
            case DOCID:
                value = value != null ? value.toString() : null;
                break;
        }
        return value;
    }
}
