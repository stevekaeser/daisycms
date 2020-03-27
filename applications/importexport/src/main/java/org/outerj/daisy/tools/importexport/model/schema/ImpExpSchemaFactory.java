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
package org.outerj.daisy.tools.importexport.model.schema;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.schema.*;

import java.util.Locale;
import java.util.List;
import java.util.Set;

public class ImpExpSchemaFactory {
    private Repository repository;
    private ImpExpSchema impExpSchema = new ImpExpSchema();

    private ImpExpSchemaFactory(Repository repository) {
        this.repository = repository;
    }

    public interface SchemaFactoryListener {
        void failedDocumentType(String name, Throwable e) throws Exception;
        void failedPartType(String name, Throwable e) throws Exception;
        void failedFieldType(String name, Throwable e) throws Exception;
    }

    /**
     * Builds an ImpExpSchema containing the specified schema types. Part and field types belonging to
     * the specified document types are automatically exported (but may be specified separately too,
     * this won't produce an error or invalid result).
     */
    public static ImpExpSchema build(Set<String> documentTypes, Set<String> fieldTypes, Set<String> partTypes,
            Repository repository, SchemaFactoryListener listener) throws Exception {
        return new ImpExpSchemaFactory(repository).build(documentTypes, fieldTypes, partTypes, listener);
    }

    private ImpExpSchema build(Set<String> documentTypes, Set<String> fieldTypes, Set<String> partTypes, SchemaFactoryListener listener) throws Exception {
        RepositorySchema schema = repository.getRepositorySchema();
        for (String documentTypeName : documentTypes) {
            DocumentType documentType;
            try {
                documentType = schema.getDocumentTypeByName(documentTypeName, false);
            } catch (RepositoryException e) {
                listener.failedDocumentType(documentTypeName, e);
                continue;
            }
            if (!impExpSchema.hasDocumentType(documentType.getName())) {
                ImpExpDocumentType impExpDocumentType = new ImpExpDocumentType(documentType.getName());
                impExpDocumentType.setDeprecated(documentType.isDeprecated());
                copyLabels(documentType, impExpDocumentType);
                copyDescriptions(documentType, impExpDocumentType);

                // field type uses
                FieldTypeUse[] fieldTypeUses = documentType.getFieldTypeUses();
                assureFieldTypesExist(fieldTypeUses);
                for (FieldTypeUse use : fieldTypeUses) {
                    ImpExpFieldTypeUse fieldTypeUse = new ImpExpFieldTypeUse(use.getFieldType().getName(), use.isRequired());
                    fieldTypeUse.setEditable(use.isEditable());
                    impExpDocumentType.addFieldTypeUse(fieldTypeUse);
                }

                // part type uses
                PartTypeUse[] partTypeUses = documentType.getPartTypeUses();
                assurePartTypesExist(partTypeUses);
                for (PartTypeUse use : partTypeUses) {
                    ImpExpPartTypeUse partTypeUse = new ImpExpPartTypeUse(use.getPartType().getName(), use.isRequired());
                    partTypeUse.setEditable(use.isEditable());
                    impExpDocumentType.addPartTypeUse(partTypeUse);
                }

                impExpSchema.addDocumentType(impExpDocumentType);
            }
        }

        for (String fieldTypeName : fieldTypes) {
            FieldType fieldType;
            try {
                fieldType = schema.getFieldTypeByName(fieldTypeName, false);
            } catch (RepositoryException e) {
                listener.failedFieldType(fieldTypeName, e);
                continue;
            }
            assureFieldTypeExists(fieldType);
        }

        for (String partTypeName : partTypes) {
            PartType partType;
            try {
                partType = schema.getPartTypeByName(partTypeName, false);
            } catch (RepositoryException e) {
                listener.failedPartType(partTypeName, e);
                continue;
            }
            assurePartTypeExists(partType);
        }

        return impExpSchema;
    }

    private void assureFieldTypesExist(FieldTypeUse[] fieldTypeUses) {
        for (FieldTypeUse fieldTypeUse : fieldTypeUses) {
            assureFieldTypeExists(fieldTypeUse.getFieldType());
        }
    }

