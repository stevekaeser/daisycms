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
package org.outerj.daisy.navigation;

import org.outerj.daisy.repository.VariantKey;
import org.outerx.daisy.x10Navigationspec.NavigationLookupResultDocument;

/**
 * Encapsulates information about the result of a navigation tree lookup.
 * Either the lookup matched a document node in which case the variantKey
 * field will identify that document, and the navigationPath
 * field will contain the corresponding path in the navigation tree (the
 * same as the one provided when doing the lookup).
 *
 * <p>If the lookup matched
 * a group node or the lookup didn't match but the path ended on a document
 * ID and the document is found at another location in the tree of one
 * of the lookup variants, the redirect field will be true, the navigationPath
 * field will contain the path to where to redirect, and the lookupAlternativeName
 * will contain the name of the lookupAlternative which matched.
 *
 * <p>If the lookup didn't match and the path ended on a document ID but
 * the document ID does not occur in the tree, then the redirect field will
 * be false, the navigationPath will contain an empty string and the variantKey
 * field will contain a variant key with that document ID and the branch
 * and language of the navigation tree document.
 *
 * <p>In all other cases, the notFound field will be true.
 */
public class NavigationLookupResult {
    private VariantKey variantKey = null;
    private String lookupAlternativeName;
    private String navigationPath;
    private boolean redirect;
    private boolean notFound;

    private NavigationLookupResult() {
    }

    public static NavigationLookupResult createMatchResult(VariantKey variantKey, String navigationPath) {
        NavigationLookupResult lookupResult = new NavigationLookupResult();
        lookupResult.variantKey = variantKey;
        lookupResult.navigationPath = navigationPath;
        lookupResult.redirect = false;
        return lookupResult;
    }

    public static NavigationLookupResult createRedirectResult(String lookupAlternativeName, String navigationPath, VariantKey variantKey) {
        NavigationLookupResult lookupResult = new NavigationLookupResult();
        lookupResult.lookupAlternativeName = lookupAlternativeName;
        lookupResult.navigationPath = navigationPath;
        lookupResult.redirect = true;
        lookupResult.variantKey = variantKey;
        return lookupResult;
    }

    public static NavigationLookupResult createNotFoundResult() {
        NavigationLookupResult lookupResult = new NavigationLookupResult();
        lookupResult.notFound = true;
        return lookupResult;
    }

    public static NavigationLookupResult createFromXml(NavigationLookupResultDocument.NavigationLookupResult resultXml) {
        NavigationLookupResult lookupResult = new NavigationLookupResult();
        if (resultXml.isSetNotFound())
            lookupResult.notFound = resultXml.getNotFound();
        if (resultXml.isSetRedirect())
            lookupResult.redirect = resultXml.getRedirect();
        if (resultXml.isSetDocumentId()) {
            lookupResult.variantKey = new VariantKey(resultXml.getDocumentId(), resultXml.getBranchId(), resultXml.getLanguageId());
        }
        if (resultXml.isSetPath())
            lookupResult.navigationPath = resultXml.getPath();
        if (resultXml.isSetLookupAlternativeName())
            lookupResult.lookupAlternativeName = resultXml.getLookupAlternativeName();

        return lookupResult;
    }

    public VariantKey getVariantKey() {
        return variantKey;
    }

    public String getNavigationPath() {
        return navigationPath;
    }

    public boolean isRedirect() {
        return redirect;
    }

    public boolean isNotFound() {
        return notFound;
    }

    public String getLookupAlternativeName() {
        return lookupAlternativeName;
    }

    public NavigationLookupResultDocument getXml() {
        NavigationLookupResultDocument resultDocument = NavigationLookupResultDocument.Factory.newInstance();
        NavigationLookupResultDocument.NavigationLookupResult resultXml = resultDocument.addNewNavigationLookupResult();

        if (notFound)
            resultXml.setNotFound(true);
        if (redirect)
            resultXml.setRedirect(true);
        if (variantKey != null) {
            resultXml.setDocumentId(variantKey.getDocumentId());
            resultXml.setBranchId(variantKey.getBranchId());
            resultXml.setLanguageId(variantKey.getLanguageId());
        }
        if (navigationPath != null)
            resultXml.setPath(navigationPath);
        if (lookupAlternativeName != null)
            resultXml.setLookupAlternativeName(lookupAlternativeName);

        return resultDocument;
    }
}
