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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.dao.ExternalEntityDao;

public class MockExternalEntityDao implements ExternalEntityDao {
  private Map<String, List<Entity>> namedEntities = new HashMap<String, List<Entity>>();

  public void addEntity(Entity entity) {
    List<Entity> list = namedEntities.get(entity.getName());
    if (list == null) {
      list = new ArrayList<Entity>();
      namedEntities.put(entity.getName(), list);
    }
    list.add(entity);
  }

  public List<Entity> getAssociatedEntities(long externalId, String associatedEntityName, String joinKey) {
    List<Entity> list = new ArrayList<Entity>();
    if (namedEntities.containsKey(associatedEntityName)){
      for (Entity entity : namedEntities.get(associatedEntityName)) {
        Attribute attr = entity.getAttributeByExternalName(joinKey);
        if (attr != null) {
          if (attr.getValues().contains(Long.toString(externalId))) {
            list.add(entity);
          }
        }
      }
    }
    return list;
  }

  public List<Entity> getEntity(String entityName, long externalId) {
      List<Entity> foundEntities = new ArrayList<Entity>();
      if (namedEntities.containsKey(entityName)) {
        for (Entity entity : namedEntities.get(entityName)) {
          if (externalId == entity.getExternalId())
            foundEntities.add(entity);
        }
      }
    return foundEntities;
  }

  public List<Long> getEntityIds(String entityName) {
    List<Long> ids = new ArrayList<Long>();
    if (namedEntities.containsKey(entityName)){
      for (Entity entity : namedEntities.get(entityName)) {
        ids.add(entity.getExternalId());
      }
    }
    return ids;
  }

  public Map<String, List<Entity>> getNamedEntities() {
    return namedEntities;
  }

  public void setNamedEntities(Map<String, List<Entity>> namedEntities) {
    this.namedEntities = namedEntities;
  }

}
