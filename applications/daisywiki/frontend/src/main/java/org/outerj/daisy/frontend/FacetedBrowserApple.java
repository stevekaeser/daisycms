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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.docbrowser.DocbrowserConfiguration;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.FacetConf;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10.FacetedQueryResultDocument;
import org.outerx.daisy.x10Facetednavdef.FacetedNavigationDefinitionDocument;
import org.outerx.daisy.x10Facetednavdef.FacetedNavigationDefinitionDocument.FacetedNavigationDefinition.Facets.Facet.Properties;
import org.outerx.daisy.x10Facetednavdef.FacetedNavigationDefinitionDocument.FacetedNavigationDefinition.Facets.Facet.Properties.Property;
import org.outerx.daisy.x10Facetednavdef.OptionsDocument.Options;
import org.outerx.daisy.x10Facetednavdef.OptionsDocument.Options.AdditionalSelects.Expression;

public class FacetedBrowserApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Locale locale = frontEndContext.getLocale();
        SiteConf siteConf = frontEndContext.getSiteConf();
        Repository repository = frontEndContext.getRepository();
        
        String optionsId = request.getParameter("opt");
        String where = request.getParameter("where");

        String definitionName = appleRequest.getSitemapParameter("definitionName");
        FacetedNavigationDefinition facetedNavDef = loadFacetDefinitions(definitionName, siteConf, optionsId, request);
        FacetDefinition[] facetDefs = facetedNavDef.getFacetDefinitions();       
        
        Filter[] filters = parseFilters(request, facetedNavDef);
        Condition[] conditions = filtersToConditions(filters, facetedNavDef);
        boolean limitToSiteCollection = RequestUtil.getBooleanParameter(request, "ltsc", facetedNavDef.getLimitToSiteCollection());
        boolean limitToSiteVariant = RequestUtil.getBooleanParameter(request, "ltsv", facetedNavDef.getLimitToSiteVariant());
        String orderBy = RequestUtil.getStringParameter(request, "ord", facetedNavDef.getDefaultOrder());
        
        String[] fullTextSearchParamNamesToCopy= {"ftse", "ftsn", "ftsc", "ftsf", "ftsb", "ftsl"};        
        String fullTextSearchQuery = RequestUtil.getStringParameter(request, "ftsq", "").trim();
        boolean fullTextSearchEscape = RequestUtil.getBooleanParameter(request, "ftse", true);
        boolean fullTextSearchName = RequestUtil.getBooleanParameter(request, "ftsn", true);
        boolean fullTextSearchContent = RequestUtil.getBooleanParameter(request, "ftsc", true);
        boolean fullTextSearchFields = RequestUtil.getBooleanParameter(request, "ftsf", true);
        String fullTextSearchBranch = RequestUtil.getStringParameter(request, "ftsb", "").trim();
        String fullTextSearchLanguage = RequestUtil.getStringParameter(request, "ftsl", "").trim();
        String activeNavigationPath = request.getParameter("anp");

        StringBuilder query = new StringBuilder();
        query.append("select ");
        
        // check if there is a document browser configuration specified
        // if so: read required column names from that configuration instead of faceted navigation config 
        String docbrowserConfig = request.getParameter("config");
        DocbrowserConfiguration docbrowserConf=null;
        if(docbrowserConfig!=null && docbrowserConfig.length()>0){
        	// configuration of documentbrowser GUI: 
        	// which columns to show in result, which tabs to show as search params
        	Configuration config = frontEndContext.getConfigurationManager().getConfiguration(siteConf.getName(), "docbrowser-" + docbrowserConfig);
        	docbrowserConf = new DocbrowserConfiguration(config);
        }
        
        List<String> selects = new ArrayList<String>(10);
        boolean withFT = fullTextSearchQuery.length() > 0;
        if(docbrowserConf!=null&&docbrowserConf.getColumns()!=null){
        	// if documentbrowser is configured to show limited list of columns: select only these columns
            
            for (DocbrowserConfiguration.Column column: docbrowserConf.getColumns()) {
                selects.add(column.getName());
            }
        } else {
            if (withFT) {
                selects.add("FullTextFragment()");
                selects.add("Round(score * 100, 0)");
            }
            selects.add("name");
            selects.add("summary");

            AdditionalSelect[] additionalSelects = facetedNavDef.getAdditionalSelects();

            for (AdditionalSelect additionalSelect: additionalSelects) {
                selects.add(additionalSelect.getExpression());
            }
            for (FacetDefinition facetDef: facetDefs) {
                selects.add(facetDef.getExpression());
            }
        }
        query.append(StringUtils.join((String[]) selects.toArray(new String[selects.size()]), ","));
        query.append(" where ");

        if (withFT){
            String fullTextClause = fullTextSearchEscape?QueryHelper.escapeFullTextQuery(fullTextSearchQuery):fullTextSearchQuery;
            query.append("FullText('").append(fullTextClause.replaceAll("'", "''")).append("',")
                .append(fullTextSearchName?"1":"0").append(",")
                .append(fullTextSearchContent?"1":"0").append(",")
                .append(fullTextSearchFields?"1":"0");
                
            if (fullTextSearchBranch.length() > 0 && fullTextSearchLanguage.length() > 0) {
                if (StringUtils.isNumeric(fullTextSearchBranch) && StringUtils.isNumeric(fullTextSearchLanguage)) {
                    query.append(",").append(fullTextSearchBranch)
                        .append(",").append(fullTextSearchLanguage);
                }
            }
            query.append(")");
        } else {
            query.append("true");
        }
        // Note: if the defaultConditions contain a fullTextClause there will be an error if the ftsq the request parameter is present and not empty
        if (facetedNavDef.getDefaultConditions() != null) {
            query.append(" and (")
                .append(facetedNavDef.getDefaultConditions())
                .append(")");
        }
        
        if (conditions.length != 0) {
            query.append(" and (");
            for (int i = 0; i < conditions.length; i++) {
                if (i > 0)
                    query.append(") and (");
                query.append(conditions[i].toString());
            }
            query.append(")");
        }
        if(where!=null && where.length()>0){
        	query.append(" and (");
        	query.append(where);
        	query.append(")");
        }
        if (limitToSiteCollection) {
            String collectionName = repository.getCollectionManager().getCollection(siteConf.getCollectionId(), false).getName();
            query.append(" and InCollection(");
            query.append(QueryHelper.formatString(collectionName));
            query.append(")");
        }
        if (limitToSiteVariant) {
            String branchName = repository.getVariantManager().getBranch(siteConf.getBranchId(), false).getName();
            String languageName = repository.getVariantManager().getLanguage(siteConf.getLanguageId(), false).getName();
            query.append(" and branch = ");
            query.append(QueryHelper.formatString(branchName));
            query.append(" and language = ");
            query.append(QueryHelper.formatString(languageName));
        }

        query.append(" order by ").append(orderBy);
        
        FacetConf[] facetConfs = parseFacetConfs(request, facetedNavDef, facetedNavDef.getAdditionalSelects().length + (withFT?4:2));
       
        VersionMode versionMode = WikiHelper.getVersionMode(request);
 
        Map<String,String> queryOptions = new HashMap<String,String>();
        queryOptions.put("chunk_offset", RequestUtil.getStringParameter(request, "cho", "1"));
        queryOptions.put("chunk_length", RequestUtil.getStringParameter(request, "chl", "10"));
        queryOptions.put("point_in_time", versionMode.toString()); 

        FacetedQueryResultDocument result = repository.getQueryManager().performFacetedQuery(query.toString(), facetConfs , queryOptions, locale);
        
        SaxBuffer navigationBuffer = null;        
        if (activeNavigationPath != null) {
            navigationBuffer = new SaxBuffer();
            NavigationManager navigationManager = (NavigationManager)repository.getExtension("NavigationManager");
            NavigationParams navigationParams = new NavigationParams(siteConf.getNavigationDoc(),
                    frontEndContext.getVersionMode(), activeNavigationPath,
                    siteConf.contextualizedTree(), siteConf.getNavigationDepth(), locale);  
            navigationManager.generateNavigationTree(navigationBuffer, navigationParams, null, true, false);
        }
        
        //passing request params for fts settings
        Map params = facetedNavDef.getFoundRequestParams();
        for (String paramName : fullTextSearchParamNamesToCopy) {
            final String paramValue =request.getParameter(paramName);
            if (paramValue != null && paramValue.length() > 0) 
                params.put(paramName, paramValue);
        }
        
        String resource = appleRequest.getSitemapParameter("resource");
        if (resource == null) {
            HashMap<String, Object> viewData = new HashMap<String,Object>();
            viewData.put("facetedQueryResult", new XmlObjectXMLizable(result));
            viewData.put("filters", filters);
            viewData.put("facetedNavDef", facetedNavDef);
            viewData.put("facetConfs", facetConfs);
            viewData.put("limitToSiteCollection", String.valueOf(limitToSiteCollection));
            viewData.put("limitToSiteVariant", String.valueOf(limitToSiteVariant));
            viewData.put("orderBy", orderBy);
            viewData.put("pageContext", frontEndContext.getPageContext());
            viewData.put("fullTextSearchQuery", fullTextSearchQuery);
            viewData.put("path", getMountPoint() + "/" + siteConf.getName() + "/facetedBrowser/" + definitionName);
            viewData.put("navigationTree", navigationBuffer);

            GenericPipeConfig pipeConf = new GenericPipeConfig();
            pipeConf.setTemplate("resources/xml/faceted_browser.xml");
            pipeConf.setStylesheet(facetedNavDef.getStylesheetSrc());
            viewData.put("pipeConf", pipeConf);

            appleResponse.sendPage("internal/genericPipe", viewData);
        } else if (resource.equals("xml")){
        	HashMap<String, Object> viewData = new HashMap<String,Object>();
            GenericPipeConfig pipeConf = GenericPipeConfig.templateOnlyPipe("resources/xml/querysearch_xmlresults.xml");
            pipeConf.setXmlSerializer();
            pipeConf.setStripNamespaces(true);
            viewData.put("pipeConf", pipeConf);
            viewData.put("pageXml", new XmlObjectXMLizable(result));
            appleResponse.sendPage("internal/genericPipe", viewData);
        }else if (resource.equals("html")){
        	HashMap<String, Object> viewData = new HashMap<String,Object>();
        	viewData.put("facetedQueryResult", new XmlObjectXMLizable(result));
            viewData.put("filters", filters);
            viewData.put("facetedNavDef", facetedNavDef);
            viewData.put("facetConfs", facetConfs);
            viewData.put("limitToSiteCollection", String.valueOf(limitToSiteCollection));
            viewData.put("limitToSiteVariant", String.valueOf(limitToSiteVariant));
            viewData.put("orderBy", orderBy);
            viewData.put("pageContext", frontEndContext.getPageContext());
            viewData.put("path", getMountPoint() + "/" + siteConf.getName() + "/facetedBrowser/" + definitionName);
            viewData.put("navigationTree", navigationBuffer);
        	
            GenericPipeConfig pipeConf = new GenericPipeConfig();
            pipeConf.setTemplate("resources/xml/faceted_browser.xml");
            pipeConf.setStylesheet("daisyskin:xslt/querysearch_facet_xmlresults.xsl");
            pipeConf.setApplyLayout(true);
            pipeConf.setStripNamespaces(true);

            viewData.put("pipeConf", pipeConf);
            appleResponse.sendPage("internal/genericPipe", viewData);
        }else {
            throw new ResourceNotFoundException("Unsupported resource: " + resource);
        }
        
        
    }

    private FacetConf[] parseFacetConfs(Request request, FacetedNavigationDefinition facetedNavDef, int nonFacetColumns) throws Exception {
        FacetDefinition[] facetDefs = facetedNavDef.getFacetDefinitions();
        FacetConf[] facetConfs = new FacetConf[nonFacetColumns + facetDefs.length];
        
        int idx = 0;
        for (int i = 0; i < nonFacetColumns; i++) {
            facetConfs[idx++] = new FacetConf(false);
        }

        for (int i = 0; i < facetDefs.length; i++) {
            String prefix = "fc." + (i + 1) + ".";
            int maxValues = RequestUtil.getIntParameter(request, prefix + "mv", 10);
            boolean sortOnValue = RequestUtil.getBooleanParameter(request, prefix + "sv", true);
            boolean sortAscending = RequestUtil.getBooleanParameter(request, prefix + "sa", true);
            facetConfs[idx] = new FacetConf(true, maxValues, sortOnValue, sortAscending, facetDefs[i].getType());
            facetConfs[idx++].setProperties(facetDefs[i].getFacetProperties());
        }

        return facetConfs;
    }

    private Filter[] parseFilters(Request request, FacetedNavigationDefinition facetedNavDef) throws Exception {
        List<Filter> filters = new ArrayList<Filter>();
        int i = 1;
        while (true) {
            String facetIndex = "f." + i;
            String facetName = request.getParameter(facetIndex + ".fn");
            if (facetName == null)
                break;
            facetedNavDef.getFacetDefinition(facetName);
            String queryValue = RequestUtil.getStringParameter(request, facetIndex + ".qv");
            String displayValue = RequestUtil.getStringParameter(request, facetIndex + ".dv");
            String operatorKey = RequestUtil.getStringParameter(request, facetIndex + ".o");
            boolean isDiscrete = RequestUtil.getBooleanParameter(request, facetIndex + ".d", false);
            Operator operator = operators.get(operatorKey);
            if (operator == null)
                throw new Exception("Non-existing operator: " + operatorKey);

            Filter filter = new Filter(facetName, operator, queryValue, displayValue, isDiscrete);
            filters.add(filter);
            i++;
        }
        return filters.toArray(new Filter[filters.size()]);
    }

    public static class Filter {
        private String facetName;
        private Operator operator;
        private String queryValue;
        private String displayValue;
        private boolean isDiscrete;

        public Filter(String facetName, Operator operator, String queryValue, String displayValue, boolean isDiscrete) {
            this.facetName = facetName;
            this.operator = operator;
            this.queryValue = queryValue;
            this.displayValue = displayValue;
            this.isDiscrete = isDiscrete;
        }

        public String getFacetName() {
            return facetName;
        }

        public Operator getOperator() {
            return operator;
        }

        public String getQueryValue() {
            return queryValue;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        public boolean isDiscrete() {
            return isDiscrete;
        }
    }

    private Condition[] filtersToConditions(Filter[] filters, FacetedNavigationDefinition facetedNavDef) {
        Map<String, Condition> conditions = new HashMap<String, Condition>();

        for (Filter filter : filters) {
            Condition condition = conditions.get(filter.getFacetName());
            FacetDefinition facetDef = facetedNavDef.getFacetDefinition(filter.getFacetName());
            if (condition == null || facetDef.isDiscrete() || filter.getOperator().equals(Operator.HAS_PATH)) {
                condition = new Condition(facetDef.getExpression(), filter.getOperator(), filter.getQueryValue());
                conditions.put(filter.getFacetName(), condition);
            } else {
                condition.addValue(filter.getQueryValue());
            }
        }

        return conditions.values().toArray(new Condition[conditions.size()]);
    }

    public static class Condition {
        private String identifier;
        private String value;
        private List<String> values;
        private Operator operator;

        public Condition(String identifier, Operator operator, String value) {
            this.identifier = identifier;
            this.operator = operator;
            this.value = value;
        }

        public void addValue(String value) {
            if (values == null) {
                values = new ArrayList<String>();
                values.add(this.value);
                this.value = null;
            }
            values.add(value);
        }

        public String toString() {
            if (values != null) {
                StringBuilder result = new StringBuilder();
                result.append(identifier);
                result.append(" has all(");                
                for (int i = 0; i < values.size(); i++) {
                    if (i > 0)
                        result.append(", ");
                    result.append(values.get(i));
                }
                result.append(")");
                return result.toString();
            } else {
                return identifier + " " + operator.getSyntax() + " " + value;
            }
        }
    }

    private static Map<String, Operator> operators = new HashMap<String, Operator>();
    static {
        operators.put(Operator.EQUALS.getKey(), Operator.EQUALS);
        operators.put(Operator.LIKE.getKey(), Operator.LIKE);
        operators.put(Operator.BETWEEN.getKey(), Operator.BETWEEN);
        operators.put(Operator.HAS_PATH.getKey(), Operator.HAS_PATH);
    }

    public static class Operator {
        private final String key;
        private final String syntax;

        private Operator(String key, String syntax) {
            this.key = key;
            this.syntax = syntax;
        }

        public String getKey() {
            return key;
        }

        public String getSyntax() {
            return syntax;
        }

        public String toString() {
            return key;
        }

        public static final Operator EQUALS = new Operator("equals", "=");
        public static final Operator LIKE = new Operator("like", "like");
        public static final Operator BETWEEN = new Operator("between", "between");
        public static final Operator HAS_PATH = new Operator ("has path", "");
    }

    private FacetedNavigationDefinition loadFacetDefinitions(String name, SiteConf siteConf, String optionsId, Request request) throws Exception {
        // TODO NEED CACHING HERE so we don't need to load/parse the definitions on each request
        File file = new File(new File(siteConf.getDirectory(), "facetednavdefs"), name + ".xml");
        if (!file.exists())
            throw new Exception("Faceted navigation definition does not exist: \"" + name + "\"");

        List errors = new ArrayList();
        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
        xmlOptions.setErrorListener(errors);
        xmlOptions.setLoadLineNumbers();
        boolean valid;
        FacetedNavigationDefinitionDocument facetedNavDefDoc;
        try {
            facetedNavDefDoc = FacetedNavigationDefinitionDocument.Factory.parse(file, xmlOptions);

            xmlOptions = new XmlOptions();
            xmlOptions.setErrorListener(errors);
            valid = facetedNavDefDoc.validate(xmlOptions);
        } catch (XmlException e) {
            throw new Exception("Error loading faceted navigation definition: " + formatXmlErrors(errors), e);
        } catch (Exception e) {
            throw new Exception("Error loading faceted navigation definition.", e);
        }

        if (!valid)
            throw new Exception("Error validating faceted navigation definition: " + formatXmlErrors(errors));

        HashSet<String> names = new HashSet<String>();
        List<FacetedNavigationDefinitionDocument.FacetedNavigationDefinition.Facets.Facet> facetDefsXml = facetedNavDefDoc.getFacetedNavigationDefinition().getFacets().getFacetList();
        FacetDefinition[] facetDefs = new FacetDefinition[facetDefsXml.size()];
        for (int i = 0; i < facetDefsXml.size(); i++) {
            FacetedNavigationDefinitionDocument.FacetedNavigationDefinition.Facets.Facet facetDefXml = facetDefsXml.get(i);
            String expression = facetDefsXml.get(i).getExpression();
            String type = facetDefXml.getType();
            Map<String, String> facetProperties = new HashMap<String, String>();
            
            Properties propertiesXml = facetDefXml.getProperties();
            if (propertiesXml != null) {
                for (Property prop : propertiesXml.getPropertyList()) {
                    facetProperties.put(prop.getName(), prop.getValue());
                }
            }
            
            String facetName = makeFacetName(expression);
            if (names.contains(facetName)) {
                String baseFacetName = facetName;
                int s = 1;
                while (names.contains(facetName)) {
                    facetName = baseFacetName + "-" + s;
                    s++;
                }
            }
            names.add(facetName);
            if (type != null)
                facetDefs[i] = new FacetDefinition(facetName, facetDefXml.getExpression(), type, facetDefXml.isSetSortingExpression()?facetDefXml.getSortingExpression():facetDefXml.getExpression());
            else
                facetDefs[i] = new FacetDefinition(facetName, facetDefXml.getExpression(), facetDefXml.isSetSortingExpression()?facetDefXml.getSortingExpression():facetDefXml.getExpression());

            facetDefs[i].setFacetProperties(facetProperties);
        }

        FacetedNavigationDefinitionDocument.FacetedNavigationDefinition.OptionsList optionsListXml = facetedNavDefDoc.getFacetedNavigationDefinition().getOptionsList();
        Options options = facetedNavDefDoc.getFacetedNavigationDefinition().getOptions();
        
        if (optionsListXml != null) {
            Map<String, Options> idOptionsMap = new HashMap<String, Options>();

            for (Options optionsList : optionsListXml.getOptionsList())
                idOptionsMap.put(optionsList.getId(), optionsList);
            
            if (!(optionsId != null && idOptionsMap.containsKey(optionsId)))
                optionsId = optionsListXml.getDefaultOptions();
                           
            options = idOptionsMap.get(optionsId);
        }
        
        AdditionalSelect[] additionalSelects = new AdditionalSelect[(options.isSetAdditionalSelects() ? options.getAdditionalSelects().sizeOfExpressionArray() : 0)];
        if (options.isSetAdditionalSelects()) {
            List<Expression> expressions = options.getAdditionalSelects().getExpressionList();
            for (int i = 0; i < expressions.size(); i++) {
                Expression expression = expressions.get(i);
                XmlCursor cursor = expression.newCursor();
                cursor.toFirstContentToken();
                String expressionBody = cursor.getChars();
                cursor.dispose();
                additionalSelects[i] = new AdditionalSelect(expressionBody, expression.isSetSortingExpression()?expression.getSortingExpression():expressionBody);
            }          
        } 
        
        FacetedNavigationDefinition navDef = new FacetedNavigationDefinition(facetDefs, additionalSelects,
                options.getLimitToSiteCollection(), options.getLimitToSiteVariant(), options.getDefaultConditions(), options.getDefaultOrder(), optionsId, request);
        
        if (facetedNavDefDoc.getFacetedNavigationDefinition().getStylesheet() != null)
            navDef.setStylesheetSrc(facetedNavDefDoc.getFacetedNavigationDefinition().getStylesheet().getSrc());
        
        return navDef;
    }

    private String formatXmlErrors(Collection xmlErrors) {
        StringBuilder message = new StringBuilder();
        for (Object xmlError : xmlErrors) {
            XmlError error = (XmlError)xmlError;
            if (message.length() > 0)
                message.append("\n");
            message.append(error.getMessage());
        }
        return message.toString();
    }

    private String makeFacetName(String expression) {
        StringBuilder name = new StringBuilder(expression.length());
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9'))
                name.append(c);
        }
        return name.toString();
    }

    public static class FacetedNavigationDefinition {
        private FacetDefinition[] facetDefinitions;
        private Map<String, FacetDefinition> facetsByName = new HashMap<String, FacetDefinition>();
        private Map<String, String> foundRequestParams = new HashMap<String, String>();
        private AdditionalSelect[] additionalSelects;
        private boolean limitToSiteCollection;
        private boolean limitToSiteVariant;
        private String defaultConditions;
        private String defaultOrder = "name ASC";
        private String selectedOptions;
        private String stylesheetSrc = "daisyskin:xslt/faceted_browser.xsl";

        public FacetedNavigationDefinition(FacetDefinition[] facetDefinitions, AdditionalSelect[] additionalSelects,
                boolean limitToSiteCollection, boolean limitToSiteVariant, String defaultConditions, String defaultOrder, String selectedOptions, Request request) throws Exception{
            this.facetDefinitions = facetDefinitions;
            for (FacetDefinition facetDefinition : facetDefinitions) {
                if (facetsByName.containsKey(facetDefinition.getName()))
                    throw new RuntimeException("Duplicate facet name: " + facetDefinition.getName());
                facetsByName.put(facetDefinition.getName(), facetDefinition);
            }
            
            RequestParamParser parser = new RequestParamParser(request);
            
            this.additionalSelects = additionalSelects;
            this.limitToSiteCollection = limitToSiteCollection;
            this.limitToSiteVariant = limitToSiteVariant;
            this.defaultConditions = parser.parse(defaultConditions, foundRequestParams);
            if (defaultOrder != null)
                this.defaultOrder = defaultOrder;
            this.selectedOptions = selectedOptions;
        }

        public FacetDefinition[] getFacetDefinitions() {
            return facetDefinitions;
        }

        public FacetDefinition getFacetDefinition(String name) {
            FacetDefinition facetDefinition = facetsByName.get(name);
            if (facetDefinition == null)
                throw new RuntimeException("Non-existing facet name: " + name);
            return facetDefinition;
        }

        public AdditionalSelect[] getAdditionalSelects() {
            return additionalSelects;
        }

        public boolean getLimitToSiteCollection() {
            return limitToSiteCollection;
        }

        public boolean getLimitToSiteVariant() {
            return limitToSiteVariant;
        }

        public String getDefaultConditions() {
            return defaultConditions;
        }

        public String getSelectedOptions() {
            return selectedOptions;
        }

        public void setSelectedOptions(String selectedOptions) {
            this.selectedOptions = selectedOptions;
        }

        public Map getFoundRequestParams() {
            return foundRequestParams;
        }

        public String getStylesheetSrc() {
            return stylesheetSrc;
        }

        public void setStylesheetSrc(String stylesheetSrc) {
            this.stylesheetSrc = stylesheetSrc;
        }

        public String getDefaultOrder() {
            return defaultOrder;
        }
    }

    public static class FacetDefinition {
        private String name;
        private String expression;
        private String type;
        private boolean isDiscrete;
        private String sortingExpression;
        private Map<String, String> facetProperties = new HashMap<String, String>();


        public FacetDefinition(String name, String identifier, String sortingExpression) {
            this(name, identifier, "DEFAULT", sortingExpression);
        }

        public FacetDefinition(String name, String identifier, String type, String sortingExpression) {
            this.name = name;
            this.expression = identifier;
            this.type = type;
            this.sortingExpression = sortingExpression;
            this.isDiscrete = !type.equals("DEFAULT");
        }

        public String getName() {
            return name;
        }

        public String getExpression() {
            return expression;
        }

        public String getType() {
            return type;
        }

        public boolean isDiscrete() {
            return isDiscrete;
        }

        public Map<String, String> getFacetProperties() {
            return facetProperties;
        }

        public void setFacetProperties(Map<String, String> facetProperties) {
            this.facetProperties = facetProperties;
        }

        public String getSortingExpression() {
            return sortingExpression;
        }
        
    }
    
    public static class AdditionalSelect {
        private String expression;
        private String sortingExpression;
        
        public AdditionalSelect(String expression, String sortingExpression) {
            this.expression = expression;
            this.sortingExpression = sortingExpression;
        }

        public String getExpression() {
            return expression;
        }

        public String getSortingExpression() {
            return sortingExpression;
        }
        
    }

    private static class RequestParamParser {
        private static String paramRegex = "\\{request-param:(.*?)\\|(.*?)\\}";
        
        private Request request;
        private Pattern pattern;
        
        public RequestParamParser(Request request) {
            this.request = request;
            pattern = Pattern.compile(paramRegex);
        }
        
        public String parse(String s, Map<String, String> foundRequestParams) throws Exception{            
            if (s != null) {
                StringBuffer out = new StringBuffer();
                Matcher m = pattern.matcher(s);            
                while (m.find()) {
                    String requestParam = m.group(1);
                    String defaultValue = m.group(2);
                    String paramValue = RequestUtil.getStringParameter(request, requestParam, defaultValue);
                    m.appendReplacement(out, Matcher.quoteReplacement(paramValue));
                    foundRequestParams.put(requestParam, paramValue);
                }
                m.appendTail(out);
                return out.toString();
            } else {
                return null;
            }
        }    
    }
}
