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
package org.outerj.daisy.sync.service.test;

import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.outerj.daisy.sync.EntityNotFoundException;
import org.outerj.daisy.sync.SyncState;
import org.outerj.daisy.sync.Synchronizer;
import org.outerj.daisy.sync.dao.InternalEntityDao;
import org.outerj.daisy.sync.dao.Locker;
import org.outerj.daisy.sync.dao.SyncEntityDao;
import org.outerj.daisy.sync.dao.test.InMemoryLocker;
import org.outerj.daisy.sync.dao.test.MockExternalEntityDao;
import org.outerj.daisy.sync.dao.test.MockInternalEntityDao;
import org.outerj.daisy.sync.dao.test.MockSyncEntityDao;
import org.outerj.daisy.sync.mapping.MappingConfiguration;
import org.outerj.daisy.sync.service.SyncServiceImpl;

public class SyncServiceImplTest extends TestCase {
  private MappingConfiguration mappingConfiguration;

  private SyncServiceImpl syncService;

  private SyncEntityDao syncEntityDao;

  private InternalEntityDao internalEntityDao;

  private Synchronizer synchronizer;

  private Locker locker;

  private Entity entity;

  protected void setUp() throws Exception {
      
    mappingConfiguration = new MappingConfiguration(this.getClass().getClassLoader().getResourceAsStream("mapping-test.xml"), null, null);
    
    syncEntityDao = new MockSyncEntityDao();
    internalEntityDao = new MockInternalEntityDao();
    locker = new InMemoryLocker();
    synchronizer = new Synchronizer(mappingConfiguration, new MockExternalEntityDao(), syncEntityDao,
        internalEntityDao, locker);
    syncService = new SyncServiceImpl(synchronizer);

    entity = new EntityImpl();
    entity.setExternalId(444);
    entity.setDaisyVariantKey(new VariantKey("1-DSY", 1, 1));
    entity.setName("somename");
    entity.setInternalName("myinternalname");
    entity.setLanguage("thelang");

    syncEntityDao.storeEntity(entity);

    super.setUp();
  }

  public void testGetConflicts() {
    Map<String, List<Entity>> list = syncService.getConflicts();
    assertTrue(list.isEmpty());
    Entity conflictingEntity = new EntityImpl();
    conflictingEntity.setExternalId(445);
    conflictingEntity.setName("conflictcase");
    conflictingEntity.setInternalName("conflictcaseinternal");
    conflictingEntity.setState(SyncState.CONFLICT);
    syncEntityDao.storeEntity(conflictingEntity);

    list = syncService.getConflicts();
    assertTrue(list.containsKey("conflictcaseinternal"));
  }

  public void testGetDaisyEntity() {
    Entity dsyEntity = new EntityImpl();
    dsyEntity.setExternalId(444);
    dsyEntity.setDaisyVariantKey(new VariantKey("1-DSY", 1, 1));
    dsyEntity.setName("somename");
    dsyEntity.setInternalName("myinternalname");
    internalEntityDao.storeEntity(dsyEntity);

    VariantKey key = dsyEntity.getDaisyVariantKey();
    Entity result = null;
    try {
      result = syncService.getDaisyEntity(key.getDocumentId(), key.getBranchId(), key.getLanguageId());
    } catch (EntityNotFoundException e) {
      e.printStackTrace();
      fail("This shouldn't cause exceptions");
    }
    assertEquals(dsyEntity.getDaisyVariantKey(), result.getDaisyVariantKey());
  }

  public void testGetSyncEntity() {
    VariantKey key = entity.getDaisyVariantKey();
    Entity result = null;
    try {
      result = syncService.getSyncEntity(key.getDocumentId(), key.getBranchId(), key.getLanguageId());
    } catch (EntityNotFoundException e) {
      e.printStackTrace();
      fail("This shouldn't cause exceptions");
    }
    assertEquals(entity.getDaisyVariantKey(), result.getDaisyVariantKey());
  }

  public void testGetDaisyDeletes() {
    Entity delent = null;
    try {
      delent = syncEntityDao.getEntity(new VariantKey("1-DSY", 1, 1));
    } catch (EntityNotFoundException e) {
      e.printStackTrace();
      fail("This shouldn't cause exceptions");
    }
    assertTrue(syncService.getDaisyDeletes().isEmpty());
    delent.setDaisyDeleted(true);
    assertTrue(syncService.getDaisyDeletes().containsKey(delent.getInternalName()));
  }

  public void testGetDaisyOnlys() {
    assertTrue(syncService.getDaisyOnlys().isEmpty());
    Entity dsyonly = null;
    try {
      dsyonly = syncEntityDao.getEntity(new VariantKey("1-DSY", 1, 1));
    } catch (EntityNotFoundException e) {
      e.printStackTrace();
      fail("This shouldn't cause exceptions");
    }
    dsyonly.setState(SyncState.DSY_ONLY);
    assertTrue(syncService.getDaisyOnlys().containsKey(dsyonly.getInternalName()));
  }

