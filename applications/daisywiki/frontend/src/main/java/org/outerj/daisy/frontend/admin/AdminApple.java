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

import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.Request;
import org.apache.excalibur.xml.sax.XMLizable;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.MultiXMLizable;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.frontend.WikiHelper;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.WfListHelper;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

public class AdminApple extends AbstractDaisyApple implements StatelessAppleController {
    private static Map<String, EntityHandler> ENTITY_HANDLERS;
    static {
        ENTITY_HANDLERS = new HashMap<String, EntityHandler>();
        ENTITY_HANDLERS.put(DocumentTypeHandler.NAME, new DocumentTypeHandler());
        ENTITY_HANDLERS.put(FieldTypeHandler.NAME, new FieldTypeHandler());
        ENTITY_HANDLERS.put(PartTypeHandler.NAME, new PartTypeHandler());
        ENTITY_HANDLERS.put(UserHandler.NAME, new UserHandler());
        ENTITY_HANDLERS.put(RoleHandler.NAME, new RoleHandler());
        ENTITY_HANDLERS.put(BranchHandler.NAME, new BranchHandler());
        ENTITY_HANDLERS.put(LanguageHandler.NAME, new LanguageHandler());
        ENTITY_HANDLERS.put(CollectionHandler.NAME, new CollectionHandler());
        ENTITY_HANDLERS.put(NamespacesHandler.NAME, new NamespacesHandler());
        ENTITY_HANDLERS.put(WfProcessDefinitionHandler.NAME, new WfProcessDefinitionHandler());
        ENTITY_HANDLERS.put(WfPoolHandler.NAME, new WfPoolHandler());
    }

    protected void processRequest(AppleRequest appleRequest, final AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        Locale locale = frontEndContext.getLocale();

        String resource = appleRequest.getSitemapParameter("resource");
        EntityHandler entityHandler = ENTITY_HANDLERS.get(resource);
        if (entityHandler == null)
            throw new ResourceNotFoundException("Unknown admin resource: " + resource);

        String method = request.getMethod();
        if (method.equals("POST")) {
            String action = RequestUtil.getStringParameter(request, "action");

            if (action.equals("delete")) {
                String idParam = RequestUtil.getStringParameter(request, "id");
                long id = Long.parseLong(idParam);
                entityHandler.deleteEntity(id, repository);
            } else {
                boolean handled = entityHandler.handleAction(action, request, repository);
                if (!handled)
                    throw new Exception("Unsupported action: " + action);
            }
        }

        String sortKey = request.getParameter("sortKey");
        if (sortKey == null)
            sortKey = entityHandler.getDefaultSortKey();
        String sortOrder = request.getParameter("sortOrder");
        if (sortOrder == null || !(sortOrder.equals("asc") || sortOrder.equals("desc")))
            sortOrder = "asc";

        // show list of the entities
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageXml", entityHandler.getEntityList(repository, locale));
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("entityName", entityHandler.getEntityName().toLowerCase());
        viewData.put("sortKey", sortKey);
        viewData.put("sortOrder", sortOrder);
        appleResponse.sendPage("EntitiesPipe", viewData);
    }

    interface EntityHandler {
        String getEntityName();

        String getDefaultSortKey();

        void deleteEntity(long id, Repository repository) throws Exception;

        /**
         * @return true if the action has been handled
         */
        boolean handleAction(String action, Request request, Repository repository) throws Exception;

        XMLizable getEntityList(Repository repository, Locale locale) throws Exception;
    }

    public static class DocumentTypeHandler implements EntityHandler {
        public static final String NAME = "documentType";

        public String getEntityName() {
            return NAME;
        }

        public String getDefaultSortKey() {
            return "name";
        }

        public void deleteEntity(long id, Repository repository) throws Exception {
            repository.getRepositorySchema().deleteDocumentType(id);
        }

        public boolean handleAction(String action, Request request, Repository repository) throws Exception {
            return false;
        }

        public XMLizable getEntityList(Repository repository, Locale locale) throws Exception {
            return new XmlObjectXMLizable(repository.getRepositorySchema().getAllDocumentTypes(false).getXml());
        }
    }

