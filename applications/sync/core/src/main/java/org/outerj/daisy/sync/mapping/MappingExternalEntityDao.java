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

import java.util.ArrayList;
import java.util.List;

import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.dao.ExternalEntityDao;

public class MappingExternalEntityDao implements ExternalEntityDao {
  private ExternalEntityDao delegate;
  private MappingConfiguration mappingConfiguration;

  public MappingExternalEntityDao (MappingConfiguration mappingConfiguration, ExternalEntityDao externalEntityDao) {
    this.mappingConfiguration = mappingConfiguration;
    delegate = externalEntityDao;
  }
  
  public List<Entity> getAssociatedEntities(long externalId, String associatedEntityName, String joinKey) {
    List<Entity> entities = delegate.getAssociatedEntities(externalId, associatedEntityName, joinKey);
    for (Entity entity : entities) {
      MappingHelper.mapEntity(entity, mappingConfiguration);
    }
    return entities;
  }  

  public List<Entity> getEntity(String entityName, long externalId) {
    List<Entity> entities = delegate.getEntity(entityName, externalId);
    List<Entity> mappedEntities = new ArrayList<Entity>();
    for(Entity entity : entities)
        mappedEntities.addAll(MappingHelper.mapEntity(entity, mappingConfiguration));   
    return mappedEntities;
  }

  public List<Long> getEntityIds(String entityName) {
    return delegate.getEntityIds(entityName);
  }
  
  

}
