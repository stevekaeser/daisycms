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
package org.outerj.daisy.tools.importexport.import_.config;

import org.outerj.daisy.tools.importexport.model.document.ImpExpDocument;
import org.outerj.daisy.tools.importexport.util.XmlizerUtil;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.util.ObjectUtils;

import java.util.*;


// IMPLEMENTOR's NOTE:
//  Please keep the getXml method and the BasicCustomizerFactory in sync
//  when adding or removing functionality.

/**
 * A basic implementation of {@link DocumentImportCustomizer}.
 *
 * It allows to:
 *
 * <ul>
 *   <li>control the part/field store/remove based on document type name and part/field type name.
 *   <li>add the document to collections, or remove the document from collections as part
 *       of the pre-import filtering.
 * </ul>
 *
 */
public class BasicCustomizer implements DocumentImportCustomizer {
    private Set<CombinedKey> partsNotToStore = new HashSet<CombinedKey>();
    private Set<CombinedKey> fieldsNotToStore = new HashSet<CombinedKey>();
    private Set<CombinedKey> partsNotToRemove = new HashSet<CombinedKey>();
    private Set<CombinedKey> fieldsNotToRemove = new HashSet<CombinedKey>();
    private boolean storeLinks = true;
    private boolean storeCustomFields = true;
    private boolean removeFromAllCollections = false;
    private List<String> addToCollections = new ArrayList<String>();
    private List<String> removeFromCollections = new ArrayList<String>();

    public void preImportFilter(ImpExpDocument impExpDocument) {
        if (removeFromAllCollections) {
            impExpDocument.clearCollections();
        } else {
            for (String collection : removeFromCollections) {
                impExpDocument.removeCollection(collection);
            }
        }

        for (String collection : addToCollections) {
            impExpDocument.addCollection(collection);
        }
    }

    public void addDocumentsToCollection(String name) {
        addToCollections.add(name);
    }

    public void removeDocumentsFromCollection(String name) {
        removeFromCollections.remove(name);
    }

    public void setRemoveFromAllCollections(boolean remove) {
        this.removeFromAllCollections = remove;
    }

    public boolean storePart(ImpExpDocument impExpDocument, Document targetDocument, String partTypeName) {
        return !contains(impExpDocument.getType(), partTypeName, partsNotToStore);
    }

    public boolean removePartWhenMissing(ImpExpDocument impExpDocument, Document targetDocument, String partTypeName) {
        return !contains(impExpDocument.getType(), partTypeName, partsNotToRemove);
    }

    public boolean storeField(ImpExpDocument impExpDocument, Document targetDocument, String fieldTypeName) {
        return !contains(impExpDocument.getType(), fieldTypeName, fieldsNotToStore);
    }

    public boolean removeFieldWhenMissing(ImpExpDocument impExpDocument, Document targetDocument, String fieldTypeName) {
        return !contains(impExpDocument.getType(), fieldTypeName, fieldsNotToRemove);
    }

    public boolean storeLinks(ImpExpDocument impExpDocument, Document targetDocument) {
        return storeLinks;
    }

    public boolean storeCustomFields(ImpExpDocument impExpDocument, Document targetDocument) {
        return storeCustomFields;
    }

    public void beforeSaveFilter(Document document) {
    }

    public void setStoreLinks(boolean storeLinks) {
        this.storeLinks = storeLinks;
    }

    public void setStoreCustomFields(boolean storeCustomFields) {
        this.storeCustomFields = storeCustomFields;
    }

    public void addPartNotToStore(String documentType, String partType) {
        addToSet(documentType, partType, partsNotToStore);
    }

    public void addPartNotToRemove(String documentType, String partType) {
        addToSet(documentType, partType, partsNotToRemove);
    }

    public void addFieldNotToStore(String documentType, String fieldType) {
        addToSet(documentType, fieldType, fieldsNotToStore);
    }

