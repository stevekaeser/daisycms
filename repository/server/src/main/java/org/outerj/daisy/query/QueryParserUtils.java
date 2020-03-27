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
package org.outerj.daisy.query;

/**
 * Utility code for the QueryParser, easier to write here then embedded
 * in the grammer.
 */
final class QueryParserUtils {
    /**
     * @param literal a text literal within single quotes, within which single quotes
     *                are escaped by using them double.
     */
    public static String unEscapeStringLiteral(final String literal) {
        StringBuilder result = new StringBuilder(literal.length());
        boolean inSingleQuote = false;
        for (int i = 1; i < literal.length() - 1; i++) {
            char c = literal.charAt(i);
            switch (c) {
                case '\'':
                    if (inSingleQuote) {
                        inSingleQuote = false;
                        result.append(c);
                    } else {
                        inSingleQuote = true;
                    }
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }
}
