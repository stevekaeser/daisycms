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
    public static DocumentImportCustomizer create(Element element, Repository repository) throws Exception {
        BasicCustomizer customizer = new BasicCustomizer();

        Element doNotAdd = DocumentHelper.getElementChild(element, "doNotAdd", false);
        if (doNotAdd != null) {
            // parts
            Element[] parts = DocumentHelper.getElementChildren(doNotAdd, "part");
            for (Element part : parts) {
                String documentType = DocumentHelper.getAttribute(part, "documentType", false);
                String type = DocumentHelper.getAttribute(part, "type", true);
                customizer.addPartNotToStore(documentType, type);
            }

            // fields
            Element[] fields = DocumentHelper.getElementChildren(doNotAdd, "field");
            for (Element field : fields) {
                String documentType = DocumentHelper.getAttribute(field, "documentType", false);
                String type = DocumentHelper.getAttribute(field, "type", true);
                customizer.addFieldNotToStore(documentType, type);
            }
        }

        Element doNotRemove = DocumentHelper.getElementChild(element, "doNotRemove", false);
        if (doNotRemove != null) {
            // parts
            Element[] parts = DocumentHelper.getElementChildren(doNotRemove, "part");
            for (Element part : parts) {
                String documentType = DocumentHelper.getAttribute(part, "documentType", false);
                String type = DocumentHelper.getAttribute(part, "type", true);
                customizer.addPartNotToRemove(documentType, type);
            }

            // fields
            Element[] fields = DocumentHelper.getElementChildren(doNotRemove, "field");
            for (Element field : fields) {
                String documentType = DocumentHelper.getAttribute(field, "documentType", false);
                String type = DocumentHelper.getAttribute(field, "type", true);
                customizer.addFieldNotToRemove(documentType, type);
            }
        }

        Element removeFromCollections = DocumentHelper.getElementChild(element, "removeFromCollections", false);
        if (removeFromCollections != null) {
            if (DocumentHelper.getBooleanAttribute(removeFromCollections, "all", false)) {
                customizer.setRemoveFromAllCollections(true);
            } else {
                Element[] collections = DocumentHelper.getElementChildren(removeFromCollections, "collection");
                for (Element collection : collections) {
                    String name = DocumentHelper.getAttribute(collection, "name", true);
                    customizer.removeDocumentsFromCollection(name);
                }
            }
        }

        Element addToCollections = DocumentHelper.getElementChild(element, "addToCollections", false);
        if (addToCollections != null) {
            Element[] collections = DocumentHelper.getElementChildren(addToCollections, "collection");
            for (Element collection : collections) {
                customizer.addDocumentsToCollection(DocumentHelper.getElementText(collection, true));
            }
        }

        Element storeLinksEl = DocumentHelper.getElementChild(element, "storeLinks", false);
        if (storeLinksEl != null) {
            customizer.setStoreLinks(DocumentHelper.getElementText(storeLinksEl, true).equalsIgnoreCase("true"));
        }

        Element storeCustomFieldsEl = DocumentHelper.getElementChild(element, "storeCustomFields", false);
        if (storeCustomFieldsEl != null) {
            customizer.setStoreCustomFields(DocumentHelper.getElementText(storeCustomFieldsEl, true).equalsIgnoreCase("true"));
        }

        return customizer;
    }
}
