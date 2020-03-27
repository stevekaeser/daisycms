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
package org.outerj.daisy.tools.importexport.export.config;

import org.outerj.daisy.tools.importexport.model.document.ImpExpDocument;
import org.outerj.daisy.tools.importexport.model.document.ImpExpPart;
import org.outerj.daisy.tools.importexport.util.XmlizerUtil;
import org.outerj.daisy.util.ObjectUtils;

import java.util.*;

/**
 * A default implementation of the {@link DocumentExportCustomizer}.
 */
public class BasicCustomizer implements DocumentExportCustomizer {
    private Set<CombinedKey> fieldsToDrop = new HashSet<CombinedKey>();
    private Set<CombinedKey> partsToDrop = new HashSet<CombinedKey>();
    private Set<String> collectionsToDrop = new HashSet<String>();
    private boolean exportLinks = true;
    private boolean exportCustomFields = true;

    public void preExportFilter(ImpExpDocument impExpDocument) {
        if (!exportCustomFields) {
            impExpDocument.clearCustomFields();
        }

        if (!exportLinks) {
            impExpDocument.clearLinks();
        }

        if (fieldsToDrop.size() > 0) {
            for (CombinedKey key : fieldsToDrop) {
                if (key.getDocumentType() != null && !impExpDocument.getType().equals(key.getDocumentType()))
                    continue;
                impExpDocument.removeField(key.getType());
            }
        }

        if (partsToDrop.size() > 0) {
            for (CombinedKey key : partsToDrop) {
                if (key.getDocumentType() != null && !impExpDocument.getType().equals(key.getDocumentType()))
                    continue;
                impExpDocument.removePart(key.getType());
            }
        }

        if (collectionsToDrop.size() > 0) {
            for (String collection : collectionsToDrop) {
                impExpDocument.removeCollection(collection);
            }
        }
    }

    /**
     *
     * @param documentTypeName specifies the field should only be dropped for this document type,
     *                         leave null to remove it from all document types.
     */
    public void addFieldToDrop(String documentTypeName, String fieldTypeName) {
        if (fieldTypeName == null)
            throw new IllegalArgumentException("Null argument: fieldTypeName");
        fieldsToDrop.add(new CombinedKey(documentTypeName, fieldTypeName));
    }

    public void addPartToDrop(String documentTypeName, String partTypeName) {
        if (partTypeName == null)
            throw new IllegalArgumentException("Null argument: partTypeName");
        partsToDrop.add(new CombinedKey(documentTypeName, partTypeName));
    }

    public void addCollectionToDrop(String collectionName) {
        if (collectionName == null)
            throw new IllegalArgumentException("Null argument: collectionName");
        collectionsToDrop.add(collectionName);
    }

    public void setExportLinks(boolean exportLinks) {
        this.exportLinks = exportLinks;
    }

    public void setExportCustomFields(boolean exportCustomFields) {
        this.exportCustomFields = exportCustomFields;
    }

    public boolean getExportLinks() {
        return exportLinks;
    }

    public boolean getExportCustomFields() {
        return exportCustomFields;
    }

    private static class CombinedKey {
        private String documentType;
        private String type;
        private int hashcode;

        public CombinedKey(String documentType, String type) {
            this.documentType = documentType;
            this.type = type;
            this.hashcode = (documentType + type).hashCode();
        }

        public boolean equals(Object obj) {
            CombinedKey other = (CombinedKey)obj;
            return ObjectUtils.safeEquals(other.documentType, documentType)
                    && ObjectUtils.safeEquals(other.type, type);
        }

        public int hashCode() {
            return hashcode;
        }

        public String getDocumentType() {
            return documentType;
        }

        public String getType() {
            return type;
        }
    }

    public String getXml() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("  <documentCustomizer factoryClass=\"").append(BasicCustomizerFactory.class.getName()).append("\">\n");

        buffer.append("    <exportLinks>").append(exportLinks).append("</exportLinks>\n");
        buffer.append("    <exportCustomFields>").append(exportLinks).append("</exportCustomFields>\n");

        buffer.append("    <!--+\n");
        buffer.append("        | To drop fields, parts or collections from the documents, use:\n");
        buffer.append("        |  <dropField type='...' documentType='...'/>\n");
        buffer.append("        |  <dropPart type='...' documentType='...'/>\n");
        buffer.append("        |  <dropCollection name='...'/>\n");
        buffer.append("        | For dropField and dropPart, the documentType attribute is optional, it specifies\n");
        buffer.append("        | that the field or part should only be dropped for documents of that type.\n");
        buffer.append("        +-->\n");

        for (CombinedKey key : fieldsToDrop) {
            buffer.append("    <dropField type=\"").append(XmlizerUtil.escapeAttr(key.getType())).append("\"");
            if (key.getDocumentType() != null)
                buffer.append(" documentType=\"").append(XmlizerUtil.escapeAttr(key.getDocumentType())).append("\"");
            buffer.append("/>\n");
        }

        for (CombinedKey key : partsToDrop) {
            buffer.append("    <dropPart type=\"").append(XmlizerUtil.escape(key.getType())).append("\"");
            if (key.getDocumentType() != null)
                buffer.append(" documentType=\"").append(XmlizerUtil.escapeAttr(key.getDocumentType())).append("\"");
            buffer.append("/>\n");
        }

        for (String collection : collectionsToDrop) {
            buffer.append("    <dropCollection name=\"").append(XmlizerUtil.escape(collection)).append("\"/>\n");
        }

        buffer.append("  </documentCustomizer>\n");

        return buffer.toString();
    }

	public String exportFilename(String suggestedFilename, ImpExpPart impExpPart) {
		return suggestedFilename;
	}
}
