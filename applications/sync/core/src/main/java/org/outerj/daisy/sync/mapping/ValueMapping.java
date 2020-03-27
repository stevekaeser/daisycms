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
import org.outerj.daisy.sync.Entity;

public class ValueMapping implements AssociatedMapping {
  private AttributeMapping attributeMapping;
  private String value;
  
  public ValueMapping(String value) {
    this.value = value;
  }

  public void applyMapping(Entity originalEntity, Entity associatedEntity) throws MappingException {
    this.applyMapping(originalEntity);
  }

  public AttributeMapping getTargetAttribute() {
    return attributeMapping;
  }

  public void setTargetAttribute(AttributeMapping attributeMapping) {
    this.attributeMapping = attributeMapping;
  }

  public void applyMapping(Entity entity) throws MappingException {
    Attribute targetAttribute = entity.getAttributeByExternalName(attributeMapping.getName());
    if (targetAttribute == null) {
      targetAttribute = entity.getAttributeByDaisyName(attributeMapping.getDaisyName(), attributeMapping.getType());
    }
    targetAttribute.addValue(this.value);
  }

}
