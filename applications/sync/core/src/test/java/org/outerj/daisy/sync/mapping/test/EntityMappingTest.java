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

import java.util.List;

import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.outerj.daisy.sync.mapping.AttributeMapping;
import org.outerj.daisy.sync.mapping.EntityMapping;
import org.outerj.daisy.sync.mapping.LanguageMapping;
import org.outerj.daisy.sync.mapping.MappingException;

import junit.framework.TestCase;

public class EntityMappingTest extends TestCase {
    private EntityMapping entityMapping;

    private AttributeMapping attributeMapping;

    private String docTypeName = "myDocType";

    private String entityName = "myEntity";

    protected void setUp() throws Exception {
        entityMapping = new EntityMapping();
        entityMapping.setDaisyDocumentTypeName(docTypeName);
        entityMapping.setEntityName(entityName);

        attributeMapping = new AttributeMapping();
        attributeMapping.setName("used");
        attributeMapping.setDaisyName("dsyUsed");
        attributeMapping.setType(AttributeType.FIELD);

        entityMapping.addChild(attributeMapping);
        super.setUp();
    }

    public void testAddChild() {
        int sizeBefore = entityMapping.getChildMappings().size();
        entityMapping.addChild(new AttributeMapping());
        int sizeAfter = entityMapping.getChildMappings().size();
        assertTrue(sizeBefore < sizeAfter);
    }

    public void testGetDaisyDocumentTypeName() {
        assertEquals(docTypeName, entityMapping.getDaisyDocumentTypeName());
    }

    public void testSetDaisyDocumentTypeName() {
        assertEquals(docTypeName, entityMapping.getDaisyDocumentTypeName());
    }

    public void testGetEntityName() {
        assertEquals(entityName, entityMapping.getEntityName());
    }

    public void testSetEntityName() {
        assertEquals(entityName, entityMapping.getEntityName());
    }

    public void testApplyMappingEntityName() {
        Entity entity = new EntityImpl();
        entity.setInternalName(docTypeName);
        entity.addAttribute(new AttributeImpl(attributeMapping.getName(), "something"));
        assertNull(entity.getName());
        try {

            List<Entity> entities = entityMapping.mapEntity(entity);
            for (Entity e : entities) {
                assertEquals(entityName, e.getName());
            }
        } catch (MappingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    public void testApplyMappingInternalName() {
        Entity entity = new EntityImpl();
        entity.setName(entityName);
        entity.addAttribute(new AttributeImpl(attributeMapping.getName(), "something"));
        assertNull(entity.getInternalName());
        try {
            List<Entity> entities = entityMapping.mapEntity(entity);
            for (Entity e : entities) {
                assertEquals(docTypeName, e.getInternalName());
            }
        } catch (MappingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

    }

    public void testApplyMappingUnusedAttributes() {
        Entity entity = new EntityImpl();
        entity.setName(entityName);
        Attribute used = new AttributeImpl(attributeMapping.getName(), "something");
        Attribute unused = new AttributeImpl("unused", "something");
        entity.addAttribute(used);
        entity.addAttribute(unused);

        int attrBefore = entity.getAttributes().size();
        try {
            List<Entity> entities = entityMapping.mapEntity(entity);
            for (Entity e : entities) {
                int attrAfter = e.getAttributes().size();
                assertTrue(attrBefore > attrAfter);
            }
        } catch (MappingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testApplyMappingLanguage() {
        Entity entity = new EntityImpl();
        entity.setName(entityName);
        entity.addAttribute(new AttributeImpl(attributeMapping.getName(), "something"));
        entity.addAttribute(new AttributeImpl("MyAttribute1", "something"));
        String lang1 = "Lang1";
        String lang2 = "Lang2";
        String requiredAttributes = "MyAttribute1, MyAttribute2";
        entityMapping.addLanguageMapping(new LanguageMapping(lang1, requiredAttributes));
        entityMapping.addLanguageMapping(new LanguageMapping(lang2, requiredAttributes));

        assertNull(entity.getInternalName());
        try {
            List<Entity> entities = entityMapping.mapEntity(entity);
            assertTrue(entities.size() == 2);
            assertEquals(lang1, entities.get(0).getLanguage());
            assertEquals(lang2, entities.get(1).getLanguage());

        } catch (MappingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    public void testApplyMappingLanguageNonExistingAttributes() {
        Entity entity = new EntityImpl();
        entity.setName(entityName);
        entity.addAttribute(new AttributeImpl(attributeMapping.getName(), "something"));
        String lang1 = "Lang1";
        String lang2 = "Lang2";
        String requiredAttributes = "MyAttribute1, MyAttribute2";
        entityMapping.addLanguageMapping(new LanguageMapping(lang1, requiredAttributes));
        entityMapping.addLanguageMapping(new LanguageMapping(lang2, requiredAttributes));

        assertNull(entity.getInternalName());
        try {
            List<Entity> entities = entityMapping.mapEntity(entity);
            assertTrue(entities.size() == 0);
        } catch (MappingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
    
    public void testApplyMappingLanguageNonExistant () {
        Entity entity = new EntityImpl();
        entity.setName(entityName);
        entity.addAttribute(new AttributeImpl(attributeMapping.getName(), "something"));
        
        String lang1 = "Lang1";
        String lang2 = "Lang2";
        String requiredAttributes = "MyAttribute1, MyAttribute2";
        entityMapping.addLanguageMapping(new LanguageMapping(lang1, requiredAttributes));
        entity.setLanguage(lang2);

        assertNull(entity.getInternalName());
        try {
            List<Entity> entities = entityMapping.mapEntity(entity);
            assertTrue(entities.size() == 0);
            

        } catch (MappingException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
    }
}
