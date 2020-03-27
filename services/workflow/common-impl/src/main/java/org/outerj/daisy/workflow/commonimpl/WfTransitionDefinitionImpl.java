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

import org.outerj.daisy.workflow.WfTransitionDefinition;
import org.outerj.daisy.i18n.I18nMessage;
import org.outerj.daisy.i18n.impl.StringI18nMessage;
import org.outerx.daisy.x10Workflow.TransitionDefinitionDocument;

public class WfTransitionDefinitionImpl implements WfTransitionDefinition {
    private String name;
    private I18nMessage label;
    private I18nMessage confirmation;

    public WfTransitionDefinitionImpl(String name, I18nMessage label, I18nMessage confirmation) {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");

        this.name = name;
        this.label = label;
        this.confirmation = confirmation;
    }

    public String getName() {
        return name;
    }

    public I18nMessage getLabel() {
        return label == null ? new StringI18nMessage(getName()) : label;
    }

    public I18nMessage getConfirmation() {
        return confirmation;
    }

    public TransitionDefinitionDocument getXml() {
        TransitionDefinitionDocument doc = TransitionDefinitionDocument.Factory.newInstance();
        TransitionDefinitionDocument.TransitionDefinition xml = doc.addNewTransitionDefinition();
        xml.setName(name);
        xml.addNewLabel().set(label != null ? WfXmlHelper.i18nMessageToXml(label) : WfXmlHelper.stringToXml(name));
        if (confirmation != null) {
            xml.addNewConfirmation().set(WfXmlHelper.i18nMessageToXml(confirmation));
        }
        return doc;
    }
}
