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

import java.util.Date;
import java.text.SimpleDateFormat;

/**
 * Helper methods for correctly formatting values when programatically assembling queries.
 */
public final class QueryHelper {
    public static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    private static final String QUOTE = "'";

    public static String formatDateTime(Date date) {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DATETIME_PATTERN);
        return QUOTE + dateTimeFormat.format(date) + QUOTE;
    }

    public static String formatDate(Date date) {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat(DATE_PATTERN);
        return QUOTE + dateTimeFormat.format(date) + QUOTE;
    }

    public static String formatTime(Date date) {
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat(TIME_PATTERN);
        return QUOTE + dateTimeFormat.format(date) + QUOTE;
    }

    public static String formatString(String text) {
        StringBuilder escaped = new StringBuilder(text.length() + 10);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'') {
                escaped.append("''");
            } else {
                escaped.append(c);
            }
        }
        return QUOTE + escaped + QUOTE;
    }

    /**
     * Escapes characters which have a special meaning in full text
     * queries.
     */
    public static String escapeFullTextQuery(String query) {

        // Replacing lucene special characters: + - && || ! ( ) { } [ ] ^ " ~ * ? : \
        // taken from http://lucene.apache.org/java/docs/queryparsersyntax.html

        StringBuilder escapedQuery = new StringBuilder(query.length() + 10);
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            switch (c) {
                case '+':
                case '-':
                case '!':
                case '(':
                case ')':
                case '{':
                case '}':
                case '[':
                case ']':
                case '^':
                case '"':
                case '~':
                case '*':
                case '?':
                case ':':
                case '\\':
                    escapedQuery.append('\\').append(c);
                    break;
                case '&':
                case '|':
                    if (i + 1 < query.length() && query.charAt(i + 1) == c) {
                        escapedQuery.append('\\').append(c).append(c);
                        i++; // skip over the next character
                    } else {
                        escapedQuery.append(c);
                    }
                    break;
                default:
                    escapedQuery.append(c);
            }
        }

        return escapedQuery.toString();
    }
}
