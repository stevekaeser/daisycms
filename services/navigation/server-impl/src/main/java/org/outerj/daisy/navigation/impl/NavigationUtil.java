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
package org.outerj.daisy.navigation.impl;

public class NavigationUtil {

    /**
     * Filters out characters that might give problems in URL paths.
     * Returns null if the node ID cannot be made valid (= when its
     * empty after filtering, or when it starts with a digit which
     * is preserved for document IDs).
     *
     * <p>See also
     * http://www.w3.org/Addressing/URL/4_URI_Recommentations.html
     */
    public static String makeNodeIdValid(String nodeId) {
        if (nodeId == null || nodeId.length() == 0)
            return null;

        StringBuilder result = new StringBuilder(nodeId.length());

        for (int i = 0; i < nodeId.length(); i++) {
            char c = nodeId.charAt(i);
            switch (c) {
                case ':':
                case '?':
                case '/':
                case '#':
                case '%':
                    break;
                default:
                    result.append(c);
            }
        }


        if (result.length() == 0)
            return null;

        // IDs starting with a digit are reserved for document IDs
        if (Character.isDigit(result.charAt(0)))
            return null;

        return result.toString();
    }
}
