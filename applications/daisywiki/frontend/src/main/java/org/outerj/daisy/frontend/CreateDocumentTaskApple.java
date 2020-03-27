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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.datatype.StaticSelectionList;
import org.apache.cocoon.forms.event.ActionEvent;
import org.apache.cocoon.forms.event.ActionListener;
import org.apache.cocoon.forms.formmodel.Action;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Messages;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.util.I18nMessage;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;
import org.outerj.daisy.doctaskrunner.DocumentSelection;
import org.outerj.daisy.doctaskrunner.DocumentTaskManager;
import org.outerj.daisy.doctaskrunner.Task;
import org.outerj.daisy.doctaskrunner.TaskDocDetail;
import org.outerj.daisy.doctaskrunner.TaskDocDetails;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.doctaskrunner.spi.TaskSpecificationImpl;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasket;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketEntry;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketHelper;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerx.daisy.x10.SearchResultDocument;

public class CreateDocumentTaskApple extends AbstractDaisyApple implements Serviceable {
    private ServiceManager serviceManager;
    private boolean init = false;
    private Form documentSelectionForm;
    private DocumentTaskParametersHandler taskParametersHandler;
    private boolean documentSelectionOk = false;
    private Form taskSpecificationForm;
    private Repository repository;
    private DocumentTaskManager taskManager;
    private Locale locale;
    private Map<String, Object> taskSpecViewData;
    private Task origTask = null;
    private TaskDocDetails origTaskDetails = null;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            locale = frontEndContext.getLocale();
            repository = frontEndContext.getRepository();
            taskManager = (DocumentTaskManager)repository.getExtension("DocumentTaskManager");
            

            if (repository.isInRole("guest") && repository.getActiveRoleIds().length == 1)
                throw new Exception("The document task functionality is not available for guest users.");

            // Prepare document selection form
            documentSelectionForm = FormHelper.createForm(serviceManager, "resources/form/doctask_docselection_definition.xml");
            documentSelectionForm.getChild("documents").addValidator(KeySetParser.getVariantKeysWidgetValidator(repository.getVariantManager(), false, true));
            ((Action)documentSelectionForm.getChild("executeQuery")).addActionListener(new ExecuteQueryActionListener());

            // Find the task that is being restarted (if any)
            {
                String origTaskId = request.getParameter("restartTask");
                if (origTaskId != null) {
                    long taskId = Long.valueOf(origTaskId);
                    if (origTaskId != null) {
                        origTask = taskManager.getTask(taskId);
                    }
                    origTaskDetails = taskManager.getTaskDocDetails(taskId);
                }
            }

            // Get list of supported action types, sort and filter out those for which we don't have a form definition
            String[] actionTypes = taskManager.getAllowedTasks();
            Arrays.sort(actionTypes);
            
            List<String> uiActions = new ArrayList<String>();
			for (String actionType: actionTypes) {
				if (haveUI(actionType)) {
					uiActions.add(actionType);
				}
            }
			
			// Prepare and create task specification form
			Map objectModel = ContextHelper.getObjectModel(getContext());
	        Object oldViewData = FlowHelper.getContextObject(objectModel);
	        try {
	        	Map<String, Object> viewData = new HashMap<String, Object>();
	        	GenericPipeConfig pipeConf = GenericPipeConfig.templateOnlyPipe("resources/form/doctask_taskspec_definition.xml");
	        	pipeConf.setXmlSerializer();
	        	pipeConf.setApplyI18n(false);
	        	viewData.put("pipeConf", pipeConf);
	        	viewData.put("actions", uiActions);
	        	FlowHelper.setContextObject(objectModel, viewData);
	            taskSpecificationForm = FormHelper.createForm(serviceManager, "cocoon:/internal/genericPipe");
	        } finally {
	        	FlowHelper.setContextObject(objectModel, oldViewData);
	        }

            taskSpecificationForm.getChild("description").setValue(origTask == null? "(no description)" : origTask.getDescription());
            Field taskTypeField = (Field)taskSpecificationForm.getChild("tasktype");
            StaticSelectionList taskTypes = new StaticSelectionList(taskTypeField.getDatatype());
            taskTypeField.setSelectionList(taskTypes);
            for (String actionType: uiActions) {
            	taskTypes.addItem(actionType, new I18nMessage("createtaskspec.tasktype-" + actionType));
            }
            String origType = (origTask == null ? null : origTask.getActionType());
            if (origType != null && uiActions.contains(origType)) {
                taskTypeField.setValue(origType);
                loadParameters(origType);
            } else if (!taskTypes.getItems().isEmpty()) {
            	taskTypeField.setValue(uiActions.get(0));
            }
            if (origType != null) {
                taskSpecificationForm.getChild("maxTries").setValue(origTask.getMaxTries());
                taskSpecificationForm.getChild("retryInterval").setValue(origTask.getRetryInterval());
            }

