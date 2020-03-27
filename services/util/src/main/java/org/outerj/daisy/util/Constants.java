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
package org.outerj.daisy.util;

import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

public class Constants {
    /**
     * Regex for parsing "daisy:" links.
     *
     * <p>The resulting groups are:
     *
     * <ul>
     * <li>1 -> document ID
     * <li>2 -> branch
     * <li>3 -> language
     * <li>4 -> version
     * <li>5 -> unused, reserved for when we might add document part addressing
     * <li>6 -> query string (without the ?)
     * <li>7 -> fragment identifier (including the #)
     * </ul>
     */
    public static final Pattern DAISY_LINK_PATTERN = Pattern.compile("^daisy:([0-9]{1,19}(?:-[a-zA-Z0-9_]{1,200})?)(?:@([^:#?]*)(?::([^:#?]*))?(?::([^:#?]*))?)?()(?:\\?([^#]*))?(#.*)?$");
    public static final String DAISY_NAMESPACE = "http://outerx.org/daisy/1.0";
    public static final String PUBLISHER_NS = "http://outerx.org/daisy/1.0#publisher";
    public static final String NAVIGATION_NS = "http://outerx.org/daisy/1.0#navigation";
    public static final String VARIABLES_NS = "http://outerx.org/daisy/1.0#variables";
    public static final Pattern DAISY_DOCID_PATTERN = Pattern.compile("^([0-9]{1,19})-([a-zA-Z0-9_]{1,200})$");

    /**
     * Compatibility pattern for Daisy document IDs: doesn't require namespace part to be present.
     * (Namespaces were introduced in Daisy 2.0)
     */
    public static final Pattern DAISY_COMPAT_DOCID_PATTERN = Pattern.compile("^([0-9]{1,19})(?:-([a-zA-Z0-9_]{1,200}))?$");

    public static final Map SUGGESTED_NAMESPACE_PREFIXES = new HashMap();
    static {
        SUGGESTED_NAMESPACE_PREFIXES.put("http://outerx.org/daisy/1.0#workflow", "wf");
    }
}
