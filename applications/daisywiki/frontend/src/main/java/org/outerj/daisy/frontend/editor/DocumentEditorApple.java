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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.formmodel.DataWidget;
import org.apache.commons.lang.StringUtils;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.frontend.util.EncodingUtil;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.AvailableVariant;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.Namespaces;
import org.outerj.daisy.repository.schema.PartTypeUse;

/**
 * This is the Apple controlling the document editing screen(s).
 */
public class DocumentEditorApple extends DocumentEditorSupport implements Contextualizable, LogEnabled {
    private DocumentEditorForm form;
    private DocumentEditorContext editorContext;
    private Map<String, Object> viewDataTemplate;
    private Logger logger;
    private boolean preSaveInteractionFinished = false; // avoids one could bypass the pre-save interaction

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        super.processRequest(appleRequest, appleResponse);
        if (document == null) {
            if (lastPathPart.equals("new")) {
                initialiseWithNewDocument(appleResponse);
            } else {
                initialiseWithExistingDocument(appleResponse);
            }
            return;
        }

        if (form == null) {
            String branch = repository.getVariantManager().getBranch(document.getBranchId(), false).getName();
            String language = repository.getVariantManager().getLanguage(document.getLanguageId(), false).getName();            
            editorContext = new DocumentEditorContext(documentType, document, repository, document.getId(), document.getBranchId(), document.getLanguageId(), branch, language, locale, serviceManager, frontEndContext.getPageContext(), logger, getContext(), frontEndContext.getDaisyCocoonPath(), lockExpires);
            editorContext.setSelectionListDataWidgetResolver(new DocumentEditorContext.SelectionListDataWidgetResolver() {

                public DataWidget lookupDataWidget(String widgetPath) {
                    return (DataWidget)form.getFieldsForm().lookupWidget(widgetPath.replace('.', '/'));
                }
                
            });
            this.configure();
                    
            final DocumentEditorForm form = DocumentEditorFormBuilder.build(documentType, document.getId(), document.getReferenceLanguageId(), editorContext);
            
            List<AvailableVariant> languageVariants = new ArrayList<AvailableVariant>();
            for (AvailableVariant variant: document.getAvailableVariants().getArray()) {
                // availableLanguageVariants us used to determine the available syncedWith values, so it 
                // must be limited to the current branch.  The client side js filters out the current document's language if required
                if (variant.getBranchId() == document.getBranchId()) {
                    languageVariants.add(variant);
                }
            }

            viewDataTemplate = new HashMap<String, Object>();
            viewDataTemplate.put("submitPath", getPath());
            viewDataTemplate.put("collectionsArray", repository.getCollectionManager().getCollections(false).getArray());
            viewDataTemplate.put("locale", locale);
            viewDataTemplate.put("localeAsString", localeAsString);
            viewDataTemplate.put("htmlareaLang", locale.getLanguage());
            viewDataTemplate.put("hasEditors", Boolean.valueOf(hasEditors()));
            viewDataTemplate.put("documentEditorForm", form);
            viewDataTemplate.put("documentEditorContext", editorContext);
            viewDataTemplate.put("document", document);
            viewDataTemplate.put("availableLanguageVariants", languageVariants);
            viewDataTemplate.put("daisyVersion", repository.getClientVersion());
            viewDataTemplate.put("heartbeatInterval", String.valueOf(DocumentEditorContext.HEARTBEAT_INTERVAL));
            viewDataTemplate.put("displayMode", "default");
            viewDataTemplate.put("serviceManager", serviceManager);
            viewDataTemplate.put("activeRoles", repository.getActiveRolesDisplayNames());

            DocumentBinding.load(form, editorContext, document, repository, locale);

            if (document.isVariantNew()) {
                form.setPublishImmediately(VersionState.PUBLISH.equals(siteConf.getNewVersionStateDefault()));
                form.setMajorChange(true);
            } else {
                Version lastVersion = document.getLastVersion();
                // if last version was published, default to published
                form.setPublishImmediately(VersionState.PUBLISH.equals(lastVersion.getState()));
                if (lastVersion.getSyncedWith() != null) {
                    form.setSyncedWithLanguageId(lastVersion.getSyncedWith().getLanguageId());
                    form.setSyncedWithVersionId(lastVersion.getSyncedWith().getVersionId());
                }
            }

            // Set form instance variable only here at the end since it is used to check
            // if this block of code ended successfully
            this.form = form;
        }

