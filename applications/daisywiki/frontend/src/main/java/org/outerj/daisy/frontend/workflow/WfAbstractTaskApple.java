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
package org.outerj.daisy.frontend.workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.FormsConstants;
import org.apache.cocoon.forms.datatype.Datatype;
import org.apache.cocoon.forms.datatype.SelectionList;
import org.apache.cocoon.forms.datatype.convertor.Convertor;
import org.apache.cocoon.forms.datatype.convertor.DefaultFormatCache;
import org.apache.cocoon.forms.formmodel.ContainerWidget;
import org.apache.cocoon.forms.formmodel.DataWidget;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Group;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
import org.apache.cocoon.forms.validation.WidgetValidator;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.outerj.daisy.frontend.PageContext;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.editor.CheckFieldsFormValidator;
import org.outerj.daisy.frontend.editor.ConditionalValidator;
import org.outerj.daisy.frontend.editor.DocumentEditorContext;
import org.outerj.daisy.frontend.editor.InlineFormConfig;
import org.outerj.daisy.frontend.editor.ValidateOnSaveCondition;
import org.outerj.daisy.frontend.editor.ValidationCondition;
import org.outerj.daisy.frontend.editor.WidgetResolver;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.user.UserNotFoundException;
import org.outerj.daisy.workflow.TaskPriority;
import org.outerj.daisy.workflow.TaskUpdateData;
import org.outerj.daisy.workflow.VariableScope;
import org.outerj.daisy.workflow.WfActorKey;
import org.outerj.daisy.workflow.WfListItem;
import org.outerj.daisy.workflow.WfNodeDefinition;
import org.outerj.daisy.workflow.WfPool;
import org.outerj.daisy.workflow.WfProcessDefinition;
import org.outerj.daisy.workflow.WfTask;
import org.outerj.daisy.workflow.WfTaskDefinition;
import org.outerj.daisy.workflow.WfUserKey;
import org.outerj.daisy.workflow.WfValueType;
import org.outerj.daisy.workflow.WfVariable;
import org.outerj.daisy.workflow.WfVariableDefinition;
import org.outerj.daisy.workflow.WfVersionKey;
import org.outerj.daisy.workflow.WorkflowManager;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Base controller for both performing a task and starting a workflow.
 */
public abstract class WfAbstractTaskApple extends AbstractDaisyApple implements Serviceable, LogEnabled {
    protected ServiceManager serviceManager;
    private boolean init = false;
    private boolean init_params = false;
    protected Form form;
    protected Map<String, Object> viewData = new HashMap<String, Object>();
    protected Locale locale;
    protected Repository repository;
    protected WorkflowManager workflowManager;
    protected SiteConf siteConf;
    protected List<WfPool> availablePools;
    protected WfTask task;
    protected WfProcessDefinition processDefinition;
    protected WfTaskDefinition taskDefinition;
    protected WfNodeDefinition nodeDefinition;
    protected InlineFormConfig formConfig;
    protected Document document;
    protected long lockExpires;
    protected DocumentEditorContext documentEditorContext = null;
    protected Logger logger;

