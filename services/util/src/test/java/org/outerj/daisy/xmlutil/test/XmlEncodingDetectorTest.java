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
package org.outerj.daisy.xmlutil.test;

import junit.framework.TestCase;
import org.outerj.daisy.xmlutil.XmlEncodingDetector;

public class XmlEncodingDetectorTest extends TestCase {

    public void testEncodingDetection() throws Exception {
        String test1 = "<abc/>";
        assertEquals("UTF-8", XmlEncodingDetector.detectEncoding(test1.getBytes("UTF-8")));

        String test2 = "<?xml encoding=\"ISO-8859-1\"?>\n<jaja/>";
        assertEquals("ISO-8859-1", XmlEncodingDetector.detectEncoding(test2.getBytes("ISO-8859-1")));

        String test3 = "<?xml encoding=\"ISO-8859-1\"?><jaja/>";
        assertEquals("ISO-8859-1", XmlEncodingDetector.detectEncoding(test3.getBytes("ISO-8859-1")));

        StringBuilder fillup = new StringBuilder(3000);
        for (int i = 0; i < 3000; i++) {
            fillup.append("a");
        }

        String test4 = "<?xml encoding=\"ISO-8859-1\"?><jaja>" + fillup + "</jaja>";
        assertEquals("ISO-8859-1", XmlEncodingDetector.detectEncoding(test4.getBytes("ISO-8859-1")));
    }
}