    public void addFieldNotToRemove(String documentType, String fieldType) {
        addToSet(documentType, fieldType, fieldsNotToRemove);
    }

    private boolean contains(String documentTypeName, String typeName, Set set) {
        if (set.contains(getKey(documentTypeName, typeName)))
            return true;
        else
            return set.contains(getKey(null, typeName));

    }

    private CombinedKey getKey(String documentType, String typeName) {
        return new CombinedKey(documentType, typeName);
    }

    private void addToSet(String documentType, String typeName, Set<CombinedKey> set) {
        if (typeName == null)
            throw new IllegalArgumentException("Null argument: typeName");

        set.add(getKey(documentType, typeName));
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
        buffer.append("  <documentCustomizer factoryClass=\"org.outerj.daisy.tools.importexport.import_.config.BasicCustomizerFactory\">\n");

        buffer.append("\n");
        buffer.append("    <!--+\n");
        buffer.append("        | To have parts and fields ignored, add them to both doNotAdd and doNotRemove\n");
        buffer.append("        +-->\n\n");

        buffer.append("    <!--+\n");
        buffer.append("        | doNotAdd: do not add these parts and fields from the import to the target document\n");
        buffer.append("        +-->\n");
        buffer.append("    <doNotAdd>\n");
        buffer.append("      <!--+\n");
        buffer.append("          | Use:\n");
        buffer.append("          |   <part documentType='...' type='...'/>\n");
        buffer.append("          |  or\n");
        buffer.append("          |   <field documentType='...' type='...'/>\n");
        buffer.append("          | If documentType is not specified, it applies to all document types.\n");
        buffer.append("          +-->\n");
        generateXmlForSet(partsNotToStore, "part", buffer);
        generateXmlForSet(fieldsNotToStore, "field", buffer);
        buffer.append("    </doNotAdd>\n\n");

        buffer.append("    <!--+\n");
        buffer.append("        | doNotRemove: do not remove these parts and fields from the target document if they are not in the import\n");
        buffer.append("        +-->\n");
        buffer.append("    <doNotRemove>\n");
        generateXmlForSet(partsNotToRemove, "part", buffer);
        generateXmlForSet(fieldsNotToRemove, "field", buffer);
        buffer.append("    </doNotRemove>\n\n");

        buffer.append("    <removeFromCollections");
        if (removeFromAllCollections)
            buffer.append(" all=\"true\"");
        buffer.append(">\n");
        buffer.append("      <!-- List one ore more <collection name='...'> elements,\n");
        buffer.append("           or add an attribute all=\"true\" on this element. -->\n");
        generateCollections(removeFromCollections, buffer);
        buffer.append("    </removeFromCollections>\n\n");

        buffer.append("    <addToCollections>\n");
        buffer.append("      <!-- List one ore more <collection name='...'> elements -->\n");
        generateCollections(removeFromCollections, buffer);
        buffer.append("    </addToCollections>\n\n");


        buffer.append("    <storeLinks>").append(storeLinks).append("</storeLinks>\n");
        buffer.append("    <storeCustomFields>").append(storeLinks).append("</storeCustomFields>\n");
        buffer.append("  </documentCustomizer>\n");
        return buffer.toString();
    }

    private void generateXmlForSet(Set<CombinedKey> set, String elementName, StringBuilder buffer) {
        for (CombinedKey key : set) {
            buffer.append("      <").append(elementName);
            if (key.getDocumentType() != null)
                buffer.append(" documentType=\"").append(XmlizerUtil.escapeAttr(key.getDocumentType())).append("\"");
            buffer.append(" type=\"").append(XmlizerUtil.escapeAttr(key.getType())).append("\"");
            buffer.append("/>\n");
        }
    }

    private void generateCollections(List<String> collections, StringBuilder buffer) {
        for (String collection : collections) {
            buffer.append("      <collection name=\"").append(XmlizerUtil.escapeAttr(collection)).append("\"/>\n");
        }
    }
}