    public static class PartTypeHandler implements EntityHandler {
        public static final String NAME = "partType";

        public String getEntityName() {
            return NAME;
        }

        public String getDefaultSortKey() {
            return "name";
        }

        public void deleteEntity(long id, Repository repository) throws Exception {
            repository.getRepositorySchema().deletePartType(id);
        }

        public boolean handleAction(String action, Request request, Repository repository) throws Exception {
            return false;
        }

        public XMLizable getEntityList(Repository repository, Locale locale) throws Exception {
            return new XmlObjectXMLizable(repository.getRepositorySchema().getAllPartTypes(false).getXml());
        }
    }

    public static class FieldTypeHandler implements EntityHandler {
        public static final String NAME = "fieldType";

        public String getEntityName() {
            return NAME;
        }

        public String getDefaultSortKey() {
            return "name";
        }

        public void deleteEntity(long id, Repository repository) throws Exception {
            repository.getRepositorySchema().deleteFieldType(id);
        }

        public boolean handleAction(String action, Request request, Repository repository) throws Exception {
            return false;
        }

        public XMLizable getEntityList(Repository repository, Locale locale) throws Exception {
            return new XmlObjectXMLizable(repository.getRepositorySchema().getAllFieldTypes(false).getXml());
        }
    }

    public static class CollectionHandler implements EntityHandler {
        public static final String NAME = "collection";

        public String getEntityName() {
            return NAME;
        }

        public String getDefaultSortKey() {
            return "name";
        }

        public void deleteEntity(long id, Repository repository) throws Exception {
            repository.getCollectionManager().deleteCollection(id);
        }

        public boolean handleAction(String action, Request request, Repository repository) throws Exception {
            return false;
        }

        public XMLizable getEntityList(Repository repository, Locale locale) throws Exception {
            return new XmlObjectXMLizable(repository.getCollectionManager().getCollections(false).getXml());
        }
    }

    public static class RoleHandler implements EntityHandler {
        public static final String NAME = "role";

        public String getEntityName() {
            return NAME;
        }

        public String getDefaultSortKey() {
            return "name";
        }

        public void deleteEntity(long id, Repository repository) throws Exception {
            repository.getUserManager().deleteRole(id);
        }

        public boolean handleAction(String action, Request request, Repository repository) throws Exception {
            return false;
        }

        public XMLizable getEntityList(Repository repository, Locale locale) throws Exception {
            return new XmlObjectXMLizable(repository.getUserManager().getRoles().getXml());
        }
    }

    public static class UserHandler implements EntityHandler {
        public static final String NAME = "user";

        public String getEntityName() {
            return NAME;
        }

        public String getDefaultSortKey() {
            return "login";
        }

        public void deleteEntity(long id, Repository repository) throws Exception {
            repository.getUserManager().deleteUser(id);
        }

        public boolean handleAction(String action, Request request, Repository repository) throws Exception {
            return false;
        }

        public XMLizable getEntityList(Repository repository, Locale locale) throws Exception {
            return new XmlObjectXMLizable(repository.getUserManager().getUsers().getXml());
        }
    }

    public static class BranchHandler implements EntityHandler {
        public static final String NAME = "branch";

        public String getEntityName() {
            return NAME;
        }

        public String getDefaultSortKey() {
            return "name";
        }

        public void deleteEntity(long id, Repository repository) throws Exception {
            repository.getVariantManager().deleteBranch(id);
        }

        public boolean handleAction(String action, Request request, Repository repository) throws Exception {
            return false;
        }

        public XMLizable getEntityList(Repository repository, Locale locale) throws Exception {
            return new XmlObjectXMLizable(repository.getVariantManager().getAllBranches(false).getXml());
        }
    }

    public static class LanguageHandler implements EntityHandler {
        public static final String NAME = "language";

        public String getEntityName() {
            return NAME;
        }

        public String getDefaultSortKey() {
            return "name";
        }

        public boolean handleAction(String action, Request request, Repository repository) throws Exception {
            return false;
        }

