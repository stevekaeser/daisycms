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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlString;
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.dao.ExternalEntityDao;
import org.outerj.daisy.sync.dao.SyncEntityDao;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Syncmapping.AssociatedAttributeDocument.AssociatedAttribute;
import org.outerx.daisy.x10Syncmapping.AssociatedEntityDocument.AssociatedEntity;
import org.outerx.daisy.x10Syncmapping.AttributeDocument.Attribute;
import org.outerx.daisy.x10Syncmapping.AttributeFilterDocument.AttributeFilter;
import org.outerx.daisy.x10Syncmapping.ConcatDocument.Concat;
import org.outerx.daisy.x10Syncmapping.MappingDocument;
import org.outerx.daisy.x10Syncmapping.MappingDocument.Mapping;
import org.outerx.daisy.x10Syncmapping.MappingDocument.Mapping.Entity.Language;
import org.xml.sax.SAXException;

public class MappingConfiguration {

    private SortedMap<String, EntityMapping> entityNameMappedEntity = new TreeMap<String, EntityMapping>();

    private SortedMap<String, EntityMapping> docTypeMappedEntity = new TreeMap<String, EntityMapping>();
    
    private List<EntityMapping> mappings = new ArrayList<EntityMapping>();
    
    private ExternalEntityDao externalEntityDao;
    
    private SyncEntityDao syncEntityDao;
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public MappingConfiguration(String configFileName, ExternalEntityDao externalEntityDao, SyncEntityDao syncEntityDao) throws MappingException {
        if(configFileName == null){
            logger.log(Level.SEVERE, "ConfigFilename is not set.");
            throw new RuntimeException("ConfigFilename is not set.");
        }
        
        String absPath = new File(configFileName).getAbsolutePath();
        FileInputStream configStream;
        try {            
            configStream = new FileInputStream(absPath);
        } catch (FileNotFoundException e) {
            logger.log(Level.SEVERE, "Config file (" + absPath + ") not found.");
            throw new MappingException(e);
        }

        this.setup(configStream, externalEntityDao, syncEntityDao);

    }

    public MappingConfiguration(InputStream configStream, ExternalEntityDao externalEntityDao, SyncEntityDao syncEntityDao) throws MappingException {
        this.setup(configStream, externalEntityDao, syncEntityDao);
    }
    
    private void setup(InputStream config, ExternalEntityDao externalEntityDao, SyncEntityDao syncEntityDao) throws MappingException {
        this.externalEntityDao = externalEntityDao;
        this.syncEntityDao = syncEntityDao;
        
        try {            
            parseConfiguration(config);
        } catch (MappingException e) {
            logger.log(Level.SEVERE, "Could not parse the mapping configuration", e);
            throw e;
        }
    }

    public List<EntityMapping> getEntityMappings() {
        return mappings;
    }

    public EntityMapping getEntityMappingByDocumentTypeName(String documentTypeName) {
        return docTypeMappedEntity.get(documentTypeName);
    }

    public EntityMapping getEntityMappingByEntityName(String entityName) {
        return entityNameMappedEntity.get(entityName);
    }

    private void addEntityMapping(EntityMapping entity) {
        this.entityNameMappedEntity.put(entity.getEntityName(), entity);
        this.docTypeMappedEntity.put(entity.getDaisyDocumentTypeName(), entity);
        this.mappings.add(entity);
    }

    private void parseConfiguration(InputStream config) throws MappingException {
        XmlOptions xmlOptions;
        try {
            xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            Mapping mapping = MappingDocument.Factory.parse(config, xmlOptions).getMapping();
            
            for (MappingDocument.Mapping.Entity xmlEntity : mapping.getEntityList()) {
                EntityMapping entity = new EntityMapping();
                entity.setEntityName(xmlEntity.getName());
                entity.setDaisyDocumentTypeName(xmlEntity.getDocumentTypeName());
    
                for (Language language : xmlEntity.getLanguageList()) {
                    LanguageMapping languageMapping = parseLanguageMapping(language);
                    entity.addLanguageMapping(languageMapping);
                }
    
                for (Attribute xmlAttribute : xmlEntity.getAttributeList()) {
                    AttributeMapping attributeMapping = parseAttributeMapping(xmlAttribute);
                    entity.addChild(attributeMapping);
                }
                
                addEntityMapping(entity);
            }
        } catch (ParserConfigurationException e) {
            throw new MappingException("Error while parsing configuration: ", e);
        } catch (SAXException e) {
            throw new MappingException("Error while parsing configuration: ", e);
        } catch (XmlException e) {
            throw new MappingException("Error while parsing configuration: ", e);
        } catch (IOException e) {
            throw new MappingException("Error while parsing configuration: ", e);
        }
    }

