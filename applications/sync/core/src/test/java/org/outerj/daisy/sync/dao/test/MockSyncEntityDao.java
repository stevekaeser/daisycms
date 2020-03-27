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
package org.outerj.daisy.sync.dao.test;

import java.util.ArrayList;
import java.util.List;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.SyncState;
import org.outerj.daisy.sync.dao.SyncEntityDao;

public class MockSyncEntityDao extends MockInternalEntityDao implements SyncEntityDao {

  public void storeEntity(Entity entity) {
    Entity clone = entity.clone();
    // name is not stored in the sync store
    //clone.setName(null);
    
    entities.put(clone.getDaisyVariantKey(), clone);
  }

  public List<Entity> getEntitiesByState(SyncState state) {
    List<Entity> list = new ArrayList<Entity>();
    for (Entity entity : this.entities.values()) {
      if (entity.getState() == state)
        list.add(entity);
    }
    return list;
  }

  public List<Entity> getDaisyDeletedEntities() {
    List<Entity> list = new ArrayList<Entity>();
    for (Entity entity : this.entities.values()) {
      if (entity.isDaisyDeleted())
        list.add(entity);
    }
    return list;

  }

  public void replaceEntity(VariantKey key, Entity replacement) {    
    this.entities.remove(key);
    storeEntity(replacement);    
  }


    public List<VariantKey> getEntityIds(String entityName) throws Exception {
        List<VariantKey> keys = new ArrayList<VariantKey>();
        for (Entity entity : entities.values()) {
            if (entity.getInternalName() != null && entity.getInternalName().equals(entityName))
                keys.add(entity.getDaisyVariantKey());
        }
        return keys;
    }
    
    public Entity getEntity(String entityName, long externalId, String language) throws Exception {        
        for (Entity entity : entities.values()) {
            if (entity.getInternalName() != null && entity.getInternalName().equals(entityName) && entity.getExternalId() == externalId && entity.getLanguage().equals(language))
                return entity;
        }
        return null;
    }
}