    private static Set<WfValueType> SEL_LIST_NOT_SUPPORTED_TYPES = new HashSet<WfValueType>();
    static {
        SEL_LIST_NOT_SUPPORTED_TYPES.add(WfValueType.ACTOR);
        SEL_LIST_NOT_SUPPORTED_TYPES.add(WfValueType.BOOLEAN);
        SEL_LIST_NOT_SUPPORTED_TYPES.add(WfValueType.DAISY_LINK);
        SEL_LIST_NOT_SUPPORTED_TYPES.add(WfValueType.USER);
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    abstract Object[] init(Request request) throws Exception;

    abstract void finish(AppleRequest appleRequest, AppleResponse appleResponse) throws RepositoryException;

    abstract String getPath();

    String getDefaultDocumentLink(Request request) {
        return null;
    }

    String getDefaultDescription(Request request) {
        return null;
    }

    abstract boolean loadInitialValues();

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        String resource = appleRequest.getSitemapParameter("resource");
        viewData.put("serviceManager", serviceManager);
        if (!init) {
            siteConf = frontEndContext.getSiteConf();
            locale = frontEndContext.getLocale();

            repository = frontEndContext.getRepository();

            workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
            if (!init_params) { 
                Object[] initResult = init(request);
                task = (WfTask)initResult[0];
                taskDefinition = (WfTaskDefinition)initResult[1];
                nodeDefinition = (WfNodeDefinition)initResult[2];
                document = (Document)initResult[3];
                formConfig = (InlineFormConfig)initResult[4];
                init_params = true;
                
                if (formConfig != null) {
                    // duplicated from DocumentEditorSupport
                    boolean showLockWarnPage = false;
                    LockInfo lockInfo = document.getLockInfo(false);
                    if (lockInfo.hasLock() && lockInfo.getUserId() != repository.getUserId()) {
                        showLockWarnPage = true;
                    } else if (siteConf.getAutomaticLocking()) {
                        boolean success = document.lock(siteConf.getDefaultLockTime(), siteConf.getLockType());
                        lockInfo = document.getLockInfo(false);
                        if (!success) {
                            showLockWarnPage = true;
                        } else {
                            lockExpires = lockInfo.getTimeAcquired().getTime() + lockInfo.getDuration();
                            // after locking the document, load it again to be sure that we have the latest version
                            // of it (somebody might have saved it during our last loading and taking the lock). In
                            // case of a pessimistic lock, the user can now be sure that saving the document will
                            // not give concurrent modification exceptions.
                            document = repository.getDocument(document.getVariantKey(), true);
                        }
                    }

                    DocumentType documentType = repository.getRepositorySchema().getDocumentTypeById(document.getDocumentTypeId(), false);
                    String branch = repository.getVariantManager().getBranch(document.getBranchId(), false).getName();
                    String language = repository.getVariantManager().getLanguage(document.getLanguageId(), false).getName();
                    PageContext pageContext = frontEndContext.getPageContext();
                    documentEditorContext = new DocumentEditorContext(documentType, document, repository, document.getId(), document.getBranchId(), document.getLanguageId(), branch, language, locale, serviceManager, pageContext, logger, getContext(), frontEndContext.getDaisyCocoonPath(), lockExpires);
                    documentEditorContext.setSelectionListDataWidgetResolver(new DocumentEditorContext.SelectionListDataWidgetResolver() {
                        public DataWidget lookupDataWidget(String widgetPath) {
                            return (DataWidget)form.lookupWidget(widgetPath.replace('.', '/'));
                        }
                    });
                    documentEditorContext.createEditors(formConfig.getParts(), formConfig.getFields());

                    if (showLockWarnPage) {
                        Map<String, Object> viewData = new HashMap<String, Object>();
                        viewData.put("lockInfo", lockInfo);
                        String userName = repository.getUserManager().getUserDisplayName(lockInfo.getUserId());
                        viewData.put("lockUserName", userName);
                        viewData.put("pageContext", frontEndContext.getPageContext());
                        viewData.put("editPath", getPath());
                        viewData.put("backLink", getMountPoint() + "/" + siteConf.getName() + "/workflow/tasks");
                        viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/wf_locked.xml"));
                
                        appleResponse.sendPage("internal/genericPipe", viewData);
                        return;
                    }
                }

            }

            if (task != null && task.getEnd() != null)
                throw new Exception("The task is already closed.");
            
            String path = getPath();
            availablePools = workflowManager.getPoolManager().getPools();

            Map objectModel = ContextHelper.getObjectModel(getContext());
            if (taskDefinition != null)
                viewData.put("wfTaskDefinitionXml", new XmlObjectXMLizable(taskDefinition.getXml()));
            viewData.put("wfTaskDefinition", taskDefinition);
            viewData.put("wfNodeDefinitionXml", new XmlObjectXMLizable(nodeDefinition.getXml()));
            viewData.put("wfNodeDefinition", nodeDefinition);
            viewData.put("wfTask", task);
            viewData.put("pageContext", frontEndContext.getPageContext());
            viewData.put("frontEndContext", frontEndContext);
            viewData.put("locale", locale);
            viewData.put("enable-htmlarea", true);
            viewData.put("htmlareaLang", locale.getLanguage());
            viewData.put("submitPath", path);
            // make pools available for selection lists
            viewData.put("pools", availablePools);
            viewData.put("formTemplate", "cocoon:/internal/workflow/taskToFormTemplate");
            viewData.put("wfFormCacheKey", "daisy-processdef-" + nodeDefinition.getProcessDefinitionId() + "-task-" + (task != null ? task.getId() : "none"));
            viewData.put("wfFormValidity", new NOPValidity());
            
            if (formConfig != null) {
                viewData.put("repository", repository);
                viewData.put("documentEditorContext", documentEditorContext);
                WfVersionKey daisyDocumentVariable = (WfVersionKey)task.getVariable("daisy_document").getValue();
                viewData.put("daisyDocumentVariable", daisyDocumentVariable);
                viewData.put("document", document);
                viewData.put("formConfig", formConfig);
                viewData.put("daisyVersion", repository.getClientVersion());
                viewData.put("heartbeatInterval", DocumentEditorContext.HEARTBEAT_INTERVAL);
                
                viewData.put("partEditors", documentEditorContext.getPartEditors());
                viewData.put("fieldEditors", documentEditorContext.getFieldEditors());
                viewData.put("displayMode", "workflow");
            }

            FlowHelper.setContextObject(objectModel, viewData);
            form = FormHelper.createForm(serviceManager, "cocoon:/internal/workflow/taskToFormDefinition");
            if (formConfig != null) {
                form.setAttribute("partEditors", documentEditorContext.getPartEditors());
                form.setAttribute("fieldEditors", documentEditorContext.getFieldEditors());
                
                ValidationCondition validateOnSave = new ValidateOnSaveCondition(documentEditorContext);
                form.addValidator(new ConditionalValidator(validateOnSave, new CheckFieldsFormValidator())); 

                documentEditorContext.setPartWidgetResolver(new WidgetResolver() {
                    public Widget resolveWidget(String key) {
                        return form.lookupWidget("part_" + documentEditorContext.getPartEditorsByName().get(key).getPartTypeUse().getPartType().getId());
                    }
                });
                documentEditorContext.setFieldWidgetResolver(new WidgetResolver() {
                    public Widget resolveWidget(String key) {
                        return form.lookupWidget("field_" + documentEditorContext.getFieldEditorsByName().get(key).getFieldTypeUse().getFieldType().getId());
                    }
                });
                AclResultInfo aclInfo = documentEditorContext.getAclInfo(document);
                documentEditorContext.setupEditors(document, aclInfo);
            }
            viewData.put("CocoonFormsInstance", form);

            prepareForm(request);
            if (task != null)
                loadTaskData(task);
            else
                initBuiltInVariables(request);

            init = true;
            appleResponse.redirectTo(path);
        } else if (documentEditorContext != null && documentEditorContext.handleCommonResources(appleRequest, appleResponse, document, resource, viewData)) {
            return;
        } else {
            String method = request.getMethod();
            if (method.equals("GET")) {
                // display the form
                appleResponse.sendPage("GenericFormPipe", viewData);
            } else if (method.equals("POST")) {
                // handle a form submit
                boolean endProcessing = form.process(new FormContext(appleRequest.getCocoonRequest(), locale));

                if (!endProcessing) {
                    appleResponse.sendPage("GenericFormPipe", viewData);
                } else {
                    if (form.getSubmitWidget() == null) {
                       appleResponse.sendPage("GenericFormPipe", viewData);
                    } else {
                        /* 
                         * this check prevents unintended saving of document fields if the task was reassigned after the user started it.
                         * It can still go wrong, but the chances are negligible
                         */
                        if (checkAcl()) { 
                            if (formConfig != null && form.getSubmitWidget() != null && !form.getSubmitWidget().getId().equals("cancel") ) {
                                documentEditorContext.saveEditors(document);
                                document.save(false); // the editors enforce requiredness (and requiredness may not correspond to actual repository schema)
                            }
                        }
                        if (formConfig != null) {
                            document.releaseLock();
                        }
                        finish(appleRequest, appleResponse);
                    }
                }
            } else {
                throw new HttpMethodNotAllowedException(method);
            }
        }
    }
    
