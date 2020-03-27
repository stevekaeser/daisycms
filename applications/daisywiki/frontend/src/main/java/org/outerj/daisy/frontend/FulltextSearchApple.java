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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.datatype.SelectionList;
import org.apache.cocoon.forms.datatype.StaticSelectionList;
import org.apache.cocoon.forms.event.ProcessingPhase;
import org.apache.cocoon.forms.event.ProcessingPhaseEvent;
import org.apache.cocoon.forms.event.ProcessingPhaseListener;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.MultiValueField;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.util.I18nMessage;
import org.apache.cocoon.forms.util.StringMessage;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.outerj.daisy.frontend.components.config.ConfigurationManager;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.search.SearchConfiguration;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerx.daisy.x10.SearchResultDocument;

/**
 * Apple for performing a simple fulltext search. For the search box on every page.
 */
public class FulltextSearchApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable {
    private static final int DEFAULT_LIMIT = 10;
    private ServiceManager serviceManager;
    private Repository repository;
    private SiteConf siteConf;

    /**
     * listener that prevents validation on the form (we want to trigger validation manually 
     */
    private static final ProcessingPhaseListener SKIP_VALIDATION_LISTENER = new ProcessingPhaseListener() {
        public void phaseEnded(ProcessingPhaseEvent phaseEvent) {
            if (phaseEvent.getPhase().equals(ProcessingPhase.READ_FROM_REQUEST)) {
                phaseEvent.getForm().endProcessing(true);
            }
        }
    };
    
    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Locale locale = frontEndContext.getLocale();
        siteConf = frontEndContext.getSiteConf();
        repository = frontEndContext.getRepository();

        ConfigurationManager configurationManager = frontEndContext.getConfigurationManager();
        Configuration searchAvalonConf = configurationManager.getConfiguration(siteConf.getName(), "search-" + appleRequest.getSitemapParameter("config", "fulltext"));
        SearchConfiguration searchConf = new SearchConfiguration(searchAvalonConf, repository);

        Form form = buildSearchForm(searchConf);

        boolean executeQuery;
        if (request.getParameter("forms_submit_id") == null) {
            if (StringUtils.isEmpty(request.getParameter("query")) && StringUtils.isEmpty(request.getParameter("documentName"))) {
                form.addProcessingPhaseListener(SKIP_VALIDATION_LISTENER);
            }
            executeQuery = form.process(new FormContext(request, locale));
            if (!RequestUtil.getBooleanParameter(request, "disableDefaults", false)) {
                loadDefaults(form, searchConf);
            }
        } else {
            executeQuery = form.process(new FormContext(request, locale));
        }

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("CocoonFormsInstance", form);
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("locale", locale);
        viewData.put("stylesheetPath", searchConf.getStylesheetPath());

        viewData.put("searchConf", searchConf);

        if (executeQuery) {
            Map<String, String> queryOptions = new HashMap<String, String>();
            String offset = form.getChild("offset").getValue().toString();
            queryOptions.put("chunk_offset", offset != null ? offset : "1" );
            queryOptions.put("chunk_length", String.valueOf(DEFAULT_LIMIT));
            SearchResultDocument searchResultDocument = repository.getQueryManager().performQuery(
                    getQuery(form, searchConf), searchConf.getExtraCond(), queryOptions, locale);
            viewData.put("pageXml", new XmlObjectXMLizable(searchResultDocument));
        }

