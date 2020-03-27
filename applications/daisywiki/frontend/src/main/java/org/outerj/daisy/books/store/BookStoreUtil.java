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
package org.outerj.daisy.books.store;

import java.util.BitSet;

public class BookStoreUtil {
    private static BitSet legalFileNameChars;
    static {
        legalFileNameChars = new BitSet(256);
        for (char x = 'a'; x <= 'z'; x++)
            legalFileNameChars.set(x);
        for (char x = '0'; x <= '9'; x++)
            legalFileNameChars.set(x);
        legalFileNameChars.set('_');
        legalFileNameChars.set('-');
        legalFileNameChars.set(' ');
        legalFileNameChars.set(',');
    }

    public static String isValidBookInstanceName(String name) {
        if (name.length() > 255)
            return "Name too long (maximum 255 characters allowed).";

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!legalFileNameChars.get(c))
                return "Name contains non-allowed characters: \"" + c + "\".";
        }
        return null;
    }

    public static String fixIllegalFileNameCharacters(String name) {
        StringBuilder fixedName = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isWhitespace(c)) {
                fixedName.append('_');
            } else if (legalFileNameChars.get(c)) {
                fixedName.append(c);
            }
            // other characters are skipped
        }
        return fixedName.toString();
    }

}
