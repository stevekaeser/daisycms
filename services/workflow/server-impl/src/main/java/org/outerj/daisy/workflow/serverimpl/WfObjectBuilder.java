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
package org.outerj.daisy.workflow.serverimpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xmlbeans.XmlObject;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.Node;
import org.jbpm.graph.def.NodeCollection;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.def.Transition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.outerj.daisy.i18n.DResourceBundles;
import org.outerj.daisy.i18n.I18nMessage;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.workflow.TaskPriority;
import org.outerj.daisy.workflow.VariableScope;
import org.outerj.daisy.workflow.WfExecutionPath;
import org.outerj.daisy.workflow.WfListItem;
import org.outerj.daisy.workflow.WfNodeDefinition;
import org.outerj.daisy.workflow.WfProcessDefinition;
import org.outerj.daisy.workflow.WfProcessInstance;
import org.outerj.daisy.workflow.WfTask;
import org.outerj.daisy.workflow.WfTaskDefinition;
import org.outerj.daisy.workflow.WfTimer;
import org.outerj.daisy.workflow.WfTransitionDefinition;
import org.outerj.daisy.workflow.WfValueType;
import org.outerj.daisy.workflow.WfVariable;
import org.outerj.daisy.workflow.WfVariableDefinition;
import org.outerj.daisy.workflow.WorkflowException;
import org.outerj.daisy.workflow.commonimpl.WfExecutionPathImpl;
import org.outerj.daisy.workflow.commonimpl.WfListItemImpl;
import org.outerj.daisy.workflow.commonimpl.WfNodeDefinitionImpl;
import org.outerj.daisy.workflow.commonimpl.WfProcessDefinitionImpl;
import org.outerj.daisy.workflow.commonimpl.WfProcessInstanceImpl;
import org.outerj.daisy.workflow.commonimpl.WfTaskDefinitionImpl;
import org.outerj.daisy.workflow.commonimpl.WfTaskImpl;
import org.outerj.daisy.workflow.commonimpl.WfTimerImpl;
import org.outerj.daisy.workflow.commonimpl.WfTransitionDefinitionImpl;
import org.outerj.daisy.workflow.commonimpl.WfVariableDefinitionImpl;
import org.outerj.daisy.workflow.commonimpl.WfVariableImpl;
import org.outerj.daisy.workflow.commonimpl.WfXmlHelper;
import org.outerx.daisy.x10Workflowmeta.I18NType;
import org.outerx.daisy.x10Workflowmeta.SelectionListDocument;
import org.outerx.daisy.x10Workflowmeta.TaskDocument;
import org.outerx.daisy.x10Workflowmeta.TransitionDocument;
import org.outerx.daisy.x10Workflowmeta.VariableDocument;
import org.outerx.daisy.x10Workflowmeta.WorkflowMetaDocument;

/**
 * Contains utility methods for building Daisy workflow objects
 * based on the data in jBPM objects.
 *
 * <p>Performs caching of the definition objects (when using the get*() methods).
 */
public class WfObjectBuilder {
    private WorkflowMetaManager wfMetaManager;
    private ConcurrentHashMap<String, Object> cache = new ConcurrentHashMap<String, Object>();
    private Log log = LogFactory.getLog(getClass());

    public WfObjectBuilder(WorkflowMetaManager workflowMetaManager) {
        this.wfMetaManager = workflowMetaManager;
    }

    public void clearCache() {
        synchronized (this) {
            cache.clear();
        }
    }

    @SuppressWarnings("unchecked")
    public WfProcessInstance buildProcessInstance(ProcessInstance processInstance, Locale locale) throws WorkflowException {
        WfExecutionPath executionPath = buildExecutionPaths(processInstance.getRootToken(), locale);

        Collection<TaskInstance> taskInstances = processInstance.getTaskMgmtInstance().getTaskInstances();
        Map<String, WfTask> tasks = new HashMap<String, WfTask>();
        for (TaskInstance taskInstance : taskInstances) {
            WfTask task = buildTask(taskInstance, locale);
            tasks.put(task.getDefinition().getName(), task);
        }

        return new WfProcessInstanceImpl(String.valueOf(processInstance.getId()),
                String.valueOf(processInstance.getProcessDefinition().getId()),
                processInstance.getStart(), processInstance.getEnd(), processInstance.isSuspended(), tasks,
                executionPath, locale);
    }

