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
package org.outerj.daisy.workflow.commonimpl;

import org.outerj.daisy.workflow.*;
import org.outerj.daisy.workflow.TaskUpdateData.VariableKey;
import org.outerj.daisy.workflow.TaskUpdateData.VariableValue;
import org.outerj.daisy.i18n.I18nMessage;
import org.outerx.daisy.x10Workflow.*;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlSaxHandler;
import org.apache.xmlbeans.XmlString;

import java.util.*;

public class WfXmlHelper {
    public static WfVariable instantiateVariable(VariableDocument.Variable xml) {
        VariableScope scope = VariableScope.fromString(xml.getScope().toString());
        ValueData data = getValue(xml, "variable");
        return new WfVariableImpl(xml.getName(), scope, data.type, data.value);
    }

    public static ValueData getValue(VariableValuesType xml, String what) {
        WfValueType type;
        Object value;
        if (xml.isSetString()) {
            type = WfValueType.STRING;
            value = xml.getString();
        } else if (xml.isSetLong()) {
            type = WfValueType.LONG;
            value = xml.getLong();
        } else if (xml.isSetDate()) {
            type = WfValueType.DATE;
            value = xml.getDate().getTime();
        } else if (xml.isSetDateTime()) {
            type = WfValueType.DATETIME;
            value = xml.getDateTime().getTime();
        } else if (xml.isSetDaisyLink()) {
            type = WfValueType.DAISY_LINK;
            value = instantiateVersionKey(xml.getDaisyLink());
        } else if (xml.isSetActor()) {
            type = WfValueType.ACTOR;
            VariableDocument.Variable.Actor actor = xml.getActor();
            if (actor.getPool()) {
                if (actor.isSetId2()) {
                    List<Long> poolIds = new ArrayList<Long>(1);
                    poolIds.add(actor.getId2());
                    value = new WfActorKey(poolIds);
                } else {
                    List<Long> poolIds = actor.getIdList();
                    List<Long> poolIdsList = new ArrayList<Long>(poolIds.size());
                    for (long poolId : poolIds) {
                        poolIdsList.add(poolId);
                    }
                    value = new WfActorKey(poolIdsList);
                }
            } else {
                value = new WfActorKey(actor.getId2());
            }
        } else if (xml.isSetBoolean()) {
            type = WfValueType.BOOLEAN;
            value = xml.getBoolean() ? Boolean.TRUE : Boolean.FALSE;
        } else if (xml.isSetUser()) {
            type = WfValueType.USER;
            value = new WfUserKey(xml.getUser());
        } else if (xml.isSetId()) {
            type = WfValueType.ID;
            value = xml.getId();
        } else {
            throw new RuntimeException("Missing tag for the value in XML representation of " + what);
        }
        ValueData data = new ValueData();
        data.value = value;
        data.type = type;
        return data;
    }

    public static class ValueData {
        public Object value;
        public WfValueType type;
    }

    public static WfVersionKey instantiateVersionKey(VariableDocument.Variable.DaisyLink xml) {
        return new WfVersionKey(xml.getDocumentId(), xml.getBranchId(), xml.getLanguageId(), xml.getVersion());
    }

    public static void setValue(VariableValuesType xml, WfValueType type, Object value) {
        switch (type) {
            case STRING:
                xml.setString((String)value);
                break;
            case LONG:
                xml.setLong((Long)value);
                break;
            case DATE:
                xml.setDate(getCalendar((Date)value));
                break;
            case DATETIME:
                xml.setDateTime(getCalendar((Date)value));
                break;
            case DAISY_LINK:
                WfVersionKey versionKey = (WfVersionKey)value;
                VariableDocument.Variable.DaisyLink daisyLink = xml.addNewDaisyLink();
                daisyLink.setDocumentId(versionKey.getDocumentId());
                daisyLink.setBranchId(versionKey.getBranchId());
                daisyLink.setLanguageId(versionKey.getLanguageId());
                if (versionKey.getVersion() != null)
                    daisyLink.setVersion(versionKey.getVersion());
                break;
            case ACTOR:
                WfActorKey actorKey = (WfActorKey)value;
                VariableDocument.Variable.Actor actorXml = xml.addNewActor();
                if (actorKey.isPool()) {
                    actorXml.setPool(true);
                    List<Long> poolIdsList = actorKey.getPoolIds();
                    long[] poolIds = new long[poolIdsList.size()];
                    for (int i = 0; i < poolIds.length; i++)
                        poolIds[i] = poolIdsList.get(i);
                    actorXml.setIdArray(poolIds);
                } else {
                    actorXml.setPool(false);
                    actorXml.setId2(actorKey.getUserId());
                }
                break;
            case BOOLEAN:
                xml.setBoolean((Boolean)value);
                break;
            case USER:
                xml.setUser(((WfUserKey)value).getId());
                break;
            case ID:
                xml.setId((String)value);
                break;
            default:
                throw new RuntimeException("Unexpected value type: " + type);
        }
    }

