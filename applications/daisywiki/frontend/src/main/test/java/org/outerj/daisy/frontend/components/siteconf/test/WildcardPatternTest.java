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
package org.outerj.daisy.frontend.components.siteconf.test;

import junit.framework.TestCase;
import org.outerj.daisy.frontend.components.siteconf.WildcardPattern;

public class WildcardPatternTest extends TestCase {
    public void testPatterns() {
        WildcardPattern pattern = new WildcardPattern("foo*bar");
        assertTrue(pattern.matches("foo2bar"));
        assertTrue(pattern.matches("foobar"));
        assertFalse(pattern.matches("foobars"));

        pattern = new WildcardPattern("foo?bar");
        assertTrue(pattern.matches("foo2bar"));
        assertFalse(pattern.matches("foobar"));
        assertFalse(pattern.matches("foo22bar"));

        pattern = new WildcardPattern("foobar");
        assertTrue(pattern.matches("foobar"));

        pattern = new WildcardPattern("foo\\bar");
        assertTrue(pattern.matches("foo\\bar"));

        pattern = new WildcardPattern("foo\\*bar");
        assertTrue(pattern.matches("foo*bar"));

        pattern = new WildcardPattern("foo\\?bar");
        assertTrue(pattern.matches("foo?bar"));

        pattern = new WildcardPattern("foo\\\\bar");
        assertTrue(pattern.matches("foo\\bar"));

        pattern = new WildcardPattern("foobar\\");
        assertTrue(pattern.matches("foobar\\"));

        pattern = new WildcardPattern("foo?*bar");
        assertTrue(pattern.matches("foo2bar"));
        assertFalse(pattern.matches("foobar"));
    }
}
