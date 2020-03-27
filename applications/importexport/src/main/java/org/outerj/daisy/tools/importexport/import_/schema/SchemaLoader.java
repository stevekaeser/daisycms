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
package org.outerj.daisy.tools.importexport.import_.schema;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.schema.*;
import org.outerj.daisy.tools.importexport.model.schema.*;
import org.outerj.daisy.tools.importexport.ImportExportException;

import java.util.*;

/**
 * Creates (and updates) a Daisy repository schema from schema types described in an XML file.
 */
public class SchemaLoader {
    private final boolean createOnly;
    private final boolean clearLocalizedData;
    private final SchemaLoadListener listener;
    private final ImpExpSchema impExpSchema;
    private final RepositorySchema schema;

    /**
     *
     * @param createOnly when true, only create missing schema types, do not update existing types
     * @param clearLocalizedData when false, the localized data will be updated but not cleared, i.e. if there
     *                           additional locales in the target schema type, they will be left untouched.
     */
    public static void load(ImpExpSchema impExpSchema, Repository repository, boolean createOnly, boolean clearLocalizedData,
            SchemaLoadListener listener) throws Exception {
        new SchemaLoader(impExpSchema, repository, createOnly, clearLocalizedData, listener).run();
    }

    private SchemaLoader(ImpExpSchema impExpSchema, Repository repository, boolean createOnly, boolean clearLocalizedData, SchemaLoadListener listener) {
        this.impExpSchema = impExpSchema;
        this.createOnly = createOnly;
        this.clearLocalizedData = clearLocalizedData;
        this.listener = listener;
        this.schema = repository.getRepositorySchema();
    }

    private void run() throws Exception {
        load();
    }

    private void load() throws Exception {
        ImpExpFieldType[] fieldTypes = impExpSchema.getFieldTypes();
        for (ImpExpFieldType fieldType : fieldTypes) {
            checkInterrupted();
            try {
                handleFieldType(fieldType);
            } catch (Throwable e) {
                throw new Exception("Error importing field type " + fieldType.getName(), e);
            }
        }

        ImpExpPartType[] partTypes = impExpSchema.getPartTypes();
        for (ImpExpPartType partType : partTypes) {
            checkInterrupted();
            try {
                handlePartType(partType);
            } catch (Throwable e) {
                throw new Exception("Error importing part type " + partType.getName(), e);
            }
        }

        ImpExpDocumentType[] documentTypes = impExpSchema.getDocumentTypes();
        for (ImpExpDocumentType documentType : documentTypes) {
            checkInterrupted();
            try {
                handleDocumentType(documentType);
            } catch (Throwable e) {
                throw new Exception("Error importing document type " + documentType.getName(), e);
            }
        }
    }