    private AttributeFilterMapping parseAttributeFilter(AttributeFilter filter) throws MappingException {
        AttributeFilterMapping attributeFilterMapping = null;
        if (filter != null) {
            attributeFilterMapping = new AttributeFilterMapping(filter.getAttributeName(), filter.getMatch());
            for (AssociatedEntity associatedEntity : filter.getAssociatedEntityList()) {
                attributeFilterMapping.addChildMapping(parseAssociatedEntity(associatedEntity));
            }
            
            for (AssociatedAttribute associatedAttribute : filter.getAssociatedAttributeList()) {
                attributeFilterMapping.addChildMapping(parseAssociatedAttribute(associatedAttribute));
            }
            
            for (AttributeFilter subFilter : filter.getAttributeFilterList()) {
                attributeFilterMapping.addChildMapping(parseAttributeFilter(subFilter));
            }
            
            for (Concat concatAttribute :  filter.getConcatList()) {
                attributeFilterMapping.addChildMapping(parseConcatMapping(concatAttribute));
            }

            for (String value : filter.getValueList()) {
                attributeFilterMapping.addChildMapping(new ValueMapping(value));
            }
            
        }
        return attributeFilterMapping;
    }

    private AssociatedEntityMapping parseAssociatedEntity(AssociatedEntity associatedEntity) throws MappingException {
        AssociatedEntityMapping associatedEntityMapping = null;
        if (associatedEntity != null) {
            associatedEntityMapping = new AssociatedEntityMapping(associatedEntity.getName(), associatedEntity.getJoinKey(), associatedEntity
                    .getJoinParentKey(), this.externalEntityDao, this.syncEntityDao, this);
            
            for (AssociatedEntity subAssociatedEntity : associatedEntity.getAssociatedEntityList())
                associatedEntityMapping.addChildMapping(parseAssociatedEntity(subAssociatedEntity));
            
            for (AttributeFilter filter : associatedEntity.getAttributeFilterList())
                associatedEntityMapping.addChildMapping(parseAttributeFilter(filter));
            
            for (AssociatedAttribute associatedAttribute : associatedEntity.getAssociatedAttributeList())
                associatedEntityMapping.addChildMapping(parseAssociatedAttribute(associatedAttribute));
            
            for (Concat concatAttribute :  associatedEntity.getConcatList()) {
                associatedEntityMapping.addChildMapping(parseConcatMapping(concatAttribute));
            }
        }
        return associatedEntityMapping;
    }

    private AssociatedAttributeMapping parseAssociatedAttribute(AssociatedAttribute associatedAttribute) {
        AssociatedAttributeMapping associatedAttributeMapping = null;
        if (associatedAttribute != null) {
            associatedAttributeMapping = new AssociatedAttributeMapping(associatedAttribute.getName());
        }
        return associatedAttributeMapping;
    }

    private AttributeMapping parseAttributeMapping(Attribute attribute) throws MappingException {
        AttributeMapping attributeMapping = null;
        if (attribute != null) {
            attributeMapping = new AttributeMapping();
            attributeMapping.setName(attribute.getName());
            attributeMapping.setDaisyName(attribute.getDaisyName());
            attributeMapping.setType(AttributeType.valueOf(attribute.getType().toString()));
            if (attribute.isSetMultivalue())
                attributeMapping.setMultivalue(attribute.getMultivalue());

            for (AssociatedEntity associatedEntity : attribute.getAssociatedEntityList()) {
                attributeMapping.addChildMapping(parseAssociatedEntity(associatedEntity));
            }

            for (AssociatedAttribute nestedAttr : attribute.getAssociatedAttributeList()) {
                attributeMapping.addChildMapping(parseAssociatedAttribute(nestedAttr));
            }
            
            for (AttributeFilter filter : attribute.getAttributeFilterList()) {
                attributeMapping.addChildMapping(parseAttributeFilter(filter));
            }
            
            for (Concat concatAttribute :  attribute.getConcatList()) {
                attributeMapping.addChildMapping(parseConcatMapping(concatAttribute));
            }
            
            for (String value : attribute.getValueList()) {
                attributeMapping.addChildMapping(new ValueMapping(value));
            }

        }
        return attributeMapping;
    }

    private LanguageMapping parseLanguageMapping(Language language) throws MappingException {
        LanguageMapping languageMapping = null;
        if (language != null) {
            languageMapping = new LanguageMapping(language.getName(), language.getCreateCondition());
            for (Attribute attribute : language.getAttributeList())
                languageMapping.addChildMapping(parseAttributeMapping(attribute));
        }

        return languageMapping;

    }
    
    private ConcatMapping parseConcatMapping (Concat concat) throws MappingException {
        ConcatMapping concatMapping = null;
        if (concat != null) {
            concatMapping = new ConcatMapping();
            XmlObject[] xmlObjects = concat.selectPath("*");
            for (XmlObject xmlObject : xmlObjects) {
                if (xmlObject instanceof AssociatedEntity) {
                    concatMapping.addChildMapping(parseAssociatedEntity((AssociatedEntity)xmlObject));
                } else if (xmlObject instanceof AssociatedAttribute) {
                    concatMapping.addChildMapping(parseAssociatedAttribute((AssociatedAttribute)xmlObject));
                } else if (xmlObject instanceof AttributeFilter) {
                    concatMapping.addChildMapping(parseAttributeFilter((AttributeFilter)xmlObject));
                } else if (xmlObject instanceof XmlString) {
                    concatMapping.addChildMapping(new ValueMapping(((XmlString) xmlObject).getStringValue()));
                }
            }
        }
        return concatMapping;
    }
    
    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
