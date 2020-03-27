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
package org.outerj.daisy.htmlcleaner;

class GeckoCorruptTagCleaner {
    /**
     * Removes invalid tags produced by gecko editor.
     *
     * <p>The Gecko Midas editor (or Mozilla Composer for that matter) sometimes
     * leaves things like '&lt;&gt;' or '&lt; /&gt;' (for those reading the
     * source: it leaves the unescaped tags), thus tags without names. This
     * is easily reproduceable by hitting enter twice followed by backspace twice
     * when in the middle of a paragraph or header.
     */
    public static String clean(String input) {
        char[] inputChars = input.toCharArray();
        StringBuilder result = new StringBuilder(input.length());

        int i = 0;
        while(i < inputChars.length) {
            char c = inputChars[i];
            if (c == '<' && i + 3 < inputChars.length) {
                if (inputChars[i + 1] == '>') {
                    i = i + 2;
                    continue;
                } else if (/* c == '<' && */ inputChars[i + 1] == ' ' && inputChars[i + 2] == '/' && inputChars[i + 3] == '>') {
                    i = i + 4;
                    continue;
                } else if (/* c == '<' && */ inputChars[i + 1] == '<') {
                    i = i + 1;
                    continue;
                }
            }
            result.append(c);
            i = i + 1;
        }

        return result.toString();
    }
}