    @SuppressWarnings("unchecked")
    public WfExecutionPath buildExecutionPaths(Token token, Locale locale) throws WorkflowException {
        List<WfExecutionPath> children;
        if (token.getChildren() != null) {
            Collection<Token> childTokens = token.getChildren().values();
            children = new ArrayList<WfExecutionPath>();
            for (Token childToken : childTokens) {
                children.add(buildExecutionPaths(childToken, locale));
            }
        } else {
            children = Collections.emptyList();
        }

        WfNodeDefinition node = getNodeDefinition(token.getNode(), locale);

        return new WfExecutionPathImpl(token.getFullName(), token.getStart(), token.getEnd(), children, node);
    }

    private List<WfTransitionDefinition> buildTransitions(List<Transition> transitions, Locale locale) throws WorkflowException {
        if (transitions == null)
            return Collections.emptyList();

        List<WfTransitionDefinition> nodeTransitions = new ArrayList<WfTransitionDefinition>(transitions.size());
        for (Transition transition : transitions) {
            // get transition meta data
            WfMetaWrapper wfMetaWrapper = wfMetaManager.getWorkflowMeta(transition.getProcessDefinition());
            TransitionDocument.Transition transitionMeta = wfMetaWrapper.getTransitionMeta(transition);

            // build labels from meta data
            I18nMessage label = null;
            if (transitionMeta != null && transitionMeta.isSetLabel()) {
                label = wfMetaWrapper.getI18nMessage(transitionMeta.getLabel(), locale);
            }
            
            I18nMessage confirmation = null;
            if (transitionMeta != null && transitionMeta.isSetConfirmation()) {
                confirmation = wfMetaWrapper.getI18nMessage(transitionMeta.getConfirmation(), locale);
            }

            nodeTransitions.add(new WfTransitionDefinitionImpl(transition.getName(), label, confirmation));
        }
        return nodeTransitions;
    }

    /**
     * Returns a possibly cached copy of the process definition.
     */
    public WfProcessDefinition getProcessDefinition(ProcessDefinition process, Locale locale) throws WorkflowException {
        String cacheKey = getCacheKey("ProcessDefinition", process.getId(), locale);
        WfProcessDefinition processDef = (WfProcessDefinition)getFromCache(cacheKey);
        if (processDef == null) {
            synchronized (this) {
                if (processDef == null) {
                    processDef = buildProcessDefinition(process, null, locale);
                    putInCache(cacheKey, processDef);
                }
            }
        }
        return processDef;
    }

    public WfProcessDefinition buildProcessDefinition(ProcessDefinition definition, List<String> problems, Locale locale) throws WorkflowException {
        String id = String.valueOf(definition.getId());
        String name = definition.getName();
        String version = String.valueOf(definition.getVersion());

        Task startTask = definition.getTaskMgmtDefinition().getStartTask();
        WfTaskDefinition startTaskDefinition = null;
        if (startTask != null) {
            startTaskDefinition = getTaskDefinition(startTask, locale);
        }

        Map<String, WfTaskDefinition> wfTasks = new HashMap<String, WfTaskDefinition>();
        Collection<Task> tasks = findTasks(definition);
        for (Task task : tasks) {
            wfTasks.put(task.getName(), getTaskDefinition(task, locale));
        }

        I18nMessage label = null;
        I18nMessage description = null;

        WfMetaWrapper wfMetaWrapper = wfMetaManager.getWorkflowMeta(definition);
        WorkflowMetaDocument.WorkflowMeta workflowMeta = wfMetaWrapper.getRoot();

        // labels and descriptions
        if (workflowMeta.isSetLabel())
            label = wfMetaWrapper.getI18nMessage(workflowMeta.getLabel(), locale);
        if (workflowMeta.isSetDescription())
            description = wfMetaWrapper.getI18nMessage(workflowMeta.getDescription(), locale);

        WfNodeDefinition startNodeDefinition = getNodeDefinition(definition.getStartState(), locale);

        // Global variables
        List<WfVariableDefinition> globalVars = new ArrayList<WfVariableDefinition>();
        if (workflowMeta.isSetVariables()) {
            for (VariableDocument.Variable variableXml : workflowMeta.getVariables().getVariableList()) {
                if (variableXml.isSetScope() && VariableScope.fromString(variableXml.getScope().toString()) == VariableScope.GLOBAL) {
                    List<VariableDocument.Variable> resolveList = new ArrayList<VariableDocument.Variable>(1);
                    resolveList.add(variableXml);
                    globalVars.add(buildVariableDefinition(resolveList, wfMetaWrapper, locale));
                }
            }
        }

        return new WfProcessDefinitionImpl(id, name, version, problems, wfTasks, startTaskDefinition,
                startNodeDefinition, label, description, globalVars);
    }