    /**
     * Should return true if the workflow manager allows the user to proceed (this is not transaction proof, but to skip saving the document to prevent accidental changes).
     * @throws RepositoryException TODO
     */
    protected abstract boolean checkAcl() throws RepositoryException;
    
    private void loadTaskData(WfTask task) {
        Group variablesGroup = (Group)form.getChild("variables");
        Iterator childIt = variablesGroup.getChildren();
        while (childIt.hasNext()) {
            Widget child = (Widget)childIt.next();
            VariableInfo variableInfo = getVariableInfo(child);

            WfVariable variable = task.getVariable(variableInfo.name, variableInfo.scope);
            if (variable != null && variable.getType() == variableInfo.type) {
                Object value = variable.getValue();
                switch (variableInfo.type) {
                    case DAISY_LINK:
                        child.setValue(WfVersionKeyUtil.versionKeyToString((WfVersionKey)value, repository));
                        break;
                    case ACTOR:
                        WfActorKey actorKey = (WfActorKey)value;
                        if (variableInfo.readOnly) {
                            String label;
                            if (actorKey.isPool()) {
                                label = "Pool(s): " + getPoolNames(actorKey.getPoolIds());
                            } else {
                                label = "User: " + getUserLogin(actorKey.getUserId());
                            }
                            child.setValue(label);
                        } else {
                            child.lookupWidget("actorCase").setValue(actorKey.isPool() ? "pool" : "user");
                            if (actorKey.isPool()) {
                                child.lookupWidget("actor/pool/pool").setValue(actorKey.getPoolIds().toArray());
                            } else {
                                String login = getUserLogin(actorKey.getUserId());
                                child.lookupWidget("actor/user/user").setValue(login);
                            }
                        }
                        break;
                    case USER:
                        String login = getUserLogin(((WfUserKey)value).getId());
                        child.setValue(login);
                        break;
                    default:
                        child.setValue(value);
                }
            }
        }

        Date dueDate = task.getDueDate();
        if (dueDate != null)
            form.getChild("dueDate").setValue(dueDate);
        form.getChild("priority").setValue(task.getPriority().toString());
    }

