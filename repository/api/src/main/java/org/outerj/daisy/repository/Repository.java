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
package org.outerj.daisy.repository;

import java.io.InputStream;

import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.comment.CommentManager;
import org.outerj.daisy.repository.namespace.NamespaceManager;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerx.daisy.x10.UserInfoDocument;

/**
 * Start point for a user to access the repository.
 *
 * <p>An instance of this object is obtained from the {@link RepositoryManager} and
 * is contextualized for a certain user. Thus instead of having to supply
 * credentials to each method, you authenticate once via the RepositoryManager
 * and can then do all further operations on this Repository object.
 *
 * <p>A Repository object should not be assumed to be thread safe.
 */
public interface Repository {

    /**
     * Returns the namespace for the current repository. All entities
     * (documents) created in this repository are by default created in
     * this namespace.
     *
     * @since Daisy 2.0
     */
    String getNamespace();
    
    /**
     * Returns the namespace for the current repository based on the given 
     * document. Which namespace the document is attributed to is specified 
     * in the configuration
     *
     * @since Daisy 2.3
     */
    String getNamespace(Document document) throws RepositoryException;

    /**
     * Creates a new document. You need to supply:
     * <ul>
     *  <li>a name for the document (which is not required to be unique)
     *  <li>a document type id. This is the id of one of the document types defined in the
     * {@link RepositorySchema}.
     *  <li>a branch id
     *  <li>a language id
     * </ul>
     *
     * <p>The document will not be stored physically
     * in the repository until {@link Document#save()} is called. Thus
     * calling this method has no permanent side effects.
     */
    Document createDocument(String name, long documentTypeId, long branchId, long languageId);

    /**
     * Same as {@link #createDocument(String, long, long, long)} but takes names instead
     * of ids.
     */
    Document createDocument(String name, String documentTypeName, String branchName, String languageName);

    /**
     * Same as {@link #createDocument(String, long, long, long)} but assumes branch id 1
     * and language id 1.
     */
    Document createDocument(String name, long documentTypeId);

    /**
     * Same as {@link #createDocument(String, long)} but takes a document type
     * name instead of an id.
     */
    Document createDocument(String name, String documentTypeName);

    /**
     * Creates a new variant on a document. If the copyContent argument is true,
     * the new variant will be immediately persisted and its first version will
     * be initialiased with the data from the start variant. The start variant
     * and version will also be stored in the variant (retrievable via
     * {@link Document#getVariantCreatedFromBranchId()} etc. methods).
     * If copyContent is false, a document object for the new variant will
     * be returned, with no data copied from the start variant (except for
     * the document name), and the new variant will not yet be persisted
     * (i.o.w. you need to call save on the returned Document object to do
     * this). Thus using copyContent = false allows to create a variant
     * from scratch, while copyContent = true branches of from an existing
     * variant.
     *
     * @param startVersionId -1 for last version, -2 for live version
     */
    Document createVariant(String documentId, long startBranchId, long startLanguageId, long startVersionId, long newBranchId, long newLanguageId, boolean copyContent) throws RepositoryException;

    Document createVariant(String documentId, String startBranchName, String startLanguageName, long startVersionId, String newBranchName, String newLanguageName, boolean copyContent) throws RepositoryException;

    /**
     * Gets a document from the repository.
     *
     * @param updateable if false, you won't be able to make modifications
     *                   to the document (and thus to save it). The repository
     *                   can return a cached copy in this case.
     *
     * @throws DocumentNotFoundException in case the document does not exist
     * @throws DocumentVariantNotFoundException in case the document exists, but the variant not
     * @throws org.outerj.daisy.repository.namespace.NamespaceNotFoundException in case the document ID contains an invalid namespace
     * @throws DocumentReadDeniedException if read access to the document is denied.
     */
    Document getDocument(String documentId, long branchId, long languageId, boolean updateable) throws RepositoryException;

    /**
     * Gets a document from the repository.
     *
     * <p>In case the branch or language does not exist, this will throw a Branch/LanugageNotFoundexception.
     *
     * @param branchName a branch name, or a branch id as string
     * @param languageName a language name, or a language id as string
     */
    Document getDocument(String documentId, String branchName, String languageName, boolean updateable) throws RepositoryException;

    /**
     * Gets a document from the repository.
     *
     * <p>In case the branch or language ID specified in the VariantKey do not exist, this
     * will not throw a Branch/LanguageNotFoundException, rather a Document(Variant)NotFoundException.
     *
     */
    Document getDocument(VariantKey key, boolean updateable) throws RepositoryException;

    Document getDocument(String documentId, boolean updateable) throws RepositoryException;

    /**
     * @deprecated use {@link #getDocument(String, boolean)} instead.
     */
    Document getDocument(long documentId, boolean updateable) throws RepositoryException;

    /**
     * Makes sure the document ID is of the form "docSeqId-namespace", if the
     * namespace is missing the default repository namespace is added. It is not
     * checked whether the namespace in the document ID actually exists.
     * Whitespace around the input string is not trimmed, the presence of
     * such whitespace will cause the documentID to be considered invalid.
     *
     * <p>This is an utility method.
     *
     * @throws IllegalArgumentException if a null documentId is supplied
     * @throws InvalidDocumentIdException if the document ID is not validly structured.
     */
    String normalizeDocumentId(String documentId);

