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
package org.outerj.daisy.books.frontend;

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.repository.Repository;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.cocoon.xml.SaxBuffer;
import org.outerx.daisy.x10.SearchResultDocument;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.util.*;

public class ManageBooksApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        Locale locale = frontEndContext.getLocale();
        SearchResultDocument searchResultDocument = repository.getQueryManager().performQuery("select name, branch, language, $BookPath where documentType = 'BookDefinition' order by name", locale);

        BookGroup rootGroup = buildHierarchicalBookIndex(searchResultDocument.getSearchResult());
        SaxBuffer hierarchicalBookIndex = new SaxBuffer();
        rootGroup.generateSaxFragment(hierarchicalBookIndex);

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("hierarchicalBookIndex", hierarchicalBookIndex);

        appleResponse.sendPage("BookManagementPipe", viewData);
    }

    private BookGroup buildHierarchicalBookIndex(SearchResultDocument.SearchResult searchResult) {
        BookGroup rootGroup = new BookGroup("root");
        List<SearchResultDocument.SearchResult.Rows.Row> rows = searchResult.getRows().getRowList();
        for (SearchResultDocument.SearchResult.Rows.Row row : rows) {
            String[] values = row.getValueList().toArray(new String[0]);
            BookNode bookNode = new BookNode(values[0], values[1], values[2], row.getDocumentId(), row.getBranchId(), row.getLanguageId());
            String path = values[3];
            BookGroup group = rootGroup.getGroup(path);
            group.addChild(bookNode);
        }
        return rootGroup;
    }

    static class BookNode implements BookGroup.BookGroupChild {
        private final String name;
        private final String branch;
        private final String language;
        private final String documentId;
        private final long branchId;
        private final long languageId;

        public BookNode(String name, String branch, String language, String documentId, long branchId, long languageId) {
            this.name = name;
            this.branch = branch;
            this.language = language;
            this.documentId = documentId;
            this.branchId = branchId;
            this.languageId = languageId;
        }

        public int compareTo(Object o) {
            BookNode otherBookNode = (BookNode)o;
            return name.compareTo(otherBookNode.name);
        }

        public void generateSaxFragment(ContentHandler contentHandler) throws SAXException {
            AttributesImpl bookAttrs = new AttributesImpl();
            bookAttrs.addCDATAAttribute("name", name);
            bookAttrs.addCDATAAttribute("branch", branch);
            bookAttrs.addCDATAAttribute("language", language);
            bookAttrs.addCDATAAttribute("documentId", documentId);
            bookAttrs.addCDATAAttribute("branchId", String.valueOf(branchId));
            bookAttrs.addCDATAAttribute("languageId", String.valueOf(languageId));
            contentHandler.startElement("", "book", "book", bookAttrs);
            contentHandler.endElement("", "book", "book");
        }
    }
}
