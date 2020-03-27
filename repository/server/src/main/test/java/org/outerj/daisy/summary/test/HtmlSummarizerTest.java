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
package org.outerj.daisy.summary.test;

import junit.framework.TestCase;
import org.outerj.daisy.summary.HtmlSummarizer;

import java.io.ByteArrayInputStream;

public class HtmlSummarizerTest extends TestCase {
    public void testSummarizer() throws Exception {
        String input, summary;

        input = "<html><body>abc</body></html>";
        summary = HtmlSummarizer.extractSummary(new ByteArrayInputStream(input.getBytes("UTF-8")), 300);
        assertEquals("abc", summary);

        input = " <html> <body>abc</body> </html> ";
        summary = HtmlSummarizer.extractSummary(new ByteArrayInputStream(input.getBytes("UTF-8")), 300);
        assertEquals("abc", summary);

        input = " <html> <body><p>abc</p></body> </html> ";
        summary = HtmlSummarizer.extractSummary(new ByteArrayInputStream(input.getBytes("UTF-8")), 300);
        assertEquals("abc", summary);

        input = " <html> <body><p>Bla bla bla</p></body> </html> ";
        summary = HtmlSummarizer.extractSummary(new ByteArrayInputStream(input.getBytes("UTF-8")), 10);
        assertEquals("Bla bla...", summary);
    }
}
