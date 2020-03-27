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

import org.outerj.daisy.repository.ValueType;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.List;
import java.util.ArrayList;

public abstract class BaseSchemaLoadListener implements SchemaLoadListener {
    private List<LoadInfo> loadedFieldTypes = new ArrayList<LoadInfo>();
    private List<LoadInfo> loadedPartTypes = new ArrayList<LoadInfo>();
    private List<LoadInfo> loadedDocumentTypes = new ArrayList<LoadInfo>();

    public void conflictingFieldType(String fieldTypeName, ValueType requiredType, ValueType foundType) throws Exception {
        throw new Exception("Conflicting field type for " + fieldTypeName + ". Expected " + requiredType + " but found " + foundType + ".");
    }

    public void conflictingMultiValue(String fieldTypeName, boolean needMultivalue, boolean foundMultivalue) throws Exception {
        throw new Exception("Conflicting multivalue setting for field type " + fieldTypeName + ". Expected " + needMultivalue + " but found " + foundMultivalue + ".");
    }

    public void conflictingHierarchical(String fieldTypeName, boolean needHierarchical, boolean foundHierarchical) throws Exception {
        throw new Exception("Conflicting hierarchical setting for field type " + fieldTypeName + ". Expected " + needHierarchical + " but found " + foundHierarchical + ".");
    }

    public void fieldTypeLoaded(String fieldTypeName, SchemaLoadResult result) {
        loadedFieldTypes.add(new LoadInfo(fieldTypeName, result));
    }

    public void partTypeLoaded(String partTypeName, SchemaLoadResult result) {
        loadedPartTypes.add(new LoadInfo(partTypeName, result));
    }

    public void documentTypeLoaded(String documentTypeName, SchemaLoadResult result) {
        loadedDocumentTypes.add(new LoadInfo(documentTypeName, result));
    }

    public List<LoadInfo> getLoadedFieldTypes() {
        return loadedFieldTypes;
    }

    public List<LoadInfo> getLoadedPartTypes() {
        return loadedPartTypes;
    }

    public List<LoadInfo> getLoadedDocumentTypes() {
        return loadedDocumentTypes;
    }

    public int count(List<LoadInfo> list, SchemaLoadResult result) {
        int count = 0;
        for (LoadInfo loadInfo : list) {
            if (loadInfo.getResult() == result)
                count++;
        }
        return count;
    }

    public void done() {
    }

    public static class LoadInfo {
        private String typeName;
        private SchemaLoadResult result;

        public LoadInfo(String typeName, SchemaLoadResult result) {
            this.typeName = typeName;
            this.result = result;
        }

        public String getTypeName() {
            return typeName;
        }

        public SchemaLoadResult getResult() {
            return result;
        }
    }

    public void generateSaxFragment(ContentHandler contentHandler) throws SAXException {
        contentHandler.startElement("", SCHEMA_EL, SCHEMA_EL, new AttributesImpl());
        generateSaxFragment(loadedFieldTypes, "fieldType", contentHandler);
        generateSaxFragment(loadedPartTypes, "partType", contentHandler);
        generateSaxFragment(loadedDocumentTypes, "documentType", contentHandler);        
        contentHandler.endElement("", SCHEMA_EL, SCHEMA_EL);
    }

    private void generateSaxFragment(List<LoadInfo> types, String tagName, ContentHandler contentHandler) throws SAXException {
        for (LoadInfo info : types) {
            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "name", "name", "CDATA", info.getTypeName());
            attrs.addAttribute("", "result", "result", "CDATA", info.getResult().toString());
            contentHandler.startElement("", tagName, tagName, attrs);
            contentHandler.endElement("", tagName, tagName);
        }
    }

    private static final String SCHEMA_EL = "schemaImportResult";
}