    /**
     * Give some standard variables default values if possible.
     */
    private void initBuiltInVariables(Request request) {
        Group variablesGroup = (Group)form.getChild("variables");
        Iterator childIt = variablesGroup.getChildren();
        while (childIt.hasNext()) {
            Widget child = (Widget)childIt.next();
            VariableInfo variableInfo = getVariableInfo(child);

            if (variableInfo.name.equals("daisy_document") && variableInfo.type == WfValueType.DAISY_LINK) {
                String defaultDocumentLink = getDefaultDocumentLink(request);
                if (defaultDocumentLink != null)
                    child.setValue(defaultDocumentLink);
            } else if (variableInfo.name.equals("daisy_description") && variableInfo.type == WfValueType.STRING) {
                String defaultDescription = getDefaultDescription(request);
                if (defaultDescription != null)
                    child.setValue(defaultDescription);
            }
        }
    }

    private String getUserLogin(long userId) {
        String login;
        try {
            login = repository.getUserManager().getPublicUserInfo(userId).getLogin();
        } catch (UserNotFoundException e) {
            login = "[error: nonexisting user: " + userId + "]";
        } catch (RepositoryException e) {
            login = "[error fetching user login for user: " + userId + "]";
        }
        return login;
    }

    private String getPoolNames(List<Long> poolIds) {
        StringBuilder result = new StringBuilder();
        for (long poolId : poolIds) {
            String label = null;
            for (WfPool pool : availablePools) {
                if (pool.getId() == poolId) {
                    label = pool.getName();
                    break;
                }
            }
            if (label == null)
                label = "[nonexisting pool: " + poolId + "]";
            if (result.length() > 0)
                result.append(", ");
            result.append(label);
        }
        return result.toString();
    }

