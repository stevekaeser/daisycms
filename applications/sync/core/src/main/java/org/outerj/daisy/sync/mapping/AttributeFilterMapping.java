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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.Entity;

public class AttributeFilterMapping implements AssociatedMapping{
  private String attributeName;
  private Pattern pattern;
  private AttributeMapping targetAttributeMapping;
  private List<Mapping> mappings = new ArrayList<Mapping>();
  
  public AttributeFilterMapping(String attributeName, String matchString){
    this.attributeName = attributeName;
    this.pattern = Pattern.compile(matchString);
  }
  
  public void addChildMapping(Mapping mapping) {
    mappings.add(mapping);
    if (targetAttributeMapping != null && mapping instanceof AssociatedMapping)
      ((AssociatedMapping)mapping).setTargetAttribute(targetAttributeMapping);
  }

  public void applyMapping(Entity entity) throws MappingException {    
    if (matchesPattern(entity)) {
      for (Mapping mapping : mappings) {
        mapping.applyMapping(entity);
      }
    }
  }

  public void applyMapping(Entity originalEntity, Entity associatedEntity) throws MappingException {
    //this.applyMapping(associatedEntity);
    if (matchesPattern(associatedEntity)) {
        for (Mapping mapping : mappings) {
            if (mapping instanceof AssociatedMapping) {
                AssociatedMapping associatedMapping = (AssociatedMapping) mapping;
                associatedMapping.applyMapping(originalEntity, associatedEntity);
            } else {
                mapping.applyMapping(associatedEntity);
            }
        }
    }
  }
  
  private boolean matchesPattern(Entity entity) {
      Attribute filterAttribute = entity.getAttributeByExternalName(attributeName);
      boolean matched = false;
      if (filterAttribute != null) {
        for(String value : filterAttribute.getValues()){
          Matcher matcher = pattern.matcher(value);
          if (matcher.matches()) {
            matched = true;
            break;
          }
        }      
      }
      return matched;
  }

  public AttributeMapping getTargetAttribute() {
    return targetAttributeMapping;
  }
  
  public void setTargetAttribute(AttributeMapping attributeMapping) {
    this.targetAttributeMapping = attributeMapping;
    for (Mapping mapping : this.mappings) {
      if (mapping instanceof AssociatedMapping) {
        AssociatedMapping associatedMapping = (AssociatedMapping) mapping;
        associatedMapping.setTargetAttribute(attributeMapping);        
      }
    }
    
  }

  public String getAttributeName() {
    return attributeName;
  }

  public Pattern getPattern() {
    return pattern;
  }

  public List<Mapping> getMappings() {
    return mappings;
  }
  
  
}
