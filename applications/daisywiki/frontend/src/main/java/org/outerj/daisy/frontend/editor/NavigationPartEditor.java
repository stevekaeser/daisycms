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

import java.util.Map;

import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Widget;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.schema.PartTypeUse;

public class NavigationPartEditor extends AbstractPartEditor {
    
    public NavigationPartEditor(PartTypeUse partTypeUse, Map properties,
            DocumentEditorContext documentEditorContext) {
        super(partTypeUse, properties, documentEditorContext);
    }

    public static class Factory implements PartEditorFactory {
        public PartEditor getPartEditor(PartTypeUse partTypeUse, Map properties, DocumentEditorContext documentEditorContext) {
            return new NavigationPartEditor(partTypeUse, properties, documentEditorContext);
        }
    }

    @Override
    protected String getDefinitionStylesheet() {
        return null;
    }

    @Override
    protected String getDefinitionTemplate() {
        return "wikidata:/resources/form/parteditor_navigation_definition.xml";
    }

    public String getFormTemplate() {
        return "wikidata:/resources/form/parteditor_navigation_template.xml";
    }

    public void init(Widget parentWidget, boolean readonly) {
        super.init(parentWidget, readonly);
        ValidationCondition validateOnSave = new ValidateOnSaveCondition(documentEditorContext);
        widget.lookupWidget("navigation").addValidator(new ConditionalValidator(validateOnSave, new PartRequiredValidator(partTypeUse.isRequired(), true)));
    }

    public void load(Document document) throws Exception {
        PartEditorHelper.load(widget.lookupWidget("navigation"), partTypeUse.getPartType(), document);
    }

    public void save(Document document) throws Exception {
        PartEditorHelper.save(widget.lookupWidget("navigation"), partTypeUse.getPartType(), document, "text/xml", null);
    }

}
