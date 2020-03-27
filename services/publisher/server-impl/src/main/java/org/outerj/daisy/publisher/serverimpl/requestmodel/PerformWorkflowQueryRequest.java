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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import java.util.ArrayList;
import java.util.List;

import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerj.daisy.workflow.QueryConditions;
import org.outerj.daisy.workflow.QueryOrderByItem;
import org.outerj.daisy.workflow.QuerySelectItem;
import org.outerj.daisy.workflow.WfValueType;
import org.outerj.daisy.workflow.WfVersionKey;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerx.daisy.x10Workflow.SearchResultDocument;
import org.xml.sax.ContentHandler;


public class PerformWorkflowQueryRequest extends AbstractRequest {
    private List<QuerySelectItem> querySelectItems;
    private WfQueryConditions wfQueryConditions;
    private List<QueryOrderByItem> orderByItems;
    private QuerySubject subject;
    
    public PerformWorkflowQueryRequest (List<QuerySelectItem> querySelectItems, WfQueryConditions queryConditions, List<QueryOrderByItem> orderByItems, QuerySubject subject, LocationInfo locationInfo) {
        super(locationInfo);
        this.querySelectItems = querySelectItems;
        this.wfQueryConditions = queryConditions;
        this.orderByItems = orderByItems;
        this.locationInfo = locationInfo;
        this.subject = subject;
    }

    @Override
    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        WorkflowManager workflowManager = (WorkflowManager)publisherContext.getRepository().getExtension("WorkflowManager");
        SearchResultDocument result;
        QueryConditions queryConditions = new QueryConditions();
        queryConditions.setMeetAllCriteria(this.wfQueryConditions.isMeetAllRequirements());
               
        // check the conditions for expressions that need evaluating.
        for (Condition cond : wfQueryConditions.getPropertyConditions()) {            
            Condition parsedCondition = parseExpressions(cond, publisherContext);
            queryConditions.addCondition(parsedCondition.name, parsedCondition.type, parsedCondition.operatorName, parsedCondition.values.toArray(new Object[parsedCondition.values.size()]));
        }
        for (Condition cond : wfQueryConditions.getProcessVariableConditions()) {
            Condition parsedCondition = parseExpressions(cond, publisherContext);
            queryConditions.addProcessVariableCondition(parsedCondition.name, parsedCondition.type, parsedCondition.operatorName, parsedCondition.values.toArray(new Object[parsedCondition.values.size()]));
        }
        for (Condition cond : wfQueryConditions.getTaskVariableConditions()) {
            Condition parsedCondition = parseExpressions(cond, publisherContext);
            queryConditions.addTaskVariableCondition(parsedCondition.name, parsedCondition.type, parsedCondition.operatorName, parsedCondition.values.toArray(new Object[parsedCondition.values.size()]));
        }
        for (SpecialCondition cond : wfQueryConditions.getSpecialConditions()) {
            Object[] condValues = new Object[cond.values.size()];
            for (int i = 0; i < cond.types.size(); i++) {
                if (cond.types.get(i) == WfValueType.DAISY_LINK) {
                    Object value = cond.values.get(i);
                    // check just in case
                    if (value instanceof DaisyLinkExpression) {
                        DaisyLinkExpression daisyLinkExpression = (DaisyLinkExpression)value;
                        condValues[i] = daisyLinkExpression.toWfVersionKey(publisherContext, this);
                    } else {
                        condValues[i] = value;
                    }
                } else {
                    condValues[i] = cond.values.get(i);
                }
            }
            queryConditions.addSpecialCondition(cond.name, cond.types.toArray(new WfValueType[cond.types.size()]), condValues);
        }
        
        if (this.subject.equals(QuerySubject.PROCESS)) {
            result = workflowManager.searchProcesses(querySelectItems, queryConditions, orderByItems, -1, -1, publisherContext.getLocale());    
        } else if (this.subject.equals(QuerySubject.TASK)) {
            result = workflowManager.searchTasks(querySelectItems, queryConditions, orderByItems, -1, -1, publisherContext.getLocale());
        } else if (this.subject.equals(QuerySubject.TIMER)) {
            result = workflowManager.searchTimers(querySelectItems, queryConditions, orderByItems, -1, -1, publisherContext.getLocale());
        } else {
            throw new PublisherException ("Unknown query subject " + this.subject.toString());
        }
        
