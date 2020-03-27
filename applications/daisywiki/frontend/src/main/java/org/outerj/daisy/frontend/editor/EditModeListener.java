/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.frontend.editor;

import org.apache.cocoon.forms.event.ValueChangedListener;
import org.apache.cocoon.forms.event.ValueChangedEvent;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Widget;

/**
 * Listener shared between forms that have a union to allow them to switch
 * between XML and GUI editing modes.
 */
public class EditModeListener implements ValueChangedListener {
    private EditModeListenerConfig config;

    public EditModeListener(EditModeListenerConfig config) {
        this.config = config;
    }

    public void valueChanged(ValueChangedEvent valueChangedEvent) {
        String editMode = (String)valueChangedEvent.getNewValue();
        String oldEditMode = (String)valueChangedEvent.getOldValue();
        if (oldEditMode == null)
            return;

        Widget groupWidget = valueChangedEvent.getSourceWidget().getParent();

        if (groupWidget.getAttribute("ignore-editmode-change") != null) {
            groupWidget.removeAttribute("ignore-editmode-change");
            return;

        }

        Widget widget = groupWidget.lookupWidget(oldEditMode.equals("xml") ? config.getXmlFieldPath() : config.getGuiFieldPath());
        boolean valid = widget.validate();
        if (!valid) {
            // stay in same mode
            widget.getForm().setAttribute("ignore-editmode-change", Boolean.TRUE);
            valueChangedEvent.getSourceWidget().setValue(oldEditMode);
        } else {
            if (editMode.equals("xml")) {
                String xml = config.getXmlFromGuiEditor();
                groupWidget.lookupWidget(config.getXmlFieldPath()).setValue(xml);
            } else if (editMode.equals("gui")) {
                String value = (String)groupWidget.lookupWidget(config.getXmlFieldPath()).getValue();
                if (value != null) {
                    if (!config.loadGui(value)) {
                        // Stay in xml mode
                        groupWidget.setAttribute("ignore-editmode-change", Boolean.TRUE);
                        groupWidget.lookupWidget("editmode").setValue("xml");
                    }
                } else {
                    config.clearGui();
                }
            }
        }
    }

    static interface EditModeListenerConfig {
        public String getXmlFieldPath();
        public String getGuiFieldPath();
        public String getXmlFromGuiEditor();
        public boolean loadGui(String xml);
        public void clearGui();
    }
}
