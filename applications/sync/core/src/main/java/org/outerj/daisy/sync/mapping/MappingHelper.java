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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.outerj.daisy.sync.Entity;

public class MappingHelper {
    private static Logger logger = Logger.getLogger("org.outerj.daisy.sync.mapping");
    protected static List<Entity> mapEntity(Entity entity, MappingConfiguration mappingConfiguration) {
        EntityMapping entityMapping = null;
        List<Entity> entities = new ArrayList<Entity>();
        if (entity != null) {
            if (entity.getName() != null) {
                entityMapping = mappingConfiguration.getEntityMappingByEntityName(entity.getName());
            } else if (entity.getInternalName() != null) {
                entityMapping = mappingConfiguration.getEntityMappingByDocumentTypeName(entity.getInternalName());
            } else {
                logger.warning("No name entries cannot be mapped");                
            }

            if (entityMapping != null) {
                try {
                    entities.addAll(entityMapping.mapEntity(entity));
                } catch (MappingException e) {
                    logger.logp(Level.WARNING, "org.outerj.daisy.sync.mapping.MappingHelper", "mapEntity", "Could not map entity " + entity.getName() + " - " + entity.getExternalId(), e);
                }
            } else  {
                logger.warning("Entity mapping could not be found for entity " + entity.getName() + " - " + entity.getInternalName());
            }
        }
        return entities;
    }
}
