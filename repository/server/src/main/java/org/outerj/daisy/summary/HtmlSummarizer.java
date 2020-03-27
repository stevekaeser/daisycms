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
package org.outerj.daisy.summary;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.mxp1.MXParser;
import org.outerj.daisy.xmlutil.XmlReader;

import java.io.InputStream;
import java.util.Set;
import java.util.HashSet;
import java.util.BitSet;

public class HtmlSummarizer {
    private static Set<String> SPECIAL_PRE_CLASSES;
    static {
        SPECIAL_PRE_CLASSES = new HashSet<String>();
        SPECIAL_PRE_CLASSES.add("query");
        SPECIAL_PRE_CLASSES.add("include");
        SPECIAL_PRE_CLASSES.add("query-and-include");
    }

    private static Set<String> SPECIAL_SPAN_CLASSES;
    static {
        SPECIAL_SPAN_CLASSES = new HashSet<String>();
        SPECIAL_SPAN_CLASSES.add("indexentry");
        SPECIAL_SPAN_CLASSES.add("footnote");
        SPECIAL_SPAN_CLASSES.add("crossreference");
    }

    /**
     *
     * @param is an inputstream from which XML-well-formed HTML markup can be read.
     */
    public static String extractSummary(InputStream is, int summaryLength) throws Exception {
        XmlPullParser parser = new MXParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        parser.setInput(new XmlReader(is));
        int eventType = parser.getEventType();
        StringBuilder summary = new StringBuilder(summaryLength);
        BitSet ignoreableElements = new BitSet();

        while (eventType != XmlPullParser.END_DOCUMENT)
        {
            eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG)
            {
                if (!parser.getName().equals("html"))
                    return null;

                // run over the children of the html element
                while (eventType != XmlPullParser.END_TAG)
                {
                    eventType = parser.next();
                    if (eventType == XmlPullParser.START_TAG)
                    {
                        if (parser.getName().equals("body")) {
                            eventType = parser.next();
                            int bodyElementNestingCount = 0;
                            while (bodyElementNestingCount >= 0)
                            {
                                switch (eventType) {
                                    case XmlPullParser.START_TAG:
                                        bodyElementNestingCount++;
                                        if (parser.getNamespace().equals("")
                                                && (
                                                  (parser.getName().equals("pre") && SPECIAL_PRE_CLASSES.contains(parser.getAttributeValue("", "class")))
                                                  ||
                                                  (parser.getName().equals("span") && SPECIAL_SPAN_CLASSES.contains(parser.getAttributeValue("", "class")))
                                                )) {
                                            ignoreableElements.set(bodyElementNestingCount);
                                        } else if (bodyElementNestingCount > 0 && ignoreableElements.get(bodyElementNestingCount - 1)) {
                                            // inherit from parent element
                                            ignoreableElements.set(bodyElementNestingCount);
                                        }
                                        break;
                                    case XmlPullParser.END_TAG:
                                        ignoreableElements.clear(bodyElementNestingCount);
                                        bodyElementNestingCount--;
                                        break;
                                    case XmlPullParser.TEXT:
                                        if (!ignoreableElements.get(bodyElementNestingCount)) {
                                            String text = collapseWhitespace(parser.getText());
                                            int interestingChars = Math.min(summaryLength - 3 - summary.length(), text.length());
                                            summary.append(text.substring(0, interestingChars));
                                            if (summary.length() == summaryLength - 3) {
                                                summary.append("...");
                                                return summary.toString();
                                            }
                                        }
                                        break;
                                }
                                eventType = parser.next();
                            }
                        } else {
                            goToEndElement(parser);
                        }
                    }
                }
            }
        }

        if (summary.length() > 0) {
            return summary.toString();
        } else {
            return null;
        }
    }


    private static void goToEndElement(XmlPullParser parser) throws Exception
    {
        // TODO rewrite this non-recursive
        int eventType = parser.next();
        while (eventType != XmlPullParser.END_TAG)
        {
            if (eventType == XmlPullParser.START_TAG)
                goToEndElement(parser);
            eventType = parser.next();
        }
    }

    private static String collapseWhitespace(String text) {
        StringBuilder buffer = new StringBuilder(text.length());
        boolean lastCharWasWhitespace = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\n':
                case '\r':
                case ' ':
                    if (!lastCharWasWhitespace) {
                        buffer.append(' ');
                        lastCharWasWhitespace = true;
                    }
                    break;
                default:
                    buffer.append(c);
                    lastCharWasWhitespace = false;
            }
        }

        return buffer.toString();
    }

}
