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
package org.outerj.daisy.tools.importexport.config;

import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchema;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpDocumentType;
import org.outerj.daisy.tools.importexport.util.XmlizerUtil;

import java.util.Set;
import java.util.HashSet;

/**
 * A default implementation of the {@link SchemaCustomizer}.
 */
public class BasicSchemaCustomizer implements SchemaCustomizer {
    private Set<String> partTypesToDrop = new HashSet<String>();
    private Set<String> fieldTypesToDrop = new HashSet<String>();
    private Set<String> documentTypesToDrop = new HashSet<String>();

    public void customize(ImpExpSchema impExpSchema) {
        removeDocumentTypes(impExpSchema);
        removePartTypes(impExpSchema);
        removeFieldTypes(impExpSchema);
    }

    private void removePartTypes(ImpExpSchema impExpSchema) {
        for (String partTypeName : partTypesToDrop) {
            impExpSchema.removePartType(partTypeName);

            // also remove it from document types
            ImpExpDocumentType[] documentTypes = impExpSchema.getDocumentTypes();
            for (ImpExpDocumentType documentType : documentTypes) {
                documentType.removeFieldTypeUse(partTypeName);
            }
        }
    }

    private void removeFieldTypes(ImpExpSchema impExpSchema) {
        for (String fieldTypeName : fieldTypesToDrop) {
            impExpSchema.removeFieldType(fieldTypeName);

            // also remove it from document types
            ImpExpDocumentType[] documentTypes = impExpSchema.getDocumentTypes();
            for (ImpExpDocumentType documentType : documentTypes) {
                documentType.removeFieldTypeUse(fieldTypeName);
            }
        }
    }

    private void removeDocumentTypes(ImpExpSchema impExpSchema) {
        for (String documentTypeName : documentTypesToDrop) {
            impExpSchema.removeDocumentType(documentTypeName);
        }
    }

    public void dropPartType(String parTypeName) {
        partTypesToDrop.add(parTypeName);
    }

    public void dropFieldType(String fieldTypeName) {
        fieldTypesToDrop.add(fieldTypeName);
    }

    public void dropDocumentType(String documentTypeName) {
        documentTypesToDrop.add(documentTypeName);
    }

    public String getXml() {
        StringBuilder buffer = new StringBuilder();

        buffer.append("  <schemaCustomizer factoryClass=\"").append(BasicSchemaCustomizerFactory.class.getName()).append("\">\n");

        buffer.append("    <!--+\n");
        buffer.append("        | Use these elements to exclude part, field and document types from the export schema:\n");
        buffer.append("        |  <dropPartType name='...'/>\n");
        buffer.append("        |  <dropFieldType name='...'/>\n");
        buffer.append("        |  <dropDocumentType name='...'/>\n");
        buffer.append("        +-->\n");

        for (String name : partTypesToDrop) {
            buffer.append("    <dropPartType name=\"").append(XmlizerUtil.escapeAttr(name)).append("\"/>\n");
        }

        for (String name : fieldTypesToDrop) {
            buffer.append("    <dropFieldType name=\"").append(XmlizerUtil.escapeAttr(name)).append("\"/>\n");
        }

        for (String name : documentTypesToDrop) {
            buffer.append("    <dropDocumentType name=\"").append(XmlizerUtil.escapeAttr(name)).append("\"/>\n");
        }


        buffer.append("  </schemaCustomizer>\n");

        return buffer.toString();
    }
}
