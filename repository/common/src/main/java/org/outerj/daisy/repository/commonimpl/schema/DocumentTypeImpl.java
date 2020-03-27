/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository.commonimpl.schema;

import org.outerj.daisy.repository.schema.*;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.Util;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.commonimpl.RepositoryImpl;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.util.LocaleMap;
import org.outerx.daisy.x10.DocumentTypeDocument;
import org.outerx.daisy.x10.PartTypeUseDocument;
import org.outerx.daisy.x10.FieldTypeUseDocument;

import java.util.*;

// IMPORTANT:
//  When adding/changing properties to a document type, or any of the objects used by it
//  be sure to update the equals method if needed, as well as the clone method

public class DocumentTypeImpl implements DocumentType {
    private long id = -1;
    private List<PartTypeUse> partTypeUses = new ArrayList<PartTypeUse>();
    private Map<Long, PartTypeUse> partTypeUsesById = new HashMap<Long, PartTypeUse>();
    private List<FieldTypeUse> fieldTypeUses = new ArrayList<FieldTypeUse>();
    private Map<Long, FieldTypeUse> fieldTypeUsesById = new HashMap<Long, FieldTypeUse>();
    private String name;
    private SchemaLocaleMap label = new SchemaLocaleMap();
    private SchemaLocaleMap description = new SchemaLocaleMap();
    private SchemaStrategy schemaStrategy;
    private CommonRepository commonRepository;
    private Repository repository;
    private long labelId = -1;
    private long descriptionId = -1;
    private Date lastModified;
    private long lastModifier=-1;
    private AuthenticatedUser currentModifier;
    private boolean deprecated = false;
    private boolean readOnly = false;
    private long updateCount = 0;
    private IntimateAccess intimateAccess = new IntimateAccess();
    private static final String READ_ONLY_MESSAGE = "This document type is read-only.";

    public DocumentTypeImpl(String name, SchemaStrategy schemaStrategy,
            CommonRepository repository, AuthenticatedUser user) {
        Util.checkName(name);
        this.name = name;
        this.schemaStrategy = schemaStrategy;
        this.commonRepository = repository;
        this.currentModifier = user;
        this.repository = new RepositoryImpl(repository, user);
    }

    public IntimateAccess getIntimateAccess(SchemaStrategy schemaStrategy) {
        if (this.schemaStrategy == schemaStrategy)
            return intimateAccess;
        else
            return null;
    }

    public long getId() {
        return id;
    }

    public PartTypeUse[] getPartTypeUses() {
        return partTypeUses.toArray(new PartTypeUse[0]);
    }

    public PartTypeUse addPartType(PartType partType, boolean required) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (!(partType instanceof PartTypeImpl))
            throw new IllegalArgumentException("Unsupported PartType supplied.");

        if (((PartTypeImpl)partType).getIntimateAccess(schemaStrategy) == null)
            throw new IllegalArgumentException("PartType is not loaded from the same Repository as this DocumentType.");

        if (partType.getId() == -1)
            throw new IllegalArgumentException("Only PartTypes which have already been created in the repository can be added to a DocumentType.");

        if (hasPartType(partType.getId()))
            throw new IllegalArgumentException("A DocumentType can only contain the same PartType once.");

