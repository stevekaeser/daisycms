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
package org.outerj.daisy.tools.importexport.model.variant;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.xmlutil.DocumentHelper;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.BufferedInputStream;

public class ImpExpVariantsDexmlizer {
    private Document xmlDoc;
    private ImpExpVariants impExpVariants;

    public static ImpExpVariants fromXml(InputStream is) throws Exception {
        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document xmlDoc = factory.newDocumentBuilder().parse(is);

        return new ImpExpVariantsDexmlizer(xmlDoc).run();
    }

    private ImpExpVariantsDexmlizer(Document xmlDoc) {
        this.xmlDoc = xmlDoc;
    }

    private ImpExpVariants run() throws Exception {
        this.impExpVariants = new ImpExpVariants();
        parseDocument();
        return impExpVariants;
    }

    private void parseDocument() throws Exception {
        Element docEl = xmlDoc.getDocumentElement();
        if (docEl.getNamespaceURI() != null || !docEl.getLocalName().equals("variants")) {
            throw new ImportExportException("Expected root element <variants>");
        }

        Element branchesEl = DocumentHelper.getElementChild(docEl, "branches", false);
        if (branchesEl != null)
            loadVariants(DocumentHelper.getElementChildren(branchesEl, "branch"), true);

        Element languagesEl = DocumentHelper.getElementChild(docEl, "languages", false);
        if (languagesEl != null)
            loadVariants(DocumentHelper.getElementChildren(languagesEl, "language"), false);
    }

    private void loadVariants(Element[] variantElements, boolean branch) throws Exception {
        for (int i = 0; i < variantElements.length; i++) {
            Element variantEl = variantElements[i];
            String name = DocumentHelper.getAttribute(variantEl, "name", true);
            String description = DocumentHelper.getAttribute(variantEl, "description", false);
            boolean required = DocumentHelper.getBooleanAttribute(variantEl, "required", false);
            if (branch)
                impExpVariants.addBranch(name, description, required);
            else
                impExpVariants.addLanguage(name, description, required);
        }
    }
}
