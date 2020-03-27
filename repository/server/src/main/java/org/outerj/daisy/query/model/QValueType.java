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
package org.outerj.daisy.query.model;

import java.util.*;

/**
 * Enumeration of types that can occur in queries. This is a superset of
 * {@link org.outerj.daisy.repository.ValueType}.
 */
public enum QValueType {
    UNDEFINED("undefined"),
    STRING("string"),
    DATE("date"),
    DATETIME("datetime"),
    LONG("long"),
    DOUBLE("double"),
    DECIMAL("decimal"),
    BOOLEAN("boolean"),
    XML("xml"),
    VERSION_STATE("version_state"),
    LINK("link"),
    DOCID("docid");

    private final String name;
    private static final EnumMap<QValueType, EnumSet<QValueType>> compatibleTypes = new EnumMap<QValueType, EnumSet<QValueType>>(QValueType.class);
    static {
        EnumSet<QValueType> UNDEFINED_SINGLETON = EnumSet.of(UNDEFINED);
        for (QValueType type : QValueType.values())
            compatibleTypes.put(type, UNDEFINED_SINGLETON);

        compatibleTypes.put(DATE, EnumSet.of(DATETIME, UNDEFINED));
        compatibleTypes.put(DATETIME, EnumSet.of(DATE, UNDEFINED));
        compatibleTypes.put(LONG, EnumSet.of(DECIMAL, UNDEFINED));
        compatibleTypes.put(DOUBLE, EnumSet.of(DECIMAL, LONG, UNDEFINED));
        compatibleTypes.put(DECIMAL, EnumSet.of(DECIMAL, LONG, UNDEFINED));
    }

    private QValueType(String name) {
        this.name = name;
    }
    
    public String toString() {
        return name;
    }

    public boolean isCompatible(QValueType otherValueType) {
        return otherValueType == this || compatibleTypes.get(this).contains(otherValueType);
    }
}