        PartTypeUseImpl partTypeUse = new PartTypeUseImpl(this, partType, required);
        partTypeUses.add(partTypeUse);
        partTypeUsesById.put(new Long(partType.getId()), partTypeUse);
        return partTypeUse;
    }

    public void clearPartTypeUses() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        partTypeUses.clear();
        partTypeUsesById.clear();
    }

    public boolean hasPartType(long id) {
        return partTypeUsesById.containsKey(new Long(id));
    }

    public PartTypeUse getPartTypeUse(long id) {
        return partTypeUsesById.get(id);
    }

    public PartTypeUse getPartTypeUse(String partTypeName) {
        // There usually won't be that many part types, so a sequential scan should be fast
        for (PartTypeUse partTypeUse : partTypeUses) {
            if (partTypeUse.getPartType().getName().equals(partTypeName))
                return partTypeUse;
        }
        return null;
    }

    public FieldTypeUse[] getFieldTypeUses() {
        return fieldTypeUses.toArray(new FieldTypeUse[0]);
    }

    public boolean hasFieldType(long id) {
        return fieldTypeUsesById.containsKey(id);
    }

    public FieldTypeUse getFieldTypeUse(long id) {
        return fieldTypeUsesById.get(id);
    }

    public FieldTypeUse getFieldTypeUse(String fieldTypeName) {
        // There usually won't be that many field types, so a sequential scan should be fast
        for (FieldTypeUse fieldTypeUse : fieldTypeUses) {
            if (fieldTypeUse.getFieldType().getName().equals(fieldTypeName))
                return fieldTypeUse;
        }
        return null;
    }

    public FieldTypeUse addFieldType(FieldType type, boolean required) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        // The field type may come from the cache, in which case it will be wrapped in a FieldTypeWrapper
        if (type instanceof FieldTypeWrapper)
            type = ((FieldTypeWrapper)type).getImpl();

        if (!(type instanceof FieldTypeImpl))
            throw new IllegalArgumentException("Unsupported FieldType supplied.");

        if (((FieldTypeImpl)type).getIntimateAccess(schemaStrategy) == null)
            throw new IllegalArgumentException("FieldType is not loaded from the same Repository as this DocumentType.");

        if (type.getId() == -1)
            throw new IllegalArgumentException("Only FieldTypes which have already been created in the repository can be added to a DocumentType.");

        if (hasFieldType(type.getId()))
            throw new IllegalArgumentException("A DocumentType can only contain the same FieldType once.");


        FieldType wrappedFieldType = new FieldTypeWrapper((FieldTypeImpl)type, currentModifier, schemaStrategy, repository);
        FieldTypeUseImpl fieldTypeUse = new FieldTypeUseImpl(this, wrappedFieldType, required, true);
        fieldTypeUses.add(fieldTypeUse);
        fieldTypeUsesById.put(type.getId(), fieldTypeUse);
        return fieldTypeUse;
    }

    public void clearFieldTypeUses() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        fieldTypeUses.clear();
        fieldTypeUsesById.clear();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        Util.checkName(name);
        this.name = name;
    }

    public String getDescription(Locale locale) {
        return (String)description.get(locale);
    }

    public String getDescriptionExact(Locale locale) {
        return (String)description.getExact(locale);
    }

    public void setDescription(Locale locale, String description) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.description.put(locale, description);
    }

    public void clearDescriptions() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        description.clear();
    }

    public Locale[] getDescriptionLocales() {
        return description.getLocales();
    }

    public void setLabel(Locale locale, String label) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.label.put(locale, label);
    }

    public String getLabel(Locale locale) {
        String result = (String)label.get(locale);
        return result == null ? getName() : result;
    }

    public String getLabelExact(Locale locale) {
        return (String)label.getExact(locale);
    }

    public void clearLabels() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        label.clear();
    }

    public Locale[] getLabelLocales() {
        return label.getLocales();
    }

    private DocumentTypeDocument getXml(boolean extended) {
        DocumentTypeDocument documentTypeDocument = DocumentTypeDocument.Factory.newInstance();

        DocumentTypeDocument.DocumentType documentType = documentTypeDocument.addNewDocumentType();

        if (id != -1) {
            documentType.setId(id);
            GregorianCalendar lastModified = new GregorianCalendar();
            lastModified.setTime(this.lastModified);
            documentType.setLastModified(lastModified);
            documentType.setLastModifier(lastModifier);
        }

        documentType.setName(name);
        documentType.setDeprecated(deprecated);
        documentType.setLabels(label.getAsLabelsXml());
        documentType.setDescriptions(description.getAsDescriptionsXml());
        documentType.setUpdateCount(updateCount);
        
        // add the part types
        PartTypeUse[] partTypeUses = getPartTypeUses();
        PartTypeUseDocument.PartTypeUse[] partTypeUsesXml = new PartTypeUseDocument.PartTypeUse[partTypeUses.length];
        for (int i = 0; i < partTypeUses.length; i++) {
            partTypeUsesXml[i] = extended ? partTypeUses[i].getExtendedXml().getPartTypeUse() : partTypeUses[i].getXml().getPartTypeUse();
        }
        documentType.addNewPartTypeUses().setPartTypeUseArray(partTypeUsesXml);

        // add the field types
        FieldTypeUse[] fieldTypeUses = getFieldTypeUses();
        FieldTypeUseDocument.FieldTypeUse[] fieldTypeUsesXml = new FieldTypeUseDocument.FieldTypeUse[fieldTypeUses.length];
        for (int i = 0; i < fieldTypeUses.length; i++) {
            fieldTypeUsesXml[i] = extended ? fieldTypeUses[i].getExtendedXml().getFieldTypeUse() : fieldTypeUses[i].getXml().getFieldTypeUse();
        }
        documentType.addNewFieldTypeUses().setFieldTypeUseArray(fieldTypeUsesXml);

        return documentTypeDocument;
    }

    public DocumentTypeDocument getExtendedXml() {
        return getXml(true);
    }

    public DocumentTypeDocument getXml() {
        return getXml(false);
    }

    public void save() throws RepositoryException {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        schemaStrategy.store(this);
    }

    public long getLastModifier() {
        return lastModifier;
    }

    public Date getLastModified() {
        if (lastModified != null)
            return (Date)lastModified.clone();
        else
            return null;
    }

    public void setDeprecated(boolean deprecated) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);
        this.deprecated = deprecated;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    /**
     * Disables all operations that can change the state of this DocumentType. Note
     * that this doesn't apply to the FieldTypes and PartTypes contained
     * by this DocumentType. This is no problem however since these are always
     * loaded read-only. The field type uses and part type uses do also
     * become read-only.
     */
    public void makeReadOnly() {
        this.readOnly = true;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public void setAllFromXml(DocumentTypeDocument.DocumentType documentTypeXml) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.name = documentTypeXml.getName();
        this.deprecated = documentTypeXml.getDeprecated();
        this.label.clear();
        this.label.readFromLabelsXml(documentTypeXml.getLabels());
        this.description.clear();
        this.description.readFromDescriptionsXml(documentTypeXml.getDescriptions());

        CommonRepositorySchema repositorySchema = commonRepository.getRepositorySchema();

        partTypeUses.clear();
        partTypeUsesById.clear();
        for (PartTypeUseDocument.PartTypeUse partTypeUseXml : documentTypeXml.getPartTypeUses().getPartTypeUseList()) {
            PartType partType;
            try {
                partType = repositorySchema.getPartTypeById(partTypeUseXml.getPartTypeId(), false, currentModifier);
            } catch (Exception e) {
                throw new RuntimeException("Error looking up a part type.", e);
            }
            PartTypeUseImpl partTypeUse = new PartTypeUseImpl(this, partType, partTypeUseXml.getRequired());
            partTypeUse.setEditable(partTypeUseXml.isSetEditable() ? partTypeUseXml.getEditable() : true);
            partTypeUses.add(partTypeUse);
            partTypeUsesById.put(partType.getId(), partTypeUse);
        }

        fieldTypeUses.clear();
        for (FieldTypeUseDocument.FieldTypeUse fieldTypeUseXml : documentTypeXml.getFieldTypeUses().getFieldTypeUseList()) {
            FieldType fieldType;
            try {
                fieldType = repositorySchema.getFieldTypeById(fieldTypeUseXml.getFieldTypeId(), false, currentModifier);
            } catch (Exception e) {
                throw new RuntimeException("Error looking up a field type.", e);
            }
            FieldTypeUseImpl fieldTypeUse = new FieldTypeUseImpl(this, fieldType, fieldTypeUseXml.getRequired(),
                    fieldTypeUseXml.isSetEditable() ? fieldTypeUseXml.getEditable() : true);
            fieldTypeUses.add(fieldTypeUse);
            fieldTypeUsesById.put(fieldType.getId(), fieldTypeUse);
        }
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DocumentTypeImpl))
            return false;

        DocumentTypeImpl other = (DocumentTypeImpl)obj;

        if (deprecated != other.deprecated)
            return false;

        if (!fieldTypeUses.equals(other.fieldTypeUses))
            return false;

        if (!partTypeUses.equals(other.partTypeUses))
            return false;

        if (!label.equals(other.label))
            return false;

        if (!description.equals(other.description))
            return false;

        return name.equals(other.name);
    }

    /**
     * Makes a clone of this document type in which the field types are
     * replaced by wrapped field types to make their behaviour user dependent.
     */
    public DocumentType clone(AuthenticatedUser user) {
        // since we keep references to objects from the original, only allow
        // cloning when the original is not modifiable too
        if (!readOnly)
            throw new RuntimeException("Can only make clones of read only document types");

        DocumentTypeImpl clone = new DocumentTypeImpl(name, schemaStrategy, commonRepository, user);
        clone.id = this.id;
        clone.partTypeUses = this.partTypeUses;
        clone.partTypeUsesById = this.partTypeUsesById;

        // Here's the special part: cloning/wrapping the fieldTypeUses/fieldTypes
        List<FieldTypeUse> clonedFieldTypeUses = new ArrayList<FieldTypeUse>(fieldTypeUses.size());
        Map<Long, FieldTypeUse> clonedFieldTypeUsesById = new HashMap<Long, FieldTypeUse>();
        Repository repository = new RepositoryImpl(commonRepository, user);
        for (FieldTypeUse fieldTypeUse : this.fieldTypeUses) {
            FieldTypeUse newFieldTypeUse = cloneFieldTypeUse(fieldTypeUse, clone, user, repository);
            clonedFieldTypeUses.add(newFieldTypeUse);
            clonedFieldTypeUsesById.put(newFieldTypeUse.getFieldType().getId(), newFieldTypeUse);
        }
        clone.fieldTypeUses = clonedFieldTypeUses;
        clone.fieldTypeUsesById = clonedFieldTypeUsesById;

        clone.label = this.label;
        clone.description = this.description;
        clone.labelId = this.labelId;
        clone.descriptionId = this.descriptionId;
        clone.lastModified = this.lastModified;
        clone.lastModifier = this.lastModifier;
        clone.deprecated = this.deprecated;
        clone.readOnly = true;
        clone.updateCount = this.updateCount;

        return clone;
    }

    private static FieldTypeUse cloneFieldTypeUse(FieldTypeUse fieldTypeUse, DocumentTypeImpl clone, AuthenticatedUser user, Repository repository) {
        FieldType fieldType = fieldTypeUse.getFieldType();
        FieldTypeImpl fieldTypeImpl = fieldType instanceof FieldTypeWrapper ? ((FieldTypeWrapper)fieldType).getImpl() : (FieldTypeImpl)fieldType;
        FieldType wrappedFieldType = new FieldTypeWrapper(fieldTypeImpl, user, clone.schemaStrategy, repository);
        FieldTypeUse newFieldTypeUse = new FieldTypeUseImpl(clone, wrappedFieldType, fieldTypeUse.isRequired(), fieldTypeUse.isEditable());
        return newFieldTypeUse;
    }

    public class IntimateAccess {
        private IntimateAccess() {
        }

        public void setLastModified(Date lastModified) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            DocumentTypeImpl.this.lastModified = lastModified;
        }

        public void setLastModifier(long lastModifier) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            DocumentTypeImpl.this.lastModifier = lastModifier;
        }

        public AuthenticatedUser getCurrentModifier() {
            return currentModifier;
        }

        public LocaleMap getLabels() {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            return label;
        }

        public LocaleMap getDescriptions() {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            return description;
        }

        public long getLabelId() {
            return labelId;
        }

        public void setLabelId(long labelId) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            DocumentTypeImpl.this.labelId = labelId;
        }

        public long getDescriptionId() {
            return descriptionId;
        }

        public void setDescriptionId(long descriptionId) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            DocumentTypeImpl.this.descriptionId = descriptionId;
        }

        public void setId(long id) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            DocumentTypeImpl.this.id = id;
        }

        public void setUpdateCount(long updateCount) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            DocumentTypeImpl.this.updateCount = updateCount;
        }
    }
}