        String resource = appleRequest.getSitemapParameter("resource");
        if (editorContext.handleCommonResources(appleRequest, appleResponse, document, resource, viewDataTemplate)) {
            return;
        } else if ("saveAndClose".equals(resource)) {
            if (!preSaveInteractionFinished) {
                throw new Exception("Continuing save is not allowed before completing the editor pre-save interaction.");
            }
            saveAndCloseEditor(appleResponse, appleRequest);
        } else if (resource == null) {
            appleResponse.redirectTo(EncodingUtil.encodePath(getPath() + "/" + form.getActiveFormName()));
        } else {
            preSaveInteractionFinished = false; // reset this to false each time the user goes back to the editor
            String method = request.getMethod();
            if (method.equals("GET")) {
                form.setActiveForm(resource);
                // show the form
                appleResponse.sendPage("internal/documentEditor/editDocumentPipe", getEditDocumentViewData(frontEndContext));
            } else if (method.equals("POST")) {
                if ("true".equals(request.getParameter("cancelEditing"))) {
                    document.releaseLock();
                    if (returnTo != null)
                        ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
                    else if (document.getId() != null)
                        ResponseUtil.safeRedirect(appleRequest, appleResponse, EncodingUtil.encodePathQuery(currentPath + "/../" + document.getId() + ".html" + (!document.isVariantNew() ? getVariantQueryString() : "")));
                    else
                        ResponseUtil.safeRedirect(appleRequest, appleResponse, EncodingUtil.encodePath(getMountPoint() + "/" + siteConf.getName() + "/"));
                } else {
                    boolean endProcessing = form.process(request, locale, resource);

                    if (endProcessing) {
                        DocumentBinding.save(form, editorContext, document, repository);
                        String preSavePath = getPreSavePath();
                        if (preSavePath == null) {
                            // There is no pre-save interaction (the usual case), go on and save.
                            saveAndCloseEditor(appleResponse, appleRequest);
                        } else {
                            if (preSavePath.startsWith("/"))
                                preSavePath = preSavePath.substring(1);
                            request.setAttribute("daisy.documenteditor.preSaveInteractionContext", new PreSaveInteractionContext());
                            appleResponse.redirectTo("cocoon://" + siteConf.getName() + "/" + preSavePath);
                        }
                    } else {
                        if (!form.getActiveFormName().equals(resource)) {
                            appleResponse.redirectTo(EncodingUtil.encodePath(getPath() + "/" + form.getActiveFormName()));
                        } else {
                            appleResponse.sendPage("internal/documentEditor/editDocumentPipe", getEditDocumentViewData(frontEndContext));
                        }
                    }
                }
            } else {
                throw new HttpMethodNotAllowedException(method);
            }
        }

    }

    private void saveAndCloseEditor(AppleResponse appleResponse, AppleRequest appleRequest) throws RepositoryException {
        long lastVersionId = document.getLastVersionId();
        document.save(form.getValidateOnSave());
        if (lastVersionId == document.getLastVersionId()) { // there was no new version, set the version-level properties manually
            Version lastVersion = document.getLastVersion();
            VersionState previousState = lastVersion.getState();
            lastVersion.setState(form.getPublishImmediately() ? VersionState.PUBLISH : VersionState.DRAFT);

            lastVersion.setChangeType(form.getMajorChange() ? ChangeType.MAJOR : ChangeType.MINOR);

            // an empty change comment means "do not change the comment if not version is changed",
            // otherwise, blanks can be used to indicate the comment must be cleared.
            if (form.getChangeComment() != null && form.getChangeComment().length() != 0) {
                lastVersion.setChangeComment(StringUtils.trimToNull(form.getChangeComment()));
            }
            
            lastVersion.setSyncedWith(form.getSyncedWithLanguageId(), form.getSyncedWithVersionId());
            
            try {
                lastVersion.save();
            } catch (AccessException e) {
                // user is not allowed to publish: ignore
                lastVersion.setState(previousState);
                lastVersion.save();
            }

        }
        document.releaseLock();
        if (returnTo != null)
            ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
        else
            ResponseUtil.safeRedirect(appleRequest, appleResponse, EncodingUtil.encodePathQuery(currentPath + "/../" + document.getId() + ".html" + getVariantQueryString()));
    }

    private Map getEditDocumentViewData(FrontEndContext frontEndContext) throws Exception {
        Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
        List<String> unmanagedNamespaces = new ArrayList<String>();
        Namespaces namespaces = frontEndContext.getRepository().getNamespaceManager().getAllNamespaces();
        for (Namespace namespace : namespaces.getArray()) {
            if (!namespace.isManaged())
                unmanagedNamespaces.add(namespace.getName());
        }
        viewData.putAll(form.getActiveFormTemplateViewData());
        viewData.put("CocoonFormsInstance", form.getActiveForm());
        viewData.put("activeFormName", form.getActiveFormName());
        viewData.put("activeFormTemplate", form.getActiveFormTemplate());
        viewData.put("pageContext", frontEndContext.getPageContext(getLayoutType("plain")));
        viewData.put("unmanagedNamespaces", unmanagedNamespaces);
        viewData.put("tabSequence", this.editorContext.getTabSequence());
        return viewData;
    }

    /**
     * Checks if there are any parts which are edited through an editor.
     */
    private boolean hasEditors() {
        PartTypeUse[] partTypeUses = documentType.getPartTypeUses();
        for (PartTypeUse partTypeUse : partTypeUses) {
            if (partTypeUse.getPartType().isDaisyHtml())
                return true;
        }
        return false;
    }

    protected String getPath() {
        return currentPath + "/edit/" + getContinuationId();
    }

    private String getPreSavePath() {
        Configuration config = frontEndContext.getConfigurationManager().getConfiguration("documenteditor");
        if (config == null)
            return null;

        return config.getChild("preSaveInteraction").getValue(null);
    }
    
    private void configure() throws ConfigurationException{
        Configuration conf = frontEndContext.getConfigurationManager().getConfiguration("documenteditor-" + documentType.getName());
        if (conf != null) {
            Configuration tabsConf = conf.getChild("tab-sequence");
            if (tabsConf != null) {
                List<String> tabSequence = new ArrayList<String>();
                for (Configuration tabConf : tabsConf.getChildren("tab")) {
                    tabSequence.add(tabConf.getValue());
                }
                this.editorContext.setTabSequence(tabSequence);
            }            
        }
    }

    /**
     * Context passed to pre-save interaction hooks.
     *
     * <p>When finished, the hook needs first to call {@link #okToContinue()}
     * and then perform a HTTP redirect to the URL returned by
     * {@link #getReturnURL()}.
     */
    public class PreSaveInteractionContext {
        public void okToContinue() {
            preSaveInteractionFinished = true;
        }

        public Document getDocument() {
            return document;
        }

        public String getEditorURL() {
            return getPath();
        }

        public String getCancelEditingURL() {
            return getPath() + "/" + form.getActiveFormName() + "?cancelEditing=true";
        }

        public String getReturnURL() {
            return getPath() + "/saveAndClose";
        }
    }

}
