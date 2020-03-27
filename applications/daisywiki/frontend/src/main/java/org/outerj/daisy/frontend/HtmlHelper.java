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
package org.outerj.daisy.frontend;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HtmlHelper {
    private static Pattern emptyHtml1 = Pattern.compile(".*<body/>.*", Pattern.DOTALL);
    private static Pattern emptyHtml2 = Pattern.compile(".*<body>\\s*?</body>.*", Pattern.DOTALL);

    /**
     * Returns true if the HTML given as a string parameter is empty.
     *
     * <p>This is needed to detect whether a field widget with HTMLArea rendering
     * actually contains entered text, either for validating of required parts
     * or for avoiding storing this "empty content". This method assumes the HTML is
     * cleaned up with the {@link org.outerj.daisy.frontend.editor.HtmlCleaningConvertor} and that in case it is empty,
     * it only contains the html and body elements, with no extra attributes.
     *
     * <p>Given a specific configuration of the HtmlCleaningConvertor, we could detect
     * empty HTML simply by a string compare, but this method is a little bit more
     * robust to changing configurations (ie changes in amount of whitespace or
     * in the produced output of the html cleaner).
     *
     * <p>Practically speaking, it works as follows:
     * <ul>
     *  <li>If the input is null or an empty string, it is empty
     *  <li>If the input text is longer then 50 characters, assume it is not empty.
     *  <li>If the input contains &lt;body/> or &lt;body>...whitespace...&lt;/body> in it, it is empty
     *  <li>else, it is not empty
     * </ul>
     */
    public static boolean isEmpty(String html) {
        if (html == null)
            return true;

        if (html.length() > 50)
            return false;

        html = html.trim();
        if (html.equals(""))
            return true;

        Matcher matcher;

        matcher = emptyHtml1.matcher(html);
        if (matcher.matches())
            return true;

        matcher = emptyHtml2.matcher(html);
        return matcher.matches();
    }
}