    /**
     * Gets the available variants of a document. This returns all variants, also the
     * variants the user may not have access too, and retired variants. Everyone can retrieve the list
     * of available variants of each document, there is no security constraint to
     * this. This information is not really sensitive, and access control works on
     * document variants and not on documents, so it would be a bit difficult to do this.
     */
    AvailableVariants getAvailableVariants(String documentId) throws RepositoryException;

    /**
     * Deletes a document permanently (unrecoverable) from the repository
     * (including all its variants).
     */
    void deleteDocument(String documentId) throws RepositoryException;

    /**
     * @see #deleteVariant(VariantKey)
     */
    void deleteVariant(String documentId, long branchId, long languageId) throws RepositoryException;

    /**
     * Deletes a document variant permanently (unrecoverable) from the repository.
     *
     * <p>To delete a document variant virtually, but not permanently, you can set it
     * retired (see {@link Document#setRetired(boolean)}).
     */
    void deleteVariant(VariantKey variantKey) throws RepositoryException;

    /**
     * Retrieves the specified blob without the need to go through the Document object.
     * Of course, all access control checks still apply.
     *
     * @throws DocumentReadDeniedException if read access to the document is denied.
     */
    InputStream getPartData(String documentId, long branchId, long languageId, long versionId, long partTypeId) throws RepositoryException;

    /**
     * Retrieves part data for the branch "main", language "default".
     */
    InputStream getPartData(String documentId, long versionId, long partTypeId) throws RepositoryException;

    RepositorySchema getRepositorySchema();

    AccessManager getAccessManager();

    QueryManager getQueryManager();

    CommentManager getCommentManager();

    VariantManager getVariantManager();

    /**
     * Returns the Collection Manager for this Repository.
     */
    CollectionManager getCollectionManager();

    /**
     * Returns the User Manager for this Repository
     */
    UserManager getUserManager();

    /**
     * @since Daisy 2.0
     */
    NamespaceManager getNamespaceManager();

    /**
     * Id of the user with who this Repository instance is associated.
     */
    long getUserId();

    /**
     * The name of the user with who this Repository instance is associated, the
     * same as returned from {@link org.outerj.daisy.repository.user.User#getDisplayName()}.
     */
    String getUserDisplayName();

    /**
     * The login of the user with who this Repository instance is associated.
     */
    String getUserLogin();

    /**
     * The roles of the user that are currently active. These can be changed
     * through {@link #setActiveRoleIds}.
     */
    long[] getActiveRoleIds();

    boolean isInRole(long roleId);

    boolean isInRole(String roleName);

    /**
     * Sets the active roles of the user.
     *
     * @param roleIds a subset of, or equal to, the roles returned by {@link #getAvailableRoles()}.
     */
    void setActiveRoleIds(long[] roleIds);

    /**
     * Returns the names of the active roles.
     */
    String[] getActiveRolesDisplayNames();

    /**
     * The id's of the available roles of the user.
     */
    long[] getAvailableRoles();

    /**
     * Changes the user's role for this Repository instance. This is the same as
     * calling {@link #setActiveRoleIds(long[])} with a one-length array.
     *
     * @param roleId a valid roleId, thus one of those returned by {@link #getAvailableRoles()}.
     */
    void switchRole(long roleId);

    /**
     * Returns an XML document containing some information about the user
     * with which this Repository instance is associated.
     */
    UserInfoDocument getUserInfoAsXml();

    /**
     * Add an event listener.
     *
     * <p>See also the comments in {@link RepositoryListener}.
     *
     * <p>Not all events are per-se also implemented in the repository client,
     * and for so far as they are, they only provide events for operations done
     * through that client, and not other ones happening on the server or through
     * other clients.
     *
     * <p>This listener functionality is mostly meant for internal use, usually
     * to clear caches. <b>For most usecases you should use the JMS-based (asynchronous)
     * event notification system.</b>
     *
     * <p>A listener stays in effect until it is removed using
     * {@link #removeListener(org.outerj.daisy.repository.RepositoryListener)}.
     */
    void addListener(RepositoryListener listener);

    /**
     * Removes an event listener.
     */
    void removeListener(RepositoryListener listener);
    
    /**
     * Retrieves an extension of the standard repository functionality.
     * Extensions are additional available repository services.
     * What these services are is not defined by this API.
     *
     * <p>The reason for making this extension functionality part of the
     * Repository API, instead of using completely separate and standalone
     * components, is that in this way the extensions can operate in the
     * authenticated context of the current user (ie Repository instance).
     *
     * <p>So, for as far as the extension performs any operations that depend
     * on the current user and its role, the extension will operate using the
     * same credentials as associated with the Repository object from which
     * the extension instance has been retrieved.
     */
    Object getExtension(String name);

    boolean hasExtension(String name);

    /**
     * Gets the version of the Daisy client API. Inside the repository server,
     * this will be the same as {@link #getServerVersion()}.
     *
     * <p>At the time of this writing, in the remote API implementation this
     * will usually also be the same, as the client and server API implementations
     * evolve together and get the same version numbers assigned.
     */
    String getClientVersion();

    /**
     * Returns the version number of the Daisy repository server.
     * Usually follows the format "major.minor.patch", in which the ".patch"
     * is optional, and the version string can be followed with
     * a suffix like "-dev".
     */
    String getServerVersion();

    /**
     * Returns the RepositoryManager this Repository was created from.
     * This allows to obtain a repository for another user in situations
     * where you can't get to the RepositoryManager in some other way.
     */
    RepositoryManager getRepositoryManager();
}
