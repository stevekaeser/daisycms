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
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.SyncState;

public class EntityMapping {
    private String entityName;

    private String daisyDocumentTypeName;

    private List<Mapping> childMappings = new ArrayList<Mapping>();

    private SortedMap<String, LanguageMapping> languageMappings = new TreeMap<String, LanguageMapping>();

    public void addChild(Mapping mapping) {
        childMappings.add(mapping);
    }

    public void addLanguageMapping(LanguageMapping languageMapping) {
        languageMappings.put(languageMapping.getName(), languageMapping);
    }

    public String getDaisyDocumentTypeName() {
        return daisyDocumentTypeName;
    }

    public void setDaisyDocumentTypeName(String daisyDocumentTypeName) {
        this.daisyDocumentTypeName = daisyDocumentTypeName;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public List<Entity> mapEntity(Entity entity) throws MappingException {
        List<Entity> entities = new ArrayList<Entity>();
        boolean isExternal = entity.getName() != null;
        MappedEntity wrappedEntity = new MappedEntity(entity, isExternal);
        entity.setInternalName(daisyDocumentTypeName);
        entity.setName(entityName);

        wrapAttributes(entity);

        if (!languageMappings.isEmpty()) {
            if (entity.getLanguage() == null) {
                for (LanguageMapping langMapping : languageMappings.values()) {
                    // only map language if one if the required attributes in the language mapping is present
                    if(entity.containsAttributeExternalName(langMapping.getRequiredFields())){
                        MappedEntity langEntity = (MappedEntity) wrappedEntity.clone();
                        wrapAttributes(langEntity);
                        entities.add(langEntity.delegate);
                        langMapping.applyMapping(langEntity);
                    }
                }
            } else {
                LanguageMapping langMapping = this.languageMappings.get(entity.getLanguage());
                // if the entity isn't in a language that's mapped just skip it
                if (langMapping != null) {
                    langMapping.applyMapping(wrappedEntity);
                    entities.add(wrappedEntity.delegate);
                }
            }
        } else {
            entities.add(wrappedEntity.delegate);
            for (Mapping mapping : childMappings) {
                mapping.applyMapping(wrappedEntity);
            }
        }

        for (Entity e : entities) {
            for (Attribute attribute : e.getAttributes()) {
                if (attribute instanceof MappedAttribute) {
                    MappedAttribute mappedAttribute = (MappedAttribute) attribute;
                    e.removeAttribute(mappedAttribute);
                    if (mappedAttribute.isMapped())
                        e.addAttribute(mappedAttribute.delegate); // unwrap attribute
                        
                } else {
                    e.removeAttribute(attribute);
                }
            }
        }
        return entities;
    }

    public List<Mapping> getChildMappings() {
        return childMappings;
    }

    private void wrapAttributes(Entity entity) {
        for (Attribute attribute : entity.getAttributes()) {
            if (!(attribute instanceof MappedAttribute)) {
                entity.removeAttribute(attribute);
                entity.addAttribute(new MappedAttribute(attribute));
            }
        }
    }
    
    public static class MappedEntity implements Entity {
        private Entity delegate;
        
        private boolean isExternal;
        
        public boolean isExternal() {
            return isExternal;
        }

        public MappedEntity(Entity entity, boolean isExternal) {
            this.delegate = entity;
            this.isExternal = isExternal;
        }

        public void addAttribute(Attribute attribute) {
            delegate.addAttribute(attribute);
        }

        public Attribute getAttributeByDaisyName(String daisyName, AttributeType type) {
            return delegate.getAttributeByDaisyName(daisyName, type);
        }

        public Attribute getAttributeByExternalName(String externalname) {
            return delegate.getAttributeByExternalName(externalname);
        }

        public Collection<Attribute> getAttributes() {
            return delegate.getAttributes();
        }

        public VariantKey getDaisyVariantKey() {
            return delegate.getDaisyVariantKey();
        }

        public long getDaisyVersion() {
            return delegate.getDaisyVersion();
        }

        public long getExternalId() {
            return delegate.getExternalId();
        }

        public Date getExternalLastModified() {
            return delegate.getExternalLastModified();
        }

        public String getInternalName() {
            return delegate.getInternalName();
        }

        public String getLanguage() {
            return delegate.getLanguage();
        }

        public Date getLastModified() {
            return delegate.getLastModified();
        }

        public String getName() {
            return delegate.getName();
        }

        public SyncState getState() {
            return delegate.getState();
        }

        public Date getUpdateTimestamp() {
            return delegate.getUpdateTimestamp();
        }

        public boolean isDaisyDeleted() {
            return delegate.isDaisyDeleted();
        }

        public boolean isExternalDeleted() {
            return delegate.isExternalDeleted();
        }

        public void removeAttribute(Attribute attribute) {
            delegate.removeAttribute(attribute);
            
        }

        public void setAttributes(Collection<Attribute> attributes) {
            delegate.setAttributes(attributes);
            
        }

        public void setDaisyDeleted(boolean daisyDeleted) {
            delegate.setDaisyDeleted(daisyDeleted);
            
        }

        public void setDaisyVariantKey(VariantKey daisyVariantKey) {
            delegate.setDaisyVariantKey(daisyVariantKey);
            
        }

        public void setDaisyVersion(long daisyVersion) {
            delegate.setDaisyVersion(daisyVersion);
            
        }

        public void setExternalDeleted(boolean externalDeleted) {
            delegate.setExternalDeleted(externalDeleted);
            
        }

        public void setExternalId(long externalId) {
            delegate.setExternalId(externalId);
        }

        public void setExternalLastModified(Date externalLastModified) {
            delegate.setExternalLastModified(externalLastModified);
        }

        public void setInternalName(String internalName) {
            delegate.setInternalName(internalName);
        }

        public void setLanguage(String language) {
            delegate.setLanguage(language);
            
        }

        public void setName(String name) {
            delegate.setName(name);
        }

        public void setState(SyncState state) {
            delegate.setState(state);
        }

        public void setUpdateTimestamp(Date updateTimestamp) {
            delegate.setUpdateTimestamp(updateTimestamp);
        }

        public boolean containsAttributeExternalName(String[] externalnames) {
            return delegate.containsAttributeExternalName(externalnames);
        }

        @Override
        public Entity clone() {
            Entity clonedEntity = new MappedEntity(delegate.clone(), isExternal);
            return clonedEntity;
        }
    }

    public static class MappedAttribute implements Attribute {
        private static final long serialVersionUID = -666209539348403896L;

        private Attribute delegate;

        private boolean isMapped = false;

        public boolean isMapped() {
            return isMapped;
        }

        public void setMapped(boolean isMapped) {
            this.isMapped = isMapped;
        }

        public MappedAttribute(Attribute attribute) {
            this.delegate = attribute;
        }

        public void addAllValues(Collection<String> values) {
            delegate.addAllValues(values);
        }

        public void addValue(String value) {
            delegate.addValue(value);
        }

        public String getDaisyName() {
            return delegate.getDaisyName();
        }

        public Entity getEntity() {
            return delegate.getEntity();
        }

        public String getExternalName() {
            return delegate.getExternalName();
        }

        public AttributeType getType() {
            return delegate.getType();
        }

        public List<String> getValues() {
            return delegate.getValues();
        }

        public void setDaisyName(String daisyFieldName) {
            delegate.setDaisyName(daisyFieldName);
            if (getEntity() != null) {
                getEntity().removeAttribute(delegate);
                getEntity().addAttribute(this);
            }
        }

        public void setEntity(Entity entity) {
            delegate.setEntity(entity);
        }

        public void setExternalName(String name) {
            delegate.setExternalName(name);
            if (getEntity() != null) {
                getEntity().removeAttribute(delegate);
                getEntity().addAttribute(this);
            }
        }

        public void setType(AttributeType type) throws Exception {
            delegate.setType(type);
            if (getEntity() != null) {
                getEntity().removeAttribute(delegate);
                getEntity().addAttribute(this);
            }
        }

        public void setValues(List<String> values) {
            delegate.setValues(values);
        }

        public boolean isMultivalue() {
            return delegate.isMultivalue();
        }

        public void setMultivalue(boolean isMultivalue) throws Exception {
            delegate.setMultivalue(isMultivalue);
        }

        public Attribute clone() {
            MappedAttribute attrCopy = null;
            attrCopy = new MappedAttribute(delegate.clone());
            if (this.delegate.getValues() != null)
                attrCopy.delegate.setValues(new ArrayList<String>(this.delegate.getValues()));
           

            return attrCopy;
        }
    }
}
