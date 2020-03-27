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
package org.outerj.daisy.frontend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.formmodel.Form;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketEntry;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketHelper;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.editor.LinkFieldHelper;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.frontend.util.XslUtil;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.query.RemoteEvaluationContext;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerx.daisy.x10.SearchResultDocument;

public class QuerySearchApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable, Contextualizable {
    private ServiceManager serviceManager;
    private static List<AutocompleteEntry> DEFAULT_AUTOCOMPLETE_ENTRIES = new ArrayList<AutocompleteEntry>();
    static {
        // core syntax
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("select", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("where", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("order by", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("limit", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("option", null));

        // query options
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("include_retired", "query option"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("search_last_version", "query option"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("point_in_time", "query option"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("style_hint", "query option"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("annotate_link_fields", "query option"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("chunk_offset", "query option"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("chunk_length", "query option"));


        // built-in operators/conditions
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("like", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("not like", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("between", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("not between", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("in", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("not in", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("is null", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("is not null", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("has all ()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("has exactly ()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("has some ()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("has any ()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("has none ()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("matchesPath()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("=>", "link dereferencing"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("InCollection()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("LinksTo()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("LinksFrom()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("LinksToVariant()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("LinksFromVariant()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("IsLinked()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("IsNotLinked()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("HasPart()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("HasPartWithMimeType()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("DoesNotHaveVariant()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("true", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("and", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("or", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("=", "equals operator"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("!=", "not equals operator"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("<", "less than operator"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry(">", "greater than operator"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("<=", "less than or equals operator"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry(">=", "greater than or equals operator"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("+", "addition"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("-", "substraction"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("*", "multiplication"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("/", "division"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("LangInSync()", "in sync with last major change of reference language?"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("LangInSync('live')", "in sync with last-up-to-live major change of reference language?"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("LangNotInSync()", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("LangNotInSync('live')", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("ReverseLangInSync('lang', 'last')", null));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("ReverseLangNotInSync('lang', 'last')", null));

        // functions
        String FUNCTION = "function";
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Concat()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Length()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Left()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Right()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Substring()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("UpperCase()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("LowerCase()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("CurrentDate()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("CurrentDateTime()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Year()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Month()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Week()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("DayOfWeek()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("DayOfMonth()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("DayOfYear()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("RelativeDate()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("RelativeDateTime()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Random()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Mod()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Abs()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Floor()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Ceiling()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Round()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("ContextDoc()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("UserId()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("Path()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("FullText()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("FullTextFragment()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("GetLinkPath()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("ReversePath()", FUNCTION));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("String()", FUNCTION));

        // identifiers
        String IDENTIFIER = "identifier: ";
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("id", IDENTIFIER + "string"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("namespace", IDENTIFIER + "string"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("name", IDENTIFIER + "string - versioned"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("branch", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("branchId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("language", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("languageId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("link", IDENTIFIER + "link (points to current doc)"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("documentType", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("versionId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("creationTime", IDENTIFIER + "datetime"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("ownerId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("ownerLogin", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("ownerName", IDENTIFIER + "string - not searchable"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("summary",  IDENTIFIER + "string - not searchable"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("retired", IDENTIFIER + "boolean"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("private", IDENTIFIER + "boolean"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("lastModified", IDENTIFIER + "datetime"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("lastModifierId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("lastModifierLogin", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("lastModifierName",  IDENTIFIER + "string - not searchable"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("variantLastModified", IDENTIFIER + "datetime"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("variantLastModifierId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("variantLastModifierLogin", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("variantLastModifierName",  IDENTIFIER + "string - not searchable"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("mimeType", "part sub-identifier"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("size", "part sub-identifier"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("content", "part sub-identifier"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("versionCreationTime", IDENTIFIER + "datetime"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("versionCreatorId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("versionCreatorLogin", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("versionCreatorName",  IDENTIFIER + "string - not searchable"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("versionState", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("totalSizeOfParts", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("versionLastModified", IDENTIFIER + "datetime"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("lockType", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("lockTimeAcquired", IDENTIFIER + "datetime"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("lockDuration", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("lockOwnerId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("lockOwnerName",  IDENTIFIER + "string - not searchable"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("lockOwnerLogin", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("collections", IDENTIFIER + "string/symbolic - multivalue"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("collections.valueCount", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("score", IDENTIFIER + "long - not searcheable"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("variants", IDENTIFIER + "link - multivalue"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("liveMajorChangeVersionId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("lastMajorChangeVersionId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("versionComment", IDENTIFIER + "string"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("versionChangeType", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("referenceLanguage", IDENTIFIER + "string/symbolic"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("referenceLanguageId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("syncedWith", IDENTIFIER + "link"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("syncedWith.versionId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("syncedWith.languageId", IDENTIFIER + "long"));
        DEFAULT_AUTOCOMPLETE_ENTRIES.add(new AutocompleteEntry("syncedWith.language", IDENTIFIER + "string/symbolic"));
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        SiteConf siteConf = frontEndContext.getSiteConf();
        Locale locale = frontEndContext.getLocale();
        Repository repository = frontEndContext.getRepository();

        Form form = FormHelper.createForm(serviceManager, "resources/form/querysearch_definition.xml");
        boolean endProcessing = false;
        String daisyQuery = request.getParameter("daisyquery");
        if (daisyQuery != null && request.getParameter("preview") == null) { // if form is present on request
            endProcessing = form.process(new FormContext(request, locale));
        } else {
            // set a default query in the form
            DocumentCollection collection = repository.getCollectionManager().getCollection(siteConf.getCollectionId(), false);
            String branch = repository.getVariantManager().getBranch(siteConf.getBranchId(), false).getName();
            String language = repository.getVariantManager().getLanguage(siteConf.getLanguageId(), false).getName();
            if (daisyQuery == null) {
                form.getChild("daisyquery").setValue("select id, name where InCollection('" + collection.getName() + "') and documentType = 'SimpleDocument' and branch = " + QueryHelper.formatString(branch) + " and language = " + QueryHelper.formatString(language));
            } else {
                form.getChild("daisyquery").setValue(daisyQuery);
            }
        }

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("locale", locale);
        viewData.put("autoCompleteEntries", getAutocompleteEntries(repository));
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("CocoonFormsInstance", form);

        boolean addToDocumentBasket = ((Boolean)form.getChild("addToDocumentBasket").getValue()).booleanValue();

        String resource = appleRequest.getSitemapParameter("resource");

        if (endProcessing) {
            QueryManager queryManager = repository.getQueryManager();

            // if extraConditions, extraOption or extraOrderByClauses are specified: pass them through
            
            String[] queryOptionsList = request.getParameterValues("optionList");
            Map<String, String> queryOptions = null;
           
            if (queryOptionsList != null && queryOptionsList.length > 0) {
                queryOptions = new HashMap<String, String>();
                for (String option : queryOptionsList) {
                    int eqPos = option.indexOf('=');
                    if (eqPos == -1)
                        throw new RuntimeException("Missing equal (=) sign in query option: " + option);

                    String optionValue = option.substring(eqPos + 1);
                    // unquote the option value if necessary
                    if (optionValue.charAt(0) == '\'' && optionValue.charAt(optionValue.length() - 1) == '\'') {
                        optionValue = optionValue.substring(1, optionValue.length() - 1);
                    }

                    queryOptions.put(option.substring(0, eqPos), optionValue);
                }
            }
            
            String contextDocString = request.getParameter("contextDocument");
            RemoteEvaluationContext evaluationContext = new RemoteEvaluationContext();
            
            if (contextDocString != null && contextDocString.length() > 0) {
                evaluationContext.pushContextDocument(LinkFieldHelper.parseVersionKey(contextDocString, repository.getVariantManager()));
            }
        
            SearchResultDocument searchResultDocument = queryManager.performQuery((String)form.getChild("daisyquery").getValue(),request.getParameter("extraCond"),request.getParameter("extraOrderBy"), queryOptions,locale, evaluationContext);
            
            if (addToDocumentBasket) {
                VariantManager variantManager = repository.getVariantManager();
                List<SearchResultDocument.SearchResult.Rows.Row> rows = searchResultDocument.getSearchResult().getRows().getRowList();
                DocumentBasketEntry[] entries = new DocumentBasketEntry[rows.size()];
                int i = 0;
                for (SearchResultDocument.SearchResult.Rows.Row row : rows) {
                    String branch = variantManager.getBranch(row.getBranchId(), false).getName();
                    String language = variantManager.getLanguage(row.getLanguageId(), false).getName();
                    entries[i++] = new DocumentBasketEntry(row.getDocumentId(), branch, language, -3, "");
                }
                DocumentBasketHelper.updateDocumentNames(entries, request, repository);
                DocumentBasketHelper.getDocumentBasket(request, true).appendEntries(entries);
            }

            viewData.put("pageXml", new XmlObjectXMLizable(searchResultDocument));
        };

        if (resource == null) {
            if (addToDocumentBasket)
                appleResponse.redirectTo(getMountPoint() + "/" + siteConf.getName() + "/documentBasket");
            else
                appleResponse.sendPage("Form-querysearch-Pipe", viewData);
        } else if (resource.equals("xml")){
            GenericPipeConfig pipeConf = GenericPipeConfig.templateOnlyPipe("resources/xml/querysearch_xmlresults.xml");
            pipeConf.setXmlSerializer();
            pipeConf.setStripNamespaces(true);
            viewData.put("pipeConf", pipeConf);
            appleResponse.sendPage("internal/genericPipe", viewData);
        } else if (resource.equals("pdf")){
            GenericPipeConfig pipeConf = GenericPipeConfig.templateOnlyPipe("resources/xml/querysearch_xmlresults.xml");
            pipeConf.setStylesheet("daisyskin:xslt/searchresult-to-xslfo.xsl");
            pipeConf.setPdfSerializer();
            viewData.put("pipeConf", pipeConf);
            
            // TODO: i18n and/or put more information in filename
            response.setHeader("Content-Disposition", "attachment; filename=\"searchresults.pdf\"");
            appleResponse.sendPage("internal/genericPipe", viewData);
        } else {
            throw new ResourceNotFoundException("Unsupported resource: " + resource);
        }

    }

    private List<AutocompleteEntry> getAutocompleteEntries(Repository repository) throws RepositoryException {
        List<AutocompleteEntry> entries = new ArrayList<AutocompleteEntry>(DEFAULT_AUTOCOMPLETE_ENTRIES.size() + 100);
        RepositorySchema schema = repository.getRepositorySchema();

        StringBuilder descr = new StringBuilder();

        for (FieldType fieldType : schema.getAllFieldTypes(false).getArray()) {
            descr.setLength(0);
            descr.append("field type: ");
            descr.append(fieldType.getValueType());
            if (fieldType.isMultiValue())
                descr.append(" - multivalue");
            if (fieldType.isHierarchical())
                descr.append(" - hierarchical");
            if (fieldType.isDeprecated())
                descr.append(" - deprecated");
            entries.add(new AutocompleteEntry("$" + fieldType.getName(), descr.toString()));
        }

        for (PartType partType : schema.getAllPartTypes(false).getArray()) {
            descr.setLength(0);
            descr.append("part type");
            if (partType.isDeprecated())
                descr.append(" - deprecated");
            entries.add(new AutocompleteEntry("%" + partType.getName(), null));
        }

        for (DocumentType documentType : schema.getAllDocumentTypes(false).getArray()) {
            descr.setLength(0);
            descr.append("document type");
            if (documentType.isDeprecated())
                descr.append(" - deprecated");
            entries.add(new AutocompleteEntry(documentType.getName(), "document type"));
        }

        for (DocumentCollection collection : repository.getCollectionManager().getCollections(false).getArray()) {
            entries.add(new AutocompleteEntry(collection.getName(), "collection"));
        }

        for (Branch branch : repository.getVariantManager().getAllBranches(false).getArray()) {
            entries.add(new AutocompleteEntry(branch.getName(), "branch"));
        }

        for (Language language : repository.getVariantManager().getAllLanguages(false).getArray()) {
            entries.add(new AutocompleteEntry(language.getName(), "language"));
        }

        entries.addAll(DEFAULT_AUTOCOMPLETE_ENTRIES);

        Collections.sort(entries);

        return entries;
    }

    public static class AutocompleteEntry implements Comparable {
        private String text;
        private String description;
        private String escapedText;
        private String escapedDescription;

        public AutocompleteEntry(String text, String description) {
            this.text = text;
            this.description = description;
            this.escapedText = XslUtil.escape(text);
            this.escapedDescription = XslUtil.escape(description);
        }

        public String getText() {
            return text;
        }

        public String getEscapedText() {
            return escapedText;
        }

        public String getDescription() {
            return description;
        }

        public String getEscapedDescription() {
            return escapedDescription;
        }

        public int compareTo(Object o) {
            return this.text.compareTo(((AutocompleteEntry)o).text);
        }
    }
}
