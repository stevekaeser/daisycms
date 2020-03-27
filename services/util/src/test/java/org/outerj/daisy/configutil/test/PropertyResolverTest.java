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
package org.outerj.daisy.configutil.test;

import junit.framework.TestCase;

import java.util.Properties;

import org.outerj.daisy.configutil.PropertyResolver;

public class PropertyResolverTest  extends TestCase {

    public void testPropertyResolving() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("prop1", "val1");
        properties.setProperty("prop2", "special things : / %");
        properties.setProperty("space", " ");
        
        properties.setProperty("recursiveA", "${recursiveB}x");
        properties.setProperty("recursiveB", "${recursiveC}y");
        properties.setProperty("recursiveC", "${recursiveA}z");

        String resolved = PropertyResolver.resolveProperties("foo ${prop1} bar", properties);
        assertEquals("foo val1 bar", resolved);

        resolved = PropertyResolver.resolveProperties("$", properties);
        assertEquals("$", resolved);

        resolved = PropertyResolver.resolveProperties("foo ${prop1} bar$", properties);
        assertEquals("foo val1 bar$", resolved);

        resolved = PropertyResolver.resolveProperties("foo ${prop1} bar${", properties);
        assertEquals("foo val1 bar${", resolved);

        resolved = PropertyResolver.resolveProperties("foo ${prop1} ${url-encode:${prop2}}${url-encode:  } bar", properties);
        assertEquals("foo val1 special+things+%3A+%2F+%25++ bar", resolved);

        resolved = PropertyResolver.resolveProperties("foo ${prop1} ${stupid${stuff${at${the${end}}}}}", properties);
        assertEquals("foo val1 ${stupid${stuff${at${the${end}}}}}", resolved);

        resolved = PropertyResolver.resolveProperties("foo ${prop1} ${stupid${stuff${at${the${end", properties);
        assertEquals("foo val1 ${stupid${stuff${at${the${end", resolved);

        resolved = PropertyResolver.resolveProperties("foo ${prop1} ${stupid${stuff${at${the${end}", properties);
        assertEquals("foo val1 ${stupid${stuff${at${the${end}", resolved);

        resolved = PropertyResolver.resolveProperties("foo ${prop1} ${end", properties);
        assertEquals("foo val1 ${end", resolved);

        resolved = PropertyResolver.resolveProperties("${tripple-url-encode: }", properties);
        assertEquals("%252B", resolved);

        resolved = PropertyResolver.resolveProperties("${tripple-url-encode:${space}other/stuff}", properties);
        assertEquals("%252Bother%25252Fstuff", resolved);
        
        resolved = PropertyResolver.resolveProperties("${recursiveA}", properties);
        assertEquals("${recursiveA}zyx", resolved);
    }
}
