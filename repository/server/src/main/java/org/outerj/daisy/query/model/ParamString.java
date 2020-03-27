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

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class ParamString {
    private StringPart[] parts;

    public ParamString(String string) {
        List<StringPart> parts = new ArrayList<StringPart>();
        StringBuilder currentWord = new StringBuilder();
        final int IN_TEXT = 0;
        final int IN_PARAM = 1;
        int state = IN_TEXT;
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
                case '{':
                    if (state == IN_PARAM) {
                        if (currentWord.length() == 0) {
                            state = IN_TEXT;
                            currentWord.append("{");
                        } else {
                            throw new RuntimeException("Error parsing pattern: nested '{'.");
                        }
                    } else {
                        if (currentWord.length() > 0) {
                            parts.add(new FixedStringPart(currentWord.toString()));
                            currentWord.setLength(0);
                        }
                        state = IN_PARAM;
                    }
                    break;
                case '}':
                    if (state == IN_PARAM) {
                        if (currentWord.length() > 0) {
                            parts.add(new ParamStringPart(currentWord.toString()));
                            currentWord.setLength(0);
                        }
                        state = IN_TEXT;
                    } else {
                        currentWord.append("}");
                    }
                    break;
                default:
                    currentWord.append(c);
            }
        }
        if (currentWord.length() > 0)
            parts.add(new FixedStringPart(currentWord.toString()));
        this.parts = parts.toArray(new StringPart[parts.size()]);
    }

    interface StringPart {
        void append(StringBuilder buffer, Map<String, String> params);
    }

    class FixedStringPart implements StringPart {
        private final String string;

        FixedStringPart(String string) {
            this.string = string;
        }

        public void append(StringBuilder buffer, Map<String, String> params) {
            buffer.append(string);
        }
    }

    class ParamStringPart implements StringPart {
        private final String name;

        ParamStringPart(String name) {
            this.name = name;
        }

        public void append(StringBuilder buffer, Map<String, String> params) {
            String value = params.get(name);
            if (value != null)
                buffer.append(value);
        }
    }

    public String toString(Map<String, String> params) {
        StringBuilder result = new StringBuilder();
        this.append(result, params);
        return result.toString();
    }

    public void append(StringBuilder buffer, Map<String, String> params) {
        for (StringPart part : parts) {
            part.append(buffer, params);
        }
    }
}
