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

import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.WidgetState;
import org.apache.commons.collections.MapUtils;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.schema.PartTypeUse;

    
public class HtmlPartEditor extends AbstractPartEditor {
    
    private String definitionTemplate;
    private String definitionStylesheet;
    private String formTemplate;
    
    private HtmlPartEditor(PartTypeUse partTypeUse, Map<String, String> properties, DocumentEditorContext documentEditorContext) {
        super(partTypeUse, properties, documentEditorContext);
        
        this.definitionTemplate = MapUtils.getString(properties, "definitionTemplate", "wikidata:/resources/form/parteditor_daisyhtml_definition.xml"); 
        this.definitionStylesheet = MapUtils.getString(properties, "definitionStylesheet", null); 
        this.formTemplate = MapUtils.getString(properties, "formTemplate", "wikidata:/resources/form/parteditor_daisyhtml_template.xml"); 
    }

    public static class Factory implements PartEditorFactory {
        public PartEditor getPartEditor(PartTypeUse partTypeUse, Map<String, String> properties, DocumentEditorContext documentEditorContext) {
            return new HtmlPartEditor(partTypeUse, properties, documentEditorContext);
        }
    }
    
    public String getDefinitionTemplate() {
        return definitionTemplate;
    }

    public String getDefinitionStylesheet() {
        return definitionStylesheet;
    }

    public String getFormTemplate() {
        return formTemplate;
    }

    @Override
    public void init(Widget parentWidget, boolean readonly) {
        super.init(parentWidget, readonly);
        if (readonly) {
            widget.setState(WidgetState.DISABLED);
        }
        ValidationCondition validateOnSave = new ValidateOnSaveCondition(documentEditorContext);
        widget.lookupWidget("part").addValidator(new ConditionalValidator(validateOnSave, new PartRequiredValidator(partTypeUse.isRequired(), true)));
        widget.lookupWidget("part").setAttribute("EditorMode", "htmlarea");
    }

    public void load(Document document) throws Exception {
        PartEditorHelper.load(widget.lookupWidget("part"), partTypeUse.getPartType(), document);
    }

    public void save(Document document) throws Exception {
        PartEditorHelper.save(widget.lookupWidget("part"), partTypeUse.getPartType(), document, "text/xml", null);
    }

}