    private void prepareForm(Request request) throws RepositoryException {
        form.addValidator(new RequiredVariablesValidator());

        List<WfVariable> initialVariableValues = null;
        if (loadInitialValues()) {
            WfVersionKey contextDoc = null;
            String link = getDefaultDocumentLink(request);
            if (link != null) {
                contextDoc = WfVersionKeyUtil.parseWfVersionKey(link, repository, siteConf);
            }
            initialVariableValues = workflowManager.getInitialVariables(this.taskDefinition.getNode().getProcessDefinitionId(), contextDoc);
        }
        if (initialVariableValues == null)
            initialVariableValues = Collections.emptyList();

        Group variablesGroup = (Group)form.getChild("variables");
        Iterator childIt = variablesGroup.getChildren();
        while (childIt.hasNext()) {
            Widget child = (Widget)childIt.next();
            VariableInfo variableInfo = getVariableInfo(child);
            if (variableInfo != null) {
                WfVariableDefinition variableDef = taskDefinition.getVariable(variableInfo.name, variableInfo.scope);
                if (variableDef == null)
                    throw new RuntimeException("Unexpected: no variable definition found for a variable on the task form.");

                // set selection list if any, and if supported
                if (child instanceof Field && !SEL_LIST_NOT_SUPPORTED_TYPES.contains(variableInfo.type)) {
                    if (variableDef.getSelectionList() != null) {
                        Field field = (Field)child;
                        field.setSelectionList(new VariableSelectionListAdapter(variableDef.getSelectionList(), field.getDatatype()));
                    }
                }

                WfVariable initialVariable = find(initialVariableValues, variableDef);
                if (!variableDef.isReadOnly() && initialVariable != null) {
                    switch (initialVariable.getType()) {
                        case DAISY_LINK:
                            String link = WfVersionKeyUtil.versionKeyToString((WfVersionKey)initialVariable.getValue(), repository);
                            child.setValue(link);
                            break;
                        case ACTOR:
                            WfActorKey actor = (WfActorKey)initialVariable.getValue();

                            ContainerWidget actorGroup = (ContainerWidget)child;
                            Widget actorCase = actorGroup.getChild("actorCase");

                            if (actor.isUser()) {
                                actorCase.setValue("user");
                                String login = repository.getUserManager().getUserLogin(actor.getUserId());
                                actorGroup.lookupWidget("actor/user/user").setValue(login);
                            } else if (actor.isPool()) {
                                actorCase.setValue("pool");
                                actorGroup.lookupWidget("actor/pool/pool").setValue(actor.getPoolIds().toArray());
                            }
                            break;
                        case USER:
                            WfUserKey user = (WfUserKey)initialVariable.getValue();
                            String login = repository.getUserManager().getUserLogin(user.getId());
                            child.setValue(login);
                        default:
                            child.setValue(initialVariable.getValue());
                    }
                }
            }
        }
    }

    private WfVariable find(List<WfVariable> variables, WfVariableDefinition definition) {
        for (WfVariable variable : variables) {
            if (variable.getName().equals(definition.getName()) && variable.getScope() == definition.getScope()) {
                return variable;
            }
        }
        return null;
    }

    private static class VariableSelectionListAdapter implements SelectionList {
        private List<WfListItem> listItems;
        private Datatype datatype;

        public VariableSelectionListAdapter(List<WfListItem> listItems, Datatype datatype) {
            this.listItems = listItems;
            this.datatype = datatype;
        }

        public Datatype getDatatype() {
            return datatype;
        }

