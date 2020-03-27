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

import java.util.Locale;

import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.validation.ValidationError;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.xml.sax.ContentHandler;

/**
 * Interface to be implemented by part editors.
 *
 * <p>An PartEditor instance is instantiated through a {@link PartEditorFactory}.
 */
public interface PartEditor {
    /**
     * Generate a piece of CForm definition XML, to be embedded in a larger form definition.
     * This should not generate start and end document events.
     *
     * <p>The generated form definition XML is inserted within a container widget with
     * a unique name, so the part editor does not have to bother about producing
     * a unique name.
     *
     * <p>The produced form definition fragment should be 'stable', that is, always
     * produce the same result for the same PartTypeUse, and not depend on factors
     * such as 'time of the day'. This is because the form definitions are cached
     * and shared between multiple for instances and users.
     * @param serviceManager TODO
     */
    void generateFormDefinitionFragment(ContentHandler contentHandler, Locale locale, String displayMode, ServiceManager serviceManager) throws Exception;

    /**
     * Returns the PartTypeUse for which this PartEditor serves.
     */
    PartTypeUse getPartTypeUse();

    /**
     * This method is called to set the validation error in case this part
     * is required but does not have a value yet.
     *
     * <p>Note that the requiredness-validation only happens after the individual
     * part editors have been successfully validated, so this will not conflict
     * with any validation the part editor does itself.
     */
    void setValidationError(ValidationError error);

    /**
     * Returns true if this part has a value (used for part-requiredness validation).
     */
    boolean hasValue(Widget widget);

    /**
     * Allows to perform initialisation of the part editor.
     *
     * @param parentWidget the widget within which the form definition fragment
     *                     produced by {@link #generateFormDefinitionFragment}
     *                     is inserted. So if you generated a widget with
     *                     ID "foo", it can be accessed using parentWidget.getChild("foo").
     * @param readonly TODO
     */
    void init(Widget parentWidget, boolean readonly);

    /**
     * Return the name of a template file to be used for displaying this parteditor.
     * 
     * @return
     */
    String getFormTemplate();
    
    /**
     * Loads the part value from the document. This method is only called
     * if the document actually has a value for the part, so it is safe
     * to assume document.getPart() returns non-null when used for the
     * part type for which this editor serves.
     */
    void load(Document document) throws Exception;

    /**
     * Stores the part value to the document. If the part has
     * no value but it is present in the document, it is the
     * responsibility of the part editor implementation to delete the field
     * from the document.
     */
    void save(Document document) throws Exception;
}
