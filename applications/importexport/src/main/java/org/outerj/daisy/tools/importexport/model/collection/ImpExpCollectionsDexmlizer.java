/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.tools.importexport.model.collection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.xmlutil.DocumentHelper;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.Set;
import java.util.HashSet;

public class ImpExpCollectionsDexmlizer {
    private Document xmlDoc;
    private Set<String> collections = new HashSet<String>();

    public static Set<String> fromXml(InputStream is) throws Exception {
        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document xmlDoc = factory.newDocumentBuilder().parse(is);

        return new ImpExpCollectionsDexmlizer(xmlDoc).run();
    }

    private ImpExpCollectionsDexmlizer(Document xmlDoc) {
        this.xmlDoc = xmlDoc;
    }

    private Set<String> run() throws Exception {
        parseDocument();
        return collections;
    }

    private void parseDocument() throws Exception {
        Element docEl = xmlDoc.getDocumentElement();
        if (docEl.getNamespaceURI() != null || !docEl.getLocalName().equals("collections")) {
            throw new ImportExportException("Expected root element <collections>");
        }

        Element[] children = DocumentHelper.getElementChildren(docEl, "collection");
        for (Element child : children) {
            String collection = DocumentHelper.getElementText(child, true);
            collections.add(collection);
        }
    }
}
