/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.doctaskrunner.serverimpl.actions.serp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Utility class with helper methods that may be used at various places where search and replace operations are performed.
 * @author karel
 *
 */
public class SearchAndReplaceUtil {

    private static final Pattern letters = Pattern.compile("\\p{L}"); // unicode version of [a-zA-Z]
    private static final Pattern noLetters = Pattern.compile("\\P{L}"); //unicode version of [^a-zA-Z]
    private static final Pattern firstUpper = Pattern.compile("^[\\p{Lu}]"); // unicode version of ^[A-Z]
    private static final Pattern lastLower = Pattern.compile("\\p{Ll}$"); // unicode version of [a-z]$

    /**
     * returns a copy of replacementText with case matched to origText
     * 
     *  NOTE: toLowerCase and toUpperCase have locale sensitive variants, but since
     *  there is no usable locale information we can not use them and we have to resort to
     *  the next best alternative (which is (arguably) Locale.getDefault());
     */
    public static String caseMatch(String origText, String replacementText) {
        String origTextLetters = noLetters.matcher(origText).replaceAll("");
        
        // check that both original text and replacement text actually contain letters.
        Matcher replacementTextMatcher = letters.matcher(replacementText);
        if (origTextLetters.length() == 0 || !replacementTextMatcher.find()) {
            return replacementText;
        }
        
        if (origText.equals(origText.toUpperCase())) { // original text is all upper case
            return replacementText.toUpperCase();
        } else if (origText.equals(origText.toLowerCase())) { // original text is all upper case
            return replacementText.toLowerCase();
        } else if (firstUpper.matcher(origTextLetters).find() &&
                    lastLower.matcher(origTextLetters).find()) {
            // if first letter is upper and last letter is lower, use capitalized replacement text
            StringBuffer result = new StringBuffer();
            result.append(replacementText.substring(0, replacementTextMatcher.start()));
            result.append(replacementText.substring(replacementTextMatcher.start(), replacementTextMatcher.end()).toUpperCase());
            result.append(replacementText.substring(replacementTextMatcher.end(), replacementText.length()).toLowerCase());
            return result.toString();
        } else {
            return replacementText; // this is the best we can do without making assumptions.
        }
    }

    public static void addFragmentXml(Node node,
            String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        org.w3c.dom.Document xmlDoc = node.getOwnerDocument();
        
        Node html = xmlDoc.createElement("html");
        node.appendChild(html);
        Node body = xmlDoc.createElement("body");
        html.appendChild(body);

        int offset = 0;
        while (matcher.find()) {
            body.appendChild(xmlDoc.createTextNode(text.substring(offset, matcher.start())));
            
            Element match = xmlDoc.createElement("span");
            match.setAttribute("class", "dsy-regexpsearch-hit");
            
            match.appendChild(xmlDoc.createTextNode(text.substring(matcher.start(), matcher.end())));
            body.appendChild(match);

            offset = matcher.end();
        }
        body.appendChild(xmlDoc.createTextNode(text.substring(offset, text.length())));
    }

    public static String performSimpleTextReplacement(String originalText, String replacement, Pattern pattern, boolean useSensibleCase) {
        Matcher m = pattern.matcher(originalText);
        StringBuffer result = new StringBuffer();
        int offset = 0;
        while (m.find()) {
            m.appendReplacement(result, replacement);
            
            String replacementResult = result.substring(m.start() + offset);
            if (useSensibleCase) {
                result.replace(m.start() + offset, result.length(), SearchAndReplaceUtil.caseMatch(originalText.substring(m.start(), m.end()), replacementResult));    
            }
            
            offset += (m.start() - m.end()) + replacementResult.length();
        }
        m.appendTail(result); 
        return result.toString();
    }

    public static int countMatches(String text, Pattern pattern) {
        int count = 0;
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            count++;
        }
        return count;
    }

}