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
package org.outerj.daisy.workflow.serverimpl.query;

import org.outerj.daisy.workflow.WfValueType;
import org.outerj.daisy.workflow.WorkflowException;
import org.outerj.daisy.workflow.WfUserKey;
import org.outerj.daisy.i18n.impl.StringI18nMessage;
import org.outerj.daisy.workflow.serverimpl.query.IntProperty.PropertyParent;

import java.util.*;

/**
 * Registry for the workflow query system containing information about properties and query operators.
 */
public class QueryMetadataRegistry {
    private Map<String, IntProperty> props = new HashMap<String, IntProperty>();
    private Map<String, IntOperator> operators = new HashMap<String, IntOperator>();
    private Map<WfValueType, List<IntOperator>> supportedOperators = new HashMap<WfValueType, List<IntOperator>>();

    public void init() {
        // Task properties
        addProp(new IntProperty("task.id", WfValueType.ID, PropertyParent.TASK, "id",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return String.valueOf(provider.getTaskInstance().getId());
                    }
                }));
        addProp(new IntProperty("task.actor", WfValueType.USER, PropertyParent.TASK, "actorId",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        String actorId = provider.getTaskInstance().getActorId();
                        if (actorId != null)
                            return new WfUserKey(Long.parseLong(provider.getTaskInstance().getActorId()));
                        else
                            return null;
                    }
                }));
        addProp(new IntProperty("task.hasPools", WfValueType.BOOLEAN, PropertyParent.TASK, null,
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        Set pooledActors = provider.getTaskInstance().getPooledActors();
                        return pooledActors != null && !pooledActors.isEmpty();
                    }
                }));
        addProp(new IntProperty("task.hasSwimlane", WfValueType.BOOLEAN, PropertyParent.TASK, "swimlane",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return provider.getTaskInstance().getSwimlaneInstance() != null;
                    }
                }));
        addProp(new IntProperty("task.description", WfValueType.STRING, PropertyParent.TASK, "description",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return provider.getTaskInstance().getDescription();
                    }
                }));
        addProp(new IntProperty("task.create", WfValueType.DATETIME, PropertyParent.TASK, "create",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return provider.getTaskInstance().getCreate();
                    }
                }));
        addProp(new IntProperty("task.dueDate", WfValueType.DATETIME, PropertyParent.TASK, "dueDate",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return provider.getTaskInstance().getDueDate();
                    }
                }));
        addProp(new IntProperty("task.isOpen", WfValueType.BOOLEAN, PropertyParent.TASK, "isOpen",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return provider.getTaskInstance().isOpen() ? Boolean.TRUE : Boolean.FALSE;
                    }
                }));
        addProp(new IntProperty("task.end", WfValueType.DATETIME, PropertyParent.TASK, "end",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return provider.getTaskInstance().getEnd();
                    }
                }));
        addProp(new IntProperty("task.priority", WfValueType.LONG, PropertyParent.TASK, "priority",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return new Long(provider.getTaskInstance().getPriority());
                    }

                    public Object getLabel(Provider provider, Object value, Locale locale) {
                        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/workflow/serverimpl/messages", locale);
                        return new StringI18nMessage(bundle.getString("priority." + value));
                    }
                }));
        addProp(new IntProperty("task.definitionLabel", null, PropertyParent.TASK, null,
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return provider.getTaskDefinition().getLabel();
                    }
                }));
        addProp(new IntProperty("task.definitionDescription", null, PropertyParent.TASK, null,
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return provider.getTaskDefinition().getDescription();
                    }
                }));

        // Process properties
        addProp(new IntProperty("process.id", WfValueType.ID, PropertyParent.PROCESS, "id",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return String.valueOf(provider.getProcessInstance().getId());
                    }
                }));
        addProp(new IntProperty("process.start", WfValueType.DATETIME, PropertyParent.PROCESS, "start",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return provider.getProcessInstance().getStart();
                    }
                }));
        addProp(new IntProperty("process.end", WfValueType.DATETIME, PropertyParent.PROCESS, "end",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return provider.getProcessInstance().getEnd();
                    }
                }));
        addProp(new IntProperty("process.suspended", WfValueType.BOOLEAN, PropertyParent.PROCESS, "isSuspended",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return provider.getProcessInstance().isSuspended() ? Boolean.TRUE : Boolean.FALSE;
                    }
                }));
        addProp(new IntProperty("process.definitionName", WfValueType.STRING, PropertyParent.PROCESS, "processDefinition.name",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return provider.getProcessInstance().getProcessDefinition().getName();
                    }
                }));
        addProp(new IntProperty("process.definitionVersion", WfValueType.LONG, PropertyParent.PROCESS, "processDefinition.version",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return new Long(provider.getProcessInstance().getProcessDefinition().getVersion());
                    }
                }));
        addProp(new IntProperty("process.definitionLabel", null, PropertyParent.PROCESS, null,
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return provider.getProcessDefinition().getLabel();
                    }
                }));
        addProp(new IntProperty("process.definitionDescription", null, PropertyParent.PROCESS, null,
                new BaseValueGetter() {
                    public Object getValue(Provider provider) throws WorkflowException {
                        return provider.getProcessDefinition().getDescription();
                    }
                }));

        // Timer properties
        addProp(new IntProperty("timer.id", WfValueType.ID, PropertyParent.TIMER, "id",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return String.valueOf(provider.getTimer().getId());
                    }
                }));
        addProp(new IntProperty("timer.name", WfValueType.STRING, PropertyParent.TIMER, "name",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return provider.getTimer().getName();
                    }
                }));
        addProp(new IntProperty("timer.dueDate", WfValueType.DATETIME, PropertyParent.TIMER, "dueDate",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return provider.getTimer().getDueDate();
                    }
                }));
        addProp(new IntProperty("timer.recurrence", WfValueType.STRING, PropertyParent.TIMER, "repeat",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return provider.getTimer().getRepeat();
                    }
                }));
        addProp(new IntProperty("timer.suspended", WfValueType.BOOLEAN, PropertyParent.TIMER, "isSuspended",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return provider.getTimer().isSuspended();
                    }
                }));
        addProp(new IntProperty("timer.exception", WfValueType.STRING, PropertyParent.TIMER, "exception",
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return provider.getTimer().getException();
                    }
                }));
        addProp(new IntProperty("timer.failed", WfValueType.BOOLEAN, PropertyParent.TIMER, null,
                new BaseValueGetter() {
                    public Object getValue(Provider provider) {
                        return provider.getTimer().getException() != null;
                    }
                }));

        // Operators
        addOperator(new IntOperator.EqOperator());
        addOperator(new IntOperator.LtOperator());
        addOperator(new IntOperator.GtOperator());
        addOperator(new IntOperator.LtEqOperator());
        addOperator(new IntOperator.GtEqOperator());
        addOperator(new IntOperator.IsNullOperator());
        addOperator(new IntOperator.IsNotNullOperator());
        addOperator(new IntOperator.LikeOperator());
        addOperator(new IntOperator.BetweenOperator());
    }

    private void addProp(IntProperty prop) {
        props.put(prop.getName(), prop);
    }

    private void addOperator(IntOperator op, WfValueType... types) {
        operators.put(op.getName(), op);
        for (WfValueType type : types) {
            addSupportedOperator(type, op);
        }
    }

    private void addSupportedOperator(WfValueType type, IntOperator op) {
        List<IntOperator> ops = supportedOperators.get(type);
        if (ops == null) {
            ops = new ArrayList<IntOperator>();
            supportedOperators.put(type, ops);
        }
        ops.add(op);
    }

    public IntProperty getProperty(String name) throws WorkflowException {
        IntProperty prop = props.get(name);
        if (prop == null)
            throw new WorkflowException("There is no property called " + name);
        return prop;
    }

    public IntOperator getOperator(String name) throws WorkflowException {
        IntOperator op = operators.get(name);
        if (op == null)
            throw new WorkflowException("There is no operator called " + name);
        return op;
    }

    void buildQueryMetadata(Locale locale) {
    }
}
