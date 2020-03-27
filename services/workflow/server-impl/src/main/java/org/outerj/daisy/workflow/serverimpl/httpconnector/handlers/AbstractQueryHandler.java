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
package org.outerj.daisy.workflow.serverimpl.httpconnector.handlers;

import org.outerj.daisy.workflow.*;
import org.outerj.daisy.workflow.commonimpl.WfXmlHelper;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.outerx.daisy.x10Workflow.QueryDocument;
import org.outerx.daisy.x10Workflow.Condition;
import org.outerx.daisy.x10Workflow.VariableValuesType;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;

public abstract class AbstractQueryHandler extends AbstractWorkflowRequestHandler {
    abstract XmlObject performQuery(WorkflowManager workflowManager, List<QuerySelectItem> selectItems,
            QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength,
            Locale locale) throws RepositoryException;

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        Locale locale = WfHttpUtil.getLocale(request);

        if (request.getMethod().equals(HttpConstants.POST)) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            QueryDocument queryDoc = QueryDocument.Factory.parse(request.getInputStream(), xmlOptions);
            QueryDocument.Query query = queryDoc.getQuery();

            // Read select clause if any
            List<QuerySelectItem> selectItems = null;
            if (query.isSetSelectClause()) {
                selectItems = new ArrayList<QuerySelectItem>();
                QueryDocument.Query.SelectClause selectClause = query.getSelectClause();
                for (QueryDocument.Query.SelectClause.Select selectXml : selectClause.getSelectList()) {
                    selectItems.add(new QuerySelectItem(selectXml.getName(), QueryValueSource.fromString(selectXml.getType())));
                }
            }

            // Read the conditions
            QueryConditions queryConditions = new QueryConditions();
            if (query.isSetConditions()) {
                QueryDocument.Query.Conditions conditionsXml = query.getConditions();
                if (conditionsXml.isSetMeetAllCriteria())
                    queryConditions.setMeetAllCriteria(conditionsXml.getMeetAllCriteria());

                // Read process variable conditions
                for (Condition cond : conditionsXml.getProcessVariableConditionList()) {
                    WfValueType type = WfValueType.fromString(cond.getValueType());
                    Object[] values = getVariableConditionValues(cond, type);
                    queryConditions.addProcessVariableCondition(cond.getName(), type, cond.getOperator(), values);
                }

                // Read task variable conditions
                for (Condition cond : conditionsXml.getTaskVariableConditionList()) {
                    WfValueType type = WfValueType.fromString(cond.getValueType());
                    Object[] values = getVariableConditionValues(cond, type);
                    queryConditions.addTaskVariableCondition(cond.getName(), type, cond.getOperator(), values);
                }

                // Read property conditions
                for (Condition cond : conditionsXml.getPropertyConditionList()) {
                    WfValueType type = WfValueType.fromString(cond.getValueType());
                    Object[] values = getVariableConditionValues(cond, type);
                    queryConditions.addCondition(cond.getName(), type, cond.getOperator(), values);
                }

                // Read special conditions
                for (QueryDocument.Query.Conditions.SpecialCondition specialCond : conditionsXml.getSpecialConditionList()) {
                    specialCond.getName();

                    List<VariableValuesType> valuesXml = specialCond.getValueList();
                    List<Object> values = new ArrayList<Object>(valuesXml.size());
                    List<WfValueType> types = new ArrayList<WfValueType>(valuesXml.size());

                    for (VariableValuesType valueXml : valuesXml) {
                        WfXmlHelper.ValueData valueData = WfXmlHelper.getValue(valueXml, "");
                        values.add(valueData.value);
                        types.add(valueData.type);
                    }

                    queryConditions.addSpecialCondition(specialCond.getName(), types.toArray(new WfValueType[0]), values.toArray());
                }
            }

            // Read the order by clause
            List<QueryOrderByItem> orderByItems = new ArrayList<QueryOrderByItem>();
            if (query.isSetOrderByClause()) {
                for (QueryDocument.Query.OrderByClause.OrderBy orderBy : query.getOrderByClause().getOrderByList()) {
                    String name = orderBy.getName();
                    QueryValueSource source = QueryValueSource.fromString(orderBy.getType());
                    SortOrder sortOrder = SortOrder.fromString(orderBy.getSortOrder());
                    orderByItems.add(new QueryOrderByItem(name, source, sortOrder));
                }
            }

            int chunkOffset = query.isSetChunkOffset() ? query.getChunkOffset() : -1;
            int chunkLength = query.isSetChunkLength() ? query.getChunkLength() : -1;

            XmlObject result = performQuery(workflowManager, selectItems, queryConditions, orderByItems, chunkOffset, chunkLength, locale);
            xmlOptions = new XmlOptions().setSaveSuggestedPrefixes(Constants.SUGGESTED_NAMESPACE_PREFIXES);
            result.save(response.getOutputStream(), xmlOptions);
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    private Object[] getVariableConditionValues(Condition cond, WfValueType type) throws WorkflowException {
        List<Object> values = new ArrayList<Object>(2);
        for (VariableValuesType value : cond.getValueList()) {
            WfXmlHelper.ValueData result = WfXmlHelper.getValue(value, "query condition value");
            if (result.type != type)
                throw new WorkflowException("Specified value does not correspond to type defined in valueType attribute for condition on " + cond.getName());
            values.add(result.value);
        }
        return values.toArray();
    }
}
