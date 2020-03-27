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
package org.outerj.daisy.frontend.editor;

import java.util.ArrayList;
import java.util.List;

import org.apache.cocoon.xml.SaxBuffer;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldTypeUse;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class InlineFormConfig {
    
    private List<FieldTypeUse> fields = new ArrayList<FieldTypeUse>();
    private List<PartTypeUse> parts = new ArrayList<PartTypeUse>();
    private boolean editDocumentName = false;
    
    public static InlineFormConfig createInlineFormConfig(SaxBuffer styledDocument, final DocumentType documentType) throws SAXException {
        final InlineFormConfig config = new InlineFormConfig();
        
        DefaultHandler contentHandler = new DefaultHandler() {
            @Override
            public void startElement(String uri, String localName, String name,
                    Attributes attributes) throws SAXException {
                for (int i=0; i<attributes.getLength(); i++) {
                    if (attributes.getURI(i).equals(InlineEditorApple.INLINEEDITOR_NAMESPACE) ){
                        if (attributes.getLocalName(i).equals("field")) {
                            FieldTypeUse fieldTypeUse = documentType.getFieldTypeUse(attributes.getValue(i));
                            if (fieldTypeUse != null) {
                                config.addField(fieldTypeUse);
                            }
                        } else if (attributes.getLocalName(i).equals("part")) {
                            PartTypeUse partTypeUse = documentType.getPartTypeUse(attributes.getValue(i));
                            if (partTypeUse != null) {
                                config.addPart(partTypeUse);
                            }
                        } else if (attributes.getLocalName(i).equals("name")) {
                            config.setEditDocumentName(true);
                        }
                    }
                }
            }
        };
        styledDocument.toSAX(contentHandler);
        
        return config;
    }
    
    public boolean isEditDocumentName() {
        return editDocumentName;
    }
    public void setEditDocumentName(boolean editDocumentName) {
        this.editDocumentName = editDocumentName;
    }
    public List<FieldTypeUse> getFields() {
        return fields;
    }
    public List<PartTypeUse> getParts() {
        return parts;
    }
    public void addField(FieldTypeUse field) {
        this.fields.add(field);
    }
    public void addPart(PartTypeUse part) {
        this.parts.add(part);
    }

}
