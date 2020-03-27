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
package org.outerj.daisy.frontend.admin;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Action;
import org.apache.cocoon.forms.binding.Binding;
import org.apache.cocoon.forms.event.ActionListener;
import org.apache.cocoon.forms.event.ActionEvent;
import org.apache.cocoon.forms.FormContext;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.activity.Disposable;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.Repository;

import java.util.*;
import java.text.Collator;

public class DocumentTypeEditorApple extends AbstractDaisyApple implements Serviceable, Disposable {
    private ServiceManager serviceManager;
    private boolean init = false;
    private Form form;
    private Binding binding;
    private Map<String, Object> viewDataTemplate;
    private Locale locale;
    /** The DocumentType we're currently editing. */
    private DocumentType documentType;
    private RepositorySchema repositorySchema;

    private AdminLocales adminLocales;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        adminLocales = (AdminLocales)serviceManager.lookup(AdminLocales.ROLE);
    }

    public void dispose() {
        serviceManager.release(adminLocales);
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            locale = frontEndContext.getLocale();

            form = FormHelper.createForm(serviceManager, "resources/form/documenttype_definition.xml");
            binding = FormHelper.createBinding(serviceManager, "resources/form/documenttype_binding.xml");

            // initialiaze repeater for displaydata with the locales we allow to be edited
            Repeater displayDataRepeater = (Repeater)form.getChild("displaydata");
            displayDataRepeater.setAttribute("adminLocales", adminLocales.getLocales().getAsObjects());

            PartType[] partTypes;
            FieldType[] fieldTypes;

            Repository repository = frontEndContext.getRepository();
            repositorySchema = repository.getRepositorySchema();
            partTypes = repositorySchema.getAllPartTypes(false).getArray();
            Arrays.sort(partTypes, PART_TYPE_COMPARATOR);
            fieldTypes = repositorySchema.getAllFieldTypes(false).getArray();
            Arrays.sort(fieldTypes, FIELD_TYPE_COMPARATOR);

            final Field availablePartTypes = (Field)form.getChild("availablePartTypes");
            availablePartTypes.setSelectionList(partTypes, "id", "name");
            final Field availableFieldTypes = (Field)form.getChild("availableFieldTypes");
            availableFieldTypes.setSelectionList(fieldTypes, "id", "name");

            Action addPartType = (Action)form.getChild("addPartType");
            addPartType.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    try {
                        Long partTypeId = (Long)availablePartTypes.getValue();
                        PartType partType = repositorySchema.getPartTypeById(partTypeId.longValue(), false);
                        Repeater.RepeaterRow row = ((Repeater)form.getChild("partTypes")).addRow();
                        row.getChild("id").setValue(partTypeId);
                        row.getChild("name").setValue(partType.getName());
                        row.getChild("required").setValue(Boolean.TRUE);
                        row.getChild("editable").setValue(Boolean.TRUE);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            Action addFieldType = (Action)form.getChild("addFieldType");
            addFieldType.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    try {
                        Long fieldTypeId = (Long)availableFieldTypes.getValue();
                        FieldType fieldType = repositorySchema.getFieldTypeById(fieldTypeId.longValue(), false);
                        Repeater.RepeaterRow row = ((Repeater)form.getChild("fieldTypes")).addRow();
                        row.getChild("id").setValue(fieldTypeId);
                        row.getChild("name").setValue(fieldType.getName());
                        row.getChild("required").setValue(Boolean.TRUE);
                        row.getChild("editable").setValue(Boolean.TRUE);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            String documentTypeId = appleRequest.getSitemapParameter("id", null);

            if (documentTypeId != null) {
                documentType = repositorySchema.getDocumentTypeById(Long.parseLong(documentTypeId), true);
                binding.loadFormFromModel(form, documentType);
            } else {
                documentTypeId = "new";
                LabelsAndDescriptionBinding.initRepeater(displayDataRepeater);
            }

            // Set the Repository as an attribute on the form so that the binding can access it.
            form.setAttribute("DaisyRepository", repository);

            String path = getMountPoint() + "/admin/documentType/" + documentTypeId + "/edit/" + getContinuationId();

            viewDataTemplate = new HashMap<String, Object>();
            viewDataTemplate.put("submitPath", path);
            if (documentType != null)
                viewDataTemplate.put("documentType", documentType);
            viewDataTemplate.put("locale", locale);
            viewDataTemplate.put("CocoonFormsInstance", form);
            viewDataTemplate.put("entityName", "documenttype");

            init = true;

            appleResponse.redirectTo(path);
        } else {
            String methodName = appleRequest.getCocoonRequest().getMethod();
            if (methodName.equals("GET")) {
                // display the form
                appleResponse.sendPage("Form-documenttype-Pipe", getViewData(frontEndContext));
            } else if (methodName.equals("POST")) {
                // handle a form submit
                boolean endProcessing = form.process(new FormContext(appleRequest.getCocoonRequest(), locale));

                if (!endProcessing) {
                    appleResponse.sendPage("Form-documenttype-Pipe", getViewData(frontEndContext));
                } else {
                    if (documentType == null) {
                        documentType = repositorySchema.createDocumentType((String)form.getChild("name").getValue());
                        viewDataTemplate.put("documentType", documentType); // for cases where the user goes back
                    }
                    binding.saveFormToModel(form, documentType);
                    documentType.save();
                    appleResponse.redirectTo(getMountPoint() + "/admin/documentType");
                }
            } else {
                throw new HttpMethodNotAllowedException(methodName);
            }
        }
    }

    private Map<String, Object> getViewData(FrontEndContext frontEndContext) {
        Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
        viewData.put("pageContext", frontEndContext.getPageContext());
        return viewData;
    }

    private static final Comparator PART_TYPE_COMPARATOR = new Comparator() {
        private Collator collator = Collator.getInstance(Locale.getDefault()); // a collator is thread-safe

        public int compare(Object o1, Object o2) {
            PartType partType1 = (PartType)o1;
            PartType partType2 = (PartType)o2;

            return collator.compare(partType1.getName(), partType2.getName());
        }
    };

    private static final Comparator FIELD_TYPE_COMPARATOR = new Comparator() {
        private Collator collator = Collator.getInstance(Locale.getDefault());

        public int compare(Object o1, Object o2) {
            FieldType fieldType1 = (FieldType)o1;
            FieldType fieldType2 = (FieldType)o2;

            return collator.compare(fieldType1.getName(), fieldType2.getName());
        }
    };
}
