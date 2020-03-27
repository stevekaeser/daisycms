/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.frontend.translmgmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.datatype.StaticSelectionList;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.util.I18nMessage;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.MultiXMLizable;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerx.daisy.x10.SearchResultDocument;
import org.outerx.daisy.x10.SearchResultDocument.SearchResult.Rows.Row;

public class TranslationManagementSearchApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable {
    private ServiceManager serviceManager;
    private Repository repository;
    private SiteConf siteConf;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Locale locale = frontEndContext.getLocale();
        siteConf = frontEndContext.getSiteConf();
        repository = frontEndContext.getRepository();

        Form form = FormHelper.createForm(serviceManager, "resources/form/transl_mgmt_search_definition.xml");
        initForm(form);
        boolean endProcessing = false;

        if (request.getParameter("language") != null) { // check if form submission by testing presence of one of the fields
            endProcessing = form.process(new FormContext(request, locale));
        }

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("CocoonFormsInstance", form);
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("locale", locale);

        if (endProcessing) {
            String query = getQuery(form);
            SearchResultDocument searchResultDocument = repository.getQueryManager().performQuery(query, null, null, locale);

            String queryType = getQueryType(form);
            MultiXMLizable pageXml = new MultiXMLizable(new XmlObjectXMLizable(searchResultDocument));
            Language[] languages = repository.getVariantManager().getAllLanguages(false).getArray();
            if (queryType.equals("overview")) {
                Map<Long, Integer> columnByLanguageId = new HashMap<Long, Integer>();
                Map<String, Integer> branchCountMap = new HashMap<String, Integer>(); // counts the number of distinct branches per document id 
                
                for (int i = 0; i < languages.length; i++) {
                    columnByLanguageId.put(languages[i].getId(), i);
                }

                int langIdPos = 3;
                int refLangIdPos = 6;
                
                List<OverviewRowData> rows = new ArrayList<OverviewRowData>();
                String lastDocumentId = null;
                long lastBranchId = -1;
                OverviewRowData rowData = null;
                
                for (Row row: searchResultDocument.getSearchResult().getRows().getRowList()) {
                    if (!row.getDocumentId().equals(lastDocumentId) || row.getBranchId() != lastBranchId) {
                      String docBranchKey = row.getDocumentId() + "@" + row.getBranchId();
                      int branchCount = 1 + (branchCountMap.containsKey(docBranchKey)?branchCountMap.get(docBranchKey):0);
                      branchCountMap.put(docBranchKey, branchCount);
                      lastDocumentId = row.getDocumentId();
                      lastBranchId = row.getBranchId();
                      rowData = new OverviewRowData(row, branchCount, new Row[languages.length]);
                      rows.add(rowData);
                    }
                    if (row.getValueList().get(langIdPos).equals(row.getValueList().get(refLangIdPos))) {
                        rowData.exampleRow = row;
                    }
                    rowData.variantRows[columnByLanguageId.get(row.getLanguageId())] = row;
                }
                viewData.put("branchCount", branchCountMap);
                viewData.put("languages", languages);
                viewData.put("tmOverviewData", rows);
            }
            viewData.put("pageXml", pageXml);
        }

