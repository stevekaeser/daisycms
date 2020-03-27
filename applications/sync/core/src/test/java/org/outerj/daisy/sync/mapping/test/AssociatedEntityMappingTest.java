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
import java.util.List;

import junit.framework.TestCase;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.outerj.daisy.sync.VariantHelper;
import org.outerj.daisy.sync.dao.test.MockExternalEntityDao;
import org.outerj.daisy.sync.dao.test.MockSyncEntityDao;
import org.outerj.daisy.sync.mapping.AssociatedAttributeMapping;
import org.outerj.daisy.sync.mapping.AssociatedEntityMapping;
import org.outerj.daisy.sync.mapping.AssociatedMapping;
import org.outerj.daisy.sync.mapping.AttributeMapping;
import org.outerj.daisy.sync.mapping.MappingException;

public class AssociatedEntityMappingTest extends TestCase {
  private AssociatedEntityMapping mapping;
  private AssociatedEntityMapping mappingParent;
  private List<AssociatedMapping> childMappings;
  private AssociatedAttributeMapping associatedAttributeMapping;
  private AttributeMapping targetAttributeMapping; 
  
  private Entity entity;
  private Entity associatedEntity;
  
  private final String associatedEntityName = "associatedEntity";
  private final String joinName = "joinKeyField";
  private final String joinParentName = "joinParentKeyField";
  

  protected void setUp() throws Exception {
    MockExternalEntityDao extDao = new MockExternalEntityDao();
    MockSyncEntityDao syncDao = new MockSyncEntityDao();
    
    entity = new EntityImpl();
    entity.setExternalId(6690);
    entity.setName("myEntity");
    entity.setLanguage("hh");
    
    childMappings = new ArrayList<AssociatedMapping>();
    this.associatedAttributeMapping = new AssociatedAttributeMapping("associatedAttribute");
    childMappings.add(associatedAttributeMapping);
    
    associatedEntity = new EntityImpl();
    associatedEntity.setName(associatedEntityName);
    associatedEntity.setExternalId(779);
    associatedEntity.setInternalName("dsy" + associatedEntityName);
    associatedEntity.setDaisyVariantKey(new VariantKey("555-VLK", 1, 1));
    associatedEntity.addAttribute(new AttributeImpl(joinName, Long.toString(entity.getExternalId())));
    associatedEntity.setLanguage("hh");
    
    associatedEntity.addAttribute(new AttributeImpl(associatedAttributeMapping.getName(), "pukkavalue"));
    mapping = new AssociatedEntityMapping(associatedEntityName, joinName, null, extDao, syncDao, null);
    mappingParent = new AssociatedEntityMapping(associatedEntityName, null, joinParentName, extDao, syncDao, null);
    
    mapping.setChildMappings(childMappings);
    
    targetAttributeMapping = new AttributeMapping();
    targetAttributeMapping.setName("targetName");
    targetAttributeMapping.setDaisyName("targetDaisyName");
    targetAttributeMapping.setType(AttributeType.CUSTOM_FIELD);
    
    mapping.setTargetAttribute(targetAttributeMapping);
    
    extDao.addEntity(entity);
    extDao.addEntity(associatedEntity);
    
    syncDao.storeEntity(associatedEntity);
    
    super.setUp();
  }

  public void testAssociatedEntityMapping() {
    assertEquals(associatedEntityName, mapping.getEntityName());
    assertEquals(joinName, mapping.getJoinKey());
    assertEquals(joinParentName, mappingParent.getJoinParentKey());
  }
  
  public void testApplyMappingEntityWithAttribute() {
    try {      
      mapping.applyMapping(entity);
      Attribute attr1 = entity.getAttributeByExternalName(targetAttributeMapping.getName());
      if (attr1 != null) {
          
        Attribute attr2 = associatedEntity.getAttributeByExternalName(associatedAttributeMapping.getName());
        for (int i = 0; i < attr1.getValues().size(); i++){
          assertEquals(attr1.getValues().get(i), attr2.getValues().get(i));
        }
      } else {
        fail("The attribute should have been found");
      }
    } catch (MappingException e) {      
      e.printStackTrace();
      fail("This shouldn't cause an error");
    }
  }
  
  public void testApplyMappingEntityWithEntity() {
    try {      
      mapping.getChildMappings().clear();
      mapping.applyMapping(entity);
      Attribute attr1 = entity.getAttributeByExternalName(targetAttributeMapping.getName());
      if (attr1 != null) {
        String value = VariantHelper.variantKeyToString(associatedEntity.getDaisyVariantKey());
        for (int i = 0; i < attr1.getValues().size(); i++){
          assertEquals(attr1.getValues().get(i), value);
        }
      } else {
        fail("The attribute should have been found");
      }
    } catch (MappingException e) {      
      e.printStackTrace();
      fail("This shouldn't cause an error");
    }
  }
  
  public void testGetTargetAttribute() {
    assertEquals(targetAttributeMapping, mapping.getTargetAttribute());
  }

  public void testSetTargetAttributeAfterChild() {
    assertEquals(targetAttributeMapping, mapping.getTargetAttribute());
    assertEquals(targetAttributeMapping, this.associatedAttributeMapping.getTargetAttribute());
  }
  
  public void testSetTargetAttributeBeforeChild() {
    mapping.setTargetAttribute(null);
    mapping.getChildMappings().clear();
    associatedAttributeMapping.setTargetAttribute(null);
    
    assertNull(mapping.getTargetAttribute());
    assertNull(associatedAttributeMapping.getTargetAttribute());
    
    mapping.setTargetAttribute(targetAttributeMapping);
    mapping.addChildMapping(associatedAttributeMapping);
    assertEquals(targetAttributeMapping, mapping.getTargetAttribute());
    assertEquals(targetAttributeMapping, associatedAttributeMapping.getTargetAttribute());
  }

  public void testGetChildMapping() {
    assertEquals(associatedAttributeMapping, mapping.getChildMappings().get(0));
  }

  public void testSetChildMapping() {
    assertEquals(associatedAttributeMapping, mapping.getChildMappings().get(0));
    assertEquals(targetAttributeMapping, associatedAttributeMapping.getTargetAttribute());
  }

}
