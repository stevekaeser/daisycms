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
package org.outerj.daisy.books.publisher.impl.bookmodel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.StringTokenizer;

import org.apache.xmlbeans.QNameSet;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.books.publisher.impl.util.BookVariablesConfigBuilder;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Bookdef.BookDocument;
import org.outerx.daisy.x10Bookdef.ImportNavigationTreeDocument;
import org.outerx.daisy.x10Bookdef.QueryDocument;
import org.outerx.daisy.x10Bookdef.SectionDocument;
import org.outerx.daisy.x10Publisher.NavigationTreeDocument;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.outerx.daisy.x10Publisher.ResolveVariablesDocument;
import org.outerx.daisy.x10Publisher.VariablesConfigType;
import org.outerx.daisy.x10Publisher.VariantKeyType;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BookBuilder {
    private Repository repository;
    private VariantManager variantManager;
    private boolean used = false;
    private long dataBranchId;
    private long dataLanguageId;
    private VersionMode dataVersion;
    private Locale locale;
    private Map<String, String> bookMetaData;
    private VariablesConfigType variablesConfig;

    public BookBuilder(Repository repository, long dataBranchId, long dataLanguageId, Map<String, String> bookMetaData,
            VersionMode dataVersion, Locale locale) {
        this.repository = repository;
        this.variantManager = repository.getVariantManager();
        this.dataBranchId = dataBranchId;
        this.dataLanguageId = dataLanguageId;
        this.bookMetaData = bookMetaData;
        this.dataVersion = dataVersion;
        this.locale = locale;
    }

    /**
     * Note: input stream must be closed by the caller.
     */
    public Book buildBook(InputStream is) throws Exception {
        if (used)
            throw new Exception("A BookBuilder can only be used once");
        used = true;

        variablesConfig = BookVariablesConfigBuilder.buildVariablesConfig(bookMetaData, String.valueOf(dataBranchId), String.valueOf(dataLanguageId));

        Book book = new Book();
        XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
        BookDocument bookDocument = BookDocument.Factory.parse(is, xmlOptions);
        XmlObject[] contentNodes = bookDocument.getBook().getContent().selectChildren(QNameSet.ALL);
        buildSections(book, contentNodes);
        resolveVariablesInTitles(book);
        return book;
    }

    private void buildSections(SectionContainer sectionContainer, XmlObject[] nodes) throws Exception {
        for (XmlObject node : nodes) {
            if (node instanceof QueryDocument.Query) {
                processQuery(sectionContainer, (QueryDocument.Query)node);
            } else if (node instanceof ImportNavigationTreeDocument.ImportNavigationTree) {
                processImportNavigationTree(sectionContainer, (ImportNavigationTreeDocument.ImportNavigationTree)node);
            } else if (node instanceof SectionDocument.Section) {
                SectionDocument.Section sectionXml = (SectionDocument.Section)node;
                Section section = new Section();

                if (sectionXml.isSetDocumentId())
                    section.setDocumentId(sectionXml.getDocumentId());
                if (sectionXml.isSetBranch())
                    section.setBranchId(variantManager.getBranch(sectionXml.getBranch(), false).getId());
                if (sectionXml.isSetLanguage())
                    section.setLanguageId(variantManager.getLanguage(sectionXml.getLanguage(), false).getId());
                section.setVersion(sectionXml.getVersion());
                section.setTitle(sectionXml.getTitle());
                section.setType(sectionXml.getType());
                section.setBookStorePath(sectionXml.getBookStorePath());

                // recursively build child section
                XmlObject[] childNodes = sectionXml.selectChildren(QNameSet.ALL);
                buildSections(section, childNodes);

                sectionContainer.addSection(section);
            } else {
                XmlOptions xmlOptions = new XmlOptions();
                xmlOptions.setSaveOuter();
                throw new Exception("Invalid node encountered in book definition: " + node.xmlText(xmlOptions));
            }

        }
    }

    private void processQuery(SectionContainer sectionContainer, QueryDocument.Query queryNode) throws Exception {
        String query = queryNode.getQ();
        String sectionType = queryNode.getSectionType();
        VariantKey[] queryResult;

        Map<String, String> queryOptions = new HashMap<String, String>();
        //TODO: should dataVersion become a VersionMode?
        queryOptions.put("point_in_time", dataVersion.toString());

        if (!queryNode.isSetFilterVariants() /* missing attribute means true */ || queryNode.getFilterVariants()) {
            String extraCond = "branchId = " + dataBranchId + " and languageId = " + dataLanguageId;
            queryResult = repository.getQueryManager().performQueryReturnKeys(query, extraCond, queryOptions, Locale.getDefault());
        } else {
            queryResult = repository.getQueryManager().performQueryReturnKeys(query, null, queryOptions, Locale.getDefault());
        }

        for (VariantKey variantKey : queryResult) {
            Section section = new Section();
            section.setDocumentId(variantKey.getDocumentId());
            section.setBranchId(variantKey.getBranchId());
            section.setLanguageId(variantKey.getLanguageId());
            section.setType(sectionType); // could be null, doesn't matter
            sectionContainer.addSection(section);
        }
    }

    private void processImportNavigationTree(SectionContainer sectionContainer, ImportNavigationTreeDocument.ImportNavigationTree importNavTreeNode) throws Exception {
        // Determine navigation doc
        String navigationDocId = importNavTreeNode.getId();
        long branchId = dataBranchId;
        long languageId = dataLanguageId;

        if (importNavTreeNode.isSetBranch())
            branchId = variantManager.getBranch(importNavTreeNode.getBranch(), false).getId();

        if (importNavTreeNode.isSetLanguage())
            languageId = variantManager.getLanguage(importNavTreeNode.getLanguage(), false).getId();

        // build publisher request
        PublisherRequestDocument publisherRequestDocument = PublisherRequestDocument.Factory.newInstance();
        PublisherRequestDocument.PublisherRequest publisherRequestXml = publisherRequestDocument.addNewPublisherRequest();
        publisherRequestXml.setVariablesConfig(variablesConfig);
        publisherRequestXml.setVersionMode(dataVersion.toString());
        publisherRequestXml.setLocale(locale.toString());

        NavigationTreeDocument.NavigationTree navTreePubReq = publisherRequestXml.addNewNavigationTree();
        navTreePubReq.setContextualized(false);
        VariantKeyType navDoc = navTreePubReq.addNewNavigationDocument();
        navDoc.setId(navigationDocId);
        navDoc.setBranch(String.valueOf(branchId));
        navDoc.setLanguage(String.valueOf(languageId));

        Publisher publisher = (Publisher)repository.getExtension("Publisher");
        publisher.processRequest(publisherRequestDocument, new Navigation2BookHandler(importNavTreeNode.getPath(), sectionContainer));
    }

    static class Navigation2BookHandler extends DefaultHandler {
        private String[] path;
        private boolean[] pathMatches;
        private int nesting = -1;
        private boolean inSubtree;
        private Stack<SectionContainer> parents = new Stack<SectionContainer>();
        private static final String NAVIGATION_NS = "http://outerx.org/daisy/1.0#navigation";

        public Navigation2BookHandler(String pathSpec, SectionContainer sectionContainer) {
            if (pathSpec == null) {
                inSubtree = true;
                path = new String[0];
            } else {
                List<String> pathList = new ArrayList<String>();
                StringTokenizer tokenizer = new StringTokenizer(pathSpec, "/");
                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken().trim();
                    if (token.length() != 0)
                        pathList.add(token);
                }
                path = pathList.toArray(new String[pathList.size()]);
                pathMatches = new boolean[path.length];
            }
            parents.push(sectionContainer);
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (uri.equals(NAVIGATION_NS)) {
                if (localName.equals("doc") || localName.equals("group")) {
                    nesting++;
                    String nodeId = attributes.getValue("id");
                    if (!inSubtree && nodeId != null && nesting < path.length && path[nesting].equals(nodeId)
                            && previousLevelsMatch(nesting)) {
                        pathMatches[nesting] = true;
                        if (nesting == path.length - 1)
                            inSubtree = true;
                    }

                    if (inSubtree) {
                        if (localName.equals("doc")) {
                            Section section = new Section();
                            section.setDocumentId(attributes.getValue("documentId"));
                            section.setBranchId(Long.parseLong(attributes.getValue("branchId")));
                            section.setLanguageId(Long.parseLong(attributes.getValue("languageId")));
                            parents.peek().addSection(section);
                            parents.push(section);
                        } else if (localName.equals("group") && nesting >= path.length) {
                            // the condition "nesting > path.length" is to only import the children of the group node
                            String title = attributes.getValue("label");
                            Section section = new Section();
                            section.setTitle(title);
                            parents.peek().addSection(section);
                            parents.push(section);
                        }
                    }
                }
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (uri.equals(NAVIGATION_NS)) {
                if (localName.equals("doc") || localName.equals("group")) {
                    if (inSubtree && !(localName.equals("group") && nesting == path.length - 1))
                        parents.pop();
                    if (inSubtree && nesting == path.length - 1)
                        inSubtree = false;
                    if (nesting < path.length)
                        pathMatches[nesting] = false;
                    nesting--;
                }
            }
        }

        private boolean previousLevelsMatch(int level) {
            for (int i = 0; i < level; i++) {
                if (!pathMatches[i])
                    return false;
            }
            return true;
        }
    }

    private void resolveVariablesInTitles(SectionContainer sectionContainer) throws RepositoryException, SAXException {
        List<Section> sectionsWithTitles = new ArrayList<Section>();
        for (Section section : sectionContainer.getSections()) {
            collectSectionsWithTitles(section, sectionsWithTitles);
        }

        if (sectionsWithTitles.size() == 0)
            return;

        PublisherRequestDocument publisherRequestDocument = PublisherRequestDocument.Factory.newInstance();
        PublisherRequestDocument.PublisherRequest publisherRequestXml = publisherRequestDocument.addNewPublisherRequest();
        publisherRequestXml.setVariablesConfig(variablesConfig);

        ResolveVariablesDocument.ResolveVariables resolveVariables = publisherRequestXml.addNewResolveVariables();
        for (Section section : sectionsWithTitles)
            resolveVariables.addText(section.getTitle());

        Publisher publisher = (Publisher)repository.getExtension("Publisher");
        publisher.processRequest(publisherRequestDocument, new ResolvedTitlesHandler(sectionsWithTitles));
    }

    private void collectSectionsWithTitles(Section section, List<Section> sectionsWithTitles) {
        if (section.getTitle() != null)
            sectionsWithTitles.add(section);

        for (Section child : section.getSections()) {
            collectSectionsWithTitles(child, sectionsWithTitles);
        }
    }

    private static class ResolvedTitlesHandler extends DefaultHandler {
        private StringBuilder buffer = new StringBuilder();
        private boolean inTitle;
        private int index = -1;
        private List<Section> sections;

        public ResolvedTitlesHandler(List<Section> sections) {
            this.sections = sections;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (uri.equals(Constants.PUBLISHER_NS) && localName.equals("text")) {
                buffer.setLength(0);
                inTitle = true;
                index++;
            }
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (inTitle)
                buffer.append(ch, start, length);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (inTitle) {
                inTitle = false;
                sections.get(index).setTitle(buffer.toString());
            }
        }
    }
}
