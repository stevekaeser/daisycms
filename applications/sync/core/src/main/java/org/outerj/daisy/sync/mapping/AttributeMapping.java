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
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.mapping.EntityMapping.MappedAttribute;
import org.outerj.daisy.sync.mapping.EntityMapping.MappedEntity;

public class AttributeMapping implements Mapping {
    private String name;

    private String daisyName;

    private AttributeType type;

    private boolean isMultivalue = false;

    private List<AssociatedMapping> childMappings = new ArrayList<AssociatedMapping>();

    public String getDaisyName() {
        return daisyName;
    }

    public void setDaisyName(String daisyName) {
        this.daisyName = daisyName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) throws MappingException {
        if (isMultivalue && type != AttributeType.FIELD)
            throw new MappingException("Only attributes of the " + AttributeType.FIELD.toString() + " may be multivalued");

        this.type = type;
    }

    public void applyMapping(Entity entity) throws MappingException {
        Attribute attribute = entity.getAttributeByExternalName(getName());
        if (attribute == null) {
            attribute = entity.getAttributeByDaisyName(daisyName, type);
            if (attribute == null) {
                // We couldn't find the attribute so maybe we could try creating
                // one
                attribute = new MappedAttribute(new AttributeImpl(getName(), null, getDaisyName(), getType()));
                entity.addAttribute(attribute);
            }
        }

        try {
            attribute.setType(getType());
            attribute.setMultivalue(isMultivalue);
            attribute.setDaisyName(getDaisyName());            
            attribute.setExternalName(getName());
            boolean isExternal = false;
            if (entity instanceof MappedEntity){
                isExternal = ((MappedEntity)entity).isExternal();
            }
            if (attribute instanceof MappedAttribute) {
                ((MappedAttribute) attribute).setMapped(true);
            }
            // Do not do mappings when coming from daisy !
            if (isExternal && attribute.getValues() == null) {
                for (Mapping mapping : childMappings)
                    mapping.applyMapping(entity);
            }
        } catch (Exception e) {
            throw new MappingException(e);
        }

    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(this.name);
        buffer.append(", ");
        buffer.append(this.daisyName);

        return buffer.toString();
    }

    public void addChildMapping(AssociatedMapping mapping) {
        mapping.setTargetAttribute(this);
        this.childMappings.add(mapping);
    }

    public List<AssociatedMapping> getChildMappings() {
        return childMappings;
    }

    public boolean isMultivalue() {
        return isMultivalue;
    }

    public void setMultivalue(boolean isMultivalue) throws MappingException {
        if (isMultivalue && this.type != AttributeType.FIELD)
            throw new MappingException("Only attributes of the " + AttributeType.FIELD.toString() + " may be multivalued");
        this.isMultivalue = isMultivalue;
    }
    
}
