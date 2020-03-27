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

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

import org.outerj.daisy.xmlutil.MergeCharacterEventsHandler;
import org.outerj.daisy.xmlutil.SaxBuffer;

/**
 * Performs cleanup of HTML documents to well-formed HTML-as-XML documents.
 *
 * <p>More information:
 * <ul>
 *  <li>To instantiate: see {@link HtmlCleanerFactory} and {@link HtmlCleanerTemplate}
 *  <li>About cleanup procedure: see {@link NekoHtmlParser}, {@link HtmlRepairer}
 *      and {@link StylingHtmlSerializer}.
 * </ul>
 */
public class HtmlCleaner {
    private HtmlCleanerTemplate template;

    HtmlCleaner(HtmlCleanerTemplate template) {
        this.template = template;
    }

    /**
     * Parses and cleans up the HTML, writing the result to the given outputstream,
     * encoded as UTF-8.
     */
    public void clean(String somethingWhichLooksLikeHtml, OutputStream outputStream) throws Exception {
        NekoHtmlParser parser = new NekoHtmlParser();
        SaxBuffer buffer = parser.parse(GeckoCorruptTagCleaner.clean(somethingWhichLooksLikeHtml));

        StylingHtmlSerializer serializer = new StylingHtmlSerializer(template);
        serializer.setOutputStream(outputStream);
        HtmlRepairer repairer = new HtmlRepairer(template);

        repairer.clean(buffer, new MergeCharacterEventsHandler(serializer));
    }

    public byte[] cleanToByteArray(String somethingWhichLooksLikeHtml) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
        clean(somethingWhichLooksLikeHtml, os);
        return os.toByteArray();
    }

    public String cleanToString(String somethingWhichLooksLikeHtml) throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream(10000);
        clean(somethingWhichLooksLikeHtml, os);
        return os.toString("UTF-8");
    }
}
