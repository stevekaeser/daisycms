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

import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.VariantHelper;
import org.outerj.daisy.sync.dao.ExternalEntityDao;
import org.outerj.daisy.sync.dao.SyncEntityDao;
import org.outerj.daisy.sync.mapping.EntityMapping.MappedAttribute;

public class AssociatedEntityMapping extends AbstractAssociatedMapping {
    private String entityName;

    private String joinKey;

    private String joinParentKey;

    private List<AssociatedMapping> childMappings = new ArrayList<AssociatedMapping>();

    private AttributeMapping targetAttribute;

    public AssociatedEntityMapping(String entityName, String joinKey, String joinParentKey, ExternalEntityDao externalEntityDao, SyncEntityDao syncEntityDao, MappingConfiguration configuration) {
        super(externalEntityDao, syncEntityDao, configuration);
        this.entityName = entityName;
        this.joinKey = joinKey;
        this.joinParentKey = joinParentKey;        
    }

    public void applyMapping(Entity originalEntity) throws MappingException {
        applyMapping(originalEntity, originalEntity);
    }
 
    // joinParentKey here then this object should make use of the associatedEntity to fetch the key 
    public void applyMapping(Entity originalEntity, Entity associatedEntity) throws MappingException {        
        List<Entity> associatedEntities = null;
        if (joinKey != null) {
            long extId = associatedEntity.getExternalId();
            associatedEntities = externalEntityDao.getAssociatedEntities(extId, entityName, joinKey);
        } else if (joinParentKey != null) {
            Attribute associatedAttr = associatedEntity.getAttributeByExternalName(joinParentKey);
            associatedEntities = new ArrayList<Entity>();
            if (associatedAttr != null) {            
                for (String joinValue : associatedAttr.getValues()) {
                    List<Entity> entities = externalEntityDao.getEntity(entityName, Long.parseLong(joinValue));
                    if (entities != null)
                        associatedEntities.addAll(entities);
                }
            }
        }

        for (Entity entity : associatedEntities) {
            if (this.childMappings.size() > 0) {
                for (AssociatedMapping childMapping : this.childMappings)            
                    childMapping.applyMapping(originalEntity, entity);
            } else {
                // if this mapping doesn't have any children get the daisy id
                // and store it in the attribute
                // TODO check if entity exists first
                try {
                    String internalEntityName = entity.getInternalName();
                    if (internalEntityName == null)
                        internalEntityName = configuration.getEntityMappingByEntityName(entity.getName()).getDaisyDocumentTypeName();
                                      
                    Entity sourceEntity = syncEntityDao.getEntity(internalEntityName, entity.getExternalId(), originalEntity.getLanguage());
                    
                    Attribute attribute = originalEntity.getAttributeByExternalName(targetAttribute.getName());
                    if (attribute == null) {
                        // if it's not found then it must be created
                        attribute = new AttributeImpl(targetAttribute.getName(), null, targetAttribute.getDaisyName(), targetAttribute.getType());
                        originalEntity.addAttribute(attribute);
                    }
                    
                    if (sourceEntity !=  null)
                        attribute.addValue(VariantHelper.variantKeyToString(sourceEntity.getDaisyVariantKey()));
                    else 
                        logger.logp(Level.WARNING, this.getClass().getName(), "applyMapping", "Could not map associated entity ({0} - {1}) since the source entity could not be found", new Object[]{entity.getName(), entity.getExternalId()});
                    if (attribute instanceof MappedAttribute)
                        ((MappedAttribute) attribute).setMapped(true);
                } catch (Exception e) {
                    logger.logp(Level.SEVERE, this.getClass().getName(), "applyMapping", "Error setting associated mapping for entity (" + entity.getName() + " - " + entity.getExternalId() + ")", e);
                }
            }
        }

    }

    public AttributeMapping getTargetAttribute() {
        return targetAttribute;
    }

    public void setTargetAttribute(AttributeMapping attributeMapping) {
        this.targetAttribute = attributeMapping;
        for (AssociatedMapping childMapping : this.childMappings)
            childMapping.setTargetAttribute(attributeMapping);
    }

    public List<AssociatedMapping> getChildMappings() {
        return childMappings;
    }

    public void setChildMappings(List<AssociatedMapping> childMappings) {
        this.childMappings = childMappings;
        if (targetAttribute != null){
            for (AssociatedMapping childMapping : this.childMappings)
                childMapping.setTargetAttribute(targetAttribute);
        }
    }
    
    public void addChildMapping(AssociatedMapping mapping) {
        this.childMappings.add(mapping);
        if (this.targetAttribute != null) {
            mapping.setTargetAttribute(this.targetAttribute);
        } 
      }

    public String getJoinKey() {
        return joinKey;
    }

    public String getJoinParentKey() {
        return joinParentKey;
    }

    public String getEntityName() {
        return entityName;
    }
}
