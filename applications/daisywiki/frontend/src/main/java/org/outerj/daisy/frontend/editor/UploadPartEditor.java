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

import org.apache.cocoon.forms.FormsConstants;
import org.apache.cocoon.forms.event.ValueChangedEvent;
import org.apache.cocoon.forms.event.ValueChangedListener;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Upload;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.WidgetState;
import org.apache.cocoon.forms.util.I18nMessage;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.validation.WidgetValidator;
import org.outerj.daisy.frontend.DaisyException;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.frontend.components.multipart.OnlyDisposeOnGCPart;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.schema.PartTypeUse;

public class UploadPartEditor extends AbstractPartEditor {
    
    public UploadPartEditor(PartTypeUse partTypeUse,
            Map<String, String> properties,
            DocumentEditorContext documentEditorContext) {
        super(partTypeUse, properties, documentEditorContext);
    }
    
    public static class Factory implements PartEditorFactory {

        public PartEditor getPartEditor(PartTypeUse partTypeUse,
                Map<String, String> properties,
                DocumentEditorContext documentEditorContext) {
            return new UploadPartEditor(partTypeUse, properties, documentEditorContext);
        }
        
    }
    
    public void load(Document document) throws Exception {
        Upload upload = (Upload)widget.lookupWidget("upload-part");
        long partTypeId = partTypeUse.getPartType().getId();
        Part part = document.getPart(partTypeId);
        upload.setValue(new ExistingPart(part));
        widget.lookupWidget("upload-part-filename").setValue(part.getFileName());
        widget.lookupWidget("upload-part-mimetype").setValue(part.getMimeType());
    }

    public void save(Document document) throws Exception {
        Upload upload = (Upload)widget.lookupWidget("upload-part");
        long partTypeId = partTypeUse.getPartType().getId();
        
        org.apache.cocoon.servlet.multipart.Part part = (org.apache.cocoon.servlet.multipart.Part)upload.getValue();
        if (part != null) {
            Field mimeTypeField = (Field)widget.lookupWidget("upload-part-mimetype");
            Field fileNameField = (Field)widget.lookupWidget("upload-part-filename");
            if (!(part instanceof ExistingPart)) {
                if (part.getSize() < 0)
                    throw new DaisyException("Uploaded part has a negative size: " + part.getSize());
                document.setPart(partTypeId, (String)mimeTypeField.getValue(), new UploadPartDataSource(part));
            } else {
                document.setPartMimeType(partTypeId, (String)mimeTypeField.getValue());
            }
            document.setPartFileName(partTypeId, (String)fileNameField.getValue());
        } else {
            document.deletePart(partTypeId);
        }
    }

    class UploadValidator implements WidgetValidator {

        public boolean validate(Widget widget) {
            boolean success = true;
            Upload upload = (Upload)widget.lookupWidget("upload-part");
            Field partMimeType = (Field)widget.lookupWidget("upload-part-mimetype");
            if (documentEditorContext.isValidateOnSave() && partTypeUse.isRequired()) {
                if (upload.getValue() == null) {
                    upload.setValidationError(new ValidationError("editdoc.part-required", true));
                    success = false;
                }
            }
            if (upload.getValue() != null && partMimeType.getValue() == null) {
                partMimeType.setValidationError(new ValidationError(new I18nMessage("general.field-required", FormsConstants.I18N_CATALOGUE)));
                success = false;
            } else if (upload.getValue() != null && !partTypeUse.getPartType().mimeTypeAllowed((String)partMimeType.getValue())) {
                // this check could easily be implemented in the cform definition.
                partMimeType.setValidationError(new ValidationError(new I18nMessage("editdoc.mime-type-not-allowed", new String[] { partTypeUse.getPartType().getMimeTypes() })));
                success = false;
            }
            return success;
        }
    }

    @Override
    protected String getDefinitionStylesheet() {
        return null;
    }

    @Override
    protected String getDefinitionTemplate() {
        return "wikidata:/resources/form/parteditor_upload_definition.xml";
    }

    public String getFormTemplate() {
        return "wikidata:/resources/form/parteditor_upload_template.xml";
    }

    @Override
    public void init(Widget parentWidget, boolean readonly) {
        super.init(parentWidget, readonly);
        if (readonly) {
            widget.setState(WidgetState.DISABLED);
        }
        widget.addValidator(new UploadValidator());

        Upload upload = (Upload)widget.lookupWidget("upload-part");
        upload.addValueChangedListener(new ValueChangedListener() {
            public void valueChanged(ValueChangedEvent valueChangedEvent) {
                Upload widget = (Upload)valueChangedEvent.getSourceWidget();
                org.apache.cocoon.servlet.multipart.Part part = (org.apache.cocoon.servlet.multipart.Part)widget.getValue();
                
                // prevent inifinite recursion (setValue - valueChanged - setValue - valueChanged - ...)
                if (part instanceof OnlyDisposeOnGCPart) {
                    return;
                }

                if (part != null) {
                    widget.lookupWidget("../upload-part-mimetype").setValue(part.getMimeType());
                    widget.lookupWidget("../upload-part-filename").setValue(RequestUtil.removePathFromUploadFileName(part.getUploadName()));

                    widget.lookupWidget("../last-upload-part").setAttribute("lastModified", System.currentTimeMillis());

                    // this makes sure the part is not disposed by the upload-part widget.
                    OnlyDisposeOnGCPart wrappedPart = new OnlyDisposeOnGCPart(part);
                    widget.lookupWidget("../last-upload-part").setValue(wrappedPart);
                    widget.lookupWidget("../upload-part").setValue(wrappedPart);
                }
                
            }
        });
    }

    
}
