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

import org.outerj.daisy.workflow.*;
import org.outerj.daisy.workflow.serverimpl.IntWfContext;
import org.outerj.daisy.workflow.QueryConditions.PropertyConditionInfo;
import org.outerj.daisy.workflow.QueryConditions.VariableConditionInfo;
import org.outerj.daisy.workflow.QueryConditions.SpecialConditionInfo;
import org.outerj.daisy.repository.RepositoryException;
import org.hibernate.Session;
import org.hibernate.Query;
import org.jbpm.context.exe.converter.BooleanToStringConverter;

import java.util.List;
import java.util.ArrayList;

public class QueryGenerator {
    private QueryConditions conditions;
    private Session session;
    private QueryMetadataRegistry registry;
    private Binder binder;
    private IntWfContext context;

    public static Query generateTaskQuery(QueryConditions conditions, Session session, IntWfContext context) throws WorkflowException {
        return new QueryGenerator(conditions, session, context).doTaskQuery();
    }

    public static Query generateProcessQuery(QueryConditions conditions, Session session, IntWfContext context) throws WorkflowException {
        return new QueryGenerator(conditions, session, context).doProcessQuery();
    }

    public static Query generateTimerQuery(QueryConditions conditions, Session session, IntWfContext context) throws WorkflowException {
        return new QueryGenerator(conditions, session, context).doTimerQuery();
    }

    private QueryGenerator(QueryConditions conditions, Session session, IntWfContext context) {
        this.conditions = conditions;
        this.session = session;
        this.registry = context.getQueryMetadataRegistry();
        this.context = context;
    }

    public Query doTaskQuery() throws WorkflowException {
        binder = new Binder();
        StringBuilder queryBuffer = new StringBuilder();
        queryBuffer.append("select distinct rti from TaskInstance rti where ");

        ConDisJunctor conDisJunctor = new ConDisJunctor(conditions.getMatchAllCriteria());

        // Add conditions checking on 'core' properties
        List<PropertyConditionInfo> propCondInfos = conditions.getPropertyConditions();
        for (PropertyConditionInfo info : propCondInfos) {
            conDisJunctor.generate(queryBuffer);
            generateTaskPropertyCondition(info, queryBuffer);
        }

        // Add conditions checking on task instance variables
        List<VariableConditionInfo> taskVarConds = conditions.getTaskVariableConditions();
        for (VariableConditionInfo info : taskVarConds) {
            conDisJunctor.generate(queryBuffer);
            generateTaskVariableCondition(info, queryBuffer);
        }

        // Add conditions checking on process instance variables
        List<VariableConditionInfo> processVarConds = conditions.getProcessVariableConditions();
        for (VariableConditionInfo info : processVarConds) {
            conDisJunctor.generate(queryBuffer);
            generateProcessVariableCondition("token.processInstance.id", info, queryBuffer);
        }

        for (QueryConditions.SpecialConditionInfo info : conditions.getSpecialConditions()) {
            conDisJunctor.generate(queryBuffer);
            generateSpecialTaskCondition(info, queryBuffer);
        }

        if (conDisJunctor.isFirst()) {
            // no conditions have been added, add a dummy condition
            queryBuffer.append(" 1=1 ");
        }

        String queryString = queryBuffer.toString();
        if (context.getLogger().isDebugEnabled())
            context.getLogger().debug("Generated task query: " + queryString);
        Query query = session.createQuery(queryString);

        binder.bind(query);

        return query;
    }

    public Query doProcessQuery() throws WorkflowException {
        binder = new Binder();
        StringBuilder queryBuffer = new StringBuilder();
        queryBuffer.append("select distinct rpi from ProcessInstance rpi where ");

        ConDisJunctor conDisJunctor = new ConDisJunctor(conditions.getMatchAllCriteria());

        // Add conditions checking on 'core' properties
        List<PropertyConditionInfo> propCondInfos = conditions.getPropertyConditions();
        for (PropertyConditionInfo info : propCondInfos) {
            conDisJunctor.generate(queryBuffer);
            generateProcessPropertyCondition(info, queryBuffer);
        }

        if (conditions.getTaskVariableConditions().size() > 0)
            throw new WorkflowException("A query on process instances cannot contain conditions on task variables.");

        // Add conditions checking on process instance variables
        List<VariableConditionInfo> processVarConds = conditions.getProcessVariableConditions();
        for (VariableConditionInfo info : processVarConds) {
            conDisJunctor.generate(queryBuffer);
            generateProcessVariableCondition("id", info, queryBuffer);
        }

        for (SpecialConditionInfo info : conditions.getSpecialConditions()) {
            conDisJunctor.generate(queryBuffer);
            generateSpecialProcessCondition(info, queryBuffer);
        }

        if (conDisJunctor.isFirst()) {
            // no conditions have been added, add a dummy condition
            queryBuffer.append(" 1=1 ");
        }

        String queryString = queryBuffer.toString();
        if (context.getLogger().isDebugEnabled())
            context.getLogger().debug("Generated process query: " + queryString);
        Query query = session.createQuery(queryString);

        binder.bind(query);

        return query;
    }

