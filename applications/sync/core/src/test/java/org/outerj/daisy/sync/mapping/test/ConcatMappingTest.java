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

import junit.framework.TestCase;

import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.outerj.daisy.sync.mapping.AssociatedAttributeMapping;
import org.outerj.daisy.sync.mapping.AssociatedMapping;
import org.outerj.daisy.sync.mapping.AttributeMapping;
import org.outerj.daisy.sync.mapping.ConcatMapping;
import org.outerj.daisy.sync.mapping.EntityMapping;
import org.outerj.daisy.sync.mapping.Mapping;
import org.outerj.daisy.sync.mapping.MappingException;
import org.outerj.daisy.sync.mapping.ValueMapping;

public class ConcatMappingTest extends TestCase {
  private ConcatMapping concat;

  private AttributeMapping attributeMapping;
  
  private final String attributeName = "myAttributeName";

  private final String dsyAttributeName = "dsyAttributeName";
  
  private final AttributeType attributeType = AttributeType.FIELD;

  private Entity entity;
  
  private AttributeMapping targetAttribute;

  protected void setUp() throws Exception  {
    concat = new ConcatMapping();
    targetAttribute = new AttributeMapping();
    targetAttribute.setName("targetName");
    targetAttribute.setDaisyName("targetDaisyName");
    targetAttribute.setType(AttributeType.FIELD);
    targetAttribute.setMultivalue(true);
    targetAttribute.addChildMapping(concat);
    concat.setTargetAttribute(targetAttribute);
    
    concat.addChildMapping(new ValueMapping("/"));
    concat.addChildMapping(new AssociatedAttributeMapping("associatedMapping"));
    concat.addChildMapping(new ValueMapping("/"));

    entity = new EntityMapping.MappedEntity (new EntityImpl(), true);
    entity.setName("myentity");
        
    super.setUp();
  }

  public void testAddChildMapping() {
    int before = concat.getChildMappings().size();
    concat.addChildMapping(attributeMapping);
    int after = concat.getChildMappings().size();
    assertTrue(before < after);
  }

  public void testApplyMapping() {
    entity.addAttribute(new EntityMapping.MappedAttribute(new AttributeImpl("associatedMapping", "thevalue")));
    
    try {
        targetAttribute.applyMapping(entity);
    } catch (MappingException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    
    assertEquals("/thevalue/", entity.getAttributeByExternalName(targetAttribute.getName()).getValues().get(0));
  }

  public void testGetTargetAttribute() {
    assertEquals(targetAttribute, concat.getTargetAttribute());
  }

  public void testSetTargetAttributeTargetBeforeChild() {
    assertEquals(targetAttribute, concat.getTargetAttribute());
    for (Mapping mapping : concat.getChildMappings()) {
      if (mapping instanceof AssociatedMapping) {
        AssociatedMapping associatedMapping = (AssociatedMapping) mapping;
        assertEquals(associatedMapping.getTargetAttribute(), targetAttribute);
      }
    }
  }
  
  public void testSetTargetAttributeTargetAfterChild() {
    AttributeMapping newTarget = new AttributeMapping();
    newTarget.setName("newtarget");
    newTarget.setDaisyName("newdsyname");
    try {
      newTarget.setType(AttributeType.PROPERTY);
    } catch (MappingException e) {      
      e.printStackTrace();
      fail("This should not throw an exception");
    }
    
    concat.setTargetAttribute(newTarget);
    
    assertEquals(newTarget, concat.getTargetAttribute());
    for (Mapping mapping : concat.getChildMappings()) {
      if (mapping instanceof AssociatedMapping) {
        AssociatedMapping associatedMapping = (AssociatedMapping) mapping;
        assertEquals(associatedMapping.getTargetAttribute(), newTarget);
      }
    }
  }

}
