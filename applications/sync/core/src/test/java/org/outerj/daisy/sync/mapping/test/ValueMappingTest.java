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

import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.outerj.daisy.sync.mapping.AttributeMapping;
import org.outerj.daisy.sync.mapping.MappingException;
import org.outerj.daisy.sync.mapping.ValueMapping;

public class ValueMappingTest extends TestCase {
  private ValueMapping valueMapping;
  private String value;
  
  public Entity entity;  
  private Attribute theAttribute;
  private AttributeMapping targetAttributeMapping;

  protected void setUp() throws Exception {
    value = "TheValueHere";
    valueMapping = new ValueMapping(value);
    
    targetAttributeMapping = new AttributeMapping();
    targetAttributeMapping.setName("targetName");
    targetAttributeMapping.setDaisyName("targetDaisyName");
    targetAttributeMapping.setType(AttributeType.CUSTOM_FIELD);
    
    valueMapping.setTargetAttribute(targetAttributeMapping);
    
    entity = new EntityImpl();
    entity.setName("myentity");
    theAttribute = new AttributeImpl();
    theAttribute.setExternalName(targetAttributeMapping.getName());
    theAttribute.setDaisyName(targetAttributeMapping.getDaisyName());
    theAttribute.setType(targetAttributeMapping.getType());
    entity.addAttribute(theAttribute);
    
    super.setUp();
  }

  public void testApplyMappingEntityEntity() {
      assertEquals(targetAttributeMapping, valueMapping.getTargetAttribute());
  }

  public void testGetTargetAttribute() {
    assertEquals(targetAttributeMapping, valueMapping.getTargetAttribute());
  }

  public void testSetTargetAttribute() {
    assertEquals(targetAttributeMapping, valueMapping.getTargetAttribute());
  }

  public void testApplyMappingEntity() {
    try {
      valueMapping.applyMapping(entity);
    } catch (MappingException e) {
      fail("This should not cause an exception");
    }
    assertEquals(value, theAttribute.getValues().get(0));
  }

}