    private void handleFieldType(ImpExpFieldType impExpFieldType) throws Exception {
        String name = impExpFieldType.getName();
        ValueType valueType = impExpFieldType.getValueType();

        SchemaLoadResult result;
        FieldType fieldType;
        FieldType origFieldType = null;
        try {
            fieldType = schema.getFieldTypeByName(name, true);
            origFieldType = schema.getFieldTypeByName(name, true);
            result = createOnly ? SchemaLoadResult.UPDATE_SKIPPED : SchemaLoadResult.UPDATED;
            if (fieldType.getValueType() != valueType) {
                listener.conflictingFieldType(name, valueType, fieldType.getValueType());
            }
            if (fieldType.isMultiValue() != impExpFieldType.isMultiValue()) {
                listener.conflictingMultiValue(name, impExpFieldType.isMultiValue(), fieldType.isMultiValue());
            }
            if (fieldType.isHierarchical() != impExpFieldType.isHierarchical()) {
                listener.conflictingHierarchical(name, impExpFieldType.isHierarchical(), fieldType.isHierarchical());
            }
        } catch (FieldTypeNotFoundException e) {
            fieldType = schema.createFieldType(name, valueType, impExpFieldType.isMultiValue(), impExpFieldType.isHierarchical());
            result = SchemaLoadResult.CREATED;
        }

        if (origFieldType != null && createOnly) {
            listener.fieldTypeLoaded(name, result);
            return;
        }

        fieldType.setSize(impExpFieldType.getSize());

        fieldType.setAclAllowed(impExpFieldType.isAclAllowed());
        fieldType.setAllowFreeEntry(impExpFieldType.getAllowFreeEntry());
        fieldType.setLoadSelectionListAsync(impExpFieldType.getLoadSelectionListAsync());
        fieldType.setDeprecated(impExpFieldType.isDeprecated());

        if (clearLocalizedData) {
            fieldType.clearLabels();
            fieldType.clearDescriptions();
        }

        loadLabels(impExpFieldType, fieldType);
        loadDescriptions(impExpFieldType, fieldType);

        Object selectionList = impExpFieldType.getSelectionList();
        if (selectionList == null) {
            fieldType.clearSelectionList();
        } else if (selectionList instanceof ImpExpStaticSelectionList) {
            ImpExpStaticSelectionList impExpList = (ImpExpStaticSelectionList)selectionList;
            StaticSelectionList list = fieldType.createStaticSelectionList();
            List<ImpExpStaticListItem> items = impExpList.getItems();
            for (ImpExpStaticListItem impExpItem : items) {
                createStaticListItem(impExpItem, list);
            }
        } else if (selectionList instanceof ImpExpQuerySelectionList) {
            ImpExpQuerySelectionList impExpList = (ImpExpQuerySelectionList)selectionList;
            fieldType.createQuerySelectionList(impExpList.getQuery(), impExpList.getFilterVariants(), impExpList.getSortOrder());
        } else if (selectionList instanceof ImpExpLinkQuerySelectionList) {
            ImpExpLinkQuerySelectionList impExpList = (ImpExpLinkQuerySelectionList)selectionList;
            fieldType.createLinkQuerySelectionList(impExpList.getWhereClause(), impExpList.getFilterVariants());
        } else if (selectionList instanceof ImpExpHierarchicalQuerySelectionList) {
            ImpExpHierarchicalQuerySelectionList impExpList = (ImpExpHierarchicalQuerySelectionList)selectionList;
            fieldType.createHierarchicalQuerySelectionList(impExpList.getWhereClause(), impExpList.getLinkFields(), impExpList.getFilterVariants());
        } else if (selectionList instanceof ImpExpParentLinkedSelectionList) {
            ImpExpParentLinkedSelectionList impExpList = (ImpExpParentLinkedSelectionList)selectionList;
            fieldType.createParentLinkedSelectionList(impExpList.getWhereClause(), impExpList.getParentLinkField(), impExpList.getFilterVariants());
        } else {
            throw new ImportExportException("Unknwown selection list class: " + selectionList.getClass().getName());
        }

        if (origFieldType == null || !origFieldType.equals(fieldType)) {
            fieldType.save();
            listener.fieldTypeLoaded(name, result);
        } else {
            listener.fieldTypeLoaded(name, SchemaLoadResult.NO_UPDATE_NEEDED);
        }
    }

    private void createStaticListItem(ImpExpStaticListItem impExpItem, StaticListItemParent parentItem) {
        StaticListItem item = parentItem.createItem(impExpItem.getValue());
        loadLabels(impExpItem, item);
        for (ImpExpStaticListItem impExpChild : impExpItem.getItems()) {
            createStaticListItem(impExpChild, item);
        }
    }

    private void handlePartType(ImpExpPartType impExpPartType) throws Exception {
        String name = impExpPartType.getName();

        SchemaLoadResult result;
        PartType partType;
        PartType origPartType = null;
        try {
            partType = schema.getPartTypeByName(name, true);
            origPartType = schema.getPartTypeByName(name, true);
            result = createOnly ? SchemaLoadResult.UPDATE_SKIPPED : SchemaLoadResult.UPDATED;
        } catch (PartTypeNotFoundException e) {
            partType = schema.createPartType(name, impExpPartType.getMimeTypes());
            result = SchemaLoadResult.CREATED;
        }

        if (origPartType != null && createOnly) {
            listener.partTypeLoaded(name, result);
            return;
        }

        partType.setMimeTypes(impExpPartType.getMimeTypes());
        partType.setDaisyHtml(impExpPartType.isDaisyHtml());
        partType.setDeprecated(impExpPartType.isDeprecated());
        partType.setLinkExtractor(impExpPartType.getLinkExtractor());

        if (clearLocalizedData) {
            partType.clearLabels();
            partType.clearDescriptions();
        }

        loadLabels(impExpPartType, partType);
        loadDescriptions(impExpPartType, partType);

        if (origPartType == null || !origPartType.equals(partType)) {
            partType.save();
            listener.partTypeLoaded(name, result);
        } else {
            listener.partTypeLoaded(name, SchemaLoadResult.NO_UPDATE_NEEDED);
        }
    }

