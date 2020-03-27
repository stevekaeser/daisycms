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
package org.outerj.daisy.sync.test;

import java.util.List;

import junit.framework.TestCase;

import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.outerj.daisy.sync.EntityNotFoundException;
import org.outerj.daisy.sync.SyncState;
import org.outerj.daisy.sync.Synchronizer;
import org.outerj.daisy.sync.SystemState;
import org.outerj.daisy.sync.dao.Locker;
import org.outerj.daisy.sync.dao.test.InMemoryLocker;
import org.outerj.daisy.sync.dao.test.MockExternalEntityDao;
import org.outerj.daisy.sync.dao.test.MockInternalEntityDao;
import org.outerj.daisy.sync.dao.test.MockSyncEntityDao;
import org.outerj.daisy.sync.mapping.MappingConfiguration;

public class SynchronizerTest extends TestCase {
  private MappingConfiguration mappingConfiguration;

  private MockExternalEntityDao externalDao;

  private MockSyncEntityDao syncDao;

  private MockInternalEntityDao daisyDao;

  private Synchronizer synchronizer;

  private Entity extEntity;

  private Locker locker;
  
  protected void setUp() throws Exception {
      
    mappingConfiguration = new MappingConfiguration(this.getClass().getClassLoader().getResourceAsStream("mapping-test.xml"), null, null);
    
    externalDao = new MockExternalEntityDao();
    syncDao = new MockSyncEntityDao();
    locker = new InMemoryLocker();
    daisyDao = new MockInternalEntityDao();

    synchronizer = new Synchronizer(mappingConfiguration, externalDao, syncDao, daisyDao, locker);

    String entityName = "plainEntity";
    long extId = 5579;
    extEntity = new EntityImpl();
    extEntity.setName(entityName);
    extEntity.setInternalName("SimpleDocument");
    extEntity.setExternalId(extId);
    extEntity.addAttribute(new AttributeImpl("attributeOne", "one value here"));
    extEntity.addAttribute(new AttributeImpl("entityName", "this is my name"));
    extEntity.setLanguage("mylang");

    externalDao.addEntity(extEntity);

    super.setUp();
  }

  // only the syncing is tested here not the mapping
  public void testNewEntity() throws EntityNotFoundException{
    // CASE 1

    assertTrue(synchronizer.startSync(true));

    waitForLock();

    Entity syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());

