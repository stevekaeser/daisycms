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
import org.outerj.daisy.sync.mapping.AttributeFilterMapping;
import org.outerj.daisy.sync.mapping.AttributeMapping;
import org.outerj.daisy.sync.mapping.Mapping;
import org.outerj.daisy.sync.mapping.MappingException;
import org.outerj.daisy.sync.mapping.ValueMapping;

public class AttributeFilterMappingTest extends TestCase {
  private AttributeFilterMapping filter;

  private final String attributeFilterName = "attributeToFilter";

  private final String match = "^a.*";

  private AttributeMapping attributeMapping;
  
  private final String attributeName = "myAttributeName";

  private final String dsyAttributeName = "dsyAttributeName";
  
  private final AttributeType attributeType = AttributeType.FIELD;

  private Entity entity;
  
  private AttributeMapping targetAttribute;

  protected void setUp() throws Exception  {
    filter = new AttributeFilterMapping(attributeFilterName, match);
    targetAttribute = new AttributeMapping();
    targetAttribute.setName("targetName");
    targetAttribute.setDaisyName("targetDaisyName");
    targetAttribute.setType(AttributeType.CUSTOM_FIELD);
    filter.setTargetAttribute(targetAttribute);
    
    filter.addChildMapping(new AssociatedAttributeMapping("associatedMapping"));
    

    attributeMapping = new AttributeMapping();
    attributeMapping.setDaisyName(dsyAttributeName);
    attributeMapping.setName(attributeName);
    attributeMapping.setType(attributeType);
    
    filter.addChildMapping(attributeMapping);

    entity = new EntityImpl();
    entity.setName("myentity");
    

    
    super.setUp();
  }

  public void testAttributeFilterMapping() {
    assertEquals(attributeFilterName, filter.getAttributeName());
    assertEquals(match, filter.getPattern().pattern());
  }

  public void testAddChildMapping() {
    int before = filter.getMappings().size();
    filter.addChildMapping(attributeMapping);
    int after = filter.getMappings().size();
    assertTrue(before < after);
  }

  public void testApplyMapping() {
    entity.addAttribute(new AttributeImpl(attributeMapping.getName(), null));
    entity.addAttribute(new AttributeImpl(filter.getAttributeName(), "a value beginning with a this should stay.  That rhymes :-)"));
    filter.getMappings().clear();
    filter.addChildMapping(new ValueMapping("NewVal"));
    filter.setTargetAttribute(attributeMapping);
    try {
      filter.applyMapping(entity);
    } catch (MappingException e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
    
    assertEquals(entity.getAttributeByExternalName(attributeMapping.getName()).getValues().get(0), "NewVal");
    assertTrue(!entity.getAttributeByExternalName(filter.getAttributeName()).getValues().get(0).equals("NewVal"));
  }

  public void testGetTargetAttribute() {
    assertEquals(targetAttribute, filter.getTargetAttribute());
  }

  public void testSetTargetAttributeTargetBeforeChild() {
    assertEquals(targetAttribute, filter.getTargetAttribute());
    for (Mapping mapping : filter.getMappings()) {
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
    
    filter.setTargetAttribute(newTarget);
    
    assertEquals(newTarget, filter.getTargetAttribute());
    for (Mapping mapping : filter.getMappings()) {
      if (mapping instanceof AssociatedMapping) {
        AssociatedMapping associatedMapping = (AssociatedMapping) mapping;
        assertEquals(associatedMapping.getTargetAttribute(), newTarget);
      }
    }
  }

}