    public Query doTimerQuery() throws WorkflowException {
        binder = new Binder();
        StringBuilder queryBuffer = new StringBuilder();
        queryBuffer.append("select distinct tmr from Timer tmr where ");

        ConDisJunctor conDisJunctor = new ConDisJunctor(conditions.getMatchAllCriteria());

        // Add conditions checking on 'core' properties
        List<PropertyConditionInfo> propCondInfos = conditions.getPropertyConditions();
        for (PropertyConditionInfo info : propCondInfos) {
            conDisJunctor.generate(queryBuffer);
            generateTimerPropertyCondition(info, queryBuffer);
        }

        // Add conditions checking on process instance variables
        List<VariableConditionInfo> processVarConds = conditions.getProcessVariableConditions();
        for (VariableConditionInfo info : processVarConds) {
            conDisJunctor.generate(queryBuffer);
            generateProcessVariableCondition("processInstance.id", info, queryBuffer);
        }

        for (QueryConditions.SpecialConditionInfo info : conditions.getSpecialConditions()) {
            conDisJunctor.generate(queryBuffer);
            generateSpecialTimerCondition(info, queryBuffer);
        }

        if (conDisJunctor.isFirst()) {
            // no conditions have been added, add a dummy condition
            queryBuffer.append(" 1=1 ");
        }

        String queryString = queryBuffer.toString();
        if (context.getLogger().isDebugEnabled())
            context.getLogger().debug("Generated timer query: " + queryString);
        Query query = session.createQuery(queryString);

        binder.bind(query);

        return query;
    }

    private void generateTaskPropertyCondition(PropertyConditionInfo info, StringBuilder builder) throws WorkflowException {
        IntProperty prop = registry.getProperty(info.propertyName);
        checkSearchable(prop);
        IntOperator op = registry.getOperator(info.operatorName);

        performValueChecks(op, prop.getType(), info.values, prop.getName());

        switch (prop.getParent()) {
            case TASK:
                builder.append("rti.").append(prop.getHqlName());
                op.generateHql(builder, prop.getType(), info.values, binder);
                break;
            case PROCESS:
                builder.append("token.processInstance.id in (select id from ProcessInstance epi where epi.");
                builder.append(prop.getHqlName());
                op.generateHql(builder, prop.getType(), info.values, binder);
                builder.append(")");
                break;
            default:
                throw new RuntimeException("Unexpected situation, unhandled prop parent: " + prop.getParent());
        }
    }

    private void generateProcessPropertyCondition(PropertyConditionInfo info, StringBuilder builder) throws WorkflowException {
        IntProperty prop = registry.getProperty(info.propertyName);
        checkSearchable(prop);
        IntOperator op = registry.getOperator(info.operatorName);

        performValueChecks(op, prop.getType(), info.values, prop.getName());

        switch (prop.getParent()) {
            case TASK:
                throw new WorkflowException("A search on process instances cannot contain conditions on task properties.");
            case PROCESS:
                builder.append("rpi.").append(prop.getHqlName());
                op.generateHql(builder, prop.getType(), info.values, binder);
                break;
            default:
                throw new RuntimeException("Unexpected situation, unhandled prop parent: " + prop.getParent());
        }
    }