            // Prepare task specification view data
            taskSpecViewData = new HashMap<String, Object>();
            taskSpecViewData.put("CocoonFormsInstance", taskSpecificationForm);
            taskSpecViewData.put("pageContext", frontEndContext.getPageContext());
            taskSpecViewData.put("locale", locale);
            taskSpecViewData.put("actions", uiActions);
            taskSpecViewData.put("collectionsArray", repository.getCollectionManager().getCollections(false).getArray());
            taskSpecViewData.put("branchesArray", repository.getVariantManager().getAllBranches(false).getArray());
            taskSpecViewData.put("languagesArray", repository.getVariantManager().getAllLanguages(false).getArray());

            // Check if we should initialize from the document basket
            String initFromDocBasket = request.getParameter("initFromDocumentBasket");
            if (initFromDocBasket != null && initFromDocBasket.equalsIgnoreCase("true")) {
                DocumentBasket documentBasket = DocumentBasketHelper.getDocumentBasket(request, true);
                List<DocumentBasketEntry> entries = documentBasket.getEntries();

                StringBuilder buffer = new StringBuilder(70 * entries.size());
                buffer.append("#\n# Entries added from your document basket.\n#");

                for (DocumentBasketEntry entry : entries) {
                    buffer.append('\n').append(entry.getDocumentId()).append(',')
                            .append(entry.getBranch()).append(',').append(entry.getLanguage())
                            .append(" # ").append(entry.getDocumentName());
                }
                documentSelectionForm.getChild("documents").setValue(buffer.toString());
            } else if (origTask != null) {
                TaskDocDetail[] docDetails = origTaskDetails.getArray();
                StringBuffer buffer = new StringBuffer(20 * docDetails.length);
                for (TaskDocDetail d: docDetails) {
                    buffer.append(d.getVariantKey().getDocumentId())
                        .append(",")
                        .append(repository.getVariantManager().getBranch(d.getVariantKey().getBranchId(), false).getName())
                        .append(",")
                        .append(repository.getVariantManager().getLanguage(d.getVariantKey().getLanguageId(), false).getName())
                        .append("# ")
                        .append(d.getState())
                        .append("\n");
                    d.getState().toString();
                }
                documentSelectionForm.getChild("documents").setValue(buffer.toString());
            }

