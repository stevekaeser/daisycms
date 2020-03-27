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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.xmlbeans.XmlException;
import org.outerj.daisy.doctaskrunner.DocumentExecutionState;
import org.outerj.daisy.doctaskrunner.DocumentSelection;
import org.outerj.daisy.doctaskrunner.DocumentTaskManager;
import org.outerj.daisy.doctaskrunner.Task;
import org.outerj.daisy.doctaskrunner.TaskDocDetail;
import org.outerj.daisy.doctaskrunner.TaskDocDetails;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.doctaskrunner.commonimpl.SearchActionResultImpl;
import org.outerj.daisy.doctaskrunner.spi.TaskSpecificationImpl;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasket;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketEntry;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketHelper;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerx.daisy.x10DocumentActions.ReplaceParametersDocument;
import org.outerx.daisy.x10DocumentActions.SearchActionResultDocument;
import org.outerx.daisy.x10DocumentActions.SearchParametersDocument;
import org.outerx.daisy.x10DocumentActions.CaseHandlingAttribute.CaseHandling;
import org.outerx.daisy.x10DocumentActions.ReplaceParametersDocument.ReplaceParameters;
import org.outerx.daisy.x10DocumentActions.SearchParametersDocument.SearchParameters;

public class SearchAndReplaceApple extends AbstractDaisyApple implements Serviceable {
    
    private SiteConf siteConf;
    private ServiceManager serviceManager;
    private Repository repository;
    private DocumentTaskManager taskManager;
    private VariantManager variantManager;
    private Locale locale;
    
    private String selectionType;
    private DocumentSelection selection;

    private String query;
    private String documentId;
    private long branchId;
    private long languageId;
    
    private Form form;
    private String needleInputValue;
    private String needle;
    private boolean regexp;
    private String caseHandling;
    private String replacementInputValue;
    private String replacement;

    private Task searchTask;
    private TaskDocDetails searchTaskDocDetails;
    private Task replaceTask;
    private TaskDocDetails replaceTaskDocDetails;
    
    private boolean init = false;
    
    private Set<VariantKey> replaceTaskKeys = new HashSet<VariantKey>();

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        repository = frontEndContext.getRepository();

        if (repository.isInRole("guest") && repository.getActiveRoleIds().length == 1)
            throw new Exception("Search and replace functionality not available for guest users.");

        if (!init) {
            siteConf = frontEndContext.getSiteConf();
            taskManager = (DocumentTaskManager)repository.getExtension("DocumentTaskManager");
            variantManager = repository.getVariantManager();
            locale = frontEndContext.getLocale();
            form = FormHelper.createForm(serviceManager, "resources/form/serp_search_definition.xml");
            init = true;
        }
        
        String resource = appleRequest.getSitemapParameter("resource");
        
