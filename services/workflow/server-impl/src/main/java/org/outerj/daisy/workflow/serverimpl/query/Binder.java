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
package org.outerj.daisy.workflow.serverimpl.query;

import org.outerj.daisy.workflow.WfValueType;
import org.outerj.daisy.workflow.WfVersionKey;
import org.outerj.daisy.workflow.WfUserKey;
import org.outerj.daisy.workflow.serverimpl.WfActorKeyToStringConverter;
import org.outerj.daisy.workflow.serverimpl.WfVersionKeyToStringConverter;
import org.hibernate.Query;

import java.util.*;

/**
 * A class collecting all values to be bound to a hibernate query.
 */
public class Binder {
    private Map<String, BindEntry> entries = new HashMap<String, BindEntry>();
    private int counter = 0;

    /**
     *
     * @param bindName obtained through {@link #getUniqueBindName()}.
     */
    public void addBind(String bindName, WfValueType type, Object value) {
        if (entries.containsKey(bindName))
            throw new RuntimeException("Tried to set bind for " + bindName + " a second time.");
        entries.put(bindName, new BindEntry(type, value, false));
    }

    public void addBindList(String bindName, Collection collection) {
        if (entries.containsKey(bindName))
            throw new RuntimeException("Tried to set bind for " + bindName + " a second time.");
        entries.put(bindName, new BindEntry(null, collection, true));
    }

    public void bind(Query query) {
        for (Map.Entry<String, BindEntry> mapEntry : entries.entrySet()) {
            BindEntry entry = mapEntry.getValue();
            bindValue(query, mapEntry.getKey(), entry.type, entry.value, entry.isList);
        }
    }

    public String getUniqueBindName() {
        return "x" + counter++;
    }

    private void bindValue(Query query, String name, WfValueType valueType, Object value, boolean isList) {
        if (isList) {
            query.setParameterList(name, (Collection)value);
        } else {
            switch (valueType) {
                case STRING:
                    query.setString(name, (String)value);
                    break;
                case LONG:
                    query.setLong(name, (Long)value);
                    break;
                case DATE:
                    query.setDate(name, (Date)value);
                    break;
                case DATETIME:
                    query.setTimestamp(name, (Date)value);
                    break;
                case ACTOR:
                    query.setString(name, (String)new WfActorKeyToStringConverter().convert(value));
                    break;
                case BOOLEAN:
                    query.setBoolean(name, (Boolean)value);
                    break;
                case DAISY_LINK:
                    WfVersionKey versionKey = (WfVersionKey)value;
                    String searchVal;
                    if (versionKey.getVersion() != null) {
                        searchVal = (String)new WfVersionKeyToStringConverter().convert(versionKey);
                    } else {
                        searchVal = versionKey.getDocumentId() + "@" + versionKey.getBranchId() + ":" + versionKey.getLanguageId() + ":%";
                    }
                    query.setString(name, searchVal);
                    break;
                case USER:
                    query.setLong(name, ((WfUserKey)value).getId());
                    break;
                case ID:
                    long parsedId;
                    try {
                        parsedId = Long.parseLong((String)value);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Invalid ID: " + value);
                    }
                    query.setLong(name, parsedId);
                    break;
                default:
                    throw new RuntimeException("Unsupported value type for query binding: " + valueType);
            }
        }
    }

    private static final class BindEntry {
        WfValueType type;
        Object value;
        boolean isList;

        public BindEntry(WfValueType type, Object value, boolean isList) {
            this.type = type;
            this.value = value;
            this.isList = isList;
        }
    }
}