        appleResponse.sendPage("Form-transl_mgmt_search-Pipe", viewData);
    }

    private void initForm(Form form) throws RepositoryException {

        // Branches selection list
        Branch[] branches = repository.getVariantManager().getAllBranches(false).getArray();
        Arrays.sort(branches, BRANCH_COMPARATOR);

        StaticSelectionList branchesList = new StaticSelectionList(((Field)form.getChild("branch")).getDatatype());
        branchesList.addItem("", new I18nMessage("select-any"));

        for (Branch branch : branches) {
            branchesList.addItem(branch.getName());
        }

        Field branchField = (Field)form.getChild("branch");
        branchField.setSelectionList(branchesList);
        branchField.setValue(repository.getVariantManager().getBranch(siteConf.getBranchId(), false).getName());


        // Collections selection list
        DocumentCollection[] collections = repository.getCollectionManager().getCollections(false).getArray();
        Arrays.sort(collections, COLLECTION_COMPARATOR);

        StaticSelectionList collectionsList = new StaticSelectionList(((Field)form.getChild("collection")).getDatatype());
        collectionsList.addItem("", new I18nMessage("select-any"));

        for (DocumentCollection collection : collections) {
            collectionsList.addItem(collection.getName());
        }

        Field collectionField = (Field)form.getChild("collection");
        collectionField.setSelectionList(collectionsList);
        collectionField.setValue(repository.getCollectionManager().getCollection(siteConf.getCollectionId(), false).getName());

        // Language
        org.outerj.daisy.repository.variant.Language[] languages = repository.getVariantManager().getAllLanguages(false).getArray();
        Arrays.sort(languages, LANGUAGE_COMPARATOR);

        StaticSelectionList languagesList = new StaticSelectionList(((Field)form.getChild("language")).getDatatype());
        languagesList.addItem("", new I18nMessage("select-any"));
        
        for (Language language : languages) {
            languagesList.addItem(language.getName());
        }

        Field languageField = (Field)form.getChild("language");
        languageField.setSelectionList(languagesList);
        languageField.setValue(repository.getVariantManager().getLanguage(siteConf.getLanguageId(), false).getName());

        // Reference language
        StaticSelectionList refLangList = new StaticSelectionList(((Field)form.getChild("referenceLanguage")).getDatatype());
        refLangList.addItem("", new I18nMessage("select-any"));

        for (Language language : languages) {
            refLangList.addItem(language.getName());
        }

        Field refLangField = (Field)form.getChild("referenceLanguage");
        refLangField.setSelectionList(refLangList);
    }

    private String getQuery(Form form) {
        String queryType = getQueryType(form);
        String refLangVersion = (String)form.getChild("referenceLanguageVersion").getValue();
        String refLang = (String)form.getChild("referenceLanguage").getValue();
        String lang = (String)form.getChild("language").getValue();

        StringBuilder query = new StringBuilder();
        query.append("select id, branch, language, languageId, name, referenceLanguage, referenceLanguageId ");

        String majorChangeField = refLangVersion.equals("last") ? "lastMajorChangeVersionId" : "liveMajorChangeVersionId";

        if (queryType.equals("in-sync") || queryType.equals("not-in-sync") || queryType.equals("not-synced-with-reference-language") || queryType.equals("overview"))
            query.append(", versionId, syncedWith.language, syncedWith.versionId, syncedWith=>").append(majorChangeField);

        query.append(" where true ");

        if (!queryType.equals("overview")) {
            String collection = (String)form.getChild("collection").getValue();
            if (collection != null)
                query.append(" and InCollection(").append(QueryHelper.formatString(collection)).append(") ");
        }

        String branch = (String)form.getChild("branch").getValue();
        if (branch != null)
            query.append(" and branch = ").append(QueryHelper.formatString(branch));


        if (queryType.equals("in-sync")) {
            query.append(getIsTranslationClause());
            query.append(getLanguageEqualsClause(lang));
            query.append(getRefLangEqualsClause(refLang));
            query.append(" and LangInSync(").append(QueryHelper.formatString(refLangVersion)).append(") ");
        } else if (queryType.equals("not-in-sync")) {
            query.append(getIsTranslationClause());
            query.append(getLanguageEqualsClause(lang));
            query.append(getRefLangEqualsClause(refLang));
            query.append(" and LangNotInSync(").append(QueryHelper.formatString(refLangVersion)).append(") ");
        } else if (queryType.equals("translation-does-not-exist")) {
            query.append(getIsReferenceClause());
            query.append(getRefLangEqualsClause(refLang));
            query.append(" and variants has none(").append(QueryHelper.formatString(branch + ":" + lang)).append(") "); // branch and language required!
        } else if (queryType.equals("only-translation-exists")) {
            // here we don't require reference doc to be set, this is to search for documents which (by accident or not) only exist in translated variant
            query.append(getIsReferenceClause());
            query.append(getLanguageEqualsClause(lang));
            query.append(" and variants has none(").append(QueryHelper.formatString(branch + ":" + refLang)).append(") "); // branch and ref lang required!
        } else if (queryType.equals("no-major-changes")) {
            query.append(getIsReferenceClause());
            query.append(getLanguageEqualsClause(lang));
            query.append(" and ").append(majorChangeField).append(" is null ");
        } else if (queryType.equals("no-reference-language")) {
            query.append(" and referenceLanguage is null ");
            query.append(getLanguageEqualsClause(lang));
        } else if (queryType.equals("not-synced-with-reference-language")) { // can apply both to reference variants and translated variants
            query.append(getRefLangEqualsClause(refLang));
            query.append(" and syncedWith is not null and syncedWith.languageId != referenceLanguageId");
        } else if (queryType.equals("overview")) {
            query.append(" and referenceLanguage is not null");
            query.append(getRefLangEqualsClause(refLang));
        } else {
            throw new RuntimeException("Invalid query type: " + queryType);
        }
        
        if (queryType.equals("overview")) {
            query.append(" order by id, branch");
        } else {
            query.append(" order by name ");
        }

        String languageVersion = (String)form.getChild("languageVersion").getValue();
        if (languageVersion.equals("last"))
            query.append(" option point_in_time = 'last'");

        return query.toString();
    }

    private String getQueryType(Form form) {
        return (String)form.getChild("query").getValue();
    }

    private String getIsTranslationClause() {
        return " and referenceLanguage is not null and referenceLanguageId != languageId"; 
    }
    
    private String getIsReferenceClause() {
        return " and referenceLanguage is not null and referenceLanguageId = languageId";
    }

    private String getLanguageEqualsClause(String lang) {
        if (lang != null)
            return " and language = " + QueryHelper.formatString(lang); 
        else
            return "";
    }

    private String getRefLangEqualsClause(String refLang) {
        if (refLang != null)
            return " and referenceLanguage = " + QueryHelper.formatString(refLang);
        else
            return "";
    }

    private static Comparator<Branch> BRANCH_COMPARATOR = new Comparator<Branch>() {
        public int compare(Branch o1, Branch o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    private static Comparator<Language> LANGUAGE_COMPARATOR = new Comparator<Language>() {
        public int compare(Language o1, Language o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

    private static Comparator<DocumentCollection> COLLECTION_COMPARATOR = new Comparator<DocumentCollection>() {
        public int compare(DocumentCollection o1, DocumentCollection o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };
    
    public class OverviewRowData {
        private Row exampleRow;
        private int branchIndex;
        private Row[] variantRows;

        public OverviewRowData(Row exampleRow, int branchIndex, Row[] variantRows) {
            this.exampleRow = exampleRow;
            this.branchIndex = branchIndex;
            this.variantRows = variantRows;
        }

        public Row getExampleRow() {
            return exampleRow;
        }

        public void setExampleRow(Row exampleRow) {
            this.exampleRow = exampleRow;
        }

        public Row[] getVariantRows() {
            return variantRows;
        }

        public void setVariantRows(Row[] variantRows) {
            this.variantRows = variantRows;
        }

        public int getBranchIndex() {
            return branchIndex;
        }

        public void setBranchIndex(int branchIndex) {
            this.branchIndex = branchIndex;
        }
    }
}