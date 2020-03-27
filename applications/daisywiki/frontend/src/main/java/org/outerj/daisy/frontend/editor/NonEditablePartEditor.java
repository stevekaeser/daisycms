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

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.schema.PartTypeUse;

public class NonEditablePartEditor extends AbstractPartEditor {

    public NonEditablePartEditor(PartTypeUse partTypeUse,
            Map<String, String> properties,
            DocumentEditorContext documentEditorContext) {
        super(partTypeUse, properties, documentEditorContext);
    }
    
    public static class Factory implements PartEditorFactory {

        public PartEditor getPartEditor(PartTypeUse partTypeUse,
                Map<String, String> properties,
                DocumentEditorContext documentEditorContext) {
            return new NonEditablePartEditor(partTypeUse, properties, documentEditorContext);
        }
        
    }

    @Override
    protected String getDefinitionStylesheet() {
        return null;
    }

    @Override
    protected String getDefinitionTemplate() {
        return "wikidata:/resources/form/parteditor_noedit_definition.xml";
    }

    public String getFormTemplate() {
        return "wikidata:/resources/form/parteditor_noedit_template.xml";
    }

    public void load(Document document) throws Exception {
        // TODO Auto-generated method stub
        
    }

    public void save(Document document) throws Exception {
        // TODO Auto-generated method stub
        
    }

}
