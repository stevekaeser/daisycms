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
package org.outerj.daisy.frontend;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.util.I18nMessage;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.excalibur.xml.sax.XMLizable;
import org.outerj.daisy.emailnotifier.EmailSubscriptionManager;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketEntry;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketHelper;
import org.outerj.daisy.frontend.util.EncodingUtil;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.MultiXMLizable;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.frontend.util.TaggedMessage;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.publisher.GlobalPublisherException;
import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.AvailableVariants;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.repository.comment.Comment;
import org.outerj.daisy.repository.comment.CommentVisibility;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.repository.user.User;
import org.outerx.daisy.x10.SearchResultDocument;

public class DocumentApple extends AbstractDocumentApple implements StatelessAppleController {
    private String publishType;
    private AppleRequest appleRequest;
    private AppleResponse appleResponse;

    protected void processDocumentRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        this.appleResponse = appleResponse;
        this.appleRequest = appleRequest;

        String whatToDo = appleRequest.getSitemapParameter("whatToDo");
        publishType = appleRequest.getSitemapParameter("publishType", null);

        if (whatToDo.equals("handleDocumentRequest")) {
            handleDocumentRequest();
        } else if (whatToDo.equals("handleVersionRequest")) {
            handleVersionRequest();
        } else if (whatToDo.equals("showVersionOverview")) {
            showVersionOverview();
        } else if (whatToDo.equals("showLiveVersionOverview")) {
            showLiveVersionOverview();
        } else if (whatToDo.equals("showReferrers")) {
            showReferrers();
        } else if (whatToDo.equals("showDiff")) {
            showDiff();
        } else {
            throw new Exception("Invalid value for whatToDo sitemap parameter.");
        }
    }

    protected boolean needsInitialisation() {
        return true;
    }

    protected void handleDocumentRequest() throws Exception {
        Map<String, String> displayParams = new HashMap<String, String>();
        boolean showComments = RequestUtil.getBooleanParameter(request, "showComments", false);
        if (showComments)
        	displayParams.put("showComments", "true");
        boolean landscape = RequestUtil.getBooleanParameter(request, "landscape", false);
        if (landscape)
        	displayParams.put("landscape", "true");

        String action = request.getParameter("action");
        if (action == null) {
            showDocument(null, displayParams);
        } else if (action.equals("delete")) {
            handleDocumentDeleteRequest();
        } else if (action.equals("changeOwner")) {
            handleChangeOwnerRequest();
        } else if (action.equals("subscribe") || action.equals("unsubscribe")) {
            if (request.getMethod().equals("POST")) {
                User user = getRepository().getUserManager().getUser(getRepository().getUserId(), false);
                // Only let the user make subscriptions if his user record is "updateableByUser". This is
                // not a requirement of the subscription system, but since that flag is usually used
                // for anonymous users like "guest", this seems logical.
                if (!user.isUpdateableByUser())
                    throw new Exception("User " + user.getLogin() + " (ID: " + user.getId() + ") is not allowed to subscribe for document notifications.");
                EmailSubscriptionManager subscriptionManager = (EmailSubscriptionManager)getRepository().getExtension("EmailSubscriptionManager");
                XMLizable message;
                if (action.equals("subscribe")) {
                    subscriptionManager.addDocumentSubscription(new VariantKey(getDocumentId(), getBranchId(), getLanguageId()));
                    message = new I18nMessage("subscription-added");
                    if (user.getEmail() == null || user.getEmail().length() < 1) {
                        message = new MultiXMLizable(message, new I18nMessage("subscription-missing-email"));
                    }
                } else {
                    message = new I18nMessage("subscription-deleted");
                    subscriptionManager.deleteDocumentSubscription(new VariantKey(getDocumentId(), getBranchId(), getLanguageId()));
                }
                showDocument(message, displayParams);
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        } else if (action.equals("addComment")) {
            if (request.getMethod().equals("POST")) {

                // disallow creating comments for the user with login "guest"
                if (getRepository().getUserLogin().equals("guest"))
                    throw new Exception("The guest user is not allowed to add comments.");

                String text = request.getParameter("commentText");
                String visibilityParam = request.getParameter("commentVisibility");
                if (text == null || text.trim().equals("")) {
                    // no comment text, show page again, scroll to comment editor
                    appleResponse.redirectTo(EncodingUtil.encodePath(getMountPoint() + "/" + getSiteConf().getName() + "/" + getRequestedNavigationPath()) + ".html?showComments=true" + getVariantParams() + "#daisycommenteditor");
                } else {
                    CommentVisibility visibility;
                    if (visibilityParam == null) {
                        visibility = CommentVisibility.PUBLIC;
                    } else if (visibilityParam.equals("public")) {
                        visibility = CommentVisibility.PUBLIC;
                    } else if (visibilityParam.equals("editors")) {
                        visibility = CommentVisibility.EDITORS;
                    } else if (visibilityParam.equals("private")) {
                        visibility = CommentVisibility.PRIVATE;
                    } else {
                        throw new Exception("Invalid value for commentVisibility parameter: " + visibilityParam);
                    }
                    Comment newComment = getRepository().getCommentManager().addComment(getDocumentId(), getBranchId(), getLanguageId(), visibility, text);
                    appleResponse.redirectTo(EncodingUtil.encodePath(getMountPoint() + "/" + getSiteConf().getName() + "/" + getRequestedNavigationPath()) + ".html?showComments=true" + getVariantParams() + "#daisycomment" + newComment.getId());
                }
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        } else if (action.equals("deleteComment")) {
            if (request.getMethod().equals("POST")) {
                long commentId = RequestUtil.getLongParameter(request, "commentId");
                getRepository().getCommentManager().deleteComment(getDocumentId(), getBranchId(), getLanguageId(), commentId);
                appleResponse.redirectTo(EncodingUtil.encodePath(getMountPoint() + "/" + getSiteConf().getName() + "/" + getRequestedNavigationPath()) + ".html?showComments=true" + getVariantParams() + "#daisycomments");
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        } else if (action.equals("addToBasket")) {
            if (request.getMethod().equals("POST")) {
                long versionId = getVersionId();
                if (versionId == 0) { // at this point requestedVersionId either holds a date or it is invalid.  
                    versionId = getRepository().getDocument(getDocumentId(), getBranch(), getLanguage(), false)
                        .getVersionId(VersionMode.get(getRequestedVersion()));
                }
                DocumentBasketEntry entry = new DocumentBasketEntry(getDocumentId(), getBranch(), getLanguage(), versionId, "");
                DocumentBasketHelper.updateDocumentNames(entry, request, getRepository());
                DocumentBasketHelper.getDocumentBasket(request, true).appendEntry(entry);
                showDocument(new I18nMessage("document-in-basket"), displayParams);
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        } else {
            throw new Exception("Unsupported action parameter value: \"" + action + "\".");
        }
    }

    private void handleDocumentDeleteRequest() throws Exception {
        String type = request.getParameter("type");
        if (type == null) {
            Document document = getRepository().getDocument(getDocumentId(), getBranchId(), getLanguageId(), true);
            Repository repository = getRepository();

            String lastQuery = "select id, branch, language, name where LinksToVariant(" + QueryHelper.formatString(getDocumentId()) + "," + getBranchId() + "," + getLanguageId() + ") option point_in_time = 'last'";
            SearchResultDocument lastLinkingDocs = repository.getQueryManager().performQuery(lastQuery, frontEndContext.getLocale());
            
            String liveQuery = "select id, branch, language, name where LinksToVariant(" + QueryHelper.formatString(getDocumentId()) + "," + getBranchId() + "," + getLanguageId() + ") option point_in_time = 'live'";
            SearchResultDocument liveLinkingDocs = repository.getQueryManager().performQuery(liveQuery, frontEndContext.getLocale());
            
            AclResultInfo aclResultInfo = repository.getAccessManager().getAclInfoOnLive(repository.getUserId(), repository.getActiveRoleIds(), getDocumentId(), getBranchId(), getLanguageId());
            MultiXMLizable data = new MultiXMLizable(
                    new XmlObjectXMLizable(document.getXml()),
                    new WrappedXMLizable("last", new XmlObjectXMLizable(lastLinkingDocs)),
                    new WrappedXMLizable("live", new XmlObjectXMLizable(liveLinkingDocs)),
                    new XmlObjectXMLizable(aclResultInfo.getXml()));

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("pageContext", frontEndContext.getPageContext());
            viewData.put("pageXml", data);
            viewData.put("navigationPath", getRequestedNavigationPath());
            viewData.put("variantParams", getVariantParams());
            viewData.put("variantQueryString", getVariantQueryString());

            GenericPipeConfig pipeConf = new GenericPipeConfig();
            pipeConf.setTemplate("resources/xml/deletedocument.xml");
            pipeConf.setStylesheet("daisyskin:xslt/deletedocument.xsl");
            viewData.put("pipeConf", pipeConf);

            appleResponse.sendPage("internal/genericPipe", viewData);
        } else {
            if (request.getMethod().equals("POST")) {
                if (type.equals("variant-permanent")) {
                    getRepository().deleteVariant(getDocumentId(), getBranchId(), getLanguageId());
                } else if (type.equals("permanent")) {
                    getRepository().deleteDocument(getDocumentId());
                } else if (type.equals("retire")) {
                    Document document = getRepository().getDocument(getDocumentId(), getBranchId(), getLanguageId(), true);
                    document.setRetired(true);
                    document.save(false);
                } else {
                    throw new Exception("Unsupported value for type parameter: " + type);
                }
                String returnTo = request.getParameter("returnTo");
                if (returnTo != null) {
                    ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
                } else {
                    ResponseUtil.safeRedirect(appleRequest, appleResponse, EncodingUtil.encodePath(getMountPoint() + "/" + getSiteConf().getName() + "/" + getSiteConf().getHomePage()));
                }
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        }
    }

    private void handleChangeOwnerRequest() throws Exception {
        Form form = FormHelper.createForm(getServiceManager(), "resources/form/changeowner_definition.xml");
        Locale locale = frontEndContext.getLocale();

        if (request.getMethod().equals("POST")) {
            FormContext formContext = new FormContext(request, locale);
            boolean finished = form.process(formContext);
            if (finished) {
                String newOwnerLogin = (String)form.getChild("newOwnerLogin").getValue();
                long newOwnerId;
                try {
                    newOwnerId = getRepository().getUserManager().getPublicUserInfo(newOwnerLogin).getId();
                } catch (RepositoryException e) {
                    throw new RuntimeException("Error trying to retrieve user ID for user login \"" + newOwnerLogin + "\".", e);
                }
                Document document = getRepository().getDocument(getDocumentId(), getBranchId(), getLanguageId(), true);
                document.setOwner(newOwnerId);
                document.save(false);
                appleResponse.redirectTo(EncodingUtil.encodePath(getMountPoint() + "/" + getSiteConf().getName() + "/" + getRequestedNavigationPath() + ".html") + getVariantQueryString());
                return;
            }
        }

        Document document = getRepository().getDocument(getDocumentId(), getBranchId(), getLanguageId(), true);
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("document", document);
        viewData.put("documentPath", getMountPoint() + "/" + getSiteConf().getName() + "/" + getRequestedNavigationPath());
        viewData.put("CocoonFormsInstance", form);
        viewData.put("variantParams", getVariantParams());
        viewData.put("variantQueryString", getVariantQueryString());
        viewData.put("locale", locale);
        appleResponse.sendPage("Form-changeowner-Pipe", viewData);
    }

    /**
     * @param message an optional message to show at top of the document.
     */
    protected void showDocument(XMLizable message, Map displayParams) throws Exception {
        // back compatibility support for "navigationType" parameter, can be removed for Daisy 1.4
        String navigationType = request.getParameter("navigationType");
        String defaultLayoutType = navigationType != null ? navigationType : getLayoutType();
        // TODO pdf rendering never requires a navigation tree -- might need better way to influence this
        String layoutType = "xslfo".equals(publishType) ? "plain" : defaultLayoutType;
        PageContext pageContext = frontEndContext.getPageContext(layoutType);
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", pageContext);
        viewData.put("variantParams", getVariantParams());
        viewData.put("variantQueryString", getVariantQueryString());
        viewData.put("documentId", getDocumentId());
        viewData.put("branch", String.valueOf(getBranchId()));
        viewData.put("language", String.valueOf(getLanguageId()));
        viewData.put("version", getVersionName(getVersionId()));
        viewData.put("localeAsString", request.getAttribute("localeAsString"));
        viewData.put("activePath", getRequestedNavigationPath());
        if (message != null)
            viewData.put("pageMessage", message);
        if (displayParams != null)
            viewData.put("displayParams", displayParams.entrySet());

        SaxBuffer publisherResponse;
        try {
            publisherResponse = performPublisherRequest("documentpage", viewData);
        } catch (GlobalPublisherException e) {
            if (e.getCause() instanceof DocumentVariantNotFoundException) {
                DocumentVariantNotFoundException dvne = (DocumentVariantNotFoundException)e.getCause();
                // Only if the variant-not-found is caused by the document the user requested
                // (rather then e.g. a document aggregated by a publisher request), the variant
                // selection page should be shown.
                if (dvne.getDocumentId().equals(getDocumentId()) && dvne.getBranchId() == getBranchId() && dvne.getLanguageId() == getLanguageId()) {
                    showVariantNotFoundPage(getDocumentId());
                    return;
                } else {
                    throw e;
                }
            } else {
                throw e;
            }
        }

        viewData.put("publisherResponse", publisherResponse);
        viewData.put("requestedVersion", getRequestedVersion());

        appleResponse.sendPage(publishType + "-DocumentPipe", viewData);
    }

    private String getVersionName(long versionId) {
        if (versionId == -3)
            return "default";
        if (versionId == -1)
            return "live";
        else if (versionId == -2)
            return "last";
        else if (versionId == 0)
            return getRequestedVersion();
        else
            return String.valueOf(versionId);
    }

    protected void showVersionOverview() throws Exception {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("variantParams", getVariantParams());
        viewData.put("variantQueryString", getVariantQueryString());
        viewData.put("documentId", String.valueOf(getDocumentId()));
        viewData.put("branch", String.valueOf(getBranchId()));
        viewData.put("language", String.valueOf(getLanguageId()));
        viewData.put("localeAsString", request.getAttribute("localeAsString"));
        viewData.put("activePath", getRequestedNavigationPath());
        if  (request.getAttribute("warnLockUserName") != null) {
            viewData.put("warnLockUserName", request.getAttribute("warnLockUserName"));
            viewData.put("attemptedMakeLiveVersionId", request.getAttribute("attemptedMakeLiveVersionId").toString());
        }
        if  (request.getAttribute("") != null) {
            viewData.put("attemptedMakeLiveVersionId", request.getAttribute("attemptedMakeLiveVersionId").toString());
        }
        GenericPipeConfig pipeConf = GenericPipeConfig.templatePipe("resources/xml/version_overview.xml");
        pipeConf.setStylesheet("daisyskin:xslt/version_overview.xsl");
        viewData.put("pipeConf", pipeConf);

        SaxBuffer publisherResponse = performPublisherRequest("versionspage", viewData);
        viewData.put("pageXml", publisherResponse);


        appleResponse.sendPage("internal/genericPipe", viewData);
    }

    protected void showLiveVersionOverview() throws Exception {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("variantParams", getVariantParams());
        viewData.put("variantQueryString", getVariantQueryString());
        viewData.put("documentId", String.valueOf(getDocumentId()));
        viewData.put("branch", String.valueOf(getBranchId()));
        viewData.put("language", String.valueOf(getLanguageId()));
        viewData.put("localeAsString", request.getAttribute("localeAsString"));
        viewData.put("activePath", getRequestedNavigationPath());
        GenericPipeConfig pipeConf = GenericPipeConfig.templatePipe("resources/xml/liveversion_overview.xml");
        pipeConf.setStylesheet("daisyskin:xslt/liveversion_overview.xsl");
        viewData.put("pipeConf", pipeConf);
        viewData.put("readOnly", true);

        SaxBuffer publisherResponse = performPublisherRequest("versionspage", viewData);
        viewData.put("pageXml", publisherResponse);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }

    protected void handleVersionRequest() throws Exception {
        Map<String, String> displayParams = new HashMap<String, String>();        
        boolean landscape = RequestUtil.getBooleanParameter(request, "landscape", false);
        if (landscape)
            displayParams.put("landscape", "true");
        
        if (request.getMethod().equals("POST")) {
            RemoteRepositoryImpl repository = getRepository();
            Document document = repository.getDocument(getDocumentId(), getBranchId(), getLanguageId(), true);
            Version version;
            if (getVersionId() == 0) {
                version = document.getVersion(VersionMode.get(getRequestedVersion()));
            } else {
                version = document.getVersion(getVersionId());    
            }
            
            VersionState previousState = version.getState();
            
            String newStateString = request.getParameter("newState");
            if (newStateString != null) {
                VersionState newState = VersionState.fromString(newStateString);
                version.setState(newState);
            }
            
            String newSyncedWithLanguageString = request.getParameter("newSyncedWithLanguageId");
            String newSyncedWithVersionString = request.getParameter("newSyncedWithVersionId");
            if (newSyncedWithLanguageString != null) {
                long syncedWithLanguageId = Long.parseLong(newSyncedWithLanguageString);
                long syncedWithVersionId = newSyncedWithVersionString != null ? Long.parseLong(newSyncedWithVersionString) : -1;
                version.setSyncedWith(syncedWithLanguageId, syncedWithVersionId);
            }

            String newChangeTypeString = request.getParameter("newChangeType");
            if (newChangeTypeString != null) {
                ChangeType newChangeType = ChangeType.fromString(newChangeTypeString);
                version.setChangeType(newChangeType);
            }

            String newChangeComment = request.getParameter("newChangeComment");
            if (newChangeTypeString != null) {
                version.setChangeComment(newChangeComment);
            }
            
            try {
                version.save();
            } catch (AccessException e) {
                // user is not allowed to publish: ignore
                version.setState(previousState);
                version.save();
            }
            
            boolean showLockWarning = false;
            long requestedLiveVersionId = 0;
            if ("true".equals(request.getParameter("makeLive"))) {
                requestedLiveVersionId = getVersionId();
            } else if ("false".equals(request.getParameter("makeLive"))) {
            	requestedLiveVersionId = -2;
            } else if (version.getId() == document.getLiveVersionId()) {
                requestedLiveVersionId = -1;
            }
            LockInfo lockInfo = null;
            if (requestedLiveVersionId != 0) {
                boolean forceChangeLive = "true".equals(request.getParameter("forceChangeLive"));
                lockInfo = document.getLockInfo(false);
                if (forceChangeLive || !lockInfo.hasLock() || lockInfo.getUserId() == repository.getUserId()) {
                    document.setRequestedLiveVersionId(requestedLiveVersionId);
                    document.save(false);
                } else {
                    showLockWarning = true;
                }
            }
            
            String returnTo = request.getParameter("returnTo");
            if (showLockWarning) {
                request.setAttribute("warnLockUserName", repository.getUserManager().getUserDisplayName(lockInfo.getUserId()));
                request.setAttribute("requestedLiveVersionId", requestedLiveVersionId);
                showVersionOverview();
            } else if (returnTo != null){
                ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
            } else {
                ResponseUtil.safeRedirect(appleRequest, appleResponse, EncodingUtil.encodePath(getMountPoint() + "/" + getSiteConf().getName() + "/" + getRequestedNavigationPath() + "/versions.html") + getVariantQueryString());
            }
        } else {
            showDocument(null, displayParams);
        }
    }

    protected void showReferrers() throws Exception {
        boolean linksInLastVersion = RequestUtil.getBooleanParameter(request, "linksInLastVersion", false);

        StringBuilder query = new StringBuilder();
        query.append("select id, branch, language, name where LinksToVariant(");
        query.append(QueryHelper.formatString(getDocumentId()));
        query.append(',').append(getBranchId()).append(',').append(getLanguageId()).append(')');
        if (linksInLastVersion) 
            query.append(" option point_in_time = 'last'");

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("variantParams", getVariantParams());
        viewData.put("variantQueryString", getVariantQueryString());
        viewData.put("referrersQuery", query.toString());
        viewData.put("linksInLastVersion", String.valueOf(linksInLastVersion));
        viewData.put("documentId", String.valueOf(getDocumentId()));
        viewData.put("branch", String.valueOf(getBranchId()));
        viewData.put("language", String.valueOf(getLanguageId()));
        viewData.put("localeAsString", request.getAttribute("localeAsString"));
        viewData.put("activePath", getRequestedNavigationPath());

        SaxBuffer publisherResponse = performPublisherRequest("referrerspage", viewData);
        viewData.put("publisherResponse", publisherResponse);

        GenericPipeConfig pipeConf = new GenericPipeConfig();
        pipeConf.setTemplate("resources/xml/referrerspage.xml");
        pipeConf.setStylesheet("daisyskin:xslt/referrers.xsl");
        viewData.put("pipeConf", pipeConf);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }

    protected void showDiff() throws Exception {
        String otherDocumentId = RequestUtil.getStringParameter(request, "otherDocumentId", getDocumentId());
        String otherVersionId = RequestUtil.getStringParameter(request, "otherVersion");
        long otherBranchId = RequestUtil.getBranchId(request.getParameter("otherBranch"), getBranchId(), getRepository());
        long otherLanguageId = RequestUtil.getLanguageId(request.getParameter("otherLanguage"), getLanguageId(), getRepository());

        String contentDiffType = request.getParameter("contentDiffType");
        if (contentDiffType == null)
            contentDiffType = "html";

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("variantParams", getVariantParams());
        viewData.put("variantQueryString", getVariantQueryString());
        viewData.put("documentId", String.valueOf(getDocumentId()));
        viewData.put("branch", String.valueOf(getBranchId()));
        viewData.put("language", String.valueOf(getLanguageId()));
        viewData.put("version", getVersionName(getVersionId()));
        viewData.put("localeAsString", request.getAttribute("localeAsString"));
        viewData.put("activePath", getRequestedNavigationPath());
        viewData.put("otherDocumentId", otherDocumentId);
        viewData.put("otherBranch", String.valueOf(otherBranchId));
        viewData.put("otherLanguage", String.valueOf(otherLanguageId));
        viewData.put("otherVersionId", String.valueOf(otherVersionId));
        viewData.put("contentDiffType", contentDiffType);

        SaxBuffer publisherResponse = performPublisherRequest("diffpage", viewData);
        viewData.put("pageXml", publisherResponse);
        viewData.put("documentKey", getVariantKey()); // for the link transformer

        GenericPipeConfig pipeConfig = GenericPipeConfig.stylesheetPipe("daisyskin:xslt/diff.xsl");
        pipeConfig.setTransformLinks(true);
        viewData.put("pipeConf", pipeConfig);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }

    private void showVariantNotFoundPage(String documentId) throws Exception {
        AvailableVariants variants = getRepository().getAvailableVariants(documentId);

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageXml", new MultiXMLizable(new XmlObjectXMLizable(variants.getXml(true)), new TaggedMessage("documentId", String.valueOf(documentId))));
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("pipeConf", GenericPipeConfig.stylesheetPipe("daisyskin:xslt/alternative_variants.xsl"));

        appleResponse.sendPage("internal/genericPipe", viewData);
    }

    private SaxBuffer performPublisherRequest(String name, Map params) throws Exception {
        String pipe = "internal/" + name + "_pubreq.xml";
        WikiPublisherHelper wikiPublisherHelper = new WikiPublisherHelper(request, getContext(), getServiceManager());
        return wikiPublisherHelper.performPublisherRequest(pipe, params, publishType);
    }
}
