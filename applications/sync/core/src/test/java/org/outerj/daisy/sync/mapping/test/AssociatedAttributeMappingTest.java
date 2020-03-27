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

import java.util.ArrayList;

import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.outerj.daisy.sync.mapping.AssociatedAttributeMapping;
import org.outerj.daisy.sync.mapping.AttributeMapping;
import org.outerj.daisy.sync.mapping.MappingException;

import junit.framework.TestCase;

public class AssociatedAttributeMappingTest extends TestCase {
  public AssociatedAttributeMapping mapping;
  public String associatedAttributeName = "associatedAttribute";
  
  public Entity entity;
  public Entity associatedEntity;
  private AttributeMapping targetAttribute;

  protected void setUp() throws Exception {
    mapping = new AssociatedAttributeMapping(associatedAttributeName);
    
    targetAttribute = new AttributeMapping();
    targetAttribute.setName("targetName");
    targetAttribute.setDaisyName("targetDaisyName");
    targetAttribute.setType(AttributeType.CUSTOM_FIELD);
    
    mapping.setTargetAttribute(targetAttribute);
    
    entity = new EntityImpl();
    entity.setName("myentity");
    Attribute attribute = new AttributeImpl();
    attribute.setExternalName(targetAttribute.getName());
    attribute.setDaisyName(targetAttribute.getDaisyName());
    attribute.setType(targetAttribute.getType());
    entity.addAttribute(attribute);
    
    associatedEntity = new EntityImpl();
    associatedEntity.setName("associatedEntity");
    Attribute assAttr = new AttributeImpl();
    assAttr.setExternalName(associatedAttributeName);
    assAttr.addValue("AssociatedAttributeValue --- WOOOW ");
    associatedEntity.addAttribute(assAttr);
    
    super.setUp();
  }


  public void testApplyMappingEntity() {
    entity.addAttribute(new AttributeImpl(associatedAttributeName, "TargetValue here"));
    try {      
      mapping.applyMapping(entity);
      Attribute attr1 = entity.getAttributeByExternalName(targetAttribute.getName());
      Attribute attr2 = entity.getAttributeByExternalName(associatedAttributeName);
      for (int i = 0; i < attr1.getValues().size(); i++) {
        assertEquals(attr1.getValues().get(i), attr2.getValues().get(i));
      }      
    } catch (MappingException e) {
      fail("This operation should NOT cause an exception");
    }
  }

  public void testApplyMappingEntityEntity() {
    try {
      mapping.applyMapping(entity, associatedEntity);
      Attribute attr1 = entity.getAttributeByExternalName(targetAttribute.getName());
      Attribute attr2 = associatedEntity.getAttributeByExternalName(associatedAttributeName);
      for (int i = 0; i < attr1.getValues().size(); i++) {
        assertEquals(attr1.getValues().get(i), attr2.getValues().get(i));
      }
    } catch (MappingException e) {      
      e.printStackTrace();
      fail("This should not throw an exception");
    }
  }
  
  public void testApplyMappingNoTargetMapping() {
    mapping.setTargetAttribute(null);
    try {
      mapping.applyMapping(entity, associatedEntity);
      fail("Should throw an exception");
    } catch (MappingException e) {}
  }
  
  public void testApplyMappingNoTargetAttr() {
    mapping.setTargetAttribute(null);
    entity.setAttributes(new ArrayList<Attribute>());
    try {
      mapping.applyMapping(entity, associatedEntity);
      fail("Should throw an exception");
    } catch (MappingException e) {}
  }
  
  public void testApplyMappingNoSrcAttr() {
    associatedEntity.setAttributes(new ArrayList<Attribute>());
    try {
      mapping.applyMapping(entity, associatedEntity);
      fail("Should throw an exception");
    } catch (MappingException e) {}
  }

  public void testGetTargetAttribute() {
    assertEquals(targetAttribute, mapping.getTargetAttribute());
  }

  public void testSetTargetAttribute() {
    assertEquals(targetAttribute, mapping.getTargetAttribute());
  }

}
