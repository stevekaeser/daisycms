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
package org.outerj.daisy.frontend.util;

/**
 * Utility functions for use in XSLTs (XPath).
 */
public class XslUtil {
    /**
     * Escape a string for usage in a (double-quotes) javascript string.
     */
    public static String escape(String value) {
        if (value == null)
            return null;

        StringBuilder result = new StringBuilder(value.length() + 10);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\'':
                    result.append("\\\'");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }

    public static String translateForHtmlArea(String value) {
        StringBuilder result = new StringBuilder((int)(value.length() * 1.1));

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case 160:
                    result.append("&nbsp;");
                    break;
                case 13:
                    // skip
                    break;
                default:
                    result.append(c);
            }
        }

        return result.toString();
    }
}
