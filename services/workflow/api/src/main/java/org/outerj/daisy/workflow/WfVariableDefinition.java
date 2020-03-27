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
package org.outerj.daisy.workflow;

import org.outerx.daisy.x10Workflow.VariableDefinitionDocument;
import org.outerj.daisy.i18n.I18nMessage;
import org.apache.xmlbeans.XmlObject;

import java.util.List;

public interface WfVariableDefinition {
    String getName();

    WfValueType getType();

    /**
     * Returns true if the variable is only meant for consultation/information,
     * but cannot be updated.
     */
    boolean isReadOnly();

    I18nMessage getLabel();

    I18nMessage getDescription();

    boolean isRequired();

    /**
     * A hidden variable should not be shown to the user during task interaction,
     * but is meant to be updated programmatically.
     */
    boolean isHidden();

    VariableScope getScope();

    /**
     * Returns null if no selection list.
     */
    List<WfListItem> getSelectionList();

    /**
     * Gets the styling information. This can be any sort of XML, and is therefore
     * returned as a generic XmlObject tag.
     *
     * Returns null if there is no styling information.
     */
    XmlObject getStyling();

    VariableDefinitionDocument getXml();
}
