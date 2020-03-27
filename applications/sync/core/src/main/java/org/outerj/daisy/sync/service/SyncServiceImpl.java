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
package org.outerj.daisy.sync.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityNotFoundException;
import org.outerj.daisy.sync.SyncState;
import org.outerj.daisy.sync.Synchronizer;
import org.outerj.daisy.sync.SystemState;
import org.outerj.daisy.sync.dao.InternalEntityDao;
import org.outerj.daisy.sync.dao.Locker;
import org.outerj.daisy.sync.dao.SyncEntityDao;

public class SyncServiceImpl implements SyncService {
  private SyncEntityDao syncEntityDao;

  private InternalEntityDao daisyEntityDao;
  
  private Synchronizer synchronizer;
  
  private Locker locker;

  public SyncServiceImpl(Synchronizer synchronizer) {
    this.synchronizer = synchronizer;
    this.syncEntityDao = synchronizer.getSyncDao();
    this.daisyEntityDao = synchronizer.getDaisyDao();
    this.locker = synchronizer.getLocker();
  }

  public Map<String, List<Entity>> getConflicts() {
    return entityListToMap(syncEntityDao.getEntitiesByState(SyncState.CONFLICT));
  }

  public Entity getDaisyEntity(String documentId, long branchId, long languageId) throws EntityNotFoundException {
    VariantKey key = new VariantKey(documentId, branchId, languageId);
    return daisyEntityDao.getEntity(key);
  }

  public Entity getSyncEntity(String documentId, long branchId, long languageId) throws EntityNotFoundException {
    VariantKey key = new VariantKey(documentId, branchId, languageId);
    return syncEntityDao.getEntity(key);
  }

  public Map<String, List<Entity>> getDaisyDeletes() {
    return entityListToMap(syncEntityDao.getDaisyDeletedEntities());
  }

  public Map<String, List<Entity>> getDaisyOnlys() {
    return entityListToMap(syncEntityDao.getEntitiesByState(SyncState.DSY_ONLY));
  }

  public Map<String, List<Entity>> getPermanentDaisyOverrules() {
    return entityListToMap(syncEntityDao.getEntitiesByState(SyncState.CONFLICT_DSY_RULES));
  }

  private Map<String, List<Entity>> entityListToMap(List<Entity> entities) {
    Map<String, List<Entity>> nameEntityMap = new TreeMap<String, List<Entity>>();
    for (Entity entity : entities) {
      List<Entity> list = nameEntityMap.get(entity.getInternalName());
      if (list == null) {
        list = new ArrayList<Entity>();
        nameEntityMap.put(entity.getInternalName(), list);
      }
      list.add(entity);
    }
    return nameEntityMap;
  }

  public void resolveConflict(String documentId, long branchId, long languageId, SyncState resolution) throws Exception {
    VariantKey key = new VariantKey(documentId, branchId, languageId);
    Entity entity = syncEntityDao.getEntity(key);
    if (entity.getState() != SyncState.CONFLICT)
      throw new Exception("The specified entity is not in conflict");
    if (resolution == SyncState.SYNC_EXT2DSY || resolution == SyncState.DSY_OVERWRITE || resolution == SyncState.CONFLICT_DSY_RULES) {
      entity.setState(resolution);      
      if (resolution == SyncState.SYNC_EXT2DSY) {
          // sync ext values to daisy
          daisyEntityDao.storeEntity(entity);
      }
      // other resolutions need only change the sync status
    } else {
      throw new Exception("Cannot resolve conflict with " + resolution);
    }
    syncEntityDao.storeEntity(entity);
  }

  public void turnOffOverride(String documentId, long branchId, long languageId, SyncState resolution) throws Exception {
    VariantKey key = new VariantKey(documentId, branchId, languageId);
    Entity entity = syncEntityDao.getEntity(key);
    if (entity.getState() != SyncState.CONFLICT_DSY_RULES)
      throw new Exception("The specified entity is not in override mode");
    if (resolution == SyncState.SYNC_EXT2DSY || resolution == SyncState.DSY_OVERWRITE) {
      entity.setState(resolution);
    } else {
      throw new Exception("Cannot turn off override with " + resolution);
    }
    syncEntityDao.storeEntity(entity);
  }

  public void recreateDeletedDocument(String documentId, long branchId, long languageId) throws Exception{
    VariantKey key = new VariantKey(documentId, branchId, languageId);
    Entity entity = syncEntityDao.getEntity(key);
    if (!entity.isDaisyDeleted())
       throw new Exception("The cannot recreate entity which has not been deleted");
    
    try {
      Entity daisyEntity = daisyEntityDao.getEntity(key);
      if (daisyEntity == null) {
        entity.setDaisyVariantKey(null);
      }
    } catch (EntityNotFoundException e) {
      entity.setDaisyVariantKey(null);
    }    
    
    entity.setDaisyDeleted(false);
    entity.setState(SyncState.SYNC_EXT2DSY);
    daisyEntityDao.storeEntity(entity);
    syncEntityDao.replaceEntity(key, entity);    
  }

  public boolean startSynchronization() {
    return this.synchronizer.startSync(true);
  }

  public SystemState getLockState() {
    return locker.getLockState();
  }
  
  
  
  
}
