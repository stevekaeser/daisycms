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
package org.outerj.daisy.sync.dao;

import java.util.List;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityNotFoundException;

public interface InternalEntityDao {
    void storeEntity(Entity entity);

    List<VariantKey> getEntityIds(String entityInternalName) throws Exception;

    Entity getEntity(VariantKey variantKey) throws EntityNotFoundException;

    Entity getEntity(String entityName, long externalId, String language) throws Exception;
}