    private void handleDocumentType(ImpExpDocumentType impExpDocumentType) throws Exception {
        String name = impExpDocumentType.getName();

        SchemaLoadResult result;
        DocumentType documentType;
        DocumentType origDocumentType = null;
        try {
            documentType = schema.getDocumentTypeByName(name, true);
            origDocumentType = schema.getDocumentTypeByName(name, true);
            result = createOnly ? SchemaLoadResult.UPDATE_SKIPPED : SchemaLoadResult.UPDATED;
        } catch (DocumentTypeNotFoundException e) {
            documentType = schema.createDocumentType(name);
            result = SchemaLoadResult.CREATED;
        }

        if (origDocumentType != null && createOnly) {
            listener.documentTypeLoaded(name, result);
            return;
        }


        if (clearLocalizedData) {
            documentType.clearLabels();
            documentType.clearDescriptions();
        }
        loadLabels(impExpDocumentType, documentType);
        loadDescriptions(impExpDocumentType, documentType);

        documentType.setDeprecated(impExpDocumentType.isDeprecated());

        documentType.clearFieldTypeUses();
        ImpExpFieldTypeUse[] impExpFieldTypeUses = impExpDocumentType.getFieldTypeUses();
        for (ImpExpFieldTypeUse impExpFieldTypeUse : impExpFieldTypeUses) {
            FieldType fieldType = schema.getFieldTypeByName(impExpFieldTypeUse.getFieldTypeName(), false);
            FieldTypeUse fieldTypeUse = documentType.addFieldType(fieldType, impExpFieldTypeUse.isRequired());
            fieldTypeUse.setEditable(impExpFieldTypeUse.isEditable());
        }

        documentType.clearPartTypeUses();
        ImpExpPartTypeUse[] impExpPartTypeUses = impExpDocumentType.getPartTypeUses();
        for (ImpExpPartTypeUse impExpPartTypeUse : impExpPartTypeUses) {
            PartType partType = schema.getPartTypeByName(impExpPartTypeUse.getPartTypeName(), false);
            PartTypeUse partTypeUse = documentType.addPartType(partType, impExpPartTypeUse.isRequired());
            partTypeUse.setEditable(impExpPartTypeUse.isEditable());
        }

        if (origDocumentType == null || !origDocumentType.equals(documentType)) {
            documentType.save();
            listener.documentTypeLoaded(name, result);
        } else {
            listener.documentTypeLoaded(name, SchemaLoadResult.NO_UPDATE_NEEDED);
        }
    }

    private void loadLabels(ImpExpLabelEnabled impExpLabelEnabled, LabelEnabled labelEnabled) {
        Map<Locale, String> labels = impExpLabelEnabled.getLabels();
        for (Map.Entry<Locale, String> entry : labels.entrySet()) {
            labelEnabled.setLabel(entry.getKey(), entry.getValue());
        }
    }

    private void loadDescriptions(ImpExpDescriptionEnabled impExpDescriptionEnabled, DescriptionEnabled descriptionEnabled) {
        Map<Locale, String> labels = impExpDescriptionEnabled.getDescriptions();
        for (Map.Entry<Locale, String> entry : labels.entrySet()) {
            descriptionEnabled.setDescription(entry.getKey(), entry.getValue());
        }
    }

    private void checkInterrupted() throws ImportExportException {
        if (listener.isInterrupted()) {
            throw new ImportExportException("Schema import was interrupted on user's request.");
        }
    }
}