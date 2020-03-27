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
package org.outerj.daisy.workflow.clientimpl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.i18n.I18nMessage;
import org.outerj.daisy.i18n.impl.I18nMessageImpl;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.PartHelper;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.workflow.QueryConditions;
import org.outerj.daisy.workflow.QueryOrderByItem;
import org.outerj.daisy.workflow.QuerySelectItem;
import org.outerj.daisy.workflow.TaskPriority;
import org.outerj.daisy.workflow.TaskUpdateData;
import org.outerj.daisy.workflow.VariableScope;
import org.outerj.daisy.workflow.WfActorKey;
import org.outerj.daisy.workflow.WfExecutionPath;
import org.outerj.daisy.workflow.WfListItem;
import org.outerj.daisy.workflow.WfNodeDefinition;
import org.outerj.daisy.workflow.WfPoolManager;
import org.outerj.daisy.workflow.WfProcessDefinition;
import org.outerj.daisy.workflow.WfProcessInstance;
import org.outerj.daisy.workflow.WfTask;
import org.outerj.daisy.workflow.WfTaskDefinition;
import org.outerj.daisy.workflow.WfTimer;
import org.outerj.daisy.workflow.WfTransitionDefinition;
import org.outerj.daisy.workflow.WfValueType;
import org.outerj.daisy.workflow.WfVariable;
import org.outerj.daisy.workflow.WfVariableDefinition;
import org.outerj.daisy.workflow.WfVersionKey;
import org.outerj.daisy.workflow.WorkflowException;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.commonimpl.WfExecutionPathImpl;
import org.outerj.daisy.workflow.commonimpl.WfListItemImpl;
import org.outerj.daisy.workflow.commonimpl.WfNodeDefinitionImpl;
import org.outerj.daisy.workflow.commonimpl.WfPoolManagerImpl;
import org.outerj.daisy.workflow.commonimpl.WfPoolStrategy;
import org.outerj.daisy.workflow.commonimpl.WfProcessDefinitionImpl;
import org.outerj.daisy.workflow.commonimpl.WfProcessInstanceImpl;
import org.outerj.daisy.workflow.commonimpl.WfTaskDefinitionImpl;
import org.outerj.daisy.workflow.commonimpl.WfTaskImpl;
import org.outerj.daisy.workflow.commonimpl.WfTimerImpl;
import org.outerj.daisy.workflow.commonimpl.WfTransitionDefinitionImpl;
import org.outerj.daisy.workflow.commonimpl.WfVariableDefinitionImpl;
import org.outerj.daisy.workflow.commonimpl.WfXmlHelper;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerx.daisy.x10Workflow.Condition;
import org.outerx.daisy.x10Workflow.ExecutionPathDocument;
import org.outerx.daisy.x10Workflow.I18NType;
import org.outerx.daisy.x10Workflow.NodeDefinitionDocument;
import org.outerx.daisy.x10Workflow.ProcessDefinitionDocument;
import org.outerx.daisy.x10Workflow.ProcessDefinitionsDocument;
import org.outerx.daisy.x10Workflow.ProcessDocument;
import org.outerx.daisy.x10Workflow.ProcessInstanceCountsDocument;
import org.outerx.daisy.x10Workflow.ProcessesDocument;
import org.outerx.daisy.x10Workflow.QueryDocument;
import org.outerx.daisy.x10Workflow.SearchResultDocument;
import org.outerx.daisy.x10Workflow.SelectionListDocument;
import org.outerx.daisy.x10Workflow.StartProcessDocument;
import org.outerx.daisy.x10Workflow.TaskDefinitionDocument;
import org.outerx.daisy.x10Workflow.TaskDocument;
import org.outerx.daisy.x10Workflow.TasksDocument;
import org.outerx.daisy.x10Workflow.TimerDocument;
import org.outerx.daisy.x10Workflow.TimersDocument;
import org.outerx.daisy.x10Workflow.TransitionDefinitionDocument;
import org.outerx.daisy.x10Workflow.UpdateTaskDocument;
import org.outerx.daisy.x10Workflow.VariableDefinitionDocument;
import org.outerx.daisy.x10Workflow.VariableDocument;
import org.outerx.daisy.x10Workflow.VariableValuesType;
import org.outerx.daisy.x10Workflow.VariablesDocument;
import org.outerx.daisy.x10Workflowmeta.WorkflowAclInfoDocument;
import org.xml.sax.SAXException;

public class RemoteWorkflowManager implements WorkflowManager {
    public final RemoteRepositoryImpl repository;
    private WfPoolStrategy wfPoolStrategy;

    public RemoteWorkflowManager(RemoteRepositoryImpl repository, WfPoolStrategy strategy) {
        this.repository = repository;
        wfPoolStrategy = strategy;
    }

    public WfPoolManager getPoolManager() {
        return new WfPoolManagerImpl(repository, wfPoolStrategy);
    }

