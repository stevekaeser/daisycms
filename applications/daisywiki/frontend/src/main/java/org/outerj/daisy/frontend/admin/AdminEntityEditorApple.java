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
import org.outerj.daisy.frontend.util.EncodingUtil;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.LinkExtractorInfo;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.AuthenticationSchemeInfo;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.WfPoolManager;
import org.outerj.daisy.workflow.WfPool;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.cocoon.forms.formmodel.*;
import org.apache.cocoon.forms.binding.Binding;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.validation.WidgetValidator;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.util.StringMessage;
import org.apache.cocoon.forms.datatype.SelectionList;
import org.apache.cocoon.forms.datatype.Datatype;
import org.apache.cocoon.forms.datatype.StaticSelectionList;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Constructor;

/**
 * Generic editor apple for similar entities.
 */
public class AdminEntityEditorApple extends AbstractDaisyApple implements Serviceable {
    private ServiceManager serviceManager;
    private AdminLocales adminLocales;
    private EntityEditor entityEditor;
    private Repository repository;
    private Form form;
    private Binding binding;
    private Object entity;
    private boolean init = false;
    private Locale locale;
    private Map<String, Object> viewDataTemplate;
    private static Map<String, Constructor> ENTITY_EDITORS;
    static {
        ENTITY_EDITORS = new HashMap<String, Constructor>();
        try {
            ENTITY_EDITORS.put("branch", BranchEntityEditor.class.getConstructor(AdminEntityEditorApple.class));
            ENTITY_EDITORS.put("language", LanguageEntityEditor.class.getConstructor(AdminEntityEditorApple.class));
            ENTITY_EDITORS.put("role", RoleEntityEditor.class.getConstructor(AdminEntityEditorApple.class));
            ENTITY_EDITORS.put("user", UserEntityEditor.class.getConstructor(AdminEntityEditorApple.class));
            ENTITY_EDITORS.put("collection", CollectionEntityEditor.class.getConstructor(AdminEntityEditorApple.class));
            ENTITY_EDITORS.put("partType", PartTypeEntityEditor.class.getConstructor(AdminEntityEditorApple.class));
            ENTITY_EDITORS.put("wfPool", PoolEntityEditor.class.getConstructor(AdminEntityEditorApple.class));
        } catch (Exception e) {
            throw new RuntimeException("Error initializing entity editor map.", e);
        }
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        adminLocales = (AdminLocales)serviceManager.lookup(AdminLocales.ROLE);
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            String resource = appleRequest.getSitemapParameter("resource");
            Constructor entityEditorConstructor = ENTITY_EDITORS.get(resource);
            if (entityEditorConstructor == null)
                throw new Exception("No editor available for this type of resource: " + resource);
            else
                entityEditor = (EntityEditor)entityEditorConstructor.newInstance(this);

            locale = frontEndContext.getLocale();
            String lowerCaseEntityName = entityEditor.getEntityName().toLowerCase();
            form = FormHelper.createForm(serviceManager, "resources/form/" + lowerCaseEntityName + "_definition.xml");
            binding = FormHelper.createBinding(serviceManager, "resources/form/" + lowerCaseEntityName + "_binding.xml");

            repository = frontEndContext.getRepository();
            String entityId = appleRequest.getSitemapParameter("id");

            entityEditor.prepareForm();

            if (entityId != null) {
                entity = entityEditor.getEntity(Long.parseLong(entityId));
                binding.loadFormFromModel(form, entity);
            } else {
                entityId = "new";
            }

            String path = getMountPoint() + "/admin/" + entityEditor.getEntityName() + "/" + entityId + "/edit/" + getContinuationId();

            viewDataTemplate = new HashMap<String, Object>();
            entityEditor.addViewData(viewDataTemplate);
            viewDataTemplate.put("submitPath", path);
            if (entity != null)
                viewDataTemplate.put(entityEditor.getEntityName(), entity);
            viewDataTemplate.put("locale", locale);
            viewDataTemplate.put("CocoonFormsInstance", form);
            viewDataTemplate.put("entityName", lowerCaseEntityName);

            init = true;

            appleResponse.redirectTo(EncodingUtil.encodePath(path));
        } else {
            String methodName = appleRequest.getCocoonRequest().getMethod();
            if (methodName.equals("GET")) {
                // display the form
                Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
                viewData.put("pageContext", frontEndContext.getPageContext());
                appleResponse.sendPage("Form-" + entityEditor.getEntityName().toLowerCase() + "-Pipe", viewData);
            } else if (methodName.equals("POST")) {
                // handle a form submit
                boolean endProcessing = form.process(new FormContext(appleRequest.getCocoonRequest(), locale));

                if (!endProcessing) {
                    Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
                    viewData.put("pageContext", frontEndContext.getPageContext());
                    appleResponse.sendPage("Form-" + entityEditor.getEntityName().toLowerCase() + "-Pipe", viewData);
                } else {
                    if (entity == null) {
                        entity = entityEditor.createEntity();
                        viewDataTemplate.put(entityEditor.getEntityName(), entity);
                    }

                    binding.saveFormToModel(form, entity);
                    entityEditor.saveEntity();
                    appleResponse.redirectTo(getMountPoint() + "/admin/" + entityEditor.getEntityName());
                }
            } else {
                throw new Exception("Unspported HTTP method: " + methodName);
            }
        }
    }

    interface EntityEditor {
        String getEntityName();

        void prepareForm() throws Exception;

        void addViewData(Map viewData) throws RepositoryException;

        Object getEntity(long id) throws Exception;

        Object createEntity() throws Exception;

        void saveEntity() throws Exception;
    }

    public class BranchEntityEditor implements EntityEditor {
        public String getEntityName() {
            return "branch";
        }

        public void prepareForm() {
        }

        public void addViewData(Map viewData) {
        }

        public Object getEntity(long id) throws Exception {
            return repository.getVariantManager().getBranch(id, true);
        }

        public Object createEntity() throws Exception {
            return repository.getVariantManager().createBranch((String)form.getChild("name").getValue());
        }

        public void saveEntity() throws Exception {
            ((Branch)entity).save();
        }
    }

    public class LanguageEntityEditor implements EntityEditor {
        public String getEntityName() {
            return "language";
        }

        public void prepareForm() {
        }

        public void addViewData(Map viewData) {
        }

        public Object getEntity(long id) throws Exception {
            return repository.getVariantManager().getLanguage(id, true);
        }

        public Object createEntity() throws Exception {
            return repository.getVariantManager().createLanguage((String)form.getChild("name").getValue());
        }

        public void saveEntity() throws Exception {
            ((Language)entity).save();
        }
    }

    public class RoleEntityEditor implements EntityEditor {
        public String getEntityName() {
            return "role";
        }

        public void prepareForm() {
        }

        public void addViewData(Map viewData) {
        }

        public Object getEntity(long id) throws Exception {
            return repository.getUserManager().getRole(id, true);
        }

        public Object createEntity() throws Exception {
            return repository.getUserManager().createRole((String)form.getChild("name").getValue());
        }

        public void saveEntity() throws Exception {
            ((Role)entity).save();
        }
    }

    public class UserEntityEditor implements EntityEditor {
        public String getEntityName() {
            return "user";
        }

        public Object getEntity(long id) throws Exception {
            return repository.getUserManager().getUser(id, true);
        }

        public Object createEntity() throws Exception {
            return repository.getUserManager().createUser((String)form.getChild("login").getValue());
        }

        public void saveEntity() throws Exception {
            ((User)entity).save();
        }

        public void prepareForm() throws Exception {
            Role[] roles = repository.getUserManager().getRoles().getArray();
            // order roles by name since we're displaying them to users
            Arrays.sort(roles, new Comparator<Role> () {
                public int compare (Role r0, Role r1) {
                    return r0.getName().compareTo(r1.getName());
                }
            });
            
            ((MultiValueField)form.getChild("roles")).setSelectionList(roles, "id", "name");
            Field defaultRoleField = (Field)form.getChild("defaultRole");
            defaultRoleField.setSelectionList(buildRolesSelectionList(roles, defaultRoleField.getDatatype()));
            Field authSchemeField = (Field)form.getChild("authenticationScheme");
            authSchemeField.setSelectionList(buildAuthSchemesSelectionList(repository.getUserManager().getAuthenticationSchemes().getArray(), authSchemeField.getDatatype()));
            authSchemeField.setValue("daisy");
            form.getChild("thepassword").addValidator(new PasswordRequiredValidator());

            // set UserManager is a form attribute so that the binding can access it
            form.setAttribute("UserManager", repository.getUserManager());

            if (entity == null) {
                form.getChild("confirmed").setValue(Boolean.TRUE);
            }
        }

        public void addViewData(Map viewData) throws RepositoryException {
        }

        SelectionList buildRolesSelectionList(Role[] roles, Datatype dataType) {
            StaticSelectionList selectionList = new StaticSelectionList(dataType);
            selectionList.addItem(null, new StringMessage("None (= all)"));
            for (int i = 0; i < roles.length; i++) {
                selectionList.addItem(new Long(roles[i].getId()), new StringMessage(roles[i].getName()));
            }
            return selectionList;
        }

        SelectionList buildAuthSchemesSelectionList(AuthenticationSchemeInfo[] schemes, Datatype dataType) {
            StaticSelectionList selectionList = new StaticSelectionList(dataType);
            selectionList.addItem(null, new StringMessage("(select a scheme)"));
            for (int i = 0; i < schemes.length; i++) {
                selectionList.addItem(schemes[i].getName(), new StringMessage(schemes[i].getName() + " -- " + schemes[i].getDescription()));
            }
            return selectionList;
        }

        class PasswordRequiredValidator implements WidgetValidator {
            public boolean validate(Widget widget) {
                Field authSchemeField = (Field)widget.getForm().getChild("authenticationScheme");
                if ("daisy".equals(authSchemeField.getValue()) && entity == null && widget.getValue() == null)
                    ((Field)widget).setValidationError(new ValidationError("Password is required", false));
                return false;
            }
        }
    }

    public class CollectionEntityEditor implements EntityEditor {
        public String getEntityName() {
            return "collection";
        }

        public void prepareForm() {
        }

        public void addViewData(Map viewData) throws RepositoryException {
        }

        public Object getEntity(long id) throws Exception {
            return repository.getCollectionManager().getCollection(id, true);
        }

        public Object createEntity() throws Exception {
            return repository.getCollectionManager().createCollection((String)form.getChild("name").getValue());
        }

        public void saveEntity() throws Exception {
            ((DocumentCollection)entity).save();
        }
    }

    public class PartTypeEntityEditor implements EntityEditor {
        public String getEntityName() {
            return "partType";
        }

        public void prepareForm() throws RepositoryException {
            // initialiaze repeater for displaydata with the locales we allow to be edited
            Repeater displayDataRepeater = (Repeater)form.getChild("displaydata");
            displayDataRepeater.setAttribute("adminLocales", adminLocales.getLocales().getAsObjects());
            LabelsAndDescriptionBinding.initRepeater(displayDataRepeater);

            // Supply link extractor selection
            Field linkExtractorField = (Field)form.getChild("linkExtractor");
            StaticSelectionList selectionList = new StaticSelectionList(linkExtractorField.getDatatype());
            selectionList.addItem("");
            LinkExtractorInfo[] extractorInfos = repository.getRepositorySchema().getLinkExtractors().getArray();
            for (int i = 0; i < extractorInfos.length; i++) {
                selectionList.addItem(extractorInfos[i].getName(), new StringMessage(extractorInfos[i].getName() + " -- " + extractorInfos[i].getDescription()));
            }
            linkExtractorField.setSelectionList(selectionList);
        }

        public void addViewData(Map viewData) throws RepositoryException {
        }

        public Object getEntity(long id) throws Exception {
            return repository.getRepositorySchema().getPartTypeById(id, true);
        }

        public Object createEntity() throws Exception {
            String mimeTypes = (String)form.getChild("mimetypes").getValue();
            if (mimeTypes == null)
                mimeTypes = "";
            return repository.getRepositorySchema().createPartType((String)form.getChild("name").getValue(), mimeTypes);
        }

        public void saveEntity() throws Exception {
            ((PartType)entity).save();
        }
    }

    public class PoolEntityEditor implements EntityEditor {
        public String getEntityName() {
            return "wfPool";
        }

        public void prepareForm() {
        }

        public void addViewData(Map viewData) {
        }

        public Object getEntity(long id) throws Exception {
            WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
            WfPoolManager poolManager = workflowManager.getPoolManager();
            return poolManager.getPool(id);
        }

        public Object createEntity() throws Exception {
            WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
            WfPoolManager poolManager = workflowManager.getPoolManager();
            return poolManager.createPool((String)form.getChild("name").getValue());
        }

        public void saveEntity() throws Exception {
            ((WfPool)entity).save();
        }
    }
}