        public void generateSaxFragment(ContentHandler contentHandler, Locale locale) throws SAXException {
            contentHandler.startElement(FormsConstants.INSTANCE_NS, SELECTION_LIST_EL, FormsConstants.INSTANCE_PREFIX_COLON + SELECTION_LIST_EL, new AttributesImpl());
            Convertor.FormatCache formatCache = new DefaultFormatCache();

            for (WfListItem listItem : listItems) {
                String stringValue = datatype.getConvertor().convertToString(listItem.getValue(), locale, formatCache);
                AttributesImpl itemAttrs = new AttributesImpl();
                itemAttrs.addCDATAAttribute("value", stringValue);
                contentHandler.startElement(FormsConstants.INSTANCE_NS, ITEM_EL, FormsConstants.INSTANCE_PREFIX_COLON + ITEM_EL, itemAttrs);
                if (listItem.getLabel() != null) {
                    contentHandler.startElement(FormsConstants.INSTANCE_NS, LABEL_EL, FormsConstants.INSTANCE_PREFIX_COLON + LABEL_EL, org.apache.cocoon.xml.XMLUtils.EMPTY_ATTRIBUTES);
                    listItem.getLabel().generateSaxFragment(contentHandler);
                    contentHandler.endElement(FormsConstants.INSTANCE_NS, LABEL_EL, FormsConstants.INSTANCE_PREFIX_COLON + LABEL_EL);
                }
                contentHandler.endElement(FormsConstants.INSTANCE_NS, ITEM_EL, FormsConstants.INSTANCE_PREFIX_COLON + ITEM_EL);
            }

            contentHandler.endElement(FormsConstants.INSTANCE_NS, SELECTION_LIST_EL, FormsConstants.INSTANCE_PREFIX_COLON + SELECTION_LIST_EL);
        }
    }

    protected TaskUpdateData getTaskUpdateData() {
        TaskUpdateData taskUpdateData = new TaskUpdateData();

        Group variablesGroup = (Group)form.getChild("variables");
        Iterator childIt = variablesGroup.getChildren();
        while (childIt.hasNext()) {
            Widget child = (Widget)childIt.next();
            VariableInfo variableInfo = getVariableInfo(child);
            if (variableInfo != null && !variableInfo.readOnly) {
                Object value = getValue(child, variableInfo.type);
                if (value != null)
                    taskUpdateData.setVariable(variableInfo.name, variableInfo.scope, variableInfo.type, value);
            }
        }

        if (form.hasChild("dueDate")) {
            Date dueDate = (Date)form.getChild("dueDate").getValue();
            if (dueDate != null)
                taskUpdateData.setDueDate(dueDate);
            else
                taskUpdateData.clearDueDate();
        }
        
        if (form.hasChild("priority")) {
            String priority = (String)form.getChild("priority").getValue();
            if (priority != null)
                taskUpdateData.setPriority(TaskPriority.fromString(priority));
        }


        return taskUpdateData;
    }
    
    private WfActorKey actorKeyFromPoolWidget(Widget poolWidget) {
        WfActorKey value = null;
        Object[] poolIds = (Object[])poolWidget.getValue();
        if (poolIds != null && poolIds.length > 0) {
            List<Long> poolIdsList = new ArrayList<Long>(poolIds.length);
            for (Object poolId : poolIds)
                poolIdsList.add((Long)poolId);
            value = new WfActorKey(poolIdsList);
        }
        return value;
    }
    private WfActorKey actorKeyFromUserWidget(Widget userWidget) {
        WfActorKey value = null;        
        String userLogin = (String)userWidget.getValue();
        if (userLogin != null)
            value = new WfActorKey(retrieveUserId(userLogin));
        return value;
    }

