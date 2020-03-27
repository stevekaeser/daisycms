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

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.Field;
import org.outerj.daisy.repository.FieldNotFoundException;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.sync.Attribute;
import org.outerj.daisy.sync.AttributeImpl;
import org.outerj.daisy.sync.AttributeType;
import org.outerj.daisy.sync.Entity;
import org.outerj.daisy.sync.EntityImpl;
import org.outerj.daisy.sync.EntityNotFoundException;
import org.outerj.daisy.sync.VariantHelper;

public class DaisyEntityDao implements InternalEntityDao {

    private Repository repository;

    private Locale locale = Locale.US;

    private DocumentCollection collection;

    private Branch branch;
    
    private VersionState documentCreateVersionState = VersionState.DRAFT;
    
    private DateFormat syncDateFormat = new SimpleDateFormat("dd/MM/yyyy");
    
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public DaisyEntityDao(RepositoryManager repositoryManager, Credentials credentials, String collectionName, String branchName, String documentCreateVersionState) {
        try {
            this.repository = repositoryManager.getRepository(credentials);
            collection = this.repository.getCollectionManager().getCollection(collectionName, false);
            branch = this.repository.getVariantManager().getBranch(branchName, false);
            this.documentCreateVersionState = VersionState.fromString(documentCreateVersionState);
        } catch (RepositoryException e) {
            
        }
    }

