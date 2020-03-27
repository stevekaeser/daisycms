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

import org.outerj.daisy.workflow.WfVariableDefinition;
import org.outerj.daisy.workflow.WfValueType;
import org.outerj.daisy.workflow.VariableScope;
import org.outerj.daisy.workflow.WfListItem;
import org.outerj.daisy.i18n.I18nMessage;
import org.outerj.daisy.i18n.impl.StringI18nMessage;
import org.outerx.daisy.x10Workflow.VariableDefinitionDocument;
import org.outerx.daisy.x10Workflow.ScopeType;
import org.outerx.daisy.x10Workflow.DataType;
import org.outerx.daisy.x10Workflow.SelectionListDocument;
import org.apache.xmlbeans.XmlObject;

import java.util.List;
import java.util.Collections;


public class WfVariableDefinitionImpl implements WfVariableDefinition {
    private String name;
    private WfValueType type;
    private boolean readOnly;
    private I18nMessage label;
    private I18nMessage description;
    private boolean required;
    private boolean hidden;
    private VariableScope scope;
    private List<WfListItem> selectionList;
    private String initialValueScript;
    private Object compiledInitialValueScript;
    private XmlObject styling;

    public WfVariableDefinitionImpl(String name, WfValueType type, boolean readOnly, I18nMessage label,
            I18nMessage description, boolean required, boolean hidden, VariableScope scope,
            List<WfListItem> selectionList, String initialValueScript, XmlObject styling) {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        if (type == null)
            throw new IllegalArgumentException("Null argument: type");
        if (scope == null)
            throw new IllegalArgumentException("Null argument: scope");

        this.name = name;
        this.type = type;
        this.readOnly = readOnly;
        this.label = label;
        this.description = description;
        this.required = required;
        this.hidden = hidden;
        this.scope = scope;
        this.selectionList = selectionList != null ? Collections.unmodifiableList(selectionList) : null;
        this.initialValueScript = initialValueScript;
        this.styling = styling;
    }

    public String getName() {
        return name;
    }

    public WfValueType getType() {
        return type;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public I18nMessage getLabel() {
        return label != null ? label : new StringI18nMessage(getName());
    }

    public I18nMessage getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isHidden() {
        return hidden;
    }

    public VariableScope getScope() {
        return scope;
    }

    public List<WfListItem> getSelectionList() {
        return selectionList;
    }

    public String getInitialValueScript() {
        return initialValueScript;
    }

    public Object getCompiledInitialValueScript() {
        return compiledInitialValueScript;
    }

    public void setCompiledInitialValueScript(Object compiledScript) {
        this.compiledInitialValueScript = compiledScript;
    }

    public XmlObject getStyling() {
        return styling;
    }

    public VariableDefinitionDocument getXml() {
        VariableDefinitionDocument doc = VariableDefinitionDocument.Factory.newInstance();
        VariableDefinitionDocument.VariableDefinition xml = doc.addNewVariableDefinition();

        xml.setName(name);
        xml.setReadOnly(readOnly);
        xml.setRequired(required);
        xml.setHidden(hidden);
        xml.setScope(ScopeType.Enum.forString(scope.toString()));
        xml.setType(DataType.Enum.forString(type.toString()));

        xml.addNewLabel().set(label != null ? WfXmlHelper.i18nMessageToXml(label) : WfXmlHelper.stringToXml(name));
        if (description != null)
            xml.addNewDescription().set(WfXmlHelper.i18nMessageToXml(description));

        if (selectionList != null) {
            SelectionListDocument.SelectionList selectionListXml = xml.addNewSelectionList();
            for (WfListItem listItem : selectionList) {
                SelectionListDocument.SelectionList.ListItem listItemXml = selectionListXml.addNewListItem();
                WfXmlHelper.setValue(listItemXml, type, listItem.getValue());
                if (listItem.getLabel() != null)
                    listItemXml.addNewLabel().set(WfXmlHelper.i18nMessageToXml(listItem.getLabel()));
            }
        }

        // Please note: initialValueScript is not communicated via XML on purpose, it is
        // unneeded and potentially dangerous to expose this.

        if (styling != null) {
            xml.addNewStyling().set(styling);
        }

        return doc;
    }
}