    private Object getValue(Widget variableWidget, WfValueType type) {
        Object value = null;
        switch (type) {
            case DAISY_LINK:
                String linkText = (String)variableWidget.getValue();
                if (linkText != null)
                    value = WfVersionKeyUtil.parseWfVersionKey(linkText, repository, siteConf);
                break;
            case ACTOR:
                Widget actorCaseWidget = variableWidget.lookupWidget("actorCase");
                Widget userWidget = variableWidget.lookupWidget("user/user");
                Widget poolWidget = variableWidget.lookupWidget("pool/pool");
                
                if (actorCaseWidget != null) {
                    String actorCase = (String)actorCaseWidget.getValue();
                    if ("user".equals(actorCase)) {
                        value = actorKeyFromUserWidget(variableWidget.lookupWidget("actor/user/user"));
                    } else if ("pool".equals(actorCase)) {
                        value = actorKeyFromPoolWidget(variableWidget.lookupWidget("actor/pool/pool"));
                    }
                } else if (userWidget != null) {
                    value = actorKeyFromUserWidget(userWidget);
                } else if (poolWidget != null) {
                    value = actorKeyFromPoolWidget(poolWidget);
                }                
                break;
            case USER:
                String userLogin = (String)variableWidget.getValue();
                if (userLogin != null)
                    value = new WfUserKey(retrieveUserId(userLogin));
                break;
            default:
                value = variableWidget.getValue();
        }
        return value;
    }

    private long retrieveUserId(String userLogin) {
        try {
            return repository.getUserManager().getPublicUserInfo(userLogin).getId();
        } catch (RepositoryException e) {
            throw new RuntimeException("Error trying to retrieve user ID for user login \"" + userLogin + "\".", e);
        }
    }

    private class RequiredVariablesValidator implements WidgetValidator {
        public boolean validate(Widget widget) {
            // Requiredness validation is implemented by custom code since
            // the variables are not required when just saving.
            if (form.getSubmitWidget() != null && form.getSubmitWidget().getId().equals("save") && form.lookupWidget("transitionName").getValue() == null) {
                return true;
            }

            boolean valid = true;

            Group variablesGroup = (Group)form.getChild("variables");
            Iterator childIt = variablesGroup.getChildren();
            while (childIt.hasNext()) {
                Widget child = (Widget)childIt.next();
                VariableInfo variableInfo = getVariableInfo(child);
                if (variableInfo != null && !variableInfo.readOnly) {
                    Object value = getValue(child, variableInfo.type);
                    if (variableInfo.required && value == null && child.validate()) {
                        getErrorTarget(child, variableInfo).setValidationError(new ValidationError("Required process variable", false));
                        valid = false;
                    }
                }
            }

            return valid;
        }
    }

    private ValidationErrorAware getErrorTarget(Widget widget, VariableInfo variableInfo) {
        if (variableInfo.type == WfValueType.ACTOR) {
            Widget actorWidget = widget.lookupWidget("actorCase");
            Widget userWidget = widget.lookupWidget("user/user");
            Widget poolWidget = widget.lookupWidget("pool/pool");
            
            if (actorWidget != null) {
                String actorType = (String)actorWidget.getValue();
                if ("user".equals(actorType)) {
                    return (ValidationErrorAware)widget.lookupWidget("actor/user/user");
                } else if ("pool".equals(actorType)) {
                    return (ValidationErrorAware)widget.lookupWidget("actor/pool/pool");
                } else {
                    return (ValidationErrorAware)actorWidget;
                }                
            } else if (userWidget != null) {
                return (ValidationErrorAware)userWidget;
            } else if (poolWidget != null){
                return (ValidationErrorAware)poolWidget;
            } else {
                return (ValidationErrorAware)widget;
            }
            
        } else {
            return (ValidationErrorAware)widget;
        }
    }

    public VariableInfo getVariableInfo(Widget widget) {
        VariableInfo variableInfo = new VariableInfo();
        variableInfo.name = (String)widget.getAttribute("variableName");
        if (variableInfo.name == null) // not a variable widget
            return null;
        variableInfo.scope = VariableScope.fromString((String)widget.getAttribute("variableScope"));
        variableInfo.type = WfValueType.fromString((String)widget.getAttribute("variableType"));
        variableInfo.readOnly = widget.getAttribute("variableReadOnly").equals("true");
        variableInfo.required = widget.getAttribute("variableRequired").equals("true");
        return variableInfo;
    }

    private static class VariableInfo {
        String name;
        VariableScope scope;
        WfValueType type;
        boolean readOnly;
        boolean required;
    }
    
    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

}