    public WfProcessDefinition deployProcessDefinition(InputStream is, String mimeType, Locale locale) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/processDefinition");

        byte[] data;
        try {
            data = PartHelper.streamToByteArrayAndClose(is, -1);
        } catch (IOException e) {
            throw new WorkflowException("Error reading process definition data", e);
        }

        List<org.apache.commons.httpclient.methods.multipart.Part> postParts = new ArrayList<org.apache.commons.httpclient.methods.multipart.Part>();
        postParts.add(new FilePart("processdefinition", new ByteArrayPartSource("processdefinition", data), mimeType, null));
        method.setRequestEntity(new MultipartRequestEntity(postParts.toArray(new org.apache.commons.httpclient.methods.multipart.Part[0]), method.getParams()));

        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});

        ProcessDefinitionDocument responseDocument = (ProcessDefinitionDocument)httpClient.executeMethod(method, ProcessDefinitionDocument.class, true);
        return instantiateProcessDefinition(responseDocument.getProcessDefinition());
    }

    public void loadSampleWorkflows() throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/processDefinition");
        method.setQueryString(new NameValuePair[] {new NameValuePair("action", "loadSamples")});
        httpClient.executeMethod(method, null, true);
    }

    public void deleteProcessDefinition(String processDefinitionId) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        String encodedId = encodeStringForUseInPath("process definition id", processDefinitionId);
        DeleteMethod method = new DeleteMethod("/workflow/processDefinition/" + encodedId);
        httpClient.executeMethod(method, null, true);
    }

    public WfProcessDefinition getProcessDefinition(String processDefinitionId, Locale locale) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        String encodedId = encodeStringForUseInPath("process definition id", processDefinitionId);
        GetMethod method = new GetMethod("/workflow/processDefinition/" + encodedId);
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});

        ProcessDefinitionDocument responseDocument = (ProcessDefinitionDocument)httpClient.executeMethod(method, ProcessDefinitionDocument.class, true);
        return instantiateProcessDefinition(responseDocument.getProcessDefinition());
    }

    public WfProcessDefinition getLatestProcessDefinition(String workflowName, Locale locale) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        String encodedName = encodeStringForUseInPath("process definition name", workflowName);
        GetMethod method = new GetMethod("/workflow/processDefinitionByName/" + encodedName);
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});

        ProcessDefinitionDocument responseDocument = (ProcessDefinitionDocument)httpClient.executeMethod(method, ProcessDefinitionDocument.class, true);
        return instantiateProcessDefinition(responseDocument.getProcessDefinition());
    }

    public List<WfProcessDefinition> getAllLatestProcessDefinitions(Locale locale) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/workflow/processDefinition");

        NameValuePair[] queryString = { new NameValuePair("latestOnly", "true"),
                new NameValuePair("locale", LocaleHelper.getString(locale)) };
        method.setQueryString(queryString);

        ProcessDefinitionsDocument responseDocument = (ProcessDefinitionsDocument)httpClient.executeMethod(method, ProcessDefinitionsDocument.class, true);
        return instantiateProcessDefinitions(responseDocument.getProcessDefinitions());
    }

    public List<WfProcessDefinition> getAllProcessDefinitions(Locale locale) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/workflow/processDefinition");
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});

        ProcessDefinitionsDocument responseDocument = (ProcessDefinitionsDocument)httpClient.executeMethod(method, ProcessDefinitionsDocument.class, true);
        return instantiateProcessDefinitions(responseDocument.getProcessDefinitions());
    }

    public Map<String, Integer> getProcessInstanceCounts() throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/workflow/processInstanceCounts");

        ProcessInstanceCountsDocument responseDocument = (ProcessInstanceCountsDocument)httpClient.executeMethod(method, ProcessInstanceCountsDocument.class, true);

        Map<String, Integer> instanceCounts = new HashMap<String, Integer>();
        for (ProcessInstanceCountsDocument.ProcessInstanceCounts.ProcessInstanceCount count : responseDocument.getProcessInstanceCounts().getProcessInstanceCountList()) {
            instanceCounts.put(count.getDefinitionId(), count.getCount());
        }

        return instanceCounts;
    }

    public List<WfVariable> getInitialVariables(String processDefinitionId, WfVersionKey contextDocument) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        String encodedId = encodeStringForUseInPath("process definition id", processDefinitionId);
        GetMethod method = new GetMethod("/workflow/processDefinition/" + encodedId + "/initialVariables");

        List<NameValuePair> queryString = new ArrayList<NameValuePair>(5);
        if (contextDocument != null) {
            queryString.add(new NameValuePair("contextDocId", contextDocument.getDocumentId()));
            queryString.add(new NameValuePair("contextDocBranch", String.valueOf(contextDocument.getBranchId())));
            queryString.add(new NameValuePair("contextDocLanguage", String.valueOf(contextDocument.getLanguageId())));
            if (contextDocument.getVersion() != null)
                queryString.add(new NameValuePair("contextDocVersion", contextDocument.getVersion()));
        }
        method.setQueryString(queryString.toArray(new NameValuePair[0]));

        VariablesDocument responseDocument = (VariablesDocument)httpClient.executeMethod(method, VariablesDocument.class, true);
        return instantiateVariables(responseDocument.getVariables());
    }

    public WfProcessInstance startProcess(String processDefinitionId, TaskUpdateData startTaskData,
            String initialTransition, Locale locale) throws RepositoryException {
        StartProcessDocument document = StartProcessDocument.Factory.newInstance();
        StartProcessDocument.StartProcess xml = document.addNewStartProcess();
        xml.setProcessDefinitionId(processDefinitionId);
        if (initialTransition != null)
            xml.setInitialTransition(initialTransition);

        if (startTaskData != null) {
            xml.setTaskUpdateData(WfXmlHelper.getTaskUpdateXml(startTaskData).getTaskUpdateData());
        }

        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/process");
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});

        method.setRequestEntity(new InputStreamRequestEntity(document.newInputStream()));

        ProcessDocument responseDocument = (ProcessDocument)httpClient.executeMethod(method, ProcessDocument.class, true);
        return instantiateProcess(responseDocument.getProcess(), locale);
    }

    public WfExecutionPath signal(String processInstanceId, String executionPathFullName, String transitionName, Locale locale) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        String encodedId = encodeStringForUseInPath("process instance id", processInstanceId);
        PostMethod method = new PostMethod("/workflow/process/" + encodedId);

        List<NameValuePair> queryString = new ArrayList<NameValuePair>(2);
        queryString.add(new NameValuePair("action", "signal"));
        queryString.add(new NameValuePair("executionPath", executionPathFullName));
        if (transitionName != null)
            queryString.add(new NameValuePair("transitionName", transitionName));
        queryString.add(new NameValuePair("locale", LocaleHelper.getString(locale)));

        method.setQueryString(queryString.toArray(new NameValuePair[0]));

        ExecutionPathDocument responseDocument = (ExecutionPathDocument)httpClient.executeMethod(method, ExecutionPathDocument.class, true);
        return instantiateExecutionPath(responseDocument.getExecutionPath());
    }

    public WfProcessInstance getProcess(String processInstanceId, Locale locale) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        String encodedId = encodeStringForUseInPath("process instance id", processInstanceId);
        GetMethod method = new GetMethod("/workflow/process/" + encodedId);
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});
        ProcessDocument responseDocument = (ProcessDocument)httpClient.executeMethod(method, ProcessDocument.class, true);
        return instantiateProcess(responseDocument.getProcess(), locale);
    }

    public WfTask updateTask(String taskId, TaskUpdateData taskUpdateData, Locale locale) throws RepositoryException {
        String encodedId = encodeStringForUseInPath("task id", taskId);
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/task/" + encodedId);

        UpdateTaskDocument document = UpdateTaskDocument.Factory.newInstance();
        UpdateTaskDocument.UpdateTask xml = document.addNewUpdateTask();
        if (taskUpdateData != null)
            xml.setTaskUpdateData(WfXmlHelper.getTaskUpdateXml(taskUpdateData).getTaskUpdateData());
        xml.setEndTask(false);

        method.setRequestEntity(new InputStreamRequestEntity(document.newInputStream()));
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});

        TaskDocument responseDocument = (TaskDocument)httpClient.executeMethod(method, TaskDocument.class, true);
        return instantiateTask(responseDocument.getTask(), locale);
    }

    public WfTask endTask(String taskId, TaskUpdateData taskUpdateData, String transitionName, Locale locale) throws RepositoryException {
        String encodedId = encodeStringForUseInPath("task id", taskId);
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/task/" + encodedId);

        UpdateTaskDocument document = UpdateTaskDocument.Factory.newInstance();
        UpdateTaskDocument.UpdateTask xml = document.addNewUpdateTask();
        if (taskUpdateData != null)
            xml.setTaskUpdateData(WfXmlHelper.getTaskUpdateXml(taskUpdateData).getTaskUpdateData());
        if (transitionName != null)
            xml.setTransitionName(transitionName);
        xml.setEndTask(true);

        method.setRequestEntity(new InputStreamRequestEntity(document.newInputStream()));
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});
        TaskDocument responseDocument = (TaskDocument)httpClient.executeMethod(method, TaskDocument.class, true);
        return instantiateTask(responseDocument.getTask(), locale);
    }

    public WfTask getTask(String taskId, Locale locale) throws RepositoryException {
        String encodedId = encodeStringForUseInPath("task id", taskId);
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/workflow/task/" + encodedId);
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});

        TaskDocument responseDocument = (TaskDocument)httpClient.executeMethod(method, TaskDocument.class, true);
        return instantiateTask(responseDocument.getTask(), locale);
    }

    public List<WfTask> getMyTasks(Locale locale) throws RepositoryException {
        return getTasks(null, locale);
    }

    public List<WfTask> getPooledTasks(Locale locale) throws RepositoryException {
        return getTasks("pooled", locale);
    }

    public List<WfTask> getTasks(QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException {
        QueryDocument queryDoc = buildQueryDocument(null, queryConditions, orderByItems, chunkOffset, chunkLength);

        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/query/task");
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});
        method.setRequestEntity(new InputStreamRequestEntity(queryDoc.newInputStream()));

        TasksDocument responseDocument = (TasksDocument)httpClient.executeMethod(method, TasksDocument.class, true);
        return instantiateTasks(responseDocument.getTasks(), locale);
    }

    public SearchResultDocument searchTasks(List<QuerySelectItem> selectItems, QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException {
        if (selectItems == null)
            throw new IllegalArgumentException("Null argument: selectItems");

        QueryDocument queryDoc = buildQueryDocument(selectItems, queryConditions, orderByItems, chunkOffset, chunkLength);

        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/query/task");
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});
        method.setRequestEntity(new InputStreamRequestEntity(queryDoc.newInputStream()));

        SearchResultDocument responseDoc = (SearchResultDocument)httpClient.executeMethod(method, SearchResultDocument.class, true);
        return responseDoc;
    }

    public List<WfProcessInstance> getProcesses(QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException {
        QueryDocument queryDoc = buildQueryDocument(null, queryConditions, orderByItems, chunkOffset, chunkLength);

        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/query/process");
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});
        method.setRequestEntity(new InputStreamRequestEntity(queryDoc.newInputStream()));

        ProcessesDocument responseDocument = (ProcessesDocument)httpClient.executeMethod(method, ProcessesDocument.class, true);
        return instantiateProcesses(responseDocument.getProcesses(), locale);
    }

    public SearchResultDocument searchProcesses(List<QuerySelectItem> selectItems, QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException {
        if (selectItems == null)
            throw new IllegalArgumentException("Null argument: selectItems");

        QueryDocument queryDoc = buildQueryDocument(selectItems, queryConditions, orderByItems, chunkOffset, chunkLength);

        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/query/process");
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});
        method.setRequestEntity(new InputStreamRequestEntity(queryDoc.newInputStream()));

        SearchResultDocument responseDoc = (SearchResultDocument)httpClient.executeMethod(method, SearchResultDocument.class, true);
        return responseDoc;
    }

    private QueryDocument buildQueryDocument(List<QuerySelectItem> selectItems, QueryConditions queryConditions,
            List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength) {

        QueryDocument queryDoc = QueryDocument.Factory.newInstance();
        QueryDocument.Query queryXml = queryDoc.addNewQuery();

        queryXml.setChunkOffset(chunkOffset);
        queryXml.setChunkLength(chunkLength);

        // Add select clause
        if (selectItems != null) {
            QueryDocument.Query.SelectClause selectClauseXml = queryXml.addNewSelectClause();
            for (QuerySelectItem select : selectItems) {
                QueryDocument.Query.SelectClause.Select selectXml = selectClauseXml.addNewSelect();
                selectXml.setName(select.getName());
                selectXml.setType(select.getType().toString());
            }
        }

        // Add conditions
        QueryDocument.Query.Conditions conditionsXml = queryXml.addNewConditions();
        conditionsXml.setMeetAllCriteria(queryConditions.getMatchAllCriteria());

        for (QueryConditions.PropertyConditionInfo cond : queryConditions.getPropertyConditions()) {
            setCondition(conditionsXml.addNewPropertyCondition(), cond.propertyName,
                    cond.type, cond.operatorName, cond.values);
        }

        for (QueryConditions.VariableConditionInfo cond : queryConditions.getTaskVariableConditions()) {
            setCondition(conditionsXml.addNewTaskVariableCondition(), cond.name, cond.type, cond.operatorName, cond.values);
        }

        for (QueryConditions.VariableConditionInfo cond : queryConditions.getProcessVariableConditions()) {
            setCondition(conditionsXml.addNewProcessVariableCondition(), cond.name, cond.type, cond.operatorName, cond.values);
        }

        for (QueryConditions.SpecialConditionInfo specialCond : queryConditions.getSpecialConditions()) {
            QueryDocument.Query.Conditions.SpecialCondition condXml = conditionsXml.addNewSpecialCondition();
            condXml.setName(specialCond.name);
            for (int i = 0; i < specialCond.argTypes.size(); i++) {
                VariableValuesType valueXml = condXml.addNewValue();
                WfXmlHelper.setValue(valueXml, specialCond.argTypes.get(i), specialCond.argValues.get(i));
            }
        }

        // Add order by clause
        for (QueryOrderByItem orderBy : orderByItems) {
            QueryDocument.Query.OrderByClause orderByClauseXml = queryXml.addNewOrderByClause();
            QueryDocument.Query.OrderByClause.OrderBy orderByXml = orderByClauseXml.addNewOrderBy();
            orderByXml.setName(orderBy.getName());
            orderByXml.setSortOrder(orderBy.getSortOrder().toString());
            orderByXml.setType(orderBy.getType().toString());
        }

        return queryDoc;
    }

    private void setCondition(Condition condition, String name, WfValueType type, String operatorName, List<Object> values) {
        condition.setName(name);
        condition.setValueType(type.toString());
        condition.setOperator(operatorName);
        for (Object value : values) {
            VariableValuesType valueXml = condition.addNewValue();
            WfXmlHelper.setValue(valueXml, type, value);
        }
    }

    private List<WfTask> getTasks(String select, Locale locale) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/workflow/task");

        List<NameValuePair> queryString = new ArrayList<NameValuePair>(2);
        queryString.add(new NameValuePair("locale", LocaleHelper.getString(locale)));
        if (select != null)
            queryString.add(new NameValuePair("select", select));

        method.setQueryString(queryString.toArray(new NameValuePair[0]));

        TasksDocument responseDocument = (TasksDocument)httpClient.executeMethod(method, TasksDocument.class, true);
        return instantiateTasks(responseDocument.getTasks(), locale);
    }

    public WfTask requestPooledTask(String taskId, Locale locale) throws RepositoryException {
        String encodedId = encodeStringForUseInPath("task id", taskId);
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/task/" + encodedId);
        method.setQueryString(new NameValuePair[] {
                new NameValuePair("action", "requestPooledTask"),
                new NameValuePair("locale", LocaleHelper.getString(locale))});

        TaskDocument responseDocument = (TaskDocument)httpClient.executeMethod(method, TaskDocument.class, true);
        return instantiateTask(responseDocument.getTask(), locale);
    }

    public WfTask unassignTask(String taskId, Locale locale) throws RepositoryException {
        String encodedId = encodeStringForUseInPath("task id", taskId);
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/task/" + encodedId);
        method.setQueryString(new NameValuePair[] {
                new NameValuePair("action", "unassignTask"),
                new NameValuePair("locale", LocaleHelper.getString(locale))});

        TaskDocument responseDocument = (TaskDocument)httpClient.executeMethod(method, TaskDocument.class, true);
        return instantiateTask(responseDocument.getTask(), locale);
    }

    public WfTask assignTask(String taskId, WfActorKey actor, boolean overwriteSwimlane, Locale locale) throws RepositoryException {
        String encodedId = encodeStringForUseInPath("task id", taskId);
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/task/" + encodedId);

        List<NameValuePair> queryString = new ArrayList<NameValuePair>();
        queryString.add(new NameValuePair("action", "assignTask"));
        queryString.add(new NameValuePair("overwriteSwimlane", String.valueOf(overwriteSwimlane)));
        queryString.add(new NameValuePair("locale", LocaleHelper.getString(locale)));
        queryString.add(new NameValuePair("actorType", actor.isUser() ? "user" : "pools"));

        if (actor.isUser()) {
            queryString.add(new NameValuePair("actor", String.valueOf(actor.getUserId())));
        } else {
            for (long poolId : actor.getPoolIds())
                queryString.add(new NameValuePair("actor", String.valueOf(poolId)));
        }

        method.setQueryString(queryString.toArray(new NameValuePair[0]));

        TaskDocument responseDocument = (TaskDocument)httpClient.executeMethod(method, TaskDocument.class, true);
        return instantiateTask(responseDocument.getTask(), locale);
    }

    public void deleteProcess(String processInstanceId) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        String encodedId = encodeStringForUseInPath("process instance id", processInstanceId);
        DeleteMethod method = new DeleteMethod("/workflow/process/" + encodedId);
        httpClient.executeMethod(method, null, true);
    }

    public WfProcessInstance suspendProcess(String processInstanceId, Locale locale) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        String encodedId = encodeStringForUseInPath("process instance id", processInstanceId);
        PostMethod method = new PostMethod("/workflow/process/" + encodedId);

        List<NameValuePair> queryString = new ArrayList<NameValuePair>(2);
        queryString.add(new NameValuePair("action", "suspend"));
        method.setQueryString(queryString.toArray(new NameValuePair[0]));

        ProcessDocument responseDocument = (ProcessDocument)httpClient.executeMethod(method, ProcessDocument.class, true);
        return instantiateProcess(responseDocument.getProcess(), locale);
    }

    public WfProcessInstance resumeProcess(String processInstanceId, Locale locale) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        String encodedId = encodeStringForUseInPath("process instance id", processInstanceId);
        PostMethod method = new PostMethod("/workflow/process/" + encodedId);

        List<NameValuePair> queryString = new ArrayList<NameValuePair>(2);
        queryString.add(new NameValuePair("action", "resume"));
        method.setQueryString(queryString.toArray(new NameValuePair[0]));

        ProcessDocument responseDocument = (ProcessDocument)httpClient.executeMethod(method, ProcessDocument.class, true);
        return instantiateProcess(responseDocument.getProcess(), locale);
    }

    public WfTimer getTimer(String timerId, Locale locale) throws RepositoryException {
        String encodedId = encodeStringForUseInPath("timer id", timerId);
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/workflow/timer/" + encodedId);
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});

        TimerDocument responseDocument = (TimerDocument)httpClient.executeMethod(method, TimerDocument.class, true);
        return instantiateTimer(responseDocument.getTimer(), locale);
    }

    public List<WfTimer> getTimers(QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException {
        QueryDocument queryDoc = buildQueryDocument(null, queryConditions, orderByItems, chunkOffset, chunkLength);

        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/query/timer");
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});
        method.setRequestEntity(new InputStreamRequestEntity(queryDoc.newInputStream()));

        TimersDocument responseDocument = (TimersDocument)httpClient.executeMethod(method, TimersDocument.class, true);
        return instantiateTimers(responseDocument.getTimers(), locale);
    }

    public SearchResultDocument searchTimers(List<QuerySelectItem> selectItems, QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException {
        if (selectItems == null)
            throw new IllegalArgumentException("Null argument: selectItems");

        QueryDocument queryDoc = buildQueryDocument(selectItems, queryConditions, orderByItems, chunkOffset, chunkLength);

        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/workflow/query/timer");
        method.setQueryString(new NameValuePair[] {new NameValuePair("locale", LocaleHelper.getString(locale))});
        method.setRequestEntity(new InputStreamRequestEntity(queryDoc.newInputStream()));

        SearchResultDocument responseDoc = (SearchResultDocument)httpClient.executeMethod(method, SearchResultDocument.class, true);
        return responseDoc;
    }

    private List<WfProcessDefinition> instantiateProcessDefinitions(ProcessDefinitionsDocument.ProcessDefinitions xml) throws WorkflowException {
        List<ProcessDefinitionDocument.ProcessDefinition> processDefsXml = xml.getProcessDefinitionList();
        List<WfProcessDefinition> processDefs = new ArrayList<WfProcessDefinition>(processDefsXml.size());
        for (ProcessDefinitionDocument.ProcessDefinition processDefXml : processDefsXml) {
            processDefs.add(instantiateProcessDefinition(processDefXml));
        }
        return processDefs;
    }

    private WfProcessDefinition instantiateProcessDefinition(ProcessDefinitionDocument.ProcessDefinition xml) throws WorkflowException {
        List<String> problems = xml.isSetProblems() ? xml.getProblems().getProblemList() : null;
        I18nMessage label = instantiateI18nMessage(xml.getLabel());
        I18nMessage description = instantiateI18nMessage(xml.getDescription());
        WfNodeDefinition startNodeDefinition = instantiateNodeDefinition(xml.getStartNodeDefinition().getNodeDefinition());
        WfTaskDefinition startTask = xml.isSetStartTask() ? instantiateTaskDefinition(xml.getStartTask().getTaskDefinition()) : null;

        Map<String, WfTaskDefinition> taskDefs = new HashMap<String, WfTaskDefinition>();
        if (xml.isSetTasks()) {
            for (TaskDefinitionDocument.TaskDefinition taskDefXml : xml.getTasks().getTaskDefinitionList()) {
                WfTaskDefinition taskDef = instantiateTaskDefinition(taskDefXml);
                taskDefs.put(taskDef.getName(), taskDef);
            }
        }

        List<WfVariableDefinition> globalVarDefs = new ArrayList<WfVariableDefinition>();
        if (xml.isSetVariableDefinitions()) {
            for (VariableDefinitionDocument.VariableDefinition varDefXml : xml.getVariableDefinitions().getVariableDefinitionList()) {
                globalVarDefs.add(instantiateVariableDefinition(varDefXml));
            }
        }

        return new WfProcessDefinitionImpl(xml.getId(), xml.getName(), xml.getVersion(), problems, taskDefs, startTask,
                startNodeDefinition, label, description, globalVarDefs);
    }

    private I18nMessage instantiateI18nMessage(I18NType i18nXml) {
        if (i18nXml == null)
            return null;

        // TODO guess the performance of this isn't too good -- needs check
        SaxBuffer buffer = new SaxBuffer();
        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setSaveInner();
        try {
            i18nXml.save(new StripDocumentHandler(buffer), null, xmlOptions);
        } catch (SAXException e) {
            throw new RuntimeException("Unexpected error building i18n message", e);
        }
        return new I18nMessageImpl(buffer);
    }

    private WfTaskDefinition instantiateTaskDefinition(TaskDefinitionDocument.TaskDefinition xml) throws WorkflowException {
        List<WfVariableDefinition> vars = new ArrayList<WfVariableDefinition>();
        if (xml.isSetVariableDefinitions()) {
            for (VariableDefinitionDocument.VariableDefinition varDefXml : xml.getVariableDefinitions().getVariableDefinitionList()) {
                vars.add(instantiateVariableDefinition(varDefXml));
            }
        }

        WfNodeDefinition node = instantiateNodeDefinition(xml.getNodeDefinition());

        return new WfTaskDefinitionImpl(xml.getName(), instantiateI18nMessage(xml.getLabel()),
                instantiateI18nMessage(xml.getDescription()), vars, node);
    }

    private WfVariableDefinition instantiateVariableDefinition(VariableDefinitionDocument.VariableDefinition xml) throws WorkflowException {
        WfValueType type = WfValueType.fromString(xml.getType().toString());
        VariableScope scope = VariableScope.fromString(xml.getScope().toString());
        boolean readOnly = xml.isSetReadOnly() ? xml.getReadOnly() : true;
        boolean hidden = xml.isSetHidden() ? xml.getHidden() : false;
        boolean required = xml.isSetRequired() ? xml.getRequired() : true;
        I18nMessage label = instantiateI18nMessage(xml.getLabel());
        I18nMessage description = instantiateI18nMessage(xml.getDescription());

        List<WfListItem> selectionList = null;
        if (xml.isSetSelectionList()) {
            selectionList = new ArrayList<WfListItem>();
            List<SelectionListDocument.SelectionList.ListItem> listItemsXml = xml.getSelectionList().getListItemList();
            for (SelectionListDocument.SelectionList.ListItem listItemXml : listItemsXml) {
                WfXmlHelper.ValueData data = WfXmlHelper.getValue(listItemXml, "selection list item");
                if (data.type != type)
                    throw new WorkflowException("List item value type does not correspond to variable value type. Expected " + type + " but got " + data.type);
                I18nMessage itemLabel = instantiateI18nMessage(listItemXml.getLabel());
                selectionList.add(new WfListItemImpl(data.value, itemLabel));
            }
        }

        XmlObject styling = xml.getStyling();

        return new WfVariableDefinitionImpl(xml.getName(), type, readOnly, label, description, required, hidden, scope, selectionList, null, styling);
    }

    private WfNodeDefinition instantiateNodeDefinition(NodeDefinitionDocument.NodeDefinition xml) {
        List<TransitionDefinitionDocument.TransitionDefinition> transDefsXml = xml.getLeavingTransitions().getTransitionDefinitionList();
        List<WfTransitionDefinition> leavingTransitions = new ArrayList<WfTransitionDefinition>(transDefsXml.size());
        for (TransitionDefinitionDocument.TransitionDefinition transDefXml : transDefsXml) {
            leavingTransitions.add(instantiateTransitionDefinition(transDefXml));
        }

        return new WfNodeDefinitionImpl(xml.getName(), xml.getFullyQualifiedName(), xml.getNodeType(),
                leavingTransitions, xml.getProcessDefinitionId(), xml.getProcessDefinitionName());
    }

    private WfTransitionDefinition instantiateTransitionDefinition(TransitionDefinitionDocument.TransitionDefinition xml) {
        return new WfTransitionDefinitionImpl(xml.getName(), instantiateI18nMessage(xml.getLabel()), instantiateI18nMessage(xml.getConfirmation()));
    }

    private WfProcessInstance instantiateProcess(ProcessDocument.Process xml, Locale locale) throws WorkflowException {
        Date start = xml.getStart().getTime();
        Date end = xml.isSetEnd() ? xml.getEnd().getTime() : null;
        WfExecutionPath rootExecutionPath = instantiateExecutionPath(xml.getExecutionPath());

        Map<String, WfTask> tasks;
        if (xml.isSetTasks()) {
            List<TaskDocument.Task> tasksXml = xml.getTasks().getTaskList();
            tasks = new HashMap<String, WfTask>();
            for (TaskDocument.Task taskXml : tasksXml) {
                WfTask task = instantiateTask(taskXml, locale);
                tasks.put(task.getDefinition().getName(), task);
            }
        } else {
            tasks = Collections.emptyMap();
        }

        return new WfProcessInstanceImpl(xml.getId(), xml.getDefinitionId(), start, end, xml.getSuspended(), tasks,
                rootExecutionPath, locale);
    }

    private List<WfTask> instantiateTasks(TasksDocument.Tasks xml, Locale locale) throws WorkflowException {
        List<TaskDocument.Task> tasksXml = xml.getTaskList();
        List<WfTask> tasks = new ArrayList<WfTask>(tasksXml.size());

        for (TaskDocument.Task taskXml : tasksXml) {
            tasks.add(instantiateTask(taskXml, locale));
        }

        return tasks;
    }

    private List<WfProcessInstance> instantiateProcesses(ProcessesDocument.Processes xml, Locale locale) throws WorkflowException {
        List<ProcessDocument.Process> processesXml = xml.getProcessList();
        List<WfProcessInstance> processes = new ArrayList<WfProcessInstance>(processesXml.size());

        for (ProcessDocument.Process processXml : processesXml) {
            processes.add(instantiateProcess(processXml, locale));
        }

        return processes;
    }

    private WfTask instantiateTask(TaskDocument.Task xml, Locale locale) throws WorkflowException {
        Date created = xml.getCreated().getTime();
        Date dueDate = xml.isSetDueDate() ? xml.getDueDate().getTime() : null;
        Date end = xml.isSetEnd() ? xml.getEnd().getTime() : null;
        TaskPriority priority = TaskPriority.fromString(xml.getPriority().toString());

        List<WfVariable> variables;
        if (xml.isSetVariables()) {
            variables = new ArrayList<WfVariable>();
            for (VariableDocument.Variable variableXml : xml.getVariables().getVariableList()) {
                variables.add(WfXmlHelper.instantiateVariable(variableXml));
            }
        } else {
            variables = Collections.emptyList();
        }

        WfTaskDefinition taskDefinition = instantiateTaskDefinition(xml.getTaskDefinition());

        return new WfTaskImpl(xml.getId(), created, end, priority, dueDate, xml.getActorId(), xml.getHasPools(),
                xml.getHasSwimlane(), xml.getProcessId(), xml.getExecutionPath(), variables, taskDefinition, locale);
    }

    private WfExecutionPath instantiateExecutionPath(ExecutionPathDocument.ExecutionPath xml) {
        List<ExecutionPathDocument.ExecutionPath> childrenXml = xml.getChildren().getExecutionPathList();
        List<WfExecutionPath> children = new ArrayList<WfExecutionPath>(childrenXml.size());
        for (ExecutionPathDocument.ExecutionPath childXml : childrenXml) {
            children.add(instantiateExecutionPath(childXml));
        }
        Date start = xml.getStart().getTime();
        Date end = xml.isSetEnd() ? xml.getEnd().getTime() : null;

        WfNodeDefinition node = instantiateNodeDefinition(xml.getNodeDefinition());
        return new WfExecutionPathImpl(xml.getPath(), start, end, children, node);
    }

    private WfTimer instantiateTimer(TimerDocument.Timer xml, Locale locale) {
        Date dueDate = xml.getDueDate() != null ? xml.getDueDate().getTime() : null;
        return new WfTimerImpl(xml.getId(), xml.getName(), dueDate, xml.getRecurrence(), xml.getException(),
                xml.getSuspended(), xml.getProcessId(), xml.getExecutionPath(), xml.getTransitionName(), locale);
    }

    private List<WfTimer> instantiateTimers(TimersDocument.Timers xml, Locale locale) {
        List<TimerDocument.Timer> timersXml = xml.getTimerList();
        List<WfTimer> timers = new ArrayList<WfTimer>(timersXml.size());

        for (TimerDocument.Timer timerXml : timersXml) {
            timers.add(instantiateTimer(timerXml, locale));
        }

        return timers;
    }

    private List<WfVariable> instantiateVariables(VariablesDocument.Variables xml) {
        List<VariableDocument.Variable> variablesXml = xml.getVariableList();
        List<WfVariable> variables = new ArrayList<WfVariable>(variablesXml.size());

        for (VariableDocument.Variable variableXml : variablesXml) {
            variables.add(WfXmlHelper.instantiateVariable(variableXml));
        }

        return variables;
    }
    
    public WorkflowAclInfoDocument getAclInfo(String taskId, String processId, String processDefinitionId, boolean includeGlobalInfo) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/workflow/aclInfo");
        List<NameValuePair> params = new ArrayList<NameValuePair>(5);
        
        if (taskId != null) 
            params.add(new NameValuePair("taskId", taskId));
        if (processId != null) 
            params.add(new NameValuePair("processId", processId));
        if (processDefinitionId != null) 
            params.add(new NameValuePair("processDefinitionId", processDefinitionId));
        if (includeGlobalInfo) {
            params.add(new NameValuePair("includeGlobalInfo", Boolean.toString(includeGlobalInfo)));
        }

        method.setQueryString(params.toArray(new NameValuePair[params.size()]));
        return (WorkflowAclInfoDocument)httpClient.executeMethod(method, WorkflowAclInfoDocument.class, true);
    }

    public static String encodeStringForUseInPath(String name, String value) throws RepositoryException {
        try {
            return URIUtil.encodeWithinPath(value);
        } catch (URIException e) {
            throw new RepositoryException("Error encoding " + name + " string", e);
        }
    }

}
