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
package org.outerj.daisy.linkextraction;

public class LinkType {
    public static final LinkType INLINE = new LinkType("inline", "L");
    public static final LinkType OUT_OF_LINE = new LinkType("out_of_line", "O");
    public static final LinkType IMAGE = new LinkType("image", "I");
    public static final LinkType INCLUDE = new LinkType("include", "A");
    public static final LinkType OTHER = new LinkType("other", "N");
    public static final LinkType FIELD = new LinkType("field", "F");

    private final String name;
    /** Code is a one-character identification of this link type. */
    private final String code;

    private LinkType(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public String toString() {
        return name;
    }

    public static LinkType fromString(String name) {
        if (INLINE.name.equals(name))
            return INLINE;
        else if (OUT_OF_LINE.name.equals(name))
            return OUT_OF_LINE;
        else if (IMAGE.name.equals(name))
            return IMAGE;
        else if (INCLUDE.name.equals(name))
            return INCLUDE;
        else if (OTHER.name.equals(name))
            return OTHER;
        else if (FIELD.name.equals(name))
            return FIELD;
        else
            throw new RuntimeException("Unrecognized link type name: " + name);
    }

    public static LinkType getByCode(String code) {
        if (INLINE.code.equals(code))
            return INLINE;
        else if (OUT_OF_LINE.code.equals(code))
            return OUT_OF_LINE;
        else if (IMAGE.code.equals(code))
            return IMAGE;
        else if (INCLUDE.code.equals(code))
            return INCLUDE;
        else if (OTHER.code.equals(code))
            return OTHER;
        else if (FIELD.code.equals(code))
            return FIELD;
        else
            throw new RuntimeException("Unrecognized link type code: " + code);
    }
}