            init = true;
            appleResponse.redirectTo(getDocumentSelectionPath());
            return;
        }

        String resource = appleRequest.getSitemapParameter("resource");
        if (resource == null) {
            throw new Exception("Missing 'resource' sitemap parameter.");
        } else if (resource.equals("documentSelection")) {
            if (request.getMethod().equals("GET")) {
                // display document selection form
                showDocumentSelectionForm(appleResponse, frontEndContext);
            } else if (request.getMethod().equals("POST")) {
                boolean finished = documentSelectionForm.process(new FormContext(request, locale));
                if (finished) {
                    documentSelectionOk = true;
                    appleResponse.redirectTo(getTaskSpecificationPath());
                } else {
                    documentSelectionOk = false;
                    showDocumentSelectionForm(appleResponse, frontEndContext);
                }
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        } else if (resource.equals("taskSpecification")) {
            if (!documentSelectionOk) {
                appleResponse.redirectTo(getDocumentSelectionPath());
            } else if (request.getMethod().equals("GET")) {
                showTaskSpecificationForm(appleResponse);
            } else if (request.getMethod().equals("POST")) {
                boolean finished = taskSpecificationForm.process(new FormContext(request, locale));
                if (finished) {
                    String documentKeys = (String)documentSelectionForm.getChild("documents").getValue();
                    VariantKey[] variantKeys = KeySetParser.parseVariantKeys(documentKeys, repository.getVariantManager(), false, true);
                    DocumentSelection documentSelection = taskManager.createEnumerationDocumentSelection(variantKeys);
                    TaskSpecification taskSpecification = createTaskSpecification();
                    long taskId = taskManager.runTask(documentSelection, taskSpecification);

                    Map<String, Object> viewData = new HashMap<String, Object>();
                    viewData.put("taskId", String.valueOf(taskId));
                    viewData.put("pageContext", frontEndContext.getPageContext());
                    viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/taskcreatedpage.xml"));
                    appleResponse.sendPage("internal/genericPipe", viewData);
                } else if (taskSpecificationForm.getSubmitWidget() != null && taskSpecificationForm.getSubmitWidget().getId().equals("back")) {
                    appleResponse.redirectTo(getDocumentSelectionPath());
                } else {
                    showTaskSpecificationForm(appleResponse);
                }
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }

        } else {
            throw new ResourceNotFoundException(resource);
        }

    }

    private void loadParameters(String taskType) throws Exception {
        Widget group = taskSpecificationForm.lookupWidget("taskParamsUnion").lookupWidget(taskType);
        getDocumentTaskParametersHandler(taskType).load(group, origTask.getActionParameters(), repository);
    }

    private boolean haveUI(String actionType) throws Exception {
        Source source = null;
        SourceResolver sourceResolver = null;
        try {
            sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);
            source = sourceResolver.resolveURI("wikidata:/resources/doctaskui/" + actionType + "_definition.xml");

            if (source.exists()) {
            	return true;
            }

            return false;
        } finally {
            if (source != null)
                sourceResolver.release(source);
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
        }
    }

	private void showDocumentSelectionForm(AppleResponse appleResponse, FrontEndContext frontEndContext) throws Exception {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("CocoonFormsInstance", documentSelectionForm);
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("locale", locale);
        appleResponse.sendPage("Form-doctask_docselection-Pipe", viewData);
    }

    private void showTaskSpecificationForm(AppleResponse appleResponse) {
        appleResponse.sendPage("Form-doctask_taskspec-Pipe", taskSpecViewData);
    }

    private String getDocumentSelectionPath() {
        return getMountPoint() + "/doctask/new/" + getContinuationId() + "/documentSelection";
    }

    private String getTaskSpecificationPath() {
        return getMountPoint() + "/doctask/new/" + getContinuationId() + "/taskSpecification";
    }

    private TaskSpecification createTaskSpecification() throws Exception {
        String description = (String)taskSpecificationForm.getChild("description").getValue();
        String taskType = (String)taskSpecificationForm.getChild("tasktype").getValue();
        boolean stopOnFirstError = ((Boolean)taskSpecificationForm.getChild("stopOnFirstError").getValue()).booleanValue();
        // TODO get retry values from the form
        int maxTries = (Integer)taskSpecificationForm.getChild("maxTries").getValue();
        int retryInterval = (Integer)taskSpecificationForm.getChild("retryInterval").getValue();
        Widget group = taskSpecificationForm.lookupWidget("taskParamsUnion").lookupWidget(taskType);
        return new TaskSpecificationImpl(description, taskType, getDocumentTaskParametersHandler(taskType).save(group), stopOnFirstError, maxTries, retryInterval);        
    }

    private DocumentTaskParametersHandler getDocumentTaskParametersHandler(String taskType) throws Exception {
        Widget group = taskSpecificationForm.lookupWidget("taskParamsUnion").lookupWidget(taskType);
		String paramsHandlerClassName = (String)group.getAttribute("parametersHandler");
        Class paramsHandlerClazz = Class.forName(paramsHandlerClassName);
        DocumentTaskParametersHandler handler = (DocumentTaskParametersHandler)paramsHandlerClazz.newInstance();
        return handler;
    }

    class ExecuteQueryActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            Field queryField = (Field)documentSelectionForm.getChild("query");
            String query = (String)queryField.getValue();
            if (query == null) {
                ((Messages)documentSelectionForm.getChild("messages")).addMessage(new I18nMessage("createdocsel.no-query-entered"));
            } else {
                SearchResultDocument searchResultDocument;
                try {
                    searchResultDocument = repository.getQueryManager().performQuery(query, locale);
                } catch (RepositoryException e) {
                    ((Messages)documentSelectionForm.getChild("messages")).addMessage(new I18nMessage("createdocsel.error-performing-query"));
                    return;
                }
                List<SearchResultDocument.SearchResult.Rows.Row> rows = searchResultDocument.getSearchResult().getRows().getRowList();
                if (rows.size() > 0) {
                    VariantManager variantManager = repository.getVariantManager();
                    String currentValue = (String)documentSelectionForm.getChild("documents").getValue();
                    StringBuilder newValue;
                    if (currentValue != null) {
                        newValue = new StringBuilder(currentValue);
                        newValue.append('\n');
                    } else {
                        newValue = new StringBuilder();
                    }

                    newValue.append("#\n");
                    newValue.append("# Documents added as result of the following query:\n");
                    newValue.append("# ").append(query).append('\n');
                    newValue.append("#\n");
                    for (SearchResultDocument.SearchResult.Rows.Row row : rows) {
                        newValue.append(row.getDocumentId());
                        newValue.append(',');
                        String branchName;
                        try {
                            branchName = variantManager.getBranch(row.getBranchId(), false).getName();
                        } catch (RepositoryException e) {
                            branchName = String.valueOf(row.getBranchId());
                        }
                        newValue.append(branchName);
                        newValue.append(',');
                        String languageName;
                        try {
                            languageName = variantManager.getLanguage(row.getLanguageId(), false).getName();
                        } catch (RepositoryException e) {
                            languageName = String.valueOf(row.getLanguageId());
                        }
                        newValue.append(languageName);
                        newValue.append(" # ");
                        newValue.append(row.getValueArray(0));
                        newValue.append('\n');
                    }
                    documentSelectionForm.getChild("documents").setValue(newValue.toString());
                    ((Messages)documentSelectionForm.getChild("messages")).addMessage(new I18nMessage("createdocsel.query-executed", new String[] {String.valueOf(rows.size())}));
                } else {
                    ((Messages)documentSelectionForm.getChild("messages")).addMessage(new I18nMessage("createdocsel.query-no-result"));
                }
            }
        }
    }

}
