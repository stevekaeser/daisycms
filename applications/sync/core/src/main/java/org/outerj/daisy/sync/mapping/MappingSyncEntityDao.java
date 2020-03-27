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
package org.outerj.daisy.sync.mapping;

import java.util.List;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityNotFoundException;
import org.outerj.daisy.sync.SyncState;
import org.outerj.daisy.sync.dao.SyncEntityDao;

public class MappingSyncEntityDao implements SyncEntityDao {
  private MappingConfiguration mappingConfiguration;
  private SyncEntityDao delegate;
  
  public MappingSyncEntityDao(MappingConfiguration mappingConfiguration, SyncEntityDao syncEntityDao) {
    this.mappingConfiguration = mappingConfiguration;
    this.delegate = syncEntityDao;
  }

  public List<Entity> getDaisyDeletedEntities() {
    List<Entity> entities = delegate.getDaisyDeletedEntities();
    for (Entity entity : entities) 
      MappingHelper.mapEntity(entity, mappingConfiguration);
    
    return entities;
  }

  public List<Entity> getEntitiesByState(SyncState state) {
    List<Entity> entities = delegate.getEntitiesByState(state);
    for (Entity entity : entities) 
      MappingHelper.mapEntity(entity, mappingConfiguration);
    
    return entities;
  }

  public void replaceEntity(VariantKey keyToBeReplaced, Entity replacement) {
    delegate.replaceEntity(keyToBeReplaced, replacement);
  }

  public Entity getEntity(String entityName, long externalId, String language) throws Exception {
    Entity entity = delegate.getEntity(entityName, externalId, language);
     
    List<Entity> entities = MappingHelper.mapEntity(entity, mappingConfiguration);
    if (entities.size() > 0)
        return entities.get(0);
    else 
        return null;
  }

  public Entity getEntity(VariantKey variantKey) throws EntityNotFoundException {
    Entity entity = delegate.getEntity(variantKey);
    MappingHelper.mapEntity(entity, mappingConfiguration);
    return entity;
  }

  public List<VariantKey> getEntityIds(String entityInternalName) throws Exception {
    return delegate.getEntityIds(entityInternalName);
  }

  public void storeEntity(Entity entity) {   
    delegate.storeEntity(entity);
  }

}
