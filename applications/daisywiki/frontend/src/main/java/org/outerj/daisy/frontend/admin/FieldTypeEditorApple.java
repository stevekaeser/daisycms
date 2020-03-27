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

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.repository.schema.*;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.query.SortOrder;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.binding.Binding;
import org.apache.cocoon.forms.formmodel.*;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.event.ValueChangedListener;
import org.apache.cocoon.forms.event.ValueChangedEvent;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.xmlbeans.XmlException;
import org.outerx.daisy.x10.StaticSelectionListDocument;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class FieldTypeEditorApple extends AbstractDaisyApple implements Serviceable, Initializable {
    private ServiceManager serviceManager;
    private AdminLocales.Locales locales;
    private Form form;
    private Binding binding;
    private RepositorySchema repositorySchema;
    private FieldType fieldType;
    private boolean init = false;
    private Locale locale;
    private Map<String, Object> viewDataTemplate;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public void initialize() throws Exception {
        AdminLocales adminLocales = (AdminLocales)serviceManager.lookup(AdminLocales.ROLE);
        try {
            locales = adminLocales.getLocales();
        } finally {
            serviceManager.release(adminLocales);
        }
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            locale = frontEndContext.getLocale();
            form = FormHelper.createForm(serviceManager, "resources/form/fieldtype_definition.xml");
            binding = FormHelper.createBinding(serviceManager, "resources/form/fieldtype_binding.xml");
            Repository repository = frontEndContext.getRepository();
            repositorySchema = repository.getRepositorySchema();

            // initialiaze repeater for displaydata with the locales we allow to be edited
            Repeater displayDataRepeater = (Repeater)form.getChild("displaydata");
            displayDataRepeater.setAttribute("adminLocales", locales.getAsObjects());

            form.getChild("valuetype").setValue(ValueType.STRING.toString());
            ((Field)form.getChild("valuetype")).addValueChangedListener(new ValueTypeChangedListener());
            form.setAttribute("repository", repository); // so that it is accessible to validators

            String fieldTypeId = appleRequest.getSitemapParameter("id");
            if (fieldTypeId != null) {
                fieldType = repositorySchema.getFieldTypeById(Long.parseLong(fieldTypeId), true);
                binding.loadFormFromModel(form, fieldType);
                form.getChild("valuetype").setValue(fieldType.getValueType().toString());
                loadSelectionList();
                form.getChild("valuetype").setState(WidgetState.OUTPUT);
                form.getChild("multivalue").setState(WidgetState.OUTPUT);
                form.getChild("hierarchical").setState(WidgetState.OUTPUT);
            } else {
                fieldTypeId = "new";
                form.getChild("size").setValue(new Integer(0));
                LabelsAndDescriptionBinding.initRepeater(displayDataRepeater);
            }

            String path = getMountPoint() + "/admin/fieldType/" + fieldTypeId + "/edit/" + getContinuationId();

            viewDataTemplate = new HashMap<String, Object>();
            viewDataTemplate.put("submitPath", path);
            viewDataTemplate.put("mountPoint", getMountPoint());
            if (fieldType != null)
                viewDataTemplate.put("fieldType", fieldType);
            viewDataTemplate.put("locale", locale);
            viewDataTemplate.put("adminLocales", locales.getAsStrings());
            viewDataTemplate.put("CocoonFormsInstance", form);
            viewDataTemplate.put("entityName", "fieldtype");

            init = true;

            appleResponse.redirectTo(path);
        } else {
            String methodName = appleRequest.getCocoonRequest().getMethod();
            if (methodName.equals("GET")) {
                // display the form
                viewDataTemplate.put("valuetype", getValueType().toString());
                appleResponse.sendPage("Form-fieldtype-Pipe", getViewData(frontEndContext));
            } else if (methodName.equals("POST")) {
                // handle a form submit
                boolean endProcessing = form.process(new FormContext(appleRequest.getCocoonRequest(), locale));

                if (!endProcessing) {
                    viewDataTemplate.put("valuetype", getValueType().toString());
                    appleResponse.sendPage("Form-fieldtype-Pipe", getViewData(frontEndContext));
                } else {
                    if (fieldType == null) {
                        boolean multiValue = (Boolean)form.getChild("multivalue").getValue();
                        boolean hierarchical = (Boolean)form.getChild("hierarchical").getValue();
                        fieldType = repositorySchema.createFieldType((String)form.getChild("name").getValue(),
                            getValueType(), multiValue, hierarchical);
                        viewDataTemplate.put("fieldType", fieldType);
                    }

                    binding.saveFormToModel(form, fieldType);
                    storeSelectionList();
                    fieldType.save();
                    appleResponse.redirectTo(getMountPoint() + "/admin/fieldType");
                }
            } else {
                throw new Exception("Unspported HTTP method: " + methodName);
            }
        }
    }

    private Map<String, Object> getViewData(FrontEndContext frontEndContext) {
        Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
        viewData.put("pageContext", frontEndContext.getPageContext());
        return viewData;
    }

    private void loadSelectionList() {
        SelectionList selectionList = fieldType.getSelectionList();
        if (selectionList == null) {
            // do nothing
        } else if (selectionList instanceof StaticSelectionList) {
            form.getChild("selectionlist_type").setValue("static");
            Field field = (Field)form.lookupWidget("selectionlist/static/staticSelectionList");
            StaticSelectionList list = (StaticSelectionList)selectionList;
            field.setValue(list.getXml().toString());
        } else if (selectionList instanceof LinkQuerySelectionList) {
            form.getChild("selectionlist_type").setValue("linkquery");
            LinkQuerySelectionList list = (LinkQuerySelectionList)selectionList;
            form.lookupWidget("selectionlist/linkquery/whereClause").setValue(list.getWhereClause());
            form.lookupWidget("selectionlist/linkquery/filterVariants").setValue(list.getFilterVariants());
        } else if (selectionList instanceof QuerySelectionList) {
            form.getChild("selectionlist_type").setValue("query");
            QuerySelectionList list = (QuerySelectionList)selectionList;
            form.lookupWidget("selectionlist/query/query").setValue(list.getQuery());
            form.lookupWidget("selectionlist/query/filterVariants").setValue(list.getFilterVariants());
            form.lookupWidget("selectionlist/query/sortOrder").setValue(list.getSortOrder());
        } else if (selectionList instanceof HierarchicalQuerySelectionList) {
            HierarchicalQuerySelectionList list = (HierarchicalQuerySelectionList)selectionList;
            form.getChild("selectionlist_type").setValue("hierarchicalquery");
            ContainerWidget parent = (ContainerWidget)form.lookupWidget("selectionlist/hierarchicalquery");
            parent.getChild("whereClause").setValue(list.getWhereClause());
            parent.getChild("filterVariants").setValue(list.getFilterVariants());
            parent.getChild("linkfields").setValue(list.getLinkFields());
        } else if (selectionList instanceof ParentLinkedSelectionList) {
            ParentLinkedSelectionList list = (ParentLinkedSelectionList)selectionList;
            form.getChild("selectionlist_type").setValue("parentlinked");
            ContainerWidget parent = (ContainerWidget)form.lookupWidget("selectionlist/parentlinked");
            parent.getChild("whereClause").setValue(list.getWhereClause());
            parent.getChild("filterVariants").setValue(list.getFilterVariants());
            parent.getChild("linkfield").setValue(list.getParentLinkField());
        }
    }

    private void storeSelectionList() {
        String selectionListType = (String)form.getChild("selectionlist_type").getValue();
        if (selectionListType.equals("static")) {
            storeStaticSelectionList();
        } else if (selectionListType.equals("linkquery")) {
            storeLinkQuerySelectionList();
        } else if (selectionListType.equals("query")) {
            storeQuerySelectionList();
        } else if (selectionListType.equals("hierarchicalquery")) {
            storeHierarchicalQuerySelectionList();
        } else if (selectionListType.equals("parentlinked")) {
            storeParentLinkedSelectionList();
        } else {
            fieldType.clearSelectionList();
        }
    }

    private void storeStaticSelectionList() {
        Field field = (Field)form.lookupWidget("selectionlist/static/staticSelectionList");
        StaticSelectionListDocument selectionListDocument;
        try {
            selectionListDocument = StaticSelectionListDocument.Factory.parse((String)field.getValue());
        } catch (XmlException e) {
            // should normally not occur, as form validation also checks the XML is correct
            throw new RuntimeException("Unexpected error parsing static selection list XML.", e);
        }
        StaticSelectionList selectionList = fieldType.createStaticSelectionList();
        selectionList.setAllFromXml(selectionListDocument.getStaticSelectionList());
    }

    private void storeLinkQuerySelectionList() {
        if (getValueType() != ValueType.LINK)
            return;

        String whereClause = (String)form.lookupWidget("selectionlist/linkquery/whereClause").getValue();
        boolean filterVariants = ((Boolean)form.lookupWidget("selectionlist/linkquery/filterVariants").getValue()).booleanValue();
        fieldType.createLinkQuerySelectionList(whereClause, filterVariants);
    }

    private void storeQuerySelectionList() {
        String query = (String)form.lookupWidget("selectionlist/query/query").getValue();
        boolean filterVariants = ((Boolean)form.lookupWidget("selectionlist/query/filterVariants").getValue()).booleanValue();
        SortOrder sortOrder = (SortOrder)form.lookupWidget("selectionlist/query/sortOrder").getValue();
        fieldType.createQuerySelectionList(query, filterVariants, sortOrder);
    }

    private void storeHierarchicalQuerySelectionList() {
        if (getValueType() != ValueType.LINK)
            return;

        ContainerWidget parent = (ContainerWidget)form.lookupWidget("selectionlist/hierarchicalquery");
        String whereClause = (String)parent.lookupWidget("whereClause").getValue();
        boolean filterVariants = (Boolean)parent.lookupWidget("filterVariants").getValue();
        String[] linkFields = objectArrayToStringArray((Object[])parent.lookupWidget("linkfields").getValue());
        fieldType.createHierarchicalQuerySelectionList(whereClause, linkFields, filterVariants);
    }

    private void storeParentLinkedSelectionList() {
        if (getValueType() != ValueType.LINK)
            return;

        ContainerWidget parent = (ContainerWidget)form.lookupWidget("selectionlist/parentlinked");
        String whereClause = (String)parent.lookupWidget("whereClause").getValue();
        boolean filterVariants = (Boolean)parent.lookupWidget("filterVariants").getValue();
        String parentLinkField = (String)parent.lookupWidget("linkfield").getValue();
        fieldType.createParentLinkedSelectionList(whereClause, parentLinkField, filterVariants);
    }

    private String[] objectArrayToStringArray(Object[] values) {
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (String)values[i];
        }
        return result;
    }

    private ValueType getValueType() {
        return fieldType != null ? fieldType.getValueType() : ValueType.fromString((String)form.getChild("valuetype").getValue());
    }

    class ValueTypeChangedListener implements ValueChangedListener {
        public void valueChanged(ValueChangedEvent valueChangedEvent) {
            // If there's a static selection list, clear it when changing the data type
            String selectionListType = (String)form.getChild("selectionlist_type").getValue();
            if (selectionListType.equals("static")) {
                Field field = (Field)form.lookupWidget("selectionlist/static/staticSelectionList");
                field.setValue(null);
            }
        }
    }
}
