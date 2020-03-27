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
package org.outerj.daisy.xmlutil.test;

import junit.framework.TestCase;
import org.outerj.daisy.i18n.impl.AggregateResourceBundle;
import org.outerj.daisy.i18n.impl.DResourceBundleFactory;
import org.outerj.daisy.i18n.DResourceBundle;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

public class DResourceBundleTest extends TestCase {
    public void testResourceBundle() throws Exception {
        // Simple test of fallback between languages and bundles
        String bundle1En = "<catalogue><message key='hi'>Hi!</message></catalogue>";
        String bundle1Nl = "<catalogue><message key='hi'>Hallo!</message> <message key='hond'>Hond</message> </catalogue>";
        String bundle1NlBe = "<catalogue><message key='hi'>Hallo!!</message></catalogue>";

        String bundle2En = "<catalogue><message key='cow'>Cow</message></catalogue>";
        String bundle2Nl = "<catalogue><message key='koe'>Koe</message></catalogue>";

        AggregateResourceBundle arb = new AggregateResourceBundle(new String[] {"bundle1", "bundle2"});

        arb.addBundle("bundle1", new Locale(""), buildBundle(bundle1En));
        arb.addBundle("bundle1", new Locale("nl"), buildBundle(bundle1Nl));
        arb.addBundle("bundle1", new Locale("nl", "BE"), buildBundle(bundle1NlBe));
        arb.addBundle("bundle2", new Locale(""), buildBundle(bundle2En));
        arb.addBundle("bundle2", new Locale("nl"), buildBundle(bundle2Nl));

        // test exact matches
        assertEquals("Hallo!", arb.get(new Locale("nl"), "hi").getText());
        assertEquals("Hallo!!", arb.get(new Locale("nl", "BE"), "hi").getText());

        // fallback between locales
        assertEquals("Hi!", arb.get(new Locale("en"), "hi").getText());
        assertEquals("Hond", arb.get(new Locale("nl", "BE"), "hond").getText());

        // fallback between locales and bundles
        assertEquals("Cow", arb.get(new Locale("en"), "cow").getText());

        // test non existing key behaviour
        assertNull(arb.get(new Locale("en"), "nonExistingKey"));
    }

    private DResourceBundle buildBundle(String content) throws IOException, ParserConfigurationException, SAXException {
        return DResourceBundleFactory.build(new InputSource(new ByteArrayInputStream(content.getBytes("UTF-8"))));
    }
}
