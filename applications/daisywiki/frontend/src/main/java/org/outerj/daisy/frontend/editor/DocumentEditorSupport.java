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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.i18n.Bundle;
import org.apache.cocoon.i18n.BundleFactory;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.EncodingUtil;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.navigation.LookupAlternative;
import org.outerj.daisy.navigation.NavigationLookupResult;
import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.DocumentTypeInconsistencyException;
import org.outerj.daisy.repository.Field;
import org.outerj.daisy.repository.Link;
import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.PartPartDataSource;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.variant.VariantManager;

public abstract class DocumentEditorSupport extends AbstractDaisyApple implements Serviceable {
    
    protected boolean init = false;
    protected String lastPathPart;
    protected Locale locale;
    protected String localeAsString;
    protected String returnTo;
    protected Repository repository;
    protected String navigationPath;
    protected String activeNavPath;
    protected Document document;
    protected SiteConf siteConf;
    protected long lockExpires;
    protected DocumentType documentType;
    protected String currentPath;
    protected ServiceManager serviceManager;
    private String branch;
    private String language;
    private String variantParams;
    private String variantQueryString;
    
    protected abstract String getPath();
    
    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    /**
     * Must be called by subclasses.
     */
    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            // start of a new editing session can only be initiated using POST (among other things
            // because it can make persistent modifications like create locks)
            if (!request.getMethod().equals("POST") && !"true".equals(request.getParameter("startWithGet"))) {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
    
            repository = frontEndContext.getRepository();
            lastPathPart = appleRequest.getSitemapParameter("lastPathPart");
            locale = frontEndContext.getLocale();
            localeAsString = frontEndContext.getLocaleAsString();
            siteConf = frontEndContext.getSiteConf();
            navigationPath = appleRequest.getSitemapParameter("navigationPath");            
            String basePath = getMountPoint() + "/" + siteConf.getName();
            currentPath = basePath + "/" + navigationPath;
            // returnTo = URL to return to after editing, optional.
            returnTo = request.getParameter("returnTo");
            
            //allow forceful setting of navpath through request parameter
            activeNavPath = appleRequest.getCocoonRequest().getParameter("activeNavPath");
            
            init = true;
        }
    }

    protected void initialiseWithExistingDocument(AppleResponse appleResponse)
    throws Exception, RepositoryException, ResourceNotFoundException {
        long requestedBranchId = RequestUtil.getBranchId(request, -1, repository);
        long requestedLanguageId = RequestUtil.getLanguageId(request, -1, repository);
        NavigationManager navigationManager = (NavigationManager)repository.getExtension("NavigationManager");
        VersionMode navVersionMode = frontEndContext.getVersionMode();
        NavigationLookupResult lookupResult = navigationManager.lookup(navigationPath, requestedBranchId, requestedLanguageId,
                new LookupAlternative[] { new LookupAlternative(siteConf.getName(), siteConf.getCollectionId(), siteConf.getNavigationDoc(), navVersionMode)}, false);
        if (lookupResult.isNotFound()) {
            throw new ResourceNotFoundException("Path not found: " + navigationPath);
        } else if (lookupResult.isRedirect() && lookupResult.getVariantKey() == null) {
            throw new Exception("Can't handle redirect navigation nodes.");
        }
    
        VariantKey variantKey = lookupResult.getVariantKey();
        Document document = repository.getDocument(variantKey, true);
        documentType = repository.getRepositorySchema().getDocumentTypeById(document.getDocumentTypeId(), false);
    
        boolean showLockWarnPage = false;
        LockInfo lockInfo = document.getLockInfo(false);
        if (lockInfo.hasLock() && lockInfo.getUserId() != repository.getUserId()) {
            showLockWarnPage = true;
        } else if (siteConf.getAutomaticLocking()) {
            boolean success = document.lock(siteConf.getDefaultLockTime(), siteConf.getLockType());
            lockInfo = document.getLockInfo(false);
            if (!success) {
                showLockWarnPage = true;
            } else {
                lockExpires = lockInfo.getTimeAcquired().getTime() + lockInfo.getDuration();
                // after locking the document, load it again to be sure that we have the latest version
                // of it (somebody might have saved it during our last loading and taking the lock). In
                // case of a pessimistic lock, the user can now be sure that saving the document will
                // not give concurrent modification exceptions.
                document = repository.getDocument(variantKey, true);
            }
        }
    
        // Check if we should revert to an older version...
        if (request.getParameter("versionId") != null) {
            long versionId = RequestUtil.getLongParameter(request, "versionId");
            if (versionId > document.getLastVersionId() || versionId < 1) {
                throw new Exception("Specified version ID is out of range: " + versionId);
            }
            revertDocument(document, versionId);
        }
    
        // assigning document instance variable confirms init is done
        this.document = document;
        setBranchAndLanguage(document.getBranchId(), document.getLanguageId());
        
        if (showLockWarnPage) {
            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("lockInfo", lockInfo);
            String userName = repository.getUserManager().getUserDisplayName(lockInfo.getUserId());
            viewData.put("lockUserName", userName);
            viewData.put("pageContext", frontEndContext.getPageContext());
            viewData.put("editPath", getPath());
            viewData.put("backLink", currentPath + ".html" + getVariantQueryString());
            viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/locked.xml"));
    
            appleResponse.sendPage("internal/genericPipe", viewData);
            return;
        } else {
            appleResponse.redirectTo(EncodingUtil.encodePath(getPath()));
            return;                    
        }
    }
    
    protected void initialiseWithNewDocument(AppleResponse appleResponse)
            throws Exception, RepositoryException {
        // A new document can be created from scratch, based on the content of
        // another document (a template),
        // as a new variant of an existing document, or using a Document object
        // in a request attribute.
        Document document;
        if (request.getParameter("documentType") != null) {
            String documentType = RequestUtil.getStringParameter(request,
                    "documentType");
            long branchId = RequestUtil.getBranchId(request, siteConf
                    .getBranchId(), repository);
            long languageId = RequestUtil.getLanguageId(request, siteConf
                    .getLanguageId(), repository);
            document = createNewDocument(documentType, branchId, languageId);
            document.setReferenceLanguageId(siteConf
                    .getDefaultReferenceLanguageId());
        } else if (request.getParameter("template") != null) {
            String template = RequestUtil.getStringParameter(request,
                    "template");
            long branchId = RequestUtil.getBranchId(request, siteConf
                    .getBranchId(), repository);
            long languageId = RequestUtil.getLanguageId(request, siteConf
                    .getLanguageId(), repository);
            document = createNewDocumentFromTemplate(template, branchId,
                    languageId);
        } else if (request.getParameter("variantOf") != null) {
            String documentId = RequestUtil.getStringParameter(request,
                    "variantOf");
            long startBranchId = getBranch(request, "startBranch");
            long startLanguageId = getLanguage(request, "startLanguage");
            long newBranchId = getBranch(request, "newBranch");
            long newLanguageId = getLanguage(request, "newLanguage");
            document = createNewDocumentVariant(documentId, startBranchId,
                    startLanguageId, newBranchId, newLanguageId);
        } else if (request.getParameter("templateDocument") != null) {
            String requestAttrName = RequestUtil.getStringParameter(request,
                    "templateDocument");
            Object object = request.getAttribute(requestAttrName);

            if (object == null)
                throw new Exception(
                        "Nothing found in request attribute specified in templateDocument parameter: "
                                + requestAttrName);
            if (!(object instanceof Document))
                throw new Exception(
                        "Object specified in templateDocument parameter is of an incorrect type, got: "
                                + object.getClass().getName());

            document = (Document) object;
            documentType = repository.getRepositorySchema()
                    .getDocumentTypeById(document.getDocumentTypeId(), false);
        } else {
            throw new Exception(
                    "Either template, documentType, variantOf or templateDocument parameter must be specified for the creation of a new document (variant).");
        }
        // assing document only here, indicates successful completion of this
        // initialisation
        this.document = document;
        setBranchAndLanguage(document.getBranchId(), document.getLanguageId());
        appleResponse.redirectTo(EncodingUtil.encodePath(getPath()));
    }
    


    /**
     * Copies data from an older version into the document.
     */
    protected void revertDocument(Document document, long versionId) throws Exception {
        // Simple version reversion feature.
        // Possible improvements:
        //    - only replace parts when they have actually changed (especially
        //      important for bigger parts). We would need some indicator on the
        //      Part objects that tells the last version for which its content changed.
        try {
            Version version = document.getVersion(versionId);

            // revert parts
            // first delete old parts
            Part[] oldParts = document.getParts().getArray();
            for (Part oldPart : oldParts) {
                if (version.hasPart(oldPart.getTypeId()) && oldPart.getDataChangedInVersion() > versionId)
                    document.deletePart(oldPart.getTypeId());
            }
            // then add the parts from the version
            Part[] parts = version.getParts().getArray();
            for (Part part : parts) {
                if (!document.hasPart(part.getTypeId()))
                    document.setPart(part.getTypeId(), part.getMimeType(), new PartPartDataSource(part));
                document.setPartFileName(part.getTypeId(), part.getFileName());
            }

            // revert fields
            // first delete old fields
            Field[] oldFields = document.getFields().getArray();
            for (Field oldField : oldFields) {
                document.deleteField(oldField.getTypeId());
            }
            // then add the fields from the version
            Field[] fields = version.getFields().getArray();
            for (Field field : fields) {
                document.setField(field.getTypeId(), field.getValue());
            }

            // revert document name
            document.setName(version.getDocumentName());

            // revert links
            document.clearLinks();
            Link[] links = version.getLinks().getArray();
            for (Link link : links) {
                document.addLink(link.getTitle(), link.getTarget());
            }
        } catch (DocumentTypeInconsistencyException e) {
            throw new Exception("Failed to revert to version " + versionId + " because of changes in the document type.", e);
        } catch (RepositoryException e) {
            throw new Exception("Error while reverting document content to version " + versionId + ".", e);
        }
    }

    protected void setBranchAndLanguage(long branchId, long languageId) throws Exception {
        VariantManager variantManager = repository.getVariantManager();
        branch = variantManager.getBranch(branchId, false).getName();
        language = variantManager.getLanguage(languageId, false).getName();

        // prepare params view can use
        if (branchId != siteConf.getBranchId() || languageId != siteConf.getLanguageId()) {
            variantParams = "&branch=" + branch + "&language=" + language;
            variantQueryString = "?branch=" + branch + "&language=" + language;
        } else {
            variantParams = "";
            variantQueryString = "";
        }
    }
    
    protected String getVariantParams() {
        return variantParams;
    }
    protected String getVariantQueryString() {
        return variantQueryString;
    }
    protected String getBranch() {
        return branch;
    }
    protected String getLanguage() {
        return language;
    }
    
    /**
     * Gets a branch id by testing for the request parameter baseName or else baseName + "Id".
     * This last case is for backwards-compatibility.
     */
    private long getBranch(Request request, String baseName) throws Exception {
        String paramName = baseName;
        String idName = baseName + "Id";
        if (request.getParameter(idName) != null)
            paramName = idName;
        else if (request.getParameter(paramName) == null)
            throw new Exception("Missing request parameter: " + baseName + " or " + idName);

        return RequestUtil.getBranchId(request.getParameter(paramName), -1, repository);
    }

    /**
     * Gets a language id by testing for the request parameter baseName or else baseName + "Id".
     * This last case is for backwards-compatibility.
     */
    private long getLanguage(Request request, String baseName) throws Exception {
        String paramName = baseName;
        String idName = baseName + "Id";
        if (request.getParameter(idName) != null)
            paramName = idName;
        else if (request.getParameter(paramName) == null)
            throw new Exception("Missing request parameter: " + baseName + " or " + idName);

        return RequestUtil.getLanguageId(request.getParameter(paramName), -1, repository);
    }

    /**
     * Returns a localized version of "New Document"
     */
    private String getNewDocumentName() throws Exception {
        Bundle bundle = null;
        BundleFactory bundleFactory = (BundleFactory)serviceManager.lookup(BundleFactory.ROLE);
        try {
            final String[] lookupSources = new String[]{ "wikidata:/resources/i18n", "resources/i18n"};            
            bundle = bundleFactory.select(lookupSources, "messages", locale);
            return bundle.getString("editdoc.new-document-name");
        } finally {
            if (bundle != null)
                bundleFactory.release(bundle);
            serviceManager.release(bundleFactory);
        }
    }

    private Document createNewDocument(String documentTypeName, long branchId, long languageId) throws Exception {
        documentType = repository.getRepositorySchema().getDocumentType(documentTypeName, false);
        Document document = repository.createDocument(getNewDocumentName(), documentType.getId(), branchId, languageId);
        DocumentCollection collection = repository.getCollectionManager().getCollection(siteConf.getCollectionId(), false);
        document.addToCollection(collection);
        return document;
    }

    private Document createNewDocumentFromTemplate(String templateDocumentId, long branchId, long languageId) throws Exception {
        Document templateDoc = repository.getDocument(templateDocumentId, branchId, languageId, false);

        Document document = repository.createDocument(getNewDocumentName(), templateDoc.getDocumentTypeId(), branchId, languageId);
        this.documentType = repository.getRepositorySchema().getDocumentTypeById(templateDoc.getDocumentTypeId(), false);

        // copy everything from the template
        org.outerj.daisy.repository.Field[] fields = templateDoc.getFields().getArray();
        for (Field field : fields) {
            // the template doc might contain fields that are no longer part of the document type,
            // therefore check with the document type if the field still belongs to it
            if (documentType.hasFieldType(field.getTypeId()))
                document.setField(field.getTypeId(), field.getValue());
        }

        Part[] parts = templateDoc.getParts().getArray();
        for (Part part : parts) {
            if (documentType.hasPartType(part.getTypeId()))
                document.setPart(part.getTypeId(), part.getMimeType(), new PartPartDataSource(part));
        }

        Link[] links = templateDoc.getLinks().getArray();
        for (Link link : links)
            document.addLink(link.getTitle(), link.getTarget());

        DocumentCollection[] collections = templateDoc.getCollections().getArray();
        for (DocumentCollection collection : collections)
            document.addToCollection(collection);

        for (Map.Entry<String, String> entry : templateDoc.getCustomFields().entrySet()) {
            document.setCustomField(entry.getKey(), entry.getValue());
        }

        return document;
    }

    private Document createNewDocumentVariant(String documentId, long startBranchId, long startLanguageId,
            long newBranchId, long newLanguageId) throws Exception {
        Document startDocument = repository.getDocument(documentId, startBranchId, startLanguageId, false);
        Document document = repository.createVariant(documentId, startBranchId, startLanguageId, -1, newBranchId, newLanguageId, false);
        documentType = repository.getRepositorySchema().getDocumentTypeById(document.getDocumentTypeId(), false);        
        for (DocumentCollection collection : startDocument.getCollections().getArray()) {
            document.addToCollection(collection);
        }
        return document;
    }

}
