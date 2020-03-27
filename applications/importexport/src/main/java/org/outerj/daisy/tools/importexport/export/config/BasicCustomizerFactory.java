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

import org.w3c.dom.Element;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.outerj.daisy.repository.Repository;

/**
 * Builds a {@link BasicCustomizer} instance from XML configuration data.
 * This is the class specified in the import or export properties.
 */
public class BasicCustomizerFactory {
    /**
     * This create method (with this signature) is required for customizer factories.
     */
    public static DocumentExportCustomizer create(Element element, Repository repository) throws Exception {
        BasicCustomizer customizer = new BasicCustomizer();

        boolean exportLinks = DocumentHelper.getBooleanElement(element, "exportLinks", customizer.getExportLinks());
        customizer.setExportLinks(exportLinks);

        boolean exportCustomFields = DocumentHelper.getBooleanElement(element, "exportCustomFields", customizer.getExportCustomFields());
        customizer.setExportCustomFields(exportCustomFields);

        Element[] dropFields = DocumentHelper.getElementChildren(element, "dropField");
        for (Element dropField : dropFields) {
            String type = DocumentHelper.getAttribute(dropField, "type", true);
            String documentType = DocumentHelper.getAttribute(dropField, "documentType", false);
            customizer.addFieldToDrop(documentType, type);
        }

        Element[] dropParts = DocumentHelper.getElementChildren(element, "dropPart");
        for (Element dropPart : dropParts) {
            String type = DocumentHelper.getAttribute(dropPart, "type", true);
            String documentType = DocumentHelper.getAttribute(dropPart, "documentType", false);
            customizer.addPartToDrop(documentType, type);
        }

        Element[] dropCollections = DocumentHelper.getElementChildren(element, "dropCollection");
        for (Element dropCollection : dropCollections) {
            String name = DocumentHelper.getAttribute(dropCollection, "name", true);
            customizer.addCollectionToDrop(name);
        }

        return customizer;
    }

}