        result.save(new StripDocumentHandler(contentHandler), new DummyLexicalHandler());
    }
    
    private Condition parseExpressions (Condition cond, PublisherContext publisherContext) throws Exception {
        List<Object> values = new ArrayList<Object>(cond.values.size());
        values.addAll(cond.values);
        Condition newCond = new Condition(
                new String(cond.name), 
                cond.type, 
                new String(cond.operatorName),
                values);
        
        if (newCond.type == WfValueType.DAISY_LINK) {
            for (int i = 0; i < newCond.values.size(); i++) {
                Object value = newCond.values.get(i);
                // check just in case
                if (value instanceof DaisyLinkExpression) {
                    DaisyLinkExpression daisyLinkExpression = (DaisyLinkExpression)value;
                    newCond.values.remove(value);
                    newCond.values.add(i, daisyLinkExpression.toWfVersionKey(publisherContext, this));
                }                    
            }
        }
        return newCond;
    }
    
    public enum QuerySubject {
        PROCESS,
        TASK,
        TIMER,
    }
    
    protected static class DaisyLinkExpression {
        private PubReqExpr documentIdExpr;
        private PubReqExpr branchIdExpr;
        private PubReqExpr languageIdExpr;
        private PubReqExpr versionExpr;
        
        public DaisyLinkExpression(PubReqExpr documentId, PubReqExpr branchId, PubReqExpr languageId, PubReqExpr version) {
            this.documentIdExpr = documentId;
            this.branchIdExpr = branchId;
            this.languageIdExpr = languageId;
            this.versionExpr = version;                
        }
        
        public WfVersionKey toWfVersionKey (PublisherContext publisherContext, Request request) throws Exception{
            String documentId = this.documentIdExpr.evalAsString(publisherContext, request);
            long branchId = this.branchIdExpr != null ? this.branchIdExpr.evalAsBranchId(publisherContext, request) : 0;
            long languageId = this.languageIdExpr != null ? this.languageIdExpr.evalAsLanguageId(publisherContext, request) : 0;
            String version = this.versionExpr != null ? this.versionExpr.evalAsString(publisherContext, request) : null;
            
            return new WfVersionKey(documentId, branchId, languageId, version);
        }
    
    }
    
    protected static class WfQueryConditions {
        
        private boolean meetAllCriteria = false;
        private List<Condition> propertyConditions = new ArrayList<Condition>();
        private List<Condition> taskVariableConditions = new ArrayList<Condition>();
        private List<Condition> processVariableConditions = new ArrayList<Condition>();
        private List<SpecialCondition> specialConditions = new ArrayList<SpecialCondition>();
        
        public void addPropertyCondition(String name, WfValueType type, String operatorName, List<Object> values) {
            this.propertyConditions.add(new Condition(name, type, operatorName, values));
        }
        public void addTaskVariableCondition(String name, WfValueType type, String operatorName, List<Object> values) {
            this.taskVariableConditions.add(new Condition(name, type, operatorName, values));
        }
        public void addProcessVariableCondition(String name, WfValueType type, String operatorName, List<Object> values) {
            this.processVariableConditions.add(new Condition(name, type, operatorName, values));
        }
        public void addSpecialCondition(String name, List<WfValueType> types, List<Object> values) {
            this.specialConditions.add(new SpecialCondition(name, types, values));
        }
        
        
        public boolean isMeetAllRequirements() {
            return meetAllCriteria;
        }
        public void setMeetAllCriteria(boolean meetAllCriteria) {
            this.meetAllCriteria = meetAllCriteria;
        }
        public boolean isMeetAllCriteria() {
            return meetAllCriteria;
        }
        public List<Condition> getPropertyConditions() {
            return propertyConditions;
        }
        public void setPropertyConditions(List<Condition> propertyConditions) {
            this.propertyConditions = propertyConditions;
        }
        public List<Condition> getTaskVariableConditions() {
            return taskVariableConditions;
        }
        public void setTaskVariableConditions(List<Condition> taskVariableConditions) {
            this.taskVariableConditions = taskVariableConditions;
        }
        public List<Condition> getProcessVariableConditions() {
            return processVariableConditions;
        }
        public void setProcessVariableConditions(List<Condition> processVariableConditions) {
            this.processVariableConditions = processVariableConditions;
        }
        public List<SpecialCondition> getSpecialConditions() {
            return specialConditions;
        }
        public void setSpecialConditions(List<SpecialCondition> specialConditions) {
            this.specialConditions = specialConditions;
        }
        
        
    
    }
    
    protected static class Condition {
        private String name;
        private WfValueType type;
        private String operatorName;
        private List<Object>values;
        
        public Condition (String name, WfValueType type, String operatorName, List<Object> values) {
            this.name = name;
            this.type = type;
            this.operatorName = operatorName;
            this.values = values;
        }
    }
    protected static class SpecialCondition {
        private String name;
        private List<WfValueType> types;
        private List<Object> values;
        
        public SpecialCondition (String name, List<WfValueType> types, List<Object> values) {
            this.name = name;
            this.types = types;
            this.values = values;
        }
    
    }

}
