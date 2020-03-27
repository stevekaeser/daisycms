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
package org.outerj.daisy.sync.mapping.test;

import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.outerj.daisy.sync.mapping.AttributeMapping;
import org.outerj.daisy.sync.mapping.LanguageMapping;
import org.outerj.daisy.sync.mapping.Mapping;
import org.outerj.daisy.sync.mapping.MappingException;

import junit.framework.TestCase;

public class LanguageMappingTest extends TestCase {

    private LanguageMapping languageMapping;

    private Entity entity;

    private String language = "MyLang";
    private String requiredAttributes = "MyAttribute1, MyAttribute2";

    protected void setUp() throws Exception {
        super.setUp();
        entity = new EntityImpl();
        languageMapping = new LanguageMapping(language, requiredAttributes);
    }

    public void testAddChild() {
        int before = languageMapping.getChildMappings().size();
        Mapping childMapping = new AttributeMapping();
        languageMapping.addChildMapping(childMapping);
        int after = languageMapping.getChildMappings().size();
        assertTrue(after > before);
    }

    public void testApplyMapping() {
        assertNull(entity.getLanguage());
        try {
            languageMapping.applyMapping(entity);
        } catch (MappingException e) {
            fail("No exceptions allowed");
        }
        assertEquals(language, entity.getLanguage());
    }

    public void testGetName() {
        assertEquals(language, languageMapping.getName());
    }

    public void testSetName() {
        String newName = "NewLang";
        languageMapping.setName(newName);
        assertEquals(newName, languageMapping.getName());
    }

}