  public void testGetPermanentDaisyOverrules() {
    assertTrue(syncService.getPermanentDaisyOverrules().isEmpty());
    Entity dsyoverrule = null;
    try {
      dsyoverrule = syncEntityDao.getEntity(new VariantKey("1-DSY", 1, 1));
    } catch (EntityNotFoundException e) {
      e.printStackTrace();
      fail("This shouldn't cause exceptions");
    }
    dsyoverrule.setState(SyncState.CONFLICT_DSY_RULES);
    assertTrue(syncService.getPermanentDaisyOverrules().containsKey(dsyoverrule.getInternalName()));
  }

  public void testResolveConflictEXT() {
    Entity conflictingEntity = setupConflict();
    VariantKey key = conflictingEntity.getDaisyVariantKey();
    try {
      syncService.resolveConflict(key.getDocumentId(), key.getBranchId(), key.getLanguageId(), SyncState.SYNC_EXT2DSY);
    } catch (Exception e) {
      e.printStackTrace();
      fail("This may not cause an exception");
    }
    assertTrue(syncService.getConflicts().isEmpty());

    try {
      assertTrue(syncEntityDao.getEntity(key).getState() == SyncState.SYNC_EXT2DSY);
    } catch (EntityNotFoundException e) {
      e.printStackTrace();
      fail("This shouldn't cause exceptions");
    }
  }

  public void testResolveConflictDSY() {
    Entity conflictingEntity = setupConflict();
    VariantKey key = conflictingEntity.getDaisyVariantKey();
    try {
      syncService.resolveConflict(key.getDocumentId(), key.getBranchId(), key.getLanguageId(), SyncState.DSY_OVERWRITE);

      assertTrue(syncService.getConflicts().isEmpty());
      assertTrue(syncEntityDao.getEntity(key).getState() == SyncState.DSY_OVERWRITE);
      assertTrue(!syncEntityDao.getEntity(key).getAttributeByExternalName("extAttrName").getValues().get(0).equals(
          internalEntityDao.getEntity(key).getAttributeByExternalName("extAttrName").getValues().get(0)));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Cannot cause an exception");
    }
  }

  public void testResolveConflictDSYRULES() {
    Entity conflictingEntity = setupConflict();
    VariantKey key = conflictingEntity.getDaisyVariantKey();
    try {
      syncService.resolveConflict(key.getDocumentId(), key.getBranchId(), key.getLanguageId(),
          SyncState.CONFLICT_DSY_RULES);

      assertTrue(syncService.getConflicts().isEmpty());
      assertTrue(syncEntityDao.getEntity(key).getState() == SyncState.CONFLICT_DSY_RULES);
      assertTrue(!syncEntityDao.getEntity(key).getAttributeByExternalName("extAttrName").getValues().get(0).equals(
          internalEntityDao.getEntity(key).getAttributeByExternalName("extAttrName").getValues().get(0)));
    } catch (Exception e) {
      e.printStackTrace();
      fail("Cannot cause an exception");
    }
  }

  public void testResolveConflictBOGUS() {
    Entity conflictingEntity = setupConflict();
    VariantKey key = conflictingEntity.getDaisyVariantKey();
    try {
      syncService.resolveConflict(key.getDocumentId(), key.getBranchId(), key.getLanguageId(), SyncState.DSY_ONLY);
      fail("Should cause an exception");
    } catch (Exception e) {
    }
  }

  public void testRecreateDocument() {
    VariantKey key = entity.getDaisyVariantKey();
    try {        
      Entity deletedEntity = syncEntityDao.getEntity(key);
      deletedEntity.setDaisyDeleted(true);
      try {
          internalEntityDao.getEntity(entity.getDaisyVariantKey());
          fail("This should throw an exception");
      } catch (EntityNotFoundException e) {
          // nothing
      }
      

      Entity internalEntity = internalEntityDao.getEntity(deletedEntity.getInternalName(), deletedEntity.getExternalId(), deletedEntity.getLanguage());
      assertNull(internalEntity);
      syncService.recreateDeletedDocument(key.getDocumentId(), key.getBranchId(), key.getLanguageId());
      internalEntity = internalEntityDao.getEntity(deletedEntity.getInternalName(), deletedEntity.getExternalId(), deletedEntity.getLanguage());
      assertNotNull(internalEntity);
      // check if a different key has been created 
      try {
          internalEntityDao.getEntity(key);
          fail("This should throw an exception");
      } catch (EntityNotFoundException e) {
          // nothing
      }
    } catch (Exception e) {
      e.printStackTrace();
      fail("This should not cause an exception");
    }
  }

  private Entity setupConflict() {
    Map<String, List<Entity>> list = syncService.getConflicts();
    assertTrue(list.isEmpty());
    Entity conflictingEntity = new EntityImpl();
    conflictingEntity.setExternalId(445);
    conflictingEntity.setDaisyVariantKey(new VariantKey("1-DSY", 1, 1));
    conflictingEntity.setName("conflictcase");
    conflictingEntity.setInternalName("conflictcaseinternal");
    conflictingEntity.setState(SyncState.CONFLICT);
    Attribute attr = new AttributeImpl("extAttrName", "extValue", "intAttrName", AttributeType.FIELD);
    conflictingEntity.addAttribute(attr);
    syncEntityDao.storeEntity(conflictingEntity);
    attr.getValues().clear();
    attr.addValue("otherValue");
    this.internalEntityDao.storeEntity(conflictingEntity);

    list = syncService.getConflicts();
    assertTrue(list.containsKey("conflictcaseinternal"));

    return conflictingEntity;
  }

}
