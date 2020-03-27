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
import java.util.Random;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityNotFoundException;
import org.outerj.daisy.sync.dao.InternalEntityDao;

public class MockInternalEntityDao implements InternalEntityDao {

    protected Map<VariantKey, Entity> entities = new HashMap<VariantKey, Entity>();

    private Random random = new Random();

    public void storeEntity(Entity entity) {
        if (entity.getDaisyVariantKey() == null) {
            entity.setDaisyVariantKey(createVariantKey());
            entity.setDaisyVersion(1);
        } else {
            entity.setDaisyVersion(entity.getDaisyVersion() + 1);
        }
        Entity clone = entity.clone();

        entities.put(clone.getDaisyVariantKey(), clone);
    }

    public List<VariantKey> getEntityIds(String entityName) throws Exception {
        List<VariantKey> keys = new ArrayList<VariantKey>();
        for (Entity entity : entities.values()) {
            if (entity.getInternalName() != null && entity.getInternalName().equals(entityName) && !entity.isDaisyDeleted())
                keys.add(entity.getDaisyVariantKey());
        }
        return keys;
    }

    public Entity getEntity(String entityName, long externalId, String language) throws Exception {        
        for (Entity entity : entities.values()) {
            if (entity.getInternalName() != null && entity.getInternalName().equals(entityName) && entity.getExternalId() == externalId && entity.getLanguage().equals(language) && !entity.isDaisyDeleted())
                return entity;
        }
        return null;
    }

    public Entity getEntity(VariantKey variantKey) throws EntityNotFoundException {
        Entity entity = entities.get(variantKey);
        if (entity == null) {
            throw new EntityNotFoundException("Could not find entity " + variantKey);
        }
        
        return entity;
    }

    public void removeEntity(VariantKey variantKey) {
        entities.remove(variantKey);
    }

    private VariantKey createVariantKey() {
        return new VariantKey(random.nextLong() + "-TST", 1, 1);
    }
}
