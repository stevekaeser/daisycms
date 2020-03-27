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
package org.outerj.daisy.repository.commonimpl;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Util {
    private static Pattern namePattern = Pattern.compile("[a-zA-Z][a-zA-Z\\-_0-9]*");

    /**
     * Checks the conditions to which names of types should adhere.
     */ 
    public static void checkName(String name) {
        if (name == null)
            throw new IllegalArgumentException("Name should not be null");

        if (name.length() == 0)
            throw new IllegalArgumentException("Name should be at least one character long");

        if (name.length() > 50)
            throw new IllegalArgumentException("Name length should not exceed 50 characters.");

        Matcher matcher = namePattern.matcher(name);
        if (!matcher.matches())
            throw new IllegalArgumentException("Name contains illegal characters.");
    }
}
