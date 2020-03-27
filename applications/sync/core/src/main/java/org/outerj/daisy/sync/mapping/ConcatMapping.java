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

import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.mapping.EntityMapping.MappedAttribute;

public class ConcatMapping implements AssociatedMapping {

    private AttributeMapping targetAttributeMapping;
    private List<Mapping> mappings = new ArrayList<Mapping>();

    public void applyMapping(Entity originalEntity, Entity associatedEntity) throws MappingException {
        Attribute targetAttribute = originalEntity.getAttributeByExternalName(targetAttributeMapping.getName());

        if (targetAttribute == null) {
            // if it's not found then it must be created
            targetAttribute = new MappedAttribute(new AttributeImpl(targetAttributeMapping.getName(), null, targetAttributeMapping.getDaisyName(),
                    targetAttributeMapping.getType()));
            targetAttribute.setValues(new ArrayList<String>());
            originalEntity.addAttribute(targetAttribute);
        }

        Entity dummyEntity = originalEntity.clone();
        Attribute attr = dummyEntity.getAttributeByDaisyName(targetAttributeMapping.getDaisyName(), targetAttributeMapping.getType());
        if (attr.getValues() != null) {
            attr.getValues().clear();
        }

        for (Mapping mapping : mappings) {
            if (mapping instanceof AssociatedMapping) {
                AssociatedMapping associatedMapping = (AssociatedMapping) mapping;
                associatedMapping.applyMapping(dummyEntity, associatedEntity);
            } else {
                mapping.applyMapping(dummyEntity);
            }
        }

        StringBuilder concatenatedValue = new StringBuilder();
        for (String val : attr.getValues()) {
            concatenatedValue.append(val);
        }

        targetAttribute.addValue(concatenatedValue.toString());

    }

    public AttributeMapping getTargetAttribute() {
        return this.targetAttributeMapping;
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

    public void applyMapping(Entity mappedObject) throws MappingException {
        this.applyMapping(mappedObject, mappedObject);
    }

    public List<Mapping> getChildMappings() {
        return mappings;
    }

    public void addChildMapping(Mapping mapping) {
        this.mappings.add(mapping);
        if (targetAttributeMapping != null && mapping instanceof AssociatedMapping)
            ((AssociatedMapping) mapping).setTargetAttribute(targetAttributeMapping);
    }

}
