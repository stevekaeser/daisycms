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
package org.outerj.daisy.tools.importexport.model.meta;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.xmlutil.DocumentHelper;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.BufferedInputStream;

public class ImpExpMetaDexmlizer {
    private Document xmlDoc;
    private ImpExpMeta meta;

    public static ImpExpMeta fromXml(InputStream is) throws Exception {
        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document xmlDoc = factory.newDocumentBuilder().parse(is);

        return new ImpExpMetaDexmlizer(xmlDoc).run();
    }

    private ImpExpMetaDexmlizer(Document xmlDoc) {
        this.xmlDoc = xmlDoc;
    }

    private ImpExpMeta run() throws Exception {
        this.meta = new ImpExpMeta();
        parseDocument();
        return meta;
    }

    private void parseDocument() throws Exception {
        Element docEl = xmlDoc.getDocumentElement();
        if (docEl.getNamespaceURI() != null || !docEl.getLocalName().equals("meta")) {
            throw new ImportExportException("Expected root element <meta>");
        }

        Element[] children = DocumentHelper.getElementChildren(docEl, "entry");
        for (Element child : children) {
            String name = DocumentHelper.getAttribute(child, "key", true);
            String value = DocumentHelper.getElementText(child, true);
            meta.put(name, value);
        }
    }
}
