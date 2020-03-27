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

import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.outerj.daisy.sync.mapping.AssociatedAttributeMapping;
import org.outerj.daisy.sync.mapping.AssociatedEntityMapping;
import org.outerj.daisy.sync.mapping.AttributeMapping;
import org.outerj.daisy.sync.mapping.EntityMapping;
import org.outerj.daisy.sync.mapping.MappingException;

import junit.framework.TestCase;

public class AttributeMappingTest extends TestCase {
  private AttributeMapping attributeMapping;
  
  private Entity entity;
  
  private AssociatedEntityMapping associatedEntityMapping;

  private final String attributeName = "myAttributeName";

  private final String dsyAttributeName = "dsyAttributeName";
  
  private final AttributeType attributeType = AttributeType.FIELD;
  
  

  protected void setUp() throws Exception {
    attributeMapping = new AttributeMapping();
    attributeMapping.setDaisyName(dsyAttributeName);
    attributeMapping.setName(attributeName);
    attributeMapping.setType(attributeType);
    
    associatedEntityMapping = new AssociatedEntityMapping("assocName", "myJoin", null, null, null, null);
    attributeMapping.addChildMapping(associatedEntityMapping);
    
    entity = new EntityImpl();
    entity.setName("myentity");
    
    
    super.setUp();
  }

  public void testGetDaisyName() {
    assertEquals(attributeMapping.getDaisyName(), dsyAttributeName);
  }

  public void testSetDaisyName() {
    assertEquals(attributeMapping.getDaisyName(), dsyAttributeName);
  }

  public void testGetName() {
    assertEquals(attributeMapping.getName(), attributeName);
  }

  public void testSetName() {
    assertEquals(attributeMapping.getName(), attributeName);
  }

  public void testGetType() {
    assertEquals(attributeMapping.getType(), attributeType);
  }

  public void testSetType() {
    assertEquals(attributeMapping.getType(), attributeType);
  }

  public void testApplyMappingExternalName() {
    Attribute attr = new AttributeImpl( attributeMapping.getName(), "something");
    entity.addAttribute(attr);
    try {
      attributeMapping.applyMapping(entity);
      assertEquals(attr.getDaisyName(), this.dsyAttributeName);
      assertEquals(attr.getType(), this.attributeType);
    } catch (MappingException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }
  
  public void testApplyMappingInternalName() {
    Attribute attr = new AttributeImpl(null, "something", attributeMapping.getDaisyName(), attributeMapping.getType());
    entity.addAttribute(attr);
    try {
      attributeMapping.applyMapping(entity);
      assertEquals(attr.getExternalName(), this.attributeName);      
    } catch (MappingException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  public void testGetAssociatedEntityMapping() {
    assertEquals(this.associatedEntityMapping, attributeMapping.getChildMappings().get(0));
  }

  public void testSetAssociatedEntityMapping() {
    assertEquals(this.associatedEntityMapping, attributeMapping.getChildMappings().get(0));
  }
  
  public void testMultiValueTypePropertyFail() {
    // may not be multivalue    
    
    try {
      this.attributeMapping.setType(AttributeType.PROPERTY);
      this.attributeMapping.setMultivalue(true);
      fail("This should thow an exception");
    } catch (MappingException e) {}
  }
  
  public void testMultiValueTypeCustomFieldFail() {
    // may not be multivalue    
    
    try {
      this.attributeMapping.setType(AttributeType.CUSTOM_FIELD);
      this.attributeMapping.setMultivalue(true);
      fail("This should thow an exception");
    } catch (MappingException e) {}
  }
  
  public void testMultiValueTypeFieldNoFail() {
    // may not be multivalue    
    
    try {
      this.attributeMapping.setType(AttributeType.FIELD);
      this.attributeMapping.setMultivalue(true);      
    } catch (MappingException e) {
      fail("This should NOT thow an exception");
    }
  }
  
  public void testAggregatedAttributeValue() {
    entity.addAttribute(new AttributeImpl("one", "value1"));
    entity.addAttribute(new AttributeImpl("two", "value2"));
    
    Entity mappedEntity = new EntityMapping.MappedEntity(entity, true);
    
    attributeMapping.getChildMappings().clear();
    attributeMapping.addChildMapping(new AssociatedAttributeMapping("one"));
    attributeMapping.addChildMapping(new AssociatedAttributeMapping("two"));
    
    assertNull(entity.getAttributeByExternalName(attributeName));
    
    try {
      attributeMapping.applyMapping(mappedEntity);
      
      assertEquals("value1 value2", entity.getAttributeByExternalName(attributeName).getValues().get(0));
    } catch (MappingException e) {      
      e.printStackTrace();
      fail("Should not thow exceptions");
    }   
  }
}
