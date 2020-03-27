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
package org.outerj.daisy.publisher.docpreparation;

import java.util.Locale;

import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.FieldHelper;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.ListItem;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.StaticListItemParent;
import org.outerj.daisy.repository.schema.StaticSelectionList;
import org.outerx.daisy.x10.DocumentDocument;
import org.outerx.daisy.x10.FieldDocument;

public class FieldAnnotator {
    private VersionMode versionMode;
    private long documentBranchId;
    private long documentLanguageId;
    private FieldType fieldType;
    private ValueType fieldValueType;
    private Locale locale;
    private Repository repository;

    public static void annotateFields(DocumentDocument.Document documentXml, Repository repository, Locale locale,
            VersionMode versionMode) throws RepositoryException {
        new FieldAnnotator().annotateFieldsInt(documentXml, repository, locale, versionMode);
    }

    private FieldAnnotator() {
    }

    private void annotateFieldsInt(DocumentDocument.Document documentXml, Repository repository, Locale locale,
            VersionMode versionMode) throws RepositoryException {
        this.locale = locale;
        this.versionMode = versionMode;
        this.repository = repository;
        this.documentBranchId = documentXml.getBranchId();
        this.documentLanguageId = documentXml.getLanguageId();

        RepositorySchema schema = repository.getRepositorySchema();

        for (FieldDocument.Field fieldXml : documentXml.getFields().getFieldList()) {
            fieldType = schema.getFieldTypeById(fieldXml.getTypeId(), false);
            String fieldLabel = fieldType.getLabel(locale);

            boolean hasStaticSelectionList = fieldType.getSelectionList() instanceof StaticSelectionList;
            fieldValueType = fieldType.getValueType();
            String formattedValue = null;
            XmlObject[] valuesXml = fieldXml.selectChildren(QNameSet.ALL);
            Object[] values = (Object[]) FieldHelper.getXmlFieldValueGetter(fieldValueType).getValue(fieldXml, true, fieldType.isHierarchical());
            if (values.length != valuesXml.length)
                throw new PublisherException("Unexpected situation during field annotation: number of XML elements doesn't equal number of values retrieved.");

            for (int k = 0; k < values.length; k++) {
                if (fieldType.isHierarchical()) {
                    HierarchyPath hierarchyPath = (HierarchyPath) values[k];
                    Object[] hierarchyElements = hierarchyPath.getElements();
                    String[] labels = hasStaticSelectionList ? getHierarchicalLabels(hierarchyElements, (StaticSelectionList)fieldType.getSelectionList()) : null;
                    XmlObject[] hierarchicalValuesXml = valuesXml[k].selectChildren(QNameSet.ALL);
                    for (int h = 0; h < hierarchicalValuesXml.length; h++) {
                        annotateFieldValue(hierarchyElements[h], hierarchicalValuesXml[h], labels != null ? labels[h] : null);
                    }
                } else {
                    String label = null;
                    if (hasStaticSelectionList)
                        label = fieldType.getSelectionList().getItemLabel(values[k], locale);
                    formattedValue = annotateFieldValue(values[k], valuesXml[k], label);
                }
            }

            XmlCursor cursor = fieldXml.newCursor();
            cursor.toNextToken();
            cursor.insertAttributeWithValue("label", fieldLabel);
            cursor.insertAttributeWithValue("name", fieldType.getName());
            cursor.insertAttributeWithValue("valueType", fieldValueType.toString());
            cursor.insertAttributeWithValue("multiValue", String.valueOf(fieldType.isMultiValue()));
            cursor.insertAttributeWithValue("hierarchical", String.valueOf(fieldType.isHierarchical()));
            // adding valueFormatted attribute here too for backwards compatibility
            if (!fieldType.isMultiValue() && !fieldType.isHierarchical())
                cursor.insertAttributeWithValue("valueFormatted", formattedValue);
            cursor.dispose();
        }
    }

    private String annotateFieldValue(Object value, XmlObject valueXml, String label) {
        String formattedValue = null;
        if (label != null) {
            formattedValue = label;
        } else if (fieldValueType == ValueType.LINK) {
            VariantKey variantKey = (VariantKey)value;
            long branchId = variantKey.getBranchId() == -1 ? documentBranchId : variantKey.getBranchId();
            long languageId = variantKey.getLanguageId() == -1 ? documentLanguageId : variantKey.getLanguageId();
            try {
                Document document = repository.getDocument(variantKey.getDocumentId(), branchId, languageId, false);

                Version version = document.getVersion(versionMode);
                if (version != null)
                    formattedValue = version.getDocumentName();
            } catch (Throwable e) {
                // ignore (doc does not exist, no access allowed, ...)
            }
        }

        String linkTarget = null;
        if (fieldValueType == ValueType.LINK) {
            linkTarget = FieldHelper.getFormattedValue(value, fieldType.getValueType(), locale, repository);
            if (formattedValue == null)
                formattedValue = linkTarget;
        } else if (formattedValue == null) {
            formattedValue = FieldHelper.getFormattedValue(value, fieldType.getValueType(), locale, repository);
        }

        XmlCursor cursor = valueXml.newCursor();
        cursor.toNextToken();
        if (linkTarget != null)
            cursor.insertAttributeWithValue("target", linkTarget);
        cursor.insertAttributeWithValue("valueFormatted", formattedValue);
        cursor.dispose();

        return formattedValue;
    }

    private String[] getHierarchicalLabels(Object[] elements, StaticSelectionList list) {
        String[] labels = new String[elements.length];
        StaticListItemParent item = list;
        for (int i = 0; i < labels.length; i++) {
            item = item.getItem(elements[i]);
            if (item == null)
                break;
            labels[i] = ((ListItem)item).getLabel(locale);
        }
        return labels;
    }
}
