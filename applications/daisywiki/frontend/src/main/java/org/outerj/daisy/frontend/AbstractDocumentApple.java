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

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.EncodingUtil;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.components.siteconf.SitesManager;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.navigation.NavigationLookupResult;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.ResourceNotFoundException;

/**
 * This class provides basic handling for all types of request related to a specific document,
 * more specifically the determination of the document variant (id, branch, language) to be
 * handled based on the URL path, the SiteConf and request parameters.
 */
public abstract class AbstractDocumentApple extends AbstractDaisyApple implements Serviceable {
    private ServiceManager serviceManager;
    private String documentId;
    private long branchId;
    private long languageId;
    private String branch;
    private String language;
    private long versionId;
    private String requestedVersion;
    private String variantParams;
    private String variantQueryString;
    private SiteConf siteConf;
    private RemoteRepositoryImpl repository;
    private String requestedNavigationPath;

    public final void service(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    protected final void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (needsInitialisation()) {
            siteConf = frontEndContext.getSiteConf();
            repository = (RemoteRepositoryImpl)frontEndContext.getRepository();

            //
            // Determine document ID
            //

            requestedNavigationPath = appleRequest.getSitemapParameter("navigationPath");
            if (requestedNavigationPath == null)
                throw new Exception("Missing sitemap parameter: navigationPath");

            long requestedBranchId = RequestUtil.getBranchId(request, -1, repository);
            long requestedLanguageId = RequestUtil.getLanguageId(request, -1, repository);

            NavigationManager navigationManager = (NavigationManager)repository.getExtension("NavigationManager");
            NavigationLookupResult lookupResult = navigationManager.lookup(requestedNavigationPath, requestedBranchId, requestedLanguageId,
                    siteConf.getNavigationLookupAlternatives(frontEndContext.getVersionMode()), true);
            if (lookupResult.isNotFound()) {
                throw new ResourceNotFoundException("Path not found: " + requestedNavigationPath);
            } else if (lookupResult.isRedirect() && request.getMethod().equals("GET")) {
                String suffix = appleRequest.getSitemapParameter("suffix");
                if (suffix == null)
                    suffix = ".html";
                String queryString = request.getQueryString();

                // If query string contained a branch and/or language request parameter, and
                // they are not needed anymore (because they are the default in the site we are moving too),
                // remove them
                if (queryString != null) {
                    SiteConf newSiteConf;
                    SitesManager sitesManager = (SitesManager)serviceManager.lookup(SitesManager.ROLE);
                    try {
                        newSiteConf = sitesManager.getSiteConf(lookupResult.getLookupAlternativeName());
                    } finally {
                        serviceManager.release(sitesManager);
                    }
                    if (newSiteConf.getBranchId() == requestedBranchId)
                        queryString = removeQueryParameter(queryString, "branch");
                    if (newSiteConf.getLanguageId() == requestedLanguageId)
                        queryString = removeQueryParameter(queryString, "language");
                }

                queryString = queryString == null || queryString.length() == 0 ? "" : "?" + queryString;
                appleResponse.redirectTo(EncodingUtil.encodePathQuery(getMountPoint() + "/" + lookupResult.getLookupAlternativeName() + lookupResult.getNavigationPath() + suffix + queryString));
                return;
            } else if (lookupResult.isRedirect() && lookupResult.getVariantKey() == null) {
                throw new Exception("Requested path \"" + requestedNavigationPath + "\" redirects to \"" + lookupResult.getNavigationPath() + "\" but request method is " + request.getMethod() + " thus cannot redirect.");
            }

            VariantKey variantKey = lookupResult.getVariantKey();
            documentId = variantKey.getDocumentId();

            //
            // Set the selected branch and/or language
            //

            setBranchAndLanguage(variantKey.getBranchId(), variantKey.getLanguageId());

            //
            // Determine version
            //

            requestedVersion = appleRequest.getSitemapParameter("versionId", null);
            try {
                if (requestedVersion == null)
                    versionId = -3; // -3 == default according to publisher version mode
                else if (requestedVersion.equalsIgnoreCase("live"))
                    versionId = -1; // -1 == live version
                else if (requestedVersion.equalsIgnoreCase("last"))
                    versionId = -2; // -2 == last version
                else try {
                    versionId = Long.parseLong(requestedVersion);
                } catch (NumberFormatException nfe) {}
            } catch (NumberFormatException e) {
                throw new Exception("Invalid version id: " + requestedVersion);
            }
        }

        processDocumentRequest(appleRequest, appleResponse);
    }

    protected void setBranchAndLanguage(long branchId, long languageId) throws Exception {
        this.branchId = branchId;
        this.languageId = languageId;
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

    protected abstract void processDocumentRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception;

    protected abstract boolean needsInitialisation();

    protected String getDocumentId() {
        return documentId;
    }

    protected long getBranchId() {
        return branchId;
    }

    protected long getLanguageId() {
        return languageId;
    }

    protected String getBranch() {
        return branch;
    }

    protected String getLanguage() {
        return language;
    }

    protected long getVersionId() {
        return versionId;
    }
    
    protected VariantKey getVariantKey() {
        return new VariantKey(documentId, branchId, languageId);
    }

    /**
     * The desired version as specified in the request, can be null (for default).
     */
    protected String getRequestedVersion() {
        return requestedVersion;
    }

    protected String getVariantParams() {
        return variantParams;
    }

    protected String getVariantQueryString() {
        return variantQueryString;
    }

    protected SiteConf getSiteConf() {
        return siteConf;
    }

    protected RemoteRepositoryImpl getRepository() {
        return repository;
    }

    protected ServiceManager getServiceManager() {
        return serviceManager;
    }

    protected String getRequestedNavigationPath() {
        return requestedNavigationPath;
    }

    /**
     * Removes a parameter from a query string (assuming at most one occurence).
     */
    private String removeQueryParameter(String queryString, String parameterName) {
        int pos = queryString.indexOf(parameterName);
        if (pos != -1
                && (pos == 0 || queryString.charAt(pos - 1) == '&')
                && queryString.length() > pos + parameterName.length()
                && queryString.charAt(pos + parameterName.length()) == '=') {
            
            int endPos = queryString.indexOf('&', pos + parameterName.length() + 1);
            String firstPart = pos > 0 ? queryString.substring(0, pos - 1) : "";
            if (endPos != -1) {
                return firstPart + queryString.substring(pos == 0 ? endPos + 1 : endPos);
            } else { // last paramter in the query string
                return firstPart;
            }
        }
        return queryString;
    }
}
