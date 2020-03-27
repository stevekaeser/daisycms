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
package org.outerj.daisy.repository;

/**
 * Enumeration of the possible kinds of values that a field can have.
 *
 * <p>Each {@link Field} is based on a {@link org.outerj.daisy.repository.schema.FieldType}
 * which defines the ValueType for the field.
 *
 */
public enum ValueType {
    STRING("string", 1, String.class),
    DATE("date", 2, java.util.Date.class),
    DATETIME("datetime", 3, java.util.Date.class),
    LONG("long", 4, Long.class),
    DOUBLE("double", 5, Double.class),
    DECIMAL("decimal", 6, java.math.BigDecimal.class),
    BOOLEAN("boolean", 7, java.lang.Boolean.class),
    LINK("link", 8, VariantKey.class);

    private final String name;
    private final int code;
    private final Class clazz;

    private ValueType(String name, int code, Class clazz) {
        this.name = name;
        this.code = code;
        this.clazz = clazz;
    }

    public String toString() {
        return name;
    }

    public int getCode() {
        return code;
    }

    public Class getTypeClass() {
        return clazz;
    }

    public static ValueType fromString(String name) {
        if (name.equals("string"))
            return ValueType.STRING;
        else if (name.equals("date"))
            return ValueType.DATE;
        else if (name.equals("datetime"))
            return ValueType.DATETIME;
        else if (name.equals("long"))
            return ValueType.LONG;
        else if (name.equals("double"))
            return ValueType.DOUBLE;
        else if (name.equals("decimal"))
            return ValueType.DECIMAL;
        else if (name.equals("boolean"))
            return ValueType.BOOLEAN;
        else if (name.equals("link"))
            return ValueType.LINK;
        else
            throw new RuntimeException("Unrecognized ValueType name: " + name);
    }

    public static ValueType getByCode(int code) {
        switch (code) {
            case 1:
                return STRING;
            case 2:
                return DATE;
            case 3:
                return DATETIME;
            case 4:
                return LONG;
            case 5:
                return DOUBLE;
            case 6:
                return DECIMAL;
            case 7:
                return BOOLEAN;
            case 8:
                return LINK;
            default:
                throw new RuntimeException("Non-existing ValueType code: " + code);
        }
    }
}