    public static Calendar getCalendar(Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }

    public static TaskUpdateData instantiateTaskUpdateData(TaskUpdateDataDocument.TaskUpdateData xml) {
        TaskUpdateData taskUpdateData = new TaskUpdateData();

        for (VariableDocument.Variable variableXml : xml.getVariableList()) {
            WfVariable variable = WfXmlHelper.instantiateVariable(variableXml);
            taskUpdateData.setVariable(variable.getName(), variable.getScope(), variable.getType(), variable.getValue());
        }

        for (TaskUpdateDataDocument.TaskUpdateData.DeletedVariable deletedVarXml : xml.getDeletedVariableList()) {
            VariableScope scope = VariableScope.fromString(deletedVarXml.getScope().toString());
            taskUpdateData.deleteVariable(deletedVarXml.getName(), scope);
        }

        TaskPriority taskPriority = xml.isSetPriority() ? TaskPriority.fromString(xml.getPriority().toString()) : null;
        taskUpdateData.setPriority(taskPriority);

        if (xml.isSetDueDate()) {
            if (xml.getDueDate().getClear())
                taskUpdateData.clearDueDate();
            else
                taskUpdateData.setDueDate(xml.getDueDate().getCalendarValue().getTime());
        }

        return taskUpdateData;
    }

    public static TaskUpdateDataDocument getTaskUpdateXml(TaskUpdateData taskUpdateData) {
        TaskUpdateDataDocument document = TaskUpdateDataDocument.Factory.newInstance();
        TaskUpdateDataDocument.TaskUpdateData xml = document.addNewTaskUpdateData();

        for (Map.Entry<VariableKey, VariableValue> entry : taskUpdateData.getVariables().entrySet()) {
            VariableDocument.Variable variableXml = xml.addNewVariable();
            variableXml.setName(entry.getKey().getName());
            variableXml.setScope(ScopeType.Enum.forString(entry.getKey().getScope().toString()));
            WfValueType type = entry.getValue().getType();
            Object value = entry.getValue().getValue();
            setValue(variableXml, type, value);
        }

        for (VariableKey deletedVar : taskUpdateData.getDeletedVariables()) {
            TaskUpdateDataDocument.TaskUpdateData.DeletedVariable deletedVarXml = xml.addNewDeletedVariable();
            deletedVarXml.setName(deletedVar.getName());
            deletedVarXml.setScope(ScopeType.Enum.forString(deletedVar.getScope().toString()));
        }

        if (taskUpdateData.getDueDate() != null) {
            xml.addNewDueDate().setCalendarValue(getCalendar(taskUpdateData.getDueDate()));
        } else if (taskUpdateData.getClearDueDate()) {
            xml.addNewDueDate().setClear(true);
        }
        if (taskUpdateData.getPriority() != null)
            xml.setPriority(PriorityType.Enum.forString(taskUpdateData.getPriority().toString()));

        return document;
    }

    public static void addAttribute(XmlObject xmlObject, String name, String value) {
        XmlCursor cursor = xmlObject.newCursor();
        cursor.toNextToken();
        cursor.insertAttributeWithValue(name, value);
        cursor.dispose();
    }

    public static XmlObject i18nMessageToXml(I18nMessage msg) {
        try {
            XmlSaxHandler saxHandler = XmlObject.Factory.newXmlSaxHandler();
            saxHandler.getContentHandler().startDocument();
            msg.generateSaxFragment(saxHandler.getContentHandler());
            saxHandler.getContentHandler().endDocument();
            return saxHandler.getObject();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception converting I18nMessage to XmlObject.", e);
        }
    }

    public static XmlString stringToXml(String msg) {
        XmlString msgXml = XmlString.Factory.newInstance();
        msgXml.setStringValue(msg);
        return msgXml;
    }

    public static VariablesDocument getVariablesAsXml(List<WfVariable> variables) {
        VariablesDocument variablesDoc = VariablesDocument.Factory.newInstance();
        VariablesDocument.Variables variablesXml = variablesDoc.addNewVariables();

        for (WfVariable variable : variables) {
            VariableDocument.Variable variableXml = variablesXml.addNewVariable();
            variableXml.setName(variable.getName());
            variableXml.setScope(ScopeType.Enum.forString(variable.getScope().toString()));
            setValue(variableXml, variable.getType(), variable.getValue());
        }

        return variablesDoc;
    }
}