    private void assureFieldTypeExists(FieldType fieldType) {
        if (!impExpSchema.hasFieldType(fieldType.getName())) {
            ImpExpFieldType impExpFieldType = new ImpExpFieldType(fieldType.getName(), fieldType.getValueType());
            impExpFieldType.setDeprecated(fieldType.isDeprecated());
            impExpFieldType.setSize(fieldType.getSize());
            impExpFieldType.setAclAllowed(fieldType.isAclAllowed());
            impExpFieldType.setAllowFreeEntry(fieldType.getAllowFreeEntry());
            impExpFieldType.setLoadSelectionListAsync(fieldType.getLoadSelectionListAsync());
            impExpFieldType.setMultiValue(fieldType.isMultiValue());
            impExpFieldType.setHierarchical(fieldType.isHierarchical());
            copyLabels(fieldType, impExpFieldType);
            copyDescriptions(fieldType, impExpFieldType);

            // selection list
            SelectionList selectionList = fieldType.getSelectionList();
            if (selectionList == null) {
                // do nothing
            } else if (selectionList instanceof StaticSelectionList) {
                ImpExpStaticSelectionList impExpList = buildStaticSelectionList((StaticSelectionList)selectionList, fieldType.getValueType());
                impExpFieldType.setSelectionList(impExpList);
            } else if (selectionList instanceof LinkQuerySelectionList) {
                LinkQuerySelectionList list = (LinkQuerySelectionList)selectionList;
                ImpExpLinkQuerySelectionList impExpList = new ImpExpLinkQuerySelectionList(list.getWhereClause(), list.getFilterVariants());
                impExpFieldType.setSelectionList(impExpList);
            } else if (selectionList instanceof QuerySelectionList) {
                QuerySelectionList list = (QuerySelectionList)selectionList;
                ImpExpQuerySelectionList impExpList = new ImpExpQuerySelectionList(list.getQuery(), list.getSortOrder(), list.getFilterVariants());
                impExpFieldType.setSelectionList(impExpList);
            } else if (selectionList instanceof HierarchicalQuerySelectionList) {
                HierarchicalQuerySelectionList list = (HierarchicalQuerySelectionList)selectionList;
                ImpExpHierarchicalQuerySelectionList impExpList = new ImpExpHierarchicalQuerySelectionList(list.getWhereClause(), list.getLinkFields(), list.getFilterVariants());
                impExpFieldType.setSelectionList(impExpList);
            } else if (selectionList instanceof ParentLinkedSelectionList) {
                ParentLinkedSelectionList list = (ParentLinkedSelectionList)selectionList;
                ImpExpParentLinkedSelectionList impExpList = new ImpExpParentLinkedSelectionList(list.getWhereClause(), list.getParentLinkField(), list.getFilterVariants());
                impExpFieldType.setSelectionList(impExpList);
            } else {
                throw new RuntimeException("Unsupported/unexpected type of selection list: " + selectionList.getClass().getName());
            }

            impExpSchema.addFieldType(impExpFieldType);
        }
    }

    private void assurePartTypesExist(PartTypeUse[] partTypeUses) {
        for (PartTypeUse partTypeUse : partTypeUses) {
            assurePartTypeExists(partTypeUse.getPartType());
        }
    }

    private void assurePartTypeExists(PartType partType) {
        if (!impExpSchema.hasPartType(partType.getName())) {
            ImpExpPartType impExpPartType = new ImpExpPartType(partType.getName());
            impExpPartType.setLinkExtractor(partType.getLinkExtractor());
            impExpPartType.setMimeTypes(partType.getMimeTypes());
            impExpPartType.setDeprecated(partType.isDeprecated());
            impExpPartType.setDaisyHtml(partType.isDaisyHtml());
            copyLabels(partType, impExpPartType);
            copyDescriptions(partType, impExpPartType);
            impExpSchema.addPartType(impExpPartType);
        }
    }

    private static void copyLabels(LabelEnabled labelEnabled, ImpExpLabelEnabled impExpLabelEnabled) {
        for (Locale locale : labelEnabled.getLabelLocales()) {
            String label = labelEnabled.getLabelExact(locale);
            impExpLabelEnabled.addLabel(locale, label);
        }
    }

    private static void copyDescriptions(DescriptionEnabled descriptionEnabled, ImpExpDescriptionEnabled impExpDescriptionEnabled) {
        for (Locale locale : descriptionEnabled.getDescriptionLocales()) {
            String label = descriptionEnabled.getDescriptionExact(locale);
            impExpDescriptionEnabled.addDescription(locale, label);
        }
    }

    private static ImpExpStaticSelectionList buildStaticSelectionList(StaticSelectionList list, ValueType valueType) {
        ImpExpStaticSelectionList impExpList = new ImpExpStaticSelectionList(valueType);
        List<StaticListItem> items = (List<StaticListItem>)list.getItems();
        for (StaticListItem item : items) {
            impExpList.addItem(buildStaticListItem(item, valueType));
        }
        return impExpList;
    }

    private static ImpExpStaticListItem buildStaticListItem(StaticListItem listItem, ValueType valueType) {
        ImpExpStaticListItem impExpItem = new ImpExpStaticListItem(listItem.getValue(), valueType);
        copyLabels(listItem, impExpItem);
        for (StaticListItem child : (List<StaticListItem>)listItem.getItems()) {
            impExpItem.addItem(buildStaticListItem(child, valueType));
        }
        return impExpItem;
    }
}
