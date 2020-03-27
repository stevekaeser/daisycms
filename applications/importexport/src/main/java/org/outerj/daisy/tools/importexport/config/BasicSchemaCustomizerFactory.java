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

import org.w3c.dom.Element;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.outerj.daisy.repository.Repository;

/**
 * Factory class building a {@link BasicSchemaCustomizer} instance from
 * XML configuration data. This is the class specified in the import or export properties.
 */
public class BasicSchemaCustomizerFactory {
    /**
     * This create method (with this signature) is required for schema customizer
     * factories.
     */
    public static SchemaCustomizer create(Element element, Repository repository) throws Exception {
        BasicSchemaCustomizer customizer = new BasicSchemaCustomizer();

        Element[] partTypes = DocumentHelper.getElementChildren(element, "partType");
        for (Element partType : partTypes) {
            customizer.dropPartType(DocumentHelper.getAttribute(partType, "name", true));
        }

        Element[] fieldTypes = DocumentHelper.getElementChildren(element, "fieldType");
        for (Element fieldType : fieldTypes) {
            customizer.dropFieldType(DocumentHelper.getAttribute(fieldType, "name", true));
        }

        Element[] docTypes = DocumentHelper.getElementChildren(element, "documentType");
        for (Element docType : docTypes) {
            customizer.dropDocumentType(DocumentHelper.getAttribute(docType, "name", true));
        }

        return customizer;
    }

}