    /**
     * Finds all tasks that are part of task nodes or the start state.
     * (and not global task definitions)
     */
    @SuppressWarnings("unchecked")
    private Collection<Task> findTasks(ProcessDefinition definition) {
        List<Task> result = new ArrayList<Task>();

        List<Node> nodes = definition.getNodes();
        for (Node node : nodes) {
            collectTasks(node, result);
        }

        Task startTask = definition.getTaskMgmtDefinition().getStartTask();
        if (startTask != null) {
            result.add(startTask);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private void collectTasks(Node node, List<Task> result) {
        if (node instanceof TaskNode) {
            Set tasks = ((TaskNode)node).getTasks();
            if (tasks != null)
                result.addAll(tasks);
        }

        if (node instanceof NodeCollection) {
            for (Node childNode : (List<Node>)((NodeCollection)node).getNodes()) {
                collectTasks(childNode, result);
            }
        }
    }

    public WfTask buildTask(TaskInstance taskInstance, Locale locale) throws WorkflowException {
        String taskId = String.valueOf(taskInstance.getId());
        ProcessInstance processInstance = taskInstance.getToken().getProcessInstance();
        String workflowId = String.valueOf(processInstance.getId());
        long actorId = taskInstance.getActorId() != null ? parseActorId(taskInstance.getActorId()) : -1;
        String executionPath = taskInstance.getToken().getFullName();
        WfTaskDefinition taskDefinition = getTaskDefinition(taskInstance.getTask(), locale);

        List<WfVariableDefinition> variableDefs = taskDefinition.getVariables();
        List<WfVariable> variables = new ArrayList<WfVariable>();
        for (WfVariableDefinition variableDef : variableDefs) {
            ContextInstance contextInstance = taskInstance.getContextInstance();
            Object value;
            switch (variableDef.getScope()) {
                case TASK:
                    value = taskInstance.getVariableLocally(variableDef.getName());
                    break;
                case GLOBAL:
                    value = contextInstance.getLocalVariable(variableDef.getName(), processInstance.getRootToken());
                    break;
                default:
                    throw new RuntimeException("Unrecognized variable scope: " + variableDef.getScope());
            }
            if (value != null)
                variables.add(new WfVariableImpl(variableDef.getName(), variableDef.getScope(), variableDef.getType(), value));
        }

        Set pooledActors = taskInstance.getPooledActors();
        boolean hasPool = pooledActors != null && !pooledActors.isEmpty();
        boolean hasSwimlane = taskInstance.getSwimlaneInstance() != null;

        return new WfTaskImpl(taskId, taskInstance.getCreate(), taskInstance.getEnd(),
                jbpmPriorityToDaisy(taskInstance.getPriority()), taskInstance.getDueDate(),
                actorId, hasPool, hasSwimlane, workflowId, executionPath, variables, taskDefinition, locale);
    }

    /**
     * Returns a possibly cached copy of the node definition.
     */
    public WfNodeDefinition getNodeDefinition(Node node, Locale locale) throws WorkflowException {
        String cacheKey = getCacheKey("NodeDefinition", node.getId(), locale);
        WfNodeDefinition nodeDef = (WfNodeDefinition)getFromCache(cacheKey);
        if (nodeDef == null) {
            synchronized (this) {
                if (nodeDef == null) {
                    nodeDef = buildNodeDefinition(node, locale);
                    putInCache(cacheKey, nodeDef);
                }
            }
        }
        return nodeDef;
    }

    private static Pattern EXTRACT_CLASSNAME_PATTERN = Pattern.compile("^.*\\.([^\\.\\$]+)(\\$.*)?$");

    private WfNodeDefinition buildNodeDefinition(Node node, Locale locale) throws WorkflowException {
        String name = node.getName();
        String nodePath = node.getFullyQualifiedName();
        String nodeType = node.getClass().getName();

        // Use (non-qualified) classname as node type
        Matcher matcher = EXTRACT_CLASSNAME_PATTERN.matcher(nodeType);
        if (matcher.matches()) // and otherwise leave to the fully qualified name
            nodeType = matcher.group(1);

        List<WfTransitionDefinition> leavingTransitions = buildTransitions(node.getLeavingTransitions(), locale);

        return new WfNodeDefinitionImpl(name, nodePath, nodeType, leavingTransitions,
                String.valueOf(node.getProcessDefinition().getId()), node.getProcessDefinition().getName());
    }

    /**
     * Returns a possibly cached copy of the task definition.
     */
    public WfTaskDefinition getTaskDefinition(Task task, Locale locale) throws WorkflowException {
        String cacheKey = getCacheKey("TaskDefinition", task.getId(), locale);
        WfTaskDefinition taskDef = (WfTaskDefinition)getFromCache(cacheKey);
        if (taskDef == null) {
            synchronized (this) {
                if (taskDef == null) {
                    taskDef = buildTaskDefinition(task, locale);
                    putInCache(cacheKey, taskDef);
                }
            }
        }
        return taskDef;
    }

    private WfTaskDefinition buildTaskDefinition(Task task, Locale locale) throws WorkflowException {
        Node node;
        if (task.getStartState() != null)
            node = task.getStartState();
        else if (task.getTaskNode() != null)
            node = task.getTaskNode();
        else
            throw new RuntimeException("Unexpected situation: encountered a task that doesn't belong to a task node or the start state.");
        WfNodeDefinition nodeDefinition = getNodeDefinition(node, locale);

        WfMetaWrapper wfMetaWrapper = wfMetaManager.getWorkflowMeta(task.getProcessDefinition());
        TaskDocument.Task taskMeta = wfMetaWrapper.getTaskMeta(task.getName());

        I18nMessage label = null;
        I18nMessage description = null;
        List<WfVariableDefinition> variables;
        if (taskMeta != null) {
            if (taskMeta.isSetLabel())
                label = wfMetaWrapper.getI18nMessage(taskMeta.getLabel(), locale);
            if (taskMeta.isSetDescription())
                description = wfMetaWrapper.getI18nMessage(taskMeta.getDescription(), locale);

            variables = buildVariables(taskMeta, wfMetaWrapper, locale);
        } else {
            variables = Collections.emptyList();
        }

        return new WfTaskDefinitionImpl(task.getName(), label, description, variables, nodeDefinition);
    }

    private List<WfVariableDefinition> buildVariables(TaskDocument.Task taskMeta, WfMetaWrapper wfMetaWrapper, Locale locale) throws WorkflowException {
        List<WfVariableDefinition> variableDefintions;

        if (taskMeta.isSetVariables()) {
            variableDefintions = new ArrayList<WfVariableDefinition>();
            for (VariableDocument.Variable variable : taskMeta.getVariables().getVariableList()) {
                WfVariableDefinition variableDef = buildTaskVariable(variable, wfMetaWrapper, taskMeta.getName(), locale);
                variableDefintions.add(variableDef);
            }
        } else {
            variableDefintions = Collections.emptyList();
        }

        return variableDefintions;
    }

    private WfVariableDefinition buildTaskVariable(VariableDocument.Variable variableXml, WfMetaWrapper wfMetaWrapper, String taskName, Locale locale) throws WorkflowException {
        // build a list of all the XML variable definitions used to build up this variable definition, as specified
        // by the 'base' properties.
        // Currently there is at most one level of extending possible, though this might be extended in the future.
        List<VariableDocument.Variable> variablesXml = new ArrayList<VariableDocument.Variable>(2);
        variablesXml.add(variableXml);

        String baseName = variableXml.getBase();
        VariableDocument.Variable globalVariableXml = null;
        if (baseName != null) {
            globalVariableXml = wfMetaWrapper.getGlobalVariable(baseName);
            if (globalVariableXml == null)
                throw new WorkflowException("Variable's base attribute doesn't refer to an existing global variable definition: " + baseName);
            variablesXml.add(globalVariableXml);
        }

        WfVariableDefinition def = buildVariableDefinition(variablesXml, wfMetaWrapper, locale);

        //
        // Some additional checks for assuring global variables are not changed structurally on the task-level
        //

        // Check global variables are declared globally
        if (def.getScope() == VariableScope.GLOBAL) {
            String ERROR_MSG = "Global-scoped variables should be declared in the global variables section of the workflow metadata [variable " + def.getName() +  " of task " + taskName + "]";
            if (globalVariableXml == null)
                throw new WorkflowException(ERROR_MSG);

            if (!globalVariableXml.isSetScope() || VariableScope.fromString(globalVariableXml.getScope().toString()) != VariableScope.GLOBAL) {
                throw new WorkflowException(ERROR_MSG);
            }
        }

        // Check selection lists for global variables should be declared globally, this is because the global
        // definitions are used to lookup labels in the query systems, and it would be confusing if this
        // would give different results.
        if (def.getScope() == VariableScope.GLOBAL) {
            if (variableXml.isSetSelectionList())
                throw new WorkflowException("Selection lists for global variables should be specified on the global variable declaration and not changed in tasks [variable " + def.getName() + " of task " + taskName + "]");
        }


        // Check global variables do not change type locally
        if (def.getScope() == VariableScope.GLOBAL) {
            if (!globalVariableXml.isSetType() || WfValueType.fromString(globalVariableXml.getType().toString()) != def.getType())
                throw new WorkflowException("The type of global-scoped variables can not be changed in tasks [variable " + def.getName() + " of task " + taskName + "]");
        }

        return def;
    }

    private WfVariableDefinition buildVariableDefinition(List<VariableDocument.Variable> variablesXml, WfMetaWrapper wfMetaWrapper, Locale locale) throws WorkflowException {
        // build the variable by resolving the information through the extends chain

        // Variable name
        String name = (String)resolve(variablesXml, new VariableInfoGetter() {
            public Object get(VariableDocument.Variable variableXml) {
                return variableXml.getName();
            }
        });
        if (name == null)
            throw new WorkflowException("A variable is missing its name property.");

        // scope
        String scopeName = (String)resolve(variablesXml, new VariableInfoGetter() {
            public Object get(VariableDocument.Variable variableXml) {
                return variableXml.isSetScope() ? variableXml.getScope().toString() : null;
            }
        });
        VariableScope scope = scopeName != null ? VariableScope.fromString(scopeName) : VariableScope.TASK;

        // Variable type
        String typeName = (String)resolve(variablesXml, new VariableInfoGetter() {
            public Object get(VariableDocument.Variable variableXml) {
                return variableXml.isSetType() ? variableXml.getType().toString() : null;
            }
        });
        if (typeName == null)
            throw new WorkflowException("Variable \"" + name + "\" is missing its type property.");
        WfValueType type = WfValueType.fromString(typeName);
        if (type == WfValueType.ID)
            throw new WorkflowException("Type \"" + type + "\" is not a valid type for variables.");

        // Variable label
        I18NType labelTag = (I18NType)resolve(variablesXml, new VariableInfoGetter() {
            public Object get(VariableDocument.Variable variableXml) {
                return variableXml.getLabel();
            }
        });
        I18nMessage label = labelTag != null ? wfMetaWrapper.getI18nMessage(labelTag, locale) : null;

        // Variable description
        I18NType descriptionTag = (I18NType)resolve(variablesXml, new VariableInfoGetter() {
            public Object get(VariableDocument.Variable variableXml) {
                return variableXml.getDescription();
            }
        });
        I18nMessage description = descriptionTag != null ? wfMetaWrapper.getI18nMessage(descriptionTag, locale) : null;

        // requiredness
        Boolean requiredObject = (Boolean)resolve(variablesXml, new VariableInfoGetter() {
            public Object get(VariableDocument.Variable variableXml) {
                return variableXml.isSetRequired() ? variableXml.getRequired() : null;
            }
        });
        boolean required = requiredObject != null ? requiredObject.booleanValue() : false;

        // readonly-ness
        Boolean readOnlyObject = (Boolean)resolve(variablesXml, new VariableInfoGetter() {
            public Object get(VariableDocument.Variable variableXml) {
                return variableXml.isSetReadOnly() ? variableXml.getReadOnly() : null;
            }
        });
        boolean readOnly = readOnlyObject != null ? readOnlyObject.booleanValue() : false;

        // selection list
        SelectionListDocument.SelectionList selectionListXml = (SelectionListDocument.SelectionList)resolve(variablesXml, new VariableInfoGetter() {
            public Object get(VariableDocument.Variable variableXml) {
                return variableXml.getSelectionList();
            }
        });
        List<WfListItem> selectionList = selectionListXml != null ? buildSelectionList(selectionListXml, type, wfMetaWrapper, locale) : null;

        // initial value script
        String initialValueScript = (String)resolve(variablesXml, new VariableInfoGetter() {
            public Object get(VariableDocument.Variable variableXml) {
                return variableXml.getInitialValueScript();
            }
        });

        // styling
        XmlObject styling = (XmlObject)resolve(variablesXml, new VariableInfoGetter() {
            public Object get(VariableDocument.Variable variableXml) {
                return variableXml.getStyling();
            }
        });

        // hidden
        Boolean hidden = (Boolean)resolve(variablesXml, new VariableInfoGetter() {
            public Object get(VariableDocument.Variable variableXml) {
                return variableXml.isSetHidden() ? variableXml.getHidden() : null;
            }
        });
        if (hidden == null)
            hidden = false;

        WfVariableDefinitionImpl variableDef = new WfVariableDefinitionImpl(name, type, readOnly, label, description,
                required, hidden, scope, selectionList, initialValueScript, styling);
        return variableDef;
    }

    private Object resolve(List<VariableDocument.Variable> variablesXml, VariableInfoGetter getter) {
        for (VariableDocument.Variable variableXml : variablesXml) {
            Object result = getter.get(variableXml);
            if (result != null)
                return result;
        }
        return null;
    }

    private interface VariableInfoGetter {
        Object get(VariableDocument.Variable variableXml);
    }

    private List<WfListItem> buildSelectionList(SelectionListDocument.SelectionList listXml, WfValueType ownerValueType,
            WfMetaWrapper wfMetaWrapper, Locale locale) throws WorkflowException {
        List<SelectionListDocument.SelectionList.ListItem> listItemsXml = listXml.getListItemList();
        List<WfListItem> items = new ArrayList<WfListItem>(listItemsXml.size());
        for (SelectionListDocument.SelectionList.ListItem listItemXml : listItemsXml) {
            WfXmlHelper.ValueData data = WfXmlHelper.getValue(listItemXml, "selection list item");
            if (data.type != ownerValueType)
                throw new WorkflowException("Selection list item contains a value of a type that does not correspond with the variable type. Expected " + ownerValueType + " but got " + data.type);
            I18nMessage label = listItemXml.isSetLabel() ? wfMetaWrapper.getI18nMessage(listItemXml.getLabel(), locale) : null;
            items.add(new WfListItemImpl(data.value, label));
        }
        return items;
    }

    public WfTimer buildTimer(org.jbpm.job.Timer timer, Locale locale) throws WorkflowException {
        return new WfTimerImpl(String.valueOf(timer.getId()), timer.getName(), timer.getDueDate(), timer.getRepeat(), timer.getException(),
                timer.isSuspended(), String.valueOf(timer.getProcessInstance().getId()), timer.getToken().getFullName(),
                timer.getTransitionName(), locale);
    }

    private TaskPriority jbpmPriorityToDaisy(int priority) {
        switch (priority) {
            case org.jbpm.taskmgmt.def.Task.PRIORITY_HIGHEST:
                return TaskPriority.HIGHEST;
            case org.jbpm.taskmgmt.def.Task.PRIORITY_HIGH:
                return TaskPriority.HIGH;
            case org.jbpm.taskmgmt.def.Task.PRIORITY_NORMAL:
                return TaskPriority.NORMAL;
            case org.jbpm.taskmgmt.def.Task.PRIORITY_LOW:
                return TaskPriority.LOW;
            case org.jbpm.taskmgmt.def.Task.PRIORITY_LOWEST:
                return TaskPriority.LOWEST;
            default:
                throw new RuntimeException("Invalid priority: " + priority);
        }
    }

    private long parseActorId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid user ID: " + id);
        }
    }

    private Object getFromCache(String cacheKey) {
        Object object = cache.get(cacheKey);
        if (log.isDebugEnabled()) {
            if (object == null) {
                log.debug("Workflow object cache miss for " + cacheKey);
            } else {
                log.debug("Worfklow object cache hit for " + cacheKey);
            }
        }
        return object;
    }

    private void putInCache(String cacheKey, Object object) {
        cache.put(cacheKey, object);
        if (log.isDebugEnabled())
            log.debug("Adding to workflow object cache: " + cacheKey);
    }

    private String getCacheKey(String typeName, long id, Locale locale) {
        return typeName + id + LocaleHelper.getString(locale);
    }

    public DResourceBundles getI18nBundle(ProcessDefinition processDefinition) throws WorkflowException {
        return wfMetaManager.getWorkflowMeta(processDefinition).getResourceBundles();
    }
}