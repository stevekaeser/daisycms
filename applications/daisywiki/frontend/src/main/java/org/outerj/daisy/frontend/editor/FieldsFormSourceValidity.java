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
package org.outerj.daisy.frontend.editor;

import org.apache.excalibur.source.SourceValidity;
import org.apache.avalon.framework.context.Context;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.components.ContextHelper;
import org.apache.commons.jxpath.JXPathContext;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldTypeUse;
import org.outerj.daisy.repository.schema.PartTypeUse;

import java.util.Map;

/**
 * A source validity which remains valid as long as a document type
 * and its contained part and field types do not change (the implementation
 * checks the 'update count' properties of these entities),
 * and as long as the classes of field editors stay the same.
 *
 * <p>It assumes the document type for which it needs to check
 * the validity is available in a flow attribute called "documentType".
 * The FieldEditor's should be available as an array in the flow
 * attribute "fieldEditors".
 *
 * <p>To create an instance, use the method {@link #getValidity}.
 *
 * <p>This validity is used to cache the CForms form definition
 * and form template of the dynamically generated fields form
 * (part of the document editor).
 *
 */
public class FieldsFormSourceValidity implements SourceValidity {
    private Context context;
    private long documentTypeId;
    private long documentTypeUpdateCount;
    private long[] fieldTypeUpdateCounts;
    private long[] partTypeUpdateCounts;
    private Class[] fieldEditorClasses;

    private FieldsFormSourceValidity(Context context, long documentTypeId, long documentTypeUpdateCount,
            long[] fieldTypeUpdateCounts, long[] partTypeUpdateCounts, Class[] fieldEditorClasses) {
        this.context = context;
        this.documentTypeId = documentTypeId;
        this.documentTypeUpdateCount = documentTypeUpdateCount;
        this.fieldTypeUpdateCounts = fieldTypeUpdateCounts;
        this.partTypeUpdateCounts = partTypeUpdateCounts;
        this.fieldEditorClasses = fieldEditorClasses;
    }

    public int isValid() {
        Map objectModel = ContextHelper.getObjectModel(context);
        Object flowContext = FlowHelper.getContextObject(objectModel);
        JXPathContext jxpc = JXPathContext.newContext(flowContext);
        DocumentType documentType = (DocumentType)jxpc.getValue("/documentType");
        FieldEditor[] fieldEditors = (FieldEditor[])jxpc.getValue("/fieldEditors");
        if (documentType == null)
            throw new RuntimeException("Error in fields form validity source validity check: no documentType available in flow context.");
        if (fieldEditors == null)
            throw new RuntimeException("Error in fields form validity source validity check: no fieldEditors available in flow context.");

        if (documentType.getId() != documentTypeId) {
            return SourceValidity.INVALID;
        }

        if (documentType.getUpdateCount() != documentTypeUpdateCount)
            return SourceValidity.INVALID;

        // check field types
        FieldTypeUse[] fieldTypeUses = documentType.getFieldTypeUses();

        if (fieldTypeUses.length != fieldTypeUpdateCounts.length) // this is an impossible situation since the doctyp was not updated
            throw new RuntimeException("Unexpected situation: number of field types do not match.");

        for (int i = 0; i < fieldTypeUses.length; i++) {
            if (fieldTypeUses[i].getFieldType().getUpdateCount() != fieldTypeUpdateCounts[i])
                return SourceValidity.INVALID;
        }

        // Check part types
        PartTypeUse[] partTypeUses = documentType.getPartTypeUses();

        if (partTypeUses.length != partTypeUpdateCounts.length)
            throw new RuntimeException("Unexpected situation: number of part types do not match.");

        for (int i = 0; i < partTypeUses.length; i++) {
            if (partTypeUses[i].getPartType().getUpdateCount() != partTypeUpdateCounts[i])
                return SourceValidity.INVALID;
        }

        // Check field editor classes
        if (fieldEditorClasses.length != fieldEditors.length)
            throw new RuntimeException("Unexpected situation: number of field editors do not match.");

        for (int i = 0; i < fieldEditorClasses.length; i++) {
            if (!fieldEditorClasses[i].equals(fieldEditors[i].getClass()))
                return SourceValidity.INVALID;
        }

        return SourceValidity.VALID;
    }

    public int isValid(SourceValidity sourceValidity) {
        throw new RuntimeException("Unexpected situation: this method should not be called.");
    }

    public static FieldsFormSourceValidity getValidity(DocumentType documentType, FieldEditor[] fieldEditors, Context context) {
        return new FieldsFormSourceValidity(context, documentType.getId(), documentType.getUpdateCount(),
                getFieldTypeUpdateCounts(documentType), getPartTypeUpdateCounts(documentType),
                getFieldEditorClasses(fieldEditors));

    }

    private static long[] getFieldTypeUpdateCounts(DocumentType documentType) {
        FieldTypeUse[] fieldTypeUses = documentType.getFieldTypeUses();
        long[] updateCounts = new long[fieldTypeUses.length];
        for (int i = 0; i < updateCounts.length; i++) {
            updateCounts[i] = fieldTypeUses[i].getFieldType().getUpdateCount();
        }
        return updateCounts;
    }

    private static long[] getPartTypeUpdateCounts(DocumentType documentType) {
        PartTypeUse[] partTypeUses = documentType.getPartTypeUses();
        long[] updateCounts = new long[partTypeUses.length];
        for (int i = 0; i < updateCounts.length; i++) {
            updateCounts[i] = partTypeUses[i].getPartType().getUpdateCount();
        }
        return updateCounts;
    }

    private static Class[] getFieldEditorClasses(FieldEditor[] fieldEditors) {
        Class[] classes = new Class[fieldEditors.length];
        for (int i = 0; i < fieldEditors.length; i++) {
            classes[i] = fieldEditors[i].getClass();
        }
        return classes;
    }
}