    assertNotNull(extEntity.getDaisyVariantKey());
    assertEquals(SyncState.SYNC_EXT2DSY, syncEntity.getState());
  }

  public void testEntityUpdateExternal() throws EntityNotFoundException{
    // CASE 2, 5, 11

    synchronizer.startSync(true);
    waitForLock();

    assertNotNull(extEntity.getDaisyVariantKey());

    List<String> values = extEntity.getAttributeByExternalName("attributeOne").getValues();
    values.clear();
    values.add("a new value");

    assertNotSame(extEntity, syncDao.getEntity(extEntity.getDaisyVariantKey()));

    synchronizer.startSync(true);
    waitForLock();

    Entity dsyEntity = daisyDao.getEntity(extEntity.getDaisyVariantKey());
    Entity syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());

    assertEquals(extEntity.getAttributeByExternalName("attributeOne").getValues().get(0), syncEntity
        .getAttributeByExternalName("attributeOne").getValues().get(0));
    assertEquals(extEntity.getAttributeByExternalName("attributeOne").getValues().get(0), dsyEntity
        .getAttributeByExternalName("attributeOne").getValues().get(0));

  }

  public void testEntityUpdateInternal() throws EntityNotFoundException {
    // CASE 3, 4, 11

    synchronizer.startSync(true);
    waitForLock();

    assertNotNull(extEntity.getDaisyVariantKey());

    Entity dsyEntity = daisyDao.getEntity(extEntity.getDaisyVariantKey());

    List<String> values = dsyEntity.getAttributeByExternalName("attributeOne").getValues();
    values.clear();
    values.add("a new value -- from daisy");

    daisyDao.storeEntity(dsyEntity);

    assertNotSame(dsyEntity, syncDao.getEntity(extEntity.getDaisyVariantKey()));

    synchronizer.startSync(true);
    waitForLock();

    Entity syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());

    assertEquals(SyncState.DSY_OVERWRITE, syncEntity.getState());
    // the sync holds on to external values in an overwrite
    assertEquals(syncEntity.getAttributeByExternalName("attributeOne").getValues().get(0), this.extEntity
        .getAttributeByExternalName("attributeOne").getValues().get(0));

  }

  public void testEntityDeleteInternal() throws EntityNotFoundException {
    // CASE 12

    synchronizer.startSync(true);
    waitForLock();

    assertNotNull(extEntity.getDaisyVariantKey());

    daisyDao.removeEntity(extEntity.getDaisyVariantKey());

    synchronizer.startSync(true);
    waitForLock();

    Entity syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());

    assertTrue(syncEntity.isDaisyDeleted());
    assertFalse(syncEntity.isExternalDeleted());
    assertEquals(SyncState.DSY_OVERWRITE, syncEntity.getState());
  }
  
  public void testEntityDeleteExternal() throws EntityNotFoundException{
    synchronizer.startSync(true);
    waitForLock();
    
    Entity syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());

    assertFalse(syncEntity.isDaisyDeleted());
    assertFalse(syncEntity.isExternalDeleted());

    assertNotNull(extEntity.getDaisyVariantKey());

    externalDao.getNamedEntities().get(extEntity.getName()).remove(extEntity);

    synchronizer.startSync(true);
    waitForLock();

    syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());

    assertTrue(syncEntity.isDaisyDeleted());
    assertTrue(syncEntity.isExternalDeleted());
  }

  public void testStartSync() {
    // LockState : IDLE
    assertFalse(synchronizer.startSync());
    waitForLock();
    assertTrue(synchronizer.startSync(true));
    waitForLock();
    assertTrue(locker.changeLockState(SystemState.IDLE, SystemState.EXT_UPDATE));
    
    assertFalse(synchronizer.startSync());
    waitForLock();
    assertFalse(synchronizer.startSync(true));
    waitForLock();

    assertTrue(locker.changeLockState( SystemState.EXT_UPDATE, SystemState.AWAITING_SYNC));
    assertTrue(synchronizer.startSync());
    waitForLock();
    assertTrue(synchronizer.startSync(true));
    
  }
  
  public void testEntityResurrect() throws EntityNotFoundException{
    createResurrectScenario();
    
    synchronizer.startSync(true);
    waitForLock();

    Entity syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());
    Entity dsyEntity = daisyDao.getEntity(syncEntity.getDaisyVariantKey());
    
    assertFalse(syncEntity.isDaisyDeleted());
    assertFalse(syncEntity.isExternalDeleted());
    assertFalse(dsyEntity.isDaisyDeleted());
    
  }
  
  public void testEntityResurrectExtChange() throws EntityNotFoundException {
      createResurrectScenario();
      
      String newValue = "after resurect value";
      Attribute attr = extEntity.getAttributeByExternalName("attributeOne");
      attr.getValues().clear();
      attr.addValue(newValue);
      
      externalDao.addEntity(extEntity);
      
      synchronizer.startSync(true);
      waitForLock();

      Entity syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());
      Entity dsyEntity = daisyDao.getEntity(syncEntity.getDaisyVariantKey());
      
      assertFalse(syncEntity.isDaisyDeleted());
      assertFalse(syncEntity.isExternalDeleted());
      assertFalse(dsyEntity.isDaisyDeleted());
      
      assertEquals(syncEntity.getAttributeByExternalName("attributeOne").getValues().get(0), newValue);
      assertEquals(dsyEntity.getAttributeByExternalName("attributeOne").getValues().get(0), newValue);
      
  }
  
  public void testEntityResurrectDsyChange() throws EntityNotFoundException {
      createResurrectScenario();
      
      Entity dsyEntity = daisyDao.getEntity(extEntity.getDaisyVariantKey());
      String newValue = "change daisy value";
      Attribute attr = dsyEntity.getAttributeByExternalName("attributeOne");
      attr.getValues().clear();
      attr.addValue(newValue);
      dsyEntity.setDaisyDeleted(false);
      
      synchronizer.startSync(true);
      waitForLock();

      Entity syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());
      dsyEntity = daisyDao.getEntity(syncEntity.getDaisyVariantKey());
      
      assertEquals(SyncState.CONFLICT, syncEntity.getState());
      
      assertTrue(syncEntity.isDaisyDeleted()); // this shouldn't change untill the conflict has been resolved
      assertFalse(syncEntity.isExternalDeleted());
      assertFalse(dsyEntity.isDaisyDeleted());
      
      // the sync holds on to external values in an overwrite
      assertEquals(syncEntity.getAttributeByExternalName("attributeOne").getValues().get(0), this.extEntity
          .getAttributeByExternalName("attributeOne").getValues().get(0));
      
  }
  
  public void testEntityResurrectDsyUnretire() throws EntityNotFoundException{
      createResurrectScenario();
      
      Entity dsyEntity = daisyDao.getEntity(extEntity.getDaisyVariantKey());
      dsyEntity.setDaisyDeleted(false);
      
      synchronizer.startSync(true);
      waitForLock();

      Entity syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());
      dsyEntity = daisyDao.getEntity(syncEntity.getDaisyVariantKey());
      
      assertEquals(SyncState.SYNC_EXT2DSY, syncEntity.getState());
      
      assertFalse(syncEntity.isDaisyDeleted());
      assertFalse(syncEntity.isExternalDeleted());
      assertFalse(dsyEntity.isDaisyDeleted());
      
  }
  
  public void testEntityResurrectDsyHardDelete() throws EntityNotFoundException{
      createResurrectScenario();
      // when an entity is resurrected it will bring back the daisy document too.
      daisyDao.removeEntity(extEntity.getDaisyVariantKey());
      
      try {
          daisyDao.getEntity(extEntity.getDaisyVariantKey());
          fail("this should give an exception since the entity doesn't exist anymore.");
      } catch (EntityNotFoundException e) {
          // do nothing
      }
      
      synchronizer.startSync(true);
      waitForLock();

      Entity syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());
      Entity dsyEntity = daisyDao.getEntity(syncEntity.getDaisyVariantKey());
      
      assertNotNull(daisyDao.getEntity(extEntity.getDaisyVariantKey()));
      
      assertEquals(SyncState.SYNC_EXT2DSY, syncEntity.getState());
      
      assertFalse(syncEntity.isDaisyDeleted());
      assertFalse(syncEntity.isExternalDeleted());
      assertFalse(dsyEntity.isDaisyDeleted());
      
  }
  
  


  private void waitForLock() {
    try {
      Thread.sleep(100);
     
      while (locker.getLockState() == SystemState.SYNCING) {

        Thread.sleep(100);
        
      }
     
    } catch (InterruptedException e) {
      e.printStackTrace();
      fail("What happend here");
    }
  }
  
  private void createResurrectScenario() throws EntityNotFoundException{
      synchronizer.startSync(true);
      waitForLock();
      
      Entity syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());

      assertFalse(syncEntity.isDaisyDeleted());
      assertFalse(syncEntity.isExternalDeleted());

      assertNotNull(extEntity.getDaisyVariantKey());

      externalDao.getNamedEntities().get(extEntity.getName()).remove(extEntity);

      synchronizer.startSync(true);
      waitForLock();

      syncEntity = syncDao.getEntity(extEntity.getDaisyVariantKey());
      Entity dsyEntity = daisyDao.getEntity(syncEntity.getDaisyVariantKey());

      assertTrue(syncEntity.isDaisyDeleted());
      assertTrue(syncEntity.isExternalDeleted());
      assertTrue(dsyEntity.isDaisyDeleted());
      
      externalDao.addEntity(extEntity);
  }
}