    public void storeEntity(Entity entity) {
        VariantKey variantKey = entity.getDaisyVariantKey();
        try {
            if (variantKey == null) {
                Language language = repository.getVariantManager().getLanguage(entity.getLanguage(), false);
                Document document = null;
                // first check and see a document already exists for this entity
                VariantKey[] keys = repository.getQueryManager().performQueryReturnKeys(
                        "select id where documentType='" + entity.getInternalName() + "' and $ExternalId=" + entity.getExternalId() + " option search_last_version='true'", locale);
                // the first key should be good enough
                if (keys.length > 0) {
                    VariantKey key = keys[0];
                    Document baseDoc = repository.getDocument(key, false);
                    document = repository.createVariant(baseDoc.getId(), baseDoc.getBranchId(), baseDoc.getLanguageId(), baseDoc.getLastVersionId(), baseDoc
                            .getBranchId(), language.getId(), true);
                } else {
                    // the name dummy should be changed later
                    document = repository.createDocument("dummy", entity.getInternalName(), branch.getName(), language.getName());
                }
                document.setNewVersionState(this.documentCreateVersionState);
                updateEntity(document, entity);
            } else {
                Document document = repository.getDocument(variantKey.getDocumentId(), variantKey.getBranchId(), variantKey.getLanguageId(), true);
                if (entity.getLanguage() == null)
                    entity.setLanguage(repository.getVariantManager().getLanguage(variantKey.getLanguageId(), false).getName());
                
                document.setNewVersionState(document.getLastVersion().getState());
                updateEntity(document, entity);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not store entity (" + entity.getName() + " - " + entity.getExternalId() + ")", e);
        }

    }

    public List<VariantKey> getEntityIds(String entityName) throws Exception {
        return Arrays.asList(repository.getQueryManager().performQueryReturnKeys("select name where documentType='" + entityName + "' option search_last_version='true'", locale));

    }

    public Entity getEntity(VariantKey variantKey) throws EntityNotFoundException {
        try {
            Document document = repository.getDocument(variantKey.getDocumentId(), variantKey.getBranchId(), variantKey.getLanguageId(), false);
            Entity entity = new EntityImpl();
            entity.setInternalName(repository.getRepositorySchema().getDocumentTypeById(document.getDocumentTypeId(), false).getName());
            entity.setDaisyVariantKey(variantKey);
            entity.setDaisyVersion(document.getLastVersionId());
            entity.setDaisyDeleted(document.isRetired());
            entity.setUpdateTimestamp(document.getLastModified());
            entity.setLanguage(repository.getVariantManager().getLanguage(variantKey.getLanguageId(), false).getName());

            for (Field field : document.getFields().getArray()) {
                Attribute attribute = new AttributeImpl();
                attribute.setType(AttributeType.FIELD);
                attribute.setDaisyName(field.getTypeName());
                attribute.setMultivalue(field.isMultiValue());
                attribute.addAllValues(valueTypeToStrings(field.getValue(), field.getValueType(), document));
                entity.addAttribute(attribute);
            }
            for (Map.Entry<String, String> entry : document.getCustomFields().entrySet()) {
                Attribute attribute = new AttributeImpl();
                attribute.setType(AttributeType.CUSTOM_FIELD);
                attribute.setDaisyName(entry.getKey());
                attribute.addValue(entry.getValue());
                entity.addAttribute(attribute);
            }

            Attribute attribute = new AttributeImpl();
            attribute.setType(AttributeType.PROPERTY);
            attribute.setDaisyName("name");
            attribute.addValue(document.getName());
            entity.addAttribute(attribute);

            if (document.hasField("ExternalId"))
                entity.setExternalId((Long) document.getField("ExternalId").getValue());

            return entity;
        } catch (FieldNotFoundException e) {
            logger.log(Level.WARNING, "Field not found while getting entity with variant key " + variantKey, e);
        } catch (RepositoryException e) {
            throw new EntityNotFoundException(e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Could not retrieve entity." + variantKey, e);
            throw new EntityNotFoundException(e);
        }
        return null;

    }

    private Object stringToValueType(String value, FieldType fieldType, Document document) throws Exception {
        ValueType valueType = fieldType.getValueType();
        Object o = null;
        if (fieldType.isHierarchical()) {
            if (value != null && value != "") {
                String[] pathStringElements = value.split("/");
                Object[] pathElements = new Object[pathStringElements.length];
                for (int i = 0; i < pathStringElements.length; i++) {
                    String pathElement = pathStringElements[i];
                    if (pathElement != null && pathElement != "") {
                        pathElements[i] = stringToSingleValue(pathElement, valueType, document);
                    }
                }
                HierarchyPath path = new HierarchyPath(pathElements);
                o = path;
            }
        } else {
            o = stringToSingleValue(value, valueType, document);
        }
        return o;
    }
    
    private Object stringToSingleValue(String value, ValueType valueType, Document document) throws Exception{
        Object o = null;
        
        try {
            if (value != null && !value.equals("")) {

                switch (valueType) {
                case STRING:
                    o = value;
                    break;
                case DATETIME:
                    DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
                    o = dateTimeFormat.parse(value);
                    break;
                case DATE:                        
                    o = syncDateFormat.parse(value);
                    break;
                case LONG:
                    o = Long.parseLong(value);
                    break;
                case DOUBLE:
                    o = Double.parseDouble(value);
                    break;
                case DECIMAL:
                    NumberFormat decimalFormat = NumberFormat.getNumberInstance(locale);
                    o = decimalFormat.parseObject(value);
                    break;
                case BOOLEAN:
                    o = Boolean.parseBoolean(value);
                    break;
                case LINK:
                    // link to a document and not to a variant
                    VariantKey key = VariantHelper.extractVariantKey(value); 
                    long branchId = key.getBranchId() == document.getBranchId() ? -1 : key.getBranchId();
                    long langId = key.getLanguageId() == document.getLanguageId() ? -1 : key.getLanguageId();
                    o = new VariantKey(key.getDocumentId(), branchId, langId);
                    break;
                default:
                    logger.severe("Unknown value type : " + valueType);
                    throw new Exception("Encountered unknow value type while converting values to strings. Value type = " + valueType);
                }
            }

        } catch (ParseException e) {
            logger.log(Level.WARNING, "Could not parse value : " + value, e);
        }
        
        return o;
    }

    private Object stringsToValueType(Collection<String> values, FieldType fieldType, Document document) throws Exception{
        Object returnObject = null;
        if (values != null && values.size() > 0) {
            if (fieldType.isMultiValue()) {
                Object[] objectArray = new Object[values.size()];
                int i = 0;
                for (String value : values) {
                    objectArray[i++] = stringToValueType(value, fieldType, document);
                }
                returnObject = objectArray;
            } else {
                // only 1 value
                for (String value : values) {
                    returnObject = stringToValueType(value, fieldType, document);
                }
            }
        } else {
            returnObject = stringToValueType("", fieldType, document);
        }
        return returnObject;
    }

    private Collection<String> valueTypeToStrings(Object value, ValueType valueType, Document document) throws RepositoryException {
        List<String> formattedValues = new ArrayList<String>();
        if (value != null) {
            if (value instanceof Object[]) {

                Object[] values = (Object[]) value;
                for (Object arrayValue : values)
                    formattedValues.addAll(valueTypeToStrings(arrayValue, valueType, document));

            } else if (value instanceof HierarchyPath) {
                HierarchyPath path = (HierarchyPath) value;
                StringBuffer result = new StringBuffer();
                for (int i = 0; i < path.getElements().length; i++) {
                	if (i>0) {
                      result.append("/");
                	}
                	Collection<String> pathStrings = valueTypeToStrings(path.getElements()[i], valueType, document);
                	for (String s : pathStrings) {
                      result.append(s);
                	}
                }
                formattedValues.add(result.toString());

            } else {
                String formattedValue = null;
                switch (valueType) {
                case STRING:
                    if (!value.equals(""))
                        formattedValue = (String) value;
                    break;
                case DATETIME:
                    DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
                    formattedValue = dateTimeFormat.format((Date) value);
                    break;
                case DATE:                    
                    formattedValue = syncDateFormat.format((Date)value);
                    break;
                case LONG:
                    formattedValue = ((Long) value).toString();
                    break;
                case DOUBLE:
                    Double d = (Double)value;
                    if (d % 1 == 0)
                        formattedValue = String.valueOf(d.intValue());
                    else
                        formattedValue = String.valueOf(d);
                    break;
                case DECIMAL:
                    BigDecimal b = (BigDecimal)value;
                    if (b.remainder(BigDecimal.ONE).equals(BigDecimal.ZERO))
                        formattedValue = String.valueOf(b.intValue());
                    else
                        formattedValue = String.valueOf(b);
                    break;
                case BOOLEAN:
                    formattedValue = ((Boolean) value).toString();
                    break;
                case LINK:
                    VariantKey key = (VariantKey) value;
                    long branchId = key.getBranchId() == -1 ? document.getBranchId() : key.getBranchId();
                    long langId = key.getLanguageId() == -1 ? document.getLanguageId() : key.getLanguageId();
                    formattedValue = VariantHelper.variantKeyToString(new VariantKey(key.getDocumentId(), branchId, langId));
                    break;
                default:
                    throw new RepositoryException("Unhandled ValueType " + value.toString());
                }
                if (formattedValue != null)
                    formattedValues.add(formattedValue);
            }
        }
        return formattedValues.isEmpty() ? null : formattedValues;
    }

    private void updateEntity(Document document, Entity entity) throws Exception {
        RepositorySchema repositorySchema = repository.getRepositorySchema();
        StringBuffer buffer = new StringBuffer();
        for (Attribute attribute : entity.getAttributes()) {
            switch (attribute.getType()) {
            case FIELD:
                FieldType fieldType = repositorySchema.getFieldTypeByName(attribute.getDaisyName(), false);
                Object fieldValue = stringsToValueType(attribute.getValues(), fieldType, document);
                if (fieldValue != null)
                    document.setField(fieldType.getId(), fieldValue);
                else
                    logger.logp(Level.FINE, this.getClass().getName(), "updateEntity", "Encountered an attribute ({0}) with a null value for entity ({1} - {2})", new Object[]{attribute.getDaisyName(), entity.getName(), entity.getExternalId()});
                
                break;
            case CUSTOM_FIELD:
            	if (attribute != null && attribute.getValues() != null) {
	                for (String s : attribute.getValues())
	                    buffer.append(s);
	                document.setCustomField(attribute.getDaisyName(), buffer.toString());
	                buffer = new StringBuffer();
            	}
                break;
            case PROPERTY:
                if (attribute != null && attribute.getValues() != null && attribute.getDaisyName().equals("name")) {
                	for (String s : attribute.getValues())
                        buffer.append(s);
                    document.setName(buffer.toString());
                    buffer = new StringBuffer();
                } else {
                    throw new RepositoryException("No such property " + attribute.getDaisyName() + " found in " + document.getClass().getName());
                }
                break;
            default:
                logger.logp(Level.SEVERE, this.getClass().getName(), "updateEntity", "Encountered an unknown attribute type ({0}) while converting string values to daisy values for entity ({1} - {2}) attribute name = {3}", new Object[]{attribute.getType(),});
            }
        }
        
        document.setField("ExternalId", entity.getExternalId());
        document.setRetired(entity.isDaisyDeleted());
        document.addToCollection(collection);
        
        document.save(false);

        // Update the entity a bit
        entity.setDaisyVariantKey(document.getVariantKey());
        entity.setDaisyVersion(document.getLastVersionId());
    }

    public Entity getEntity(String entityName, long externalId, String language) throws Exception {
        Entity entity = null;
        VariantKey[] variantKeys = repository.getQueryManager().performQueryReturnKeys(
                "select name where documentType='" + entityName + "' and $ExternalId=" + externalId + " and language='" + language + "' option search_last_version='true'", locale);
        if (variantKeys.length > 0)
            entity = getEntity(variantKeys[0]);

        return entity;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
