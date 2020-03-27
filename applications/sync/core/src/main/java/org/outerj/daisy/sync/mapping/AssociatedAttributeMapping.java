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

import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.mapping.EntityMapping.MappedAttribute;

public class AssociatedAttributeMapping implements AssociatedMapping {

  private String name;

  private AttributeMapping targetAttributeMapping;

  public AssociatedAttributeMapping(String attributeName) {
    this.name = attributeName;
  }

  public void applyMapping(Entity entity) throws MappingException {
    // source and target entity are one and the same
    applyMapping(entity, entity);
  }

  public void applyMapping(Entity originalEntity, Entity associatedEntity) throws MappingException {
    if (targetAttributeMapping == null)
      throw new MappingException("No target mapping specified so nowhere to map to");
    
    Attribute targetAttribute = originalEntity.getAttributeByExternalName(targetAttributeMapping.getName());
    
    if (targetAttribute == null) {
      // if it's not found then it must be created
      targetAttribute = new MappedAttribute(new AttributeImpl(targetAttributeMapping.getName(), null,
          targetAttributeMapping.getDaisyName(), targetAttributeMapping.getType()));
      originalEntity.addAttribute(targetAttribute);
    }

    Attribute sourceAttribute = associatedEntity.getAttributeByExternalName(name);
    if (sourceAttribute == null)
      throw new MappingException("Could not find source attribute " + name);

    if (sourceAttribute.getValues() != null)
      targetAttribute.addAllValues(sourceAttribute.getValues());

    if (targetAttribute instanceof MappedAttribute)
        ((MappedAttribute)targetAttribute).setMapped(true);
  }

  public AttributeMapping getTargetAttribute() {
    return targetAttributeMapping;
  }

  public void setTargetAttribute(AttributeMapping attributeMapping) {
    this.targetAttributeMapping = attributeMapping;
  }

  public String getName() {
    return name;
  }
}