    private void generateTimerPropertyCondition(PropertyConditionInfo info, StringBuilder builder) throws WorkflowException {
        IntProperty prop = registry.getProperty(info.propertyName);
        checkSearchable(prop);
        IntOperator op = registry.getOperator(info.operatorName);

        performValueChecks(op, prop.getType(), info.values, prop.getName());

        switch (prop.getParent()) {
            case TASK:
                throw new WorkflowException("A search on timers cannot contain conditions on task properties.");
            case TIMER:
                builder.append("tmr.").append(prop.getHqlName());
                op.generateHql(builder, prop.getType(), info.values, binder);
                break;
            case PROCESS:
                builder.append("tmr.processInstance.id in (select id from ProcessInstance epi where epi.");
                builder.append(prop.getHqlName());
                op.generateHql(builder, prop.getType(), info.values, binder);
                builder.append(")");
                break;
            default:
                throw new RuntimeException("Unexpected situation, unhandled prop parent: " + prop.getParent());
        }
    }

    private void checkSearchable(IntProperty prop) throws WorkflowException {
        if (prop.getHqlName() == null)
            throw new WorkflowException("The property " + prop.getName() + " is not searchable.");
    }

    private void performValueChecks(IntOperator op, WfValueType type, List<Object> values, String name) throws WorkflowException {
        if (values == null)
            throw new WorkflowException("Missing values for workflow search condition.");

        if (values.size() != op.getArgumentCount())
            throw new WorkflowException("Number of values specified for workflow search condition does not match the number amount required by the selected operator. Expected " + op.getArgumentCount() + " but got " + values.size() + " (" + name + ")");

        if (!op.supportsType(type))
            throw new WorkflowException("Operator " + op.getName() + " cannot be used with type " + type + " (for search on " + name  + ")");

        for (Object value : values) {
            if (value == null)
                throw new WorkflowException("Null value supplied in workflow search condition values.");
            Class expectedClass = type.getTypeClass();
            if (!expectedClass.isAssignableFrom(value.getClass()))
                throw new WorkflowException("Incorrect type of value supplied in workflow search condition values. Expected a " + expectedClass.getName() + " but got a " + value.getClass().getName() + " (" + name + ")");
        }
    }

    private void generateTaskVariableCondition(VariableConditionInfo info, StringBuilder builder) throws WorkflowException {
        IntOperator op = registry.getOperator(info.operatorName);
        performValueChecks(op, info.type, info.values, info.name);
        String[] classAndConverter = getClassAndConvertor(info.type);

        // Note: selecting from the specific VariableInstance class (e.g. LongInstance instead of VariableInstance)
        // is necessary so that vi.value would resolve to the correct database column
        //builder.append("id in ( select ti.id from TaskInstance ti join ti.variableInstances tvi join ").append(classAndConverter[0]).append(" vi where ");
        builder.append("id in ( select ti.id from TaskInstance ti, ").append(classAndConverter[0]).append(" vi where ti.variableInstances.id = vi.id and ");
        //builder.append("where vi.id not in (select vis.id from TaskInstance ti join ti.variableInstances vis) and ");
        addVariableInstanceClause(info, op, classAndConverter, builder);
        builder.append(") ");
    }

    private void generateProcessVariableCondition(String processInstanceRef, VariableConditionInfo info, StringBuilder builder) throws WorkflowException {
        IntOperator op = registry.getOperator(info.operatorName);
        performValueChecks(op, info.type, info.values, info.name);
        String[] classAndConverter = getClassAndConvertor(info.type);

        builder.append(processInstanceRef);
        builder.append(" in (select processInstance.id from ").append(classAndConverter[0]).append(" vi ");
        // when searching on global process variables, exclude the task-associated variables
        builder.append("where vi.id not in (select vis.id from TaskInstance ti join ti.variableInstances vis) and ");
        addVariableInstanceClause(info, op, classAndConverter, builder);
        // Note: for now we only support searching on variables associated with the root token
        builder.append(" and vi.token.id = processInstance.rootToken.id");
        builder.append(")");
    }

    private void addVariableInstanceClause(VariableConditionInfo info, IntOperator op, String[] classAndConverter, StringBuilder builder) {
        builder.append(" vi.class = ").append(classAndConverter[0]).append("");
        builder.append(" and vi.converter ");
        if (classAndConverter[1] == null) {
            builder.append(" is null ");
        } else {
            builder.append(" ='").append(classAndConverter[1]).append("' ");
        }
        String bindName = binder.getUniqueBindName();
        builder.append(" and vi.name = :").append(bindName);
        binder.addBind(bindName, WfValueType.STRING, info.name);
        builder.append(" and vi.value ");

        WfValueType bindType;
        List<Object> bindValue;
        switch (info.type) {
            case BOOLEAN:
                bindType = WfValueType.STRING;
                bindValue = new ArrayList<Object>(info.values.size());
                for (Object value : info.values)
                    bindValue.add(new BooleanToStringConverter().convert(value));
                break;
            default:
                bindType = info.type;
                bindValue = info.values;
        }
        op.generateHql(builder, bindType, bindValue, binder);
    }

