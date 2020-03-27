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

import java.util.Locale;

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * The NavigationManager generates hierarchical navigation trees from an XML
 * navigation tree specification. The navigation tree specification is read from
 * a Daisy repository document (from the content of the "NavigationData" part) and
 * can contain a literal enumeration of documents but also queries.
 *
 * <p>The navigation trees support quite some features, we refer to the Daisy documentation
 * for more information on this.
 *
 * <p>This is an optional repository extension component.
 *
 * <p>The NavigationManager is obtained from the {@link org.outerj.daisy.repository.Repository Repository} as
 * follows:
 *
 * <pre>
 * NavigationManager navManager = (NavigationManager)repository.getExtension("NavigationManager");
 * </pre>
 *
 * <p>In the remote repository API, the NavigationManager extension can be registered as follows:
 *
 * <pre>
 * RemoteRepositoryManager repositoryManager = ...;
 * repositoryManager.registerExtension("NavigationManager",
 *     new Packages.org.outerj.daisy.navigation.clientimpl.RemoteNavigationManagerProvider());
 * </pre>
 */
public interface NavigationManager {
    /**
     * Generates a navigation tree as XML.
     *
     * <p>If the document specified in the NavigationParams as 'activeDocument' cannot be found,
     * a 'closed' tree will be produced (i.e. a tree generated to node depth level 1) if contextualized is true.
     * If contextualized is false, a full tree will be genereated.
     *
     * <p>The same holds if the the 'activeDocument', or one of the nodes on the path leading to
     * that document, is not readable by the current user and role.
     *
     * @param contentHandler resulting XML will be pushed on this ContentHandler
     * @param activeDocument the document that should be marked as 'selected' (null for none)
     * @param handleErrors if true, the navigation tree generation output will first be buffered so that
     *                     in case an error occurs, some information about this error can be streamed
     *                     instead of the normal navigation tree outpus
     * @param allowOld if true, the navigation tree may be slightly outdated (if there have been recent repository changes)
     *                 if false, you always get the correct up-to-date navigation tree (but you may
     *                 have to wait a bit longer).
     * @throws NavigationException in case some error occurs
     * @throws SAXException never thrown by us, but unavoidable when pushing things to a ContentHandler.
     */
    public void generateNavigationTree(ContentHandler contentHandler, NavigationParams navigationParams,
            VariantKey activeDocument, boolean handleErrors, boolean allowOld)
            throws RepositoryException, SAXException;

    /**
     * Same as generateNavigationTree(contentHandler, navigationParams, activeDocument, handleErrors, false).
     * See {@link #generateNavigationTree(ContentHandler, NavigationParams, VariantKey, boolean, boolean);
     */
    public void generateNavigationTree(ContentHandler contentHandler, NavigationParams navigationParams,
            VariantKey activeDocument, boolean handleErrors)
            throws RepositoryException, SAXException;

    /**
     * Generates a navigation tree based on the XML given in the navigationTreeXml parameter.
     * This allows to generate navigation trees from an XML description not stored in the repository.
     *
     * <p>The branch and language parameters should be the branch and language that are or will be used
     * on the site where this navigation tree is published (and thus the branch and language under which
     * the navigation tree would normally be stored in the repository). They are used as default branch and language
     * there where one is needed (eg. document and import nodes).
     */
    public void generatePreviewNavigationTree(ContentHandler contentHandler, String navigationTreeXml,
            long branchId, long languageId, Locale locale) throws RepositoryException, SAXException;

    /**
     * Same as the other generatePreviewNavigationTree method, but without Locale parameter
     * (defaults to Locale.US), for backwards compatibility.
     */
    public void generatePreviewNavigationTree(ContentHandler contentHandler, String navigationTreeXml,
            long branchId, long languageId) throws RepositoryException, SAXException;