        appleResponse.sendPage("Form-fulltextsearch-Pipe", viewData);
    }

    private void loadDefaults(Form form, SearchConfiguration searchConfiguration) throws Exception,
            RepositoryException, ConfigurationException {
        CollectionManager collectionManager = repository.getCollectionManager();
        RepositorySchema schema = repository.getRepositorySchema();

        SiteConf siteConf = frontEndContext.getSiteConf();

        //Should we add the site collection beforehand in SearchConfiguration?
        //TODO: avoid this work if corresponding request param is set.
        List<String> initCollections = new ArrayList<String>();
        if (searchConfiguration.isCollectionsUseSiteCollection()) {
             initCollections.add(collectionManager.getCollection(siteConf.getCollectionId(), false).getName());
        }
        initCollections.addAll(searchConfiguration.getStaticCollections());
        
        List<String> initDocumentTypes = searchConfiguration.getStaticDocumentTypes();

        List<String> initPartTypes = searchConfiguration.getStaticPartTypes();

        setDefaultValue(form.getChild("queryType"), "literalString");
        setDefaultValue(form.getChild("collection"), initCollections.toArray(new String[initCollections.size()]));
        setDefaultValue(form.getChild("documentType"), initDocumentTypes.toArray(new String[initDocumentTypes.size()]));
        setDefaultValue(form.getChild("excludeDocumentType"), searchConfiguration.isDocumentTypesExcludeDefault());
        setDefaultValue(form.getChild("partType"), initPartTypes.toArray(new String[initPartTypes.size()]));
        setDefaultValue(form.getChild("searchName"), Boolean.TRUE);
        setDefaultValue(form.getChild("searchContent"), Boolean.TRUE);
        setDefaultValue(form.getChild("searchFields"), Boolean.TRUE);
        setDefaultValue(form.getChild("branchId"), new Long(siteConf.getBranchId()));
        setDefaultValue(form.getChild("languageId"), new Long(siteConf.getLanguageId()));
        setDefaultValue(form.getChild("offset"), new Integer(1));
    }

    private void setDefaultValue(Widget widget, Object value) {
        if (request.getParameter(widget.getRequestParameterName()) == null) {
            widget.setValue(value);
        }
    }

    private Form buildSearchForm(SearchConfiguration searchConfiguration)
            throws Exception {
        CollectionManager collectionManager = repository.getCollectionManager();
        RepositorySchema repositorySchema = repository.getRepositorySchema();
        Form form = FormHelper.createForm(serviceManager, "resources/form/fulltextsearch_definition.xml");

        ((MultiValueField)form.getChild("collection")).setSelectionList(createList(collectionManager.getCollections(false).getArray(), form));
        ((MultiValueField)form.getChild("documentType")).setSelectionList(createList(repositorySchema.getAllDocumentTypes(false).getArray(), form));
        ((MultiValueField)form.getChild("partType")).setSelectionList(createList(repositorySchema.getAllPartTypes(false).getArray(), form));
        ((Field)form.getChild("branchId")).setSelectionList(createList(repository.getVariantManager().getAllBranches(false).getArray(), form));
        ((Field)form.getChild("languageId")).setSelectionList(createList(repository.getVariantManager().getAllLanguages(false).getArray(), form));
        
        if  (searchConfiguration.isFullTextShow()) {
            ((Field)form.getChild("query")).setRequired(searchConfiguration.isFullTextRequired());
        }
        
        return form;
    }

    private String getQuery(Form form, SearchConfiguration searchConfiguration) throws RepositoryException {
        String ftQuery = (String)form.getChild("query").getValue();
        String queryType = (String)form.getChild("queryType").getValue();

        long branchId = -1L;
        long languageId = -1L;
        
        if (form.getChild("branchId").getValue() != null) {
            branchId = (Long)form.getChild("branchId").getValue();
        }
        if (form.getChild("languageId").getValue() != null) {
            languageId = (Long)form.getChild("languageId").getValue();
        }

        StringBuilder query = new StringBuilder(300);
        if (ftQuery != null) {
            if (queryType == null || queryType.equals("literalString")) {
                ftQuery = QueryHelper.escapeFullTextQuery(ftQuery);
            }

            query.append("select name, summary, FullTextFragment(4), versionLastModified, Round(score * 100, 0)");
            for (String selectExpr : searchConfiguration.getAdditionalSelectExpr()) {
            	query.append(", ").append(selectExpr);
            }
            query.append(" where FullText(").append(QueryHelper.formatString(ftQuery));
            query.append(", ").append(form.getChild("searchName").getValue().equals(Boolean.TRUE) ? "1" : "0");
            query.append(", ").append(form.getChild("searchContent").getValue().equals(Boolean.TRUE) ? "1" : "0");
            query.append(", ").append(form.getChild("searchFields").getValue().equals(Boolean.TRUE) ? "1" : "0");
            query.append(", ").append(branchId);
            query.append(", ").append(languageId);
            query.append(")");
        } else {
            query.append("select name, summary, '', versionLastModified, 100");
            for (String selectExpr : searchConfiguration.getAdditionalSelectExpr()) {
            	query.append(", ").append(selectExpr);
            }
            query.append("where true ");
        }

        boolean nonFullTextConditions = false;

        String documentName = (String)form.getChild("documentName").getValue();
        if (documentName != null) {
            if (searchConfiguration.isDocumentNameAutoWildcards()) {
                query.append(" and name like ").append(QueryHelper.formatString("%" + documentName + "%"));
            } else {
                query.append(" and name like ").append(QueryHelper.formatString(documentName));
            }
            nonFullTextConditions = true;
        }

        String[] collections = getQueryFormattedStrings((MultiValueField)form.getChild("collection"));
        if (collections.length > 0) {
            query.append(" and InCollection(");
            query.append(StringUtils.join(collections,","));
            query.append(")");
            nonFullTextConditions = true;
        }
        
        String[] documentTypes = getQueryFormattedStrings((MultiValueField)form.getChild("documentType"));
        if (documentTypes.length > 0) {
            if (BooleanUtils.isTrue((Boolean)form.getChild("excludeDocumentType").getValue())) {
                query.append(" and (documentType!=");
                query.append(StringUtils.join(documentTypes, " and documentType!="));
                query.append(")");
            } else {
                query.append(" and (documentType=");
                query.append(StringUtils.join(documentTypes, " or documentType="));
                query.append(")");
            }
            nonFullTextConditions = true;
        }
        
        String[] partTypes = getQueryFormattedStrings((MultiValueField)form.getChild("partType"));
        if (partTypes.length > 0) {
            query.append(" and (HasPart(");
            query.append(StringUtils.join(partTypes, ") or HasPart("));
            query.append("))");
            nonFullTextConditions = true;
        }

        if (nonFullTextConditions) {
            if (branchId != -1)
                query.append(" and branchId = ").append(branchId);
            if (languageId != -1)
                query.append(" and languageId = ").append(languageId);
        }

        return query.toString();
    }
    
    private String[] getQueryFormattedStrings(MultiValueField field) {
        Object[] values = (Object[])field.getValue();
        if (values == null)
            return ArrayUtils.EMPTY_STRING_ARRAY;
        
        List<String> formatted = new ArrayList<String>(values.length);
        
        for (int i=0; i < values.length; i++) {
            String value = (String)values[i];
            if (!StringUtils.isEmpty(value)) {
                formatted.add(QueryHelper.formatString(value));
            }
        }
        
        return (String[]) formatted.toArray(new String[formatted.size()]);
    }
    
    private SelectionList createList(DocumentCollection[] collections, Form form) {
        StaticSelectionList selectionList = new StaticSelectionList(((MultiValueField)form.getChild("collection")).getDatatype());

        for (DocumentCollection collection: collections) {
            selectionList.addItem(collection.getName());
        }

        return selectionList;
    }

    private SelectionList createList(Branch[] branches, Form form) {
        StaticSelectionList selectionList = new StaticSelectionList(((Field)form.getChild("branchId")).getDatatype());
        selectionList.addItem(new Long(-1), new I18nMessage("select-any"));

        for (Branch branch : branches) {
            selectionList.addItem(new Long(branch.getId()), new StringMessage(branch.getName()));
        }

        return selectionList;
    }

    private SelectionList createList(Language[] languages, Form form) {
        StaticSelectionList selectionList = new StaticSelectionList(((Field)form.getChild("languageId")).getDatatype());
        selectionList.addItem(new Long(-1), new I18nMessage("select-any"));

        for (Language language : languages) {
            selectionList.addItem(new Long(language.getId()), new StringMessage(language.getName()));
        }

        return selectionList;
    }

    private SelectionList createList(DocumentType[] documentTypes, Form form) {
        StaticSelectionList selectionList = new StaticSelectionList(((MultiValueField)form.getChild("documentType")).getDatatype());

        for (DocumentType documentType: documentTypes) {
            selectionList.addItem(documentType.getName());
        }

        return selectionList;
    }

    private SelectionList createList(PartType[] partTypes, Form form) {
        StaticSelectionList selectionList = new StaticSelectionList(((MultiValueField)form.getChild("partType")).getDatatype());

        for (PartType partType: partTypes) {
            selectionList.addItem(partType.getName());
        }

        return selectionList;
    }


}