    private String[] getClassAndConvertor(WfValueType type) throws WorkflowException {
        String className;
        String converter;
        switch (type) {
            case STRING:
                className = "StringInstance";
                converter = null;
                break;
            case ACTOR:
                className = "StringInstance";
                converter = "P";
                break;
            case LONG:
                className = "LongInstance";
                converter = null;
                break;
            case DATE:
            case DATETIME:
                className = "DateInstance";
                converter = null;
                break;
            case BOOLEAN:
                className = "StringInstance";
                converter = "B";
                break;
            case DAISY_LINK:
                className = "StringInstance";
                converter = "Z";
                break;
            case USER:
                className = "StringInstance";
                converter = "Q";
                break;
            default:
                throw new WorkflowException("Unsupported type for searching on task variables: " + type);
        }

        return new String[] {className, converter};
    }

    private void generateSpecialTaskCondition(SpecialConditionInfo info, StringBuilder builder) throws WorkflowException {
        if (info.name.equals("tasksInMyPool")) {
            WfPoolManager poolManager = context.getPoolManager();
            List<WfPool> pools;
            try {
                pools = poolManager.getPoolsForUser(context.getRepository().getUserId());
            } catch (RepositoryException e) {
                throw new WorkflowException("Error retrieving the workflow pools for a user.", e);
            }
            List<String> actorIds = new ArrayList<String>(pools.size());
            for (WfPool pool : pools) {
                actorIds.add(String.valueOf(pool.getId()));
                // this is in the case someone chooses to use the poolname instead of the poolid.
                // eg. in the processdefinition the name of a pool could be used instead of an ID
                // for a task/swimlane assignment
                actorIds.add(pool.getName());
            }

            if (actorIds.size() > 0) {
                String bindName = binder.getUniqueBindName();
                builder.append(" pooledActors.actorId in ( :").append(bindName).append(" )");
                binder.addBindList(bindName, actorIds);
            } else {
                // actor is in no pool, so there will be no pooled tasks for him, so generate an always-false condition
                builder.append(" 1 != 1 ");
            }
        } else {
            throw new WorkflowException("Invalid special condition (for task-search): " + info.name);
        }
    }

    private void generateSpecialProcessCondition(SpecialConditionInfo info, StringBuilder builder) throws WorkflowException {
        if (info.name.equals("relatedToDocument")) {
            generateRelatedToDocumentCondition(info, builder, "id");
        } else {
            throw new WorkflowException("Invalid special condition (for process-search): " + info.name);
        }
    }

    private void generateSpecialTimerCondition(SpecialConditionInfo info, StringBuilder builder) throws WorkflowException {
        if (info.name.equals("processRelatedToDocument")) {
            generateRelatedToDocumentCondition(info, builder, "tmr.processInstance.id");
        } else {
            throw new WorkflowException("Invalid special condition (for process-search): " + info.name);
        }
    }

    private void generateRelatedToDocumentCondition(SpecialConditionInfo info, StringBuilder builder, String processIdRef) throws WorkflowException {
        if (info.argTypes.size() != 1 || info.argTypes.get(0) != WfValueType.DAISY_LINK)
            throw new WorkflowException("Incorrect arguments for special condition " + info.name);

        String bindName = binder.getUniqueBindName();

        builder.append(" ").append(processIdRef);
        builder.append(" in (select processInstance.id from StringInstance si where si.class = StringInstance and si.converter = 'Z' and si.value like ");
        builder.append(":").append(bindName).append(" and processInstance.id is not null) ");

        binder.addBind(bindName, WfValueType.DAISY_LINK, info.argValues.get(0));
    }

    private static class ConDisJunctor {
        private boolean first = true;
        private boolean matchAllCriteria;

        public ConDisJunctor(boolean matchAllCriteria) {
            this.matchAllCriteria = matchAllCriteria;
        }

        public void generate(StringBuilder buffer) {
            if (first) {
                first = false;
                return;
            }

            buffer.append(matchAllCriteria ? " and " : " or ");
        }

        public boolean isFirst() {
            return first;
        }
    }

}
