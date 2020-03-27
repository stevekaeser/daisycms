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
package org.outerj.daisy.tools.importexport.docset;

import org.apache.commons.collections.set.ListOrderedSet;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.variant.Language;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;

/**
 * Builds a {@link DocumentSet} from an XML description of the document set.
 * The XML description can contain both manually enumerated documents as well
 * as queries. See the Daisy documentation for the exact XML syntax.
 */
public class DocumentSetFactory {
    protected Set<VariantKey> variantKeys = ListOrderedSet.decorate(new HashSet<VariantKey>(200));
    protected Repository repository;
    protected VariantManager variantManager;

    public static DocumentSet parseFromXml(InputStream is, Repository repository) throws Exception {
        return new DocumentSetFactory(repository).process(is);
    }

    protected DocumentSetFactory(Repository repository) {
        this.repository = repository;
        this.variantManager = repository.getVariantManager();
    }

    protected DocumentSet process(InputStream is) throws Exception {
        Document document = parse(is);
        process(document);
        return new ListDocumentSet(variantKeys);
    }

    protected Document parse(InputStream is) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);

        Document document = factory.newDocumentBuilder().parse(is);
        return document;
    }

    protected boolean isValidDocumentElementName(String name) {
        return name.equals("documents");
    }

    protected void process(Document document) throws Exception {
        Element root = document.getDocumentElement();

        if (root.getNamespaceURI() != null || !isValidDocumentElementName(root.getLocalName())) {
            throw new ImportExportException("Invalid document (root) element: " + root.getLocalName());
        }

        Element[] elements = DocumentHelper.getElementChildren(root);
        for (Element element : elements) {
            processElement(element);
        }
    }

    protected void addVariantKey(VariantKey variantKey) {
        variantKeys.add(variantKey);
    }

    protected void processElement(Element element) throws Exception {
        if (element.getNamespaceURI() == null && element.getLocalName().equals("document")) {
            String documentId = DocumentHelper.getAttribute(element, "id", true);
            String branch = DocumentHelper.getAttribute(element, "branch", false);
            String language = DocumentHelper.getAttribute(element, "language", false);

            documentId = repository.normalizeDocumentId(documentId);

            long branchId;
            long languageId;

            if (branch == null) {
                branchId = Branch.MAIN_BRANCH_ID;
            } else {
                branchId = variantManager.getBranch(branch, false).getId();
            }

            if (language == null) {
                languageId = Language.DEFAULT_LANGUAGE_ID;
            } else {
                languageId = variantManager.getLanguage(language, false).getId();
            }

            addVariantKey(new VariantKey(documentId, branchId, languageId));
        } else if (element.getNamespaceURI() == null && element.getLocalName().equals("query")) {
            String query = DocumentHelper.getElementText(element, true);
            VariantKey[] queryResults = repository.getQueryManager().performQueryReturnKeys(query, Locale.getDefault());
            for (VariantKey queryResult : queryResults) variantKeys.add(queryResult);
        }
    }
}