        public void deleteEntity(long id, Repository repository) throws Exception {
            repository.getVariantManager().deleteLanguage(id);
        }

        public XMLizable getEntityList(Repository repository, Locale locale) throws Exception {
            return new XmlObjectXMLizable(repository.getVariantManager().getAllLanguages(false).getXml());
        }
    }

    public static class NamespacesHandler implements EntityHandler {
        public static final String NAME = "namespace";

        public String getEntityName() {
            return NAME;
        }

        public String getDefaultSortKey() {
            return "name";
        }

        public void deleteEntity(long id, Repository repository) throws Exception {
            repository.getNamespaceManager().unregisterNamespace(id);
        }

        public boolean handleAction(String action, Request request, Repository repository) throws Exception {
            if ("register".equals(action)) {
                String namespaceName = request.getParameter("name");
                String fingerprint = request.getParameter("fingerprint");
                
                if (fingerprint != null && !"".equals(fingerprint)) {
                    repository.getNamespaceManager().registerNamespace(namespaceName, fingerprint);    
                } else {
                    repository.getNamespaceManager().registerNamespace(namespaceName);
                }
                
                return true;
            } else if("manage".equals(action)) {
                long namespaceId = Long.valueOf(request.getParameter("id"));
                long documentCount = Long.valueOf(request.getParameter("documentCount"));
                
                Namespace ns = repository.getNamespaceManager().getNamespace(namespaceId);
                ns.setDocumentCount(documentCount);
                ns.setManaged(true);
                
                repository.getNamespaceManager().updateNamespace(ns);
                return true;
            } else if ("unmanage".equals(action)) {
                long namespaceId = Long.valueOf(request.getParameter("id"));
                
                Namespace ns = repository.getNamespaceManager().getNamespace(namespaceId);
                ns.setManaged(false);
                ns.setDocumentCount(0);
                
                repository.getNamespaceManager().updateNamespace(ns);
                return true;
            } else {
                return false;
            }
        }

        public XMLizable getEntityList(Repository repository, Locale locale) throws Exception {
            return new XmlObjectXMLizable(repository.getNamespaceManager().getAllNamespaces().getXml());
        }
    }

    public static class WfProcessDefinitionHandler implements EntityHandler {
        public static final String NAME = "wfProcessDefinition";

        public String getEntityName() {
            return NAME;
        }

        public String getDefaultSortKey() {
            return "name";
        }

        public void deleteEntity(long id, Repository repository) throws Exception {
            WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
            workflowManager.deleteProcessDefinition(String.valueOf(id));
        }

        public boolean handleAction(String action, Request request, Repository repository) throws Exception {
            if ("loadSamples".equals(action)) {
                WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
                workflowManager.loadSampleWorkflows();
                return true;
            }
            return false;
        }

        public XMLizable getEntityList(Repository repository, Locale locale) throws Exception {
            WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");

            Map<String, Integer> instanceCounts = workflowManager.getProcessInstanceCounts();
            XMLizable instanceCountsXml = new XmlObjectXMLizable(WfListHelper.getProcessInstanceCountsAsXml(instanceCounts));
            XMLizable processDefinitionsXml = new XmlObjectXMLizable(WfListHelper.getProcessDefinitionsAsXml(workflowManager.getAllProcessDefinitions(locale)));

            return new MultiXMLizable(instanceCountsXml, processDefinitionsXml);
        }
    }

    public static class WfPoolHandler implements EntityHandler {
        public static final String NAME = "wfPool";

        public String getEntityName() {
            return NAME;
        }

        public String getDefaultSortKey() {
            return "name";
        }

        public void deleteEntity(long id, Repository repository) throws Exception {
            WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
            workflowManager.getPoolManager().deletePool(id);
        }

        public boolean handleAction(String action, Request request, Repository repository) throws Exception {
            return false;
        }

        public XMLizable getEntityList(Repository repository, Locale locale) throws Exception {
            WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
            return new XmlObjectXMLizable(WfListHelper.getPoolsAsXml(workflowManager.getPoolManager().getPools()));
        }
    }
}