        if (resource == null) {
            if (request.getParameter("documentId") != null) {
                selectionType="document";
                documentId = request.getParameter("documentId");
                branchId = RequestUtil.getBranchId(request, siteConf.getBranchId(), repository);
                languageId = RequestUtil.getBranchId(request, siteConf.getLanguageId(), repository);
                selection = taskManager.createEnumerationDocumentSelection(new VariantKey[] { new VariantKey(documentId, branchId, languageId) });
            } else if (request.getParameter("query") != null){
                query = request.getParameter("query");
                selectionType="query";
                selection = taskManager.createQueryDocumentSelection(request.getParameter("query"));
            } else if  (RequestUtil.getBooleanParameter(request, "useBasket", false)) {
                selectionType="basket";
                DocumentBasket documentBasket = DocumentBasketHelper.getDocumentBasket(request, true);
                if (documentBasket == null || documentBasket.size() == 0) {
                    appleResponse.redirectTo(frontEndContext.getMountPoint() + frontEndContext.getSiteConf().getName() + "/searchAndReplace");
                }
                selection = createDocumentBasketBasedSelection(documentBasket);
            }
            
            form.getChild("needle").setValue(request.getParameter("needle"));
            form.getChild("replacement").setValue(request.getParameter("replacement"));
            String caseHandlingParam = RequestUtil.getStringParameter(request, "caseHandling", "sensible");
            CaseHandling.Enum caseHandlingEnum = CaseHandling.Enum.forString(caseHandlingParam);
            caseHandling = caseHandlingEnum==null?"sensible":caseHandlingEnum.toString();
            form.getChild("caseHandling").setValue(caseHandling);
            form.getChild("regexp").setValue(RequestUtil.getBooleanParameter(request, "regexp", false));
            sendSearchForm(appleResponse);
        } else if (resource.equals("search")) {
            if (appleRequest.getCocoonRequest().getMethod().equals("GET")) {
                sendSearchForm(appleResponse);
                return;
            } else {
                boolean endProcessing = form.process(new FormContext(request, locale));
                if (endProcessing) {
                    regexp = (Boolean)form.getChild("regexp").getValue();
                    caseHandling = (String)form.getChild("caseHandling").getValue();
                    needleInputValue = (String)form.getChild("needle").getValue();
                    replacementInputValue = (String)form.getChild("replacement").getValue();
                    
                    if (regexp) {
                        needle = needleInputValue;
                        replacement = replacementInputValue;
                    } else {
                        needle = Pattern.quote(needleInputValue);
                        replacement = Matcher.quoteReplacement(replacementInputValue);
                    }
                    
                    SearchParametersDocument searchParamsDoc = SearchParametersDocument.Factory.newInstance();
                    SearchParameters params = searchParamsDoc.addNewSearchParameters();
                    
                    params.setCaseHandling(CaseHandling.Enum.forString(caseHandling));
                    params.setRegexp(needle);                    

                    TaskSpecification spec = new TaskSpecificationImpl("Search and replace - search phase", "search", searchParamsDoc.toString(), false);
                    
                    long taskId = taskManager.runTask(selection, spec);
                    searchTaskDocDetails = null;
                    searchTask = taskManager.getTask(taskId);
                    sendTaskProgressPage(appleResponse, searchTask, "daisyskin:xslt/serp_searchtask_progress.xsl");
                } else {
                    sendSearchForm(appleResponse);
                    return;
                }
            }
        } else if (resource.equals("searchProgress")) {
            updateSearchTask();
            sendTaskXml(appleResponse, searchTask);
        } else if (resource.equals("replace")) {
            if (searchTask == null) {
                throw new IllegalStateException("search task is not started");
            }
            if (appleRequest.getCocoonRequest().getMethod().equals("GET")) {
                if (searchTaskDocDetails == null) {
                    updateSearchTask();
                    sendTaskProgressPage(appleResponse, searchTask, "daisyskin:xslt/serp_searchtask_progress.xsl");
                } else {
                    sendReplaceForm(appleResponse);
                }
            } else {
                updateReplaceTaskKeys(request);
                if (request.getParameter("startReplaceTask") != null) {
                    if (replaceTask != null) {
                        throw new IllegalStateException("The replace task was already started, can not start it again");
                    }
                    ReplaceParametersDocument replaceParamsDoc = ReplaceParametersDocument.Factory.newInstance();
                    ReplaceParameters params = replaceParamsDoc.addNewReplaceParameters();
                        
                    params.setRegexp(needle);
                    params.setReplacement(replacement);
                    params.setCaseHandling(CaseHandling.Enum.forString(caseHandling));
   
                    TaskSpecification spec = new TaskSpecificationImpl("Search and replace - replace phase", "replace", replaceParamsDoc.toString(), false);

                    DocumentSelection selection = createDocumentSelectionForReplaceTask();
                    
                    long taskId = taskManager.runTask(selection, spec);
                    replaceTaskDocDetails = null;
                    replaceTask = taskManager.getTask(taskId);
                    
                    sendTaskProgressPage(appleResponse, replaceTask, "daisyskin:xslt/serp_replacetask_progress.xsl");
                } else {
                    sendReplaceForm(appleResponse);
                }
            }
        } else if (resource.equals("replaceProgress")) {
            updateReplaceTask();
            sendTaskXml(appleResponse, replaceTask);
        } else if (resource.equals("review")){
            if (replaceTaskDocDetails == null) {
                throw new IllegalStateException("Replace task not finished yet");
            }
            sendReviewPage(appleResponse);
            return;
        } else {
            throw new Exception("Unsupported resource parameter value: \"" + resource + "\".");
        }
    }

    private void sendTaskXml(AppleResponse appleResponse, Task task) {
        GenericPipeConfig config = new GenericPipeConfig();
        config.setApplyLayout(false);
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pipeConf", config);
        viewData.put("pageXml", new XmlObjectXMLizable(task.getXml()));
        config.setXmlSerializer();
        // Without the next line, some namespace attributes are duplicated,
        // (this causes dojo.io.bind requests to fail because firefox can not parse the documents with duplicate namespace attributes
        config.setStripNamespaces(true); 
        appleResponse.sendPage(frontEndContext.getDaisyCocoonPath() + "/internal/genericPipe", viewData);
    }

    /**
     * @return a document selection with keys in the same order as the search-document task
     */
    private DocumentSelection createDocumentSelectionForReplaceTask() {
        List<VariantKey> keys = new ArrayList<VariantKey>(replaceTaskKeys.size());
        for (TaskDocDetail detail: searchTaskDocDetails.getArray()) {
            if (replaceTaskKeys.contains(detail.getVariantKey())) {
                keys.add(detail.getVariantKey());
            }
        }
        
        return taskManager.createEnumerationDocumentSelection((VariantKey[]) keys.toArray(new VariantKey[keys.size()]));
    }

    private void updateReplaceTaskKeys(Request request) {
        for (TaskDocDetail detail: searchTaskDocDetails.getArray()) {
            String paramName = new StringBuffer("select-")
                .append(detail.getVariantKey().getDocumentId())
                .append("@")
                .append(detail.getVariantKey().getBranchId())
                .append(":")
                .append(detail.getVariantKey().getLanguageId()).toString();

            String paramValue = request.getParameter(paramName);
            if (paramValue == null)
                continue;
            
            if (paramValue.equals("on")) {
                replaceTaskKeys.add(detail.getVariantKey());
            } else {
                replaceTaskKeys.remove(detail.getVariantKey());
            }
        }
        
    }

    private void updateSearchTask() throws RepositoryException, XmlException {
        if (searchTask == null) {
            throw new IllegalStateException("The search task has not been started yet"); // should redirect to "search" resource?
        }
        if (searchTaskDocDetails != null) {
            return; // update not needed anymore
        }
        searchTask = taskManager.getTask(searchTask.getId());
        if (searchTask.getState().isStoppedState()) {
            searchTaskDocDetails = taskManager.getTaskDocDetails(searchTask.getId());
            for (TaskDocDetail detail: searchTaskDocDetails.getArray()) {
                if ( detail.getState().equals(DocumentExecutionState.DONE) ) {
                    SearchActionResultImpl result = new SearchActionResultImpl();
                    result.setFromXml(SearchActionResultDocument.Factory.parse(detail.getDetails()));
                    if (result.getAclResultInfo().isAllowed(AclPermission.WRITE)) {
                        replaceTaskKeys.add(detail.getVariantKey());
                    } 
                }
            }
        }
    }

    private void updateReplaceTask() throws RepositoryException {
        if (replaceTask == null) {
            throw new IllegalStateException("The replace task has not been started yet"); // should redirect to "replace" resource?
        }
        if (replaceTask.getState().isStoppedState())
            return; // update not needed anymore
        replaceTask = taskManager.getTask(replaceTask.getId());
        if (replaceTask.getState().isStoppedState()) {
            replaceTaskDocDetails = taskManager.getTaskDocDetails(replaceTask.getId());
        }
    }

    private void sendSearchForm(AppleResponse appleResponse) throws RepositoryException {
        Map<String, Object> viewData = createBasicViewData();

        DocumentBasket documentBasket = DocumentBasketHelper.getDocumentBasket(request, false);
        if (documentBasket == null)
            documentBasket = new DocumentBasket(); // not stored in session on purpose: avoids session creation

        viewData.put("documentBasket", documentBasket);
        viewData.put("locale", locale);
        viewData.put("pageContext", frontEndContext.getPageContext("searchAndReplace"));
        viewData.put("CocoonFormsInstance", form);

        appleResponse.sendPage("Form-serp_search-Pipe", viewData);
    }

    private void sendReplaceForm(AppleResponse appleResponse) throws RepositoryException {
        Map<String, Object> viewData = createBasicViewData();

        viewData.put("CocoonFormsInstance", form);

        viewData.put("searchTaskXml", new XmlObjectXMLizable(searchTask.getXml()));
        viewData.put("searchTaskDocDetailsXml", new XmlObjectXMLizable(searchTaskDocDetails.getAnnotatedXml(repository)));
        viewData.put("searchTaskDocDetails", searchTaskDocDetails.getArray());
        viewData.put("selecteddocuments", replaceTaskKeys);
        GenericPipeConfig genericPipeConfig = new GenericPipeConfig();
        genericPipeConfig.setTemplate("resources/xml/serp_searchresults.xml");
        genericPipeConfig.setStylesheet("daisyskin:xslt/serp_searchresults.xsl");
        viewData.put("pipeConf", genericPipeConfig);
        
        appleResponse.sendPage(frontEndContext.getDaisyCocoonPath() + "/internal/genericPipe", viewData);
    }
    
    private void sendReviewPage(AppleResponse appleResponse) throws RepositoryException {
        Map<String, Object> viewData = createBasicViewData();

        viewData.put("CocoonFormsInstance", form);

        viewData.put("searchTaskXml", new XmlObjectXMLizable(searchTask.getXml()));
        viewData.put("searchTaskDocDetailsXml", new XmlObjectXMLizable(searchTaskDocDetails.getAnnotatedXml(repository)));
        viewData.put("searchTaskDocDetails", searchTaskDocDetails.getArray());
        viewData.put("replaceTaskXml", new XmlObjectXMLizable(replaceTask.getXml()));
        viewData.put("replaceTaskDocDetailsXml", new XmlObjectXMLizable(replaceTaskDocDetails.getAnnotatedXml(repository)));
        viewData.put("replaceTaskDocDetails", replaceTaskDocDetails.getArray());

        viewData.put("selecteddocuments", replaceTaskKeys);
        GenericPipeConfig genericPipeConfig = new GenericPipeConfig();
        genericPipeConfig.setTemplate("resources/xml/serp_replaceresults.xml");
        genericPipeConfig.setStylesheet("daisyskin:xslt/serp_replaceresults.xsl");
        viewData.put("pipeConf", genericPipeConfig);
        
        appleResponse.sendPage(frontEndContext.getDaisyCocoonPath() + "/internal/genericPipe", viewData);
        
    }

    private Map<String, Object> createBasicViewData()
            throws RepositoryException {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext("searchAndReplace"));
        viewData.put("selectionType", selectionType);
        viewData.put("query", query);
        viewData.put("documentId", documentId);
        if (branchId > 0) {
            viewData.put("branch", variantManager.getBranch(branchId, false).getName());
            viewData.put("language", variantManager.getLanguage(languageId, false).getName());
        }
        viewData.put("needle", needleInputValue);
        viewData.put("replacement", replacementInputValue);
        viewData.put("caseHandling", caseHandling.toString());
        viewData.put("regexp", Boolean.toString(regexp));
        return viewData;
    }

    private void sendTaskProgressPage(AppleResponse appleResponse, Task task, String stylesheet) {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("task", task);
        viewData.put("taskXml", new XmlObjectXMLizable(task.getXml()));
        viewData.put("pageContext", frontEndContext.getPageContext());
        GenericPipeConfig pipeConf = GenericPipeConfig.templatePipe("resources/xml/serp_task_progress.xml");
        pipeConf.setStylesheet(stylesheet);
        viewData.put("pipeConf", pipeConf);
        
        appleResponse.sendPage(frontEndContext.getDaisyCocoonPath() + "/internal/genericPipe", viewData);
    }

    public DocumentSelection createDocumentBasketBasedSelection(DocumentBasket documentBasket) throws RepositoryException {
        final VariantKey[] keys = new VariantKey[documentBasket.size()];
        for (int i = 0; i < documentBasket.size(); i++) {
            DocumentBasketEntry entry = documentBasket.getEntries().get(i);
            long branchId = variantManager.getBranch(entry.getBranch(), false).getId();
            long languageId = variantManager.getLanguage(entry.getLanguage(), false).getId();
            VariantKey key = new VariantKey(entry.getDocumentId(), branchId, languageId);
            keys[i] = key;
        }
        return taskManager.createEnumerationDocumentSelection(keys);
    }
    
    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

}