    /**
     * Resolves a path (of the form id/id/...) against a (series of) navigation tree(s),
     * giving a certain result. See {@link NavigationLookupResult}.
     *
     * <p>This method is best understood with the structure of the Daisy Wiki, where each 'site'
     * is associated with a collection and a navigation tree, and the branch and language of the
     * navigation tree document correspond to the 'default' branch and language for that site.
     *
     * <p>The algortihm followed is described below.
     *
     * <p>Is the navigationPath found in the navigation tree of the first lookup alternative?
     *
     * <ul>
     *
     * <li>1. If yes, what is the type of node it addresses?
     *
     * <ul>
     * <li>1-a. If it is a group node, set the result to a redirect to the first (depth-first) document child
     * descendant of the group node. If the group node does not have any such children, set the result to 'path not found'.
     *
     * <li>1-b. If it is a document node, check if the branch and language of the navtree node match the
     * requested branch and language (if not -1)
     *
     * <ul>
     *  <li>1-b-i.  If they match, set the result to 'match'.
     *  <li>1-b-ii. If no, search among the lookup alternatives.
     * </ul>
     * </ul>
     *
     * <li>2. If no, then does the path end on a numeric ID?
     *
     * <ul>
     *  <li>2-a. If no, set the result to 'path not found'.
     *  <li>2-b. If yes, follow the following algorithm:
     *  <ul>
     *   <li>Determine the branch and language (is either the requested branch and language, or those of the navtree
     *       of the first lookup alternative)
     *   <li>Retrieve the collections the document belongs to. If this fails for whathever reason (usually permissions
     *       or existence), just set the result to 'match'. When the front end will then try
     *       to display the document, it will run into this error and show it to the user.
     *   <li>Run over the lookup alternatives:
     *  <ul>
     *  <li>If the collection, navtree-branch and navtree-language of a lookup alternative match those of the document:
     *  <ul>
     *   <li>If this is the first match, remember it as "firstMatch"
     *   <li>If found in the navtree of this lookup alternative, set the result to a redirect to this lookup alternative
     *       and navigation path. Done.
     *  </ul>
     *  <li>If we ran over all the lookup alternatives and none matched:
     *  <ul>
     *   <li>If 'firstMatch' is set and is different from first lookup alternative, set result to redirect to the lookup alternative
     *   <li>Otherwise set result to 'match' (as if it was found at whatever location that was requested).
     *  </ul>
     * </ul>
     * </ul>
     *
     * @param requestedBranchId if the branch of the document differs the one in the navtree node, this argument
     *               specifies the branch ID, otherwise supply -1.
     * @param requestedLanguageId dito as for requestedBranchId
     * @param lookupAlternatives should at least contain one entry
     */
    public NavigationLookupResult lookup(String navigationPath, long requestedBranchId, long requestedLanguageId,
            LookupAlternative[] lookupAlternatives, boolean allowOld) throws RepositoryException;


    /**
     * Same as lookup(navigationPath, requestedBranchId, requestedLanguageId, lookupAlternatives, false).
     * See {@link #lookup(String, long, long, LookupAlternative[], boolean);
     */
    public NavigationLookupResult lookup(String navigationPath, long requestedBranchId, long requestedLanguageId,
            LookupAlternative[] lookupAlternatives) throws RepositoryException;

    /**
     * Searches a  possible path in the navigation tree for the given document, or null if none
     * exists.
     */
    public String reverseLookup(VariantKey document, VariantKey navigationDoc, VersionMode versionMode, boolean allowOld) throws RepositoryException;

    /**
     * Same as reverseLookup(document, navigationDoc, versionMode, false)
     * See {@link #reverseLookup(VariantKey, VariantKey, VersionMode, boolean);
     */
    public String reverseLookup(VariantKey document, VariantKey navigationDoc, VersionMode versionMode) throws RepositoryException;

    /**
     * Same as the other reverseLookup method, but defaults to the live version mode.
     */
    public String reverseLookup(VariantKey document, VariantKey navigationDoc, boolean allowOld) throws RepositoryException;

    /**
     * Same as reverseLookup(document, navigationDoc, VersionMode.LIVE, false)
     * See {@link #reverseLookup(VariantKey, VariantKey, boolean);
     */
    public String reverseLookup(VariantKey document, VariantKey navigationDoc) throws RepositoryException;

}
