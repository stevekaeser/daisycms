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

import java.util.Date;
import java.util.Map;

import org.outerx.daisy.x10.DocumentDocument;

/**
 * A document in the repository. This object actually always represents one
 * particular variant (branch-language combination) of the document. There
 * is no separate object representing a document itself as such, access to
 * documents is always done through a certain variant of it (since access
 * control is also determined based on the information that is part of the
 * variant). Letting a document object represent both the document and
 * document variant was also the most backwards-compatible way to introduce
 * the variants feature.
 *
 * <p>Since one object is used to manipulate both the document and document
 * variant, most methods include a mentioning of whether they apply to the
 * document or the document variant. All methods inherited from
 * {@link VersionedData} are variant-level methods.
 *
 * <p>A document can be created using {@link Repository#createDocument(String, String)}
 * or retrieved using {@link Repository#getDocument(String, boolean)}.
 *
 */
public interface Document extends VersionedData {
    /**
     * Returns the id of this document. For newly created documents, this method
     * returns null until {@link #save()} is called.
     */
    String getId();

    /**
     * Returns the sequence ID part of the document ID. This method returns -1
     * until {@link #save()} is called.
     *
     * @since Daisy 2.0
     */
    long getSeqId();

    /**
     * Returns the sequence ID part of the document ID. This method returns -1
     * until {@link #save()} is called.
     *
     * @since Daisy 2.0
     */
    String getNamespace();

    /**
     * Returns true if this document has never been saved yet (thus does
     * not exist in the repository yet).
     */
    boolean isNew();

    long getBranchId();

    long getLanguageId();

    /**
     * @return null for documents which have not yet been saved.
     */
    VariantKey getVariantKey();

    /**
     * Returns true if the variant represented by this document object has
     * never been stored yet.
     */
    boolean isVariantNew();

    /**
     * Requests that this new document will be saved with the given document ID.
     *
     * <p>This can only be done for document IDs in a namespace other than
     * the namespace of the current repository. This is simply because the
     * current repository is responsible itself for the assigning numbers
     * to its documents. For foreign namespaces, it is assumed that some
     * external entity is responsible for the numbering (typically another
     * Daisy repository, though it could be anything).
     *
     * <p>Setting the document ID is only possible for new, not-yet-saved
     * documents.
     *
     * <p>If a document with the given ID would already exist in the
     * repository, then this will give an exception when saving the document.
     *
     * @param documentId allowed to be null, in which case a previous setting is reset
     *
     * @since Daisy 2.0
     *
     */
    void setRequestedId(String documentId);

    String getRequestedId();

    /**
     * Returns the available variants for this document. If the document is new and unsaved, this
     * returns an empty list. If the document variant in this document is new and unsaved, the
     * returned list will not yet contain this new variant. Thus only variants persisted in the
     * repository are returned. This method does the same as {@link Repository#getAvailableVariants(String)}.
     */
    AvailableVariants getAvailableVariants() throws RepositoryException;

    /**
     * Returns the id of the document type to which this document adheres.
     * Document types themselve are managed via the {@link org.outerj.daisy.repository.schema.RepositorySchema}.
     *
     * <p>The document type is a variant-level property.
     */
    long getDocumentTypeId();

    /**
     * Changes the document type of this document.
     *
     * <p>Will throw an exception if the document type does not exist.
     *
     * <p>Changing the documenttype does not influence the content of the document,
     * you have to assure yourself that you update/add/remove parts and fields so
     * that the document conforms to the document type.
     *
     * <p>The document type is a variant-level property.
     */
    void changeDocumentType(long documentTypeId) throws RepositoryException;

    /**
     * See {@link #changeDocumentType(long)}.
     */ 
    void changeDocumentType(String documentTypeName) throws RepositoryException;

    /**
     * Gets the name of this document.
     *
     * <p>The document name is a variant-level property.
     *
     * <p>This is exactly the same as calling {@link VersionedData#getDocumentName}.
     */
    String getName();

    /**
     * Sets the name of the document. The document name is not required to be unique,
     * and is versioned, ie changing the document name and then saving the document
     * will create a new version.
     *
     * <p>The document name is a variant-level property.
     */
    void setName(String name);

    /**
     * Returns the user id of the owner of this document. The owner is the user that
     * created the document, unless changed.
     *
     * <p>The owner is a document-level property.
     */
    long getOwner();

    /**
     * Change the owner of this document. Only users acting in the Administrator role,
     * or the current owner, can change the owner.
     *
     * <p>The owner is a document-level property.
     */
    void setOwner(long userId);

    /**
     * Is this a private document or not. A private document is a document that can
     * only be accessed by its owner, or users having (and acting) in the role of
     * Administrator.
     *
     * <p>This is a document-level property.
     */
    boolean isPrivate();

    /**
     * Sets the private flag for this document, see also {@link #isPrivate()}.
     * New documents are not private by default.
     *
     * <p>This is a document-level property.
     */
    void setPrivate(boolean _private);

    /**
     * Gets the time this document was created.
     *
     * <p>This is a document-level property.
     */
    Date getCreated();

    /**
     * Is this document retired or not. A retired document behaves very much likes
     * a deleted document. Retired documents don't show up in query results (unless
     * specifically requested).
     *
     * <p>This is a variant-level property.
     */
    boolean isRetired();

    /**
     * Sets the retired flag for this document, see also {@link #isRetired()}.
     *
     * <p>This is a variant-level property.
     */
    void setRetired(boolean retired);

    /**
     * The time this document was last saved. Returns null for documents that
     * have never been saved yet.
     *
     * <p>This is a document-level property. See also {@link #getVariantLastModified()}.
     */
    Date getLastModified();

    /**
     * Returns the user id of the last user who saved this document.
     * Returns -1 for documents that have never been saved yet.
     *
     * <p>This is a document-level property. See also {@link #getVariantLastModifier()}.
     */
    long getLastModifier();

    /**
     * The time the document variant was last saved. Returns null for document variants that
     * have never been saved yet.
     *
     * <p>This is a variant-level property. See also {@link #getLastModified()}.
     */
    Date getVariantLastModified();

    /**
     * Returns the user id of the last user who saved this document variant.
     * Returns -1 for document variants that have never been saved yet.
     *
     * <p>This is a variant-level property. See also {@link #getLastModifier()}.
     */
    long getVariantLastModifier();

    /**
     * Returns all versions stored for this document.
     *
     * <p>In a typical implementation, the returned Version objects will only contain
     * basic information about the version (like its id and creation time) which is
     * needed to show a version list, but the actual version content (fields,
     * parts, ...) will only be loaded when requested.
     *
     * <p>This is a variant-level method.
     */
    Versions getVersions() throws RepositoryException;

    /**
     * Returns the requested version.
     *
     * <p>Note that in contrast with the {@link #getVersions()} method, typical
     * implementations will usually return a Version object containing all details
     * about the version.
     *
     * <p>This is a variant-level method.
     */
    Version getVersion(long id) throws RepositoryException;

    /**
     * Returns the id of the last stored version. For new, unsaved documents this
     * returns -1.
     *
     * <p>This is a variant-level property.
     */
    long getLastVersionId();

    /**
     * Returns the Version object for the last stored version, or null for unsaved
     * documents.
     *
     * <p>This is a variant-level method.
     */
    Version getLastVersion() throws RepositoryException;

    /**
     * Returns the live version of this document, or null if there is none.
     * The live version is the most recent version that has the state 'publish'.
     *
     * <p>This is a variant-level method.
     */
    Version getLiveVersion() throws RepositoryException;

    /**
     * Returns the id of the live version, or -1 if there is none.
     *
     * <p>This is a variant-level method.
     */
    long getLiveVersionId();

    /**
     * Returns true if the current user can only access the current live data of the document.
     */
    boolean canReadLiveOnly();

    /**
     * Returns true if the current user can access all versions of the document.
     */
    boolean canReadAllVersions();

    /**
     * Sets the value of a field. The field must be part of the document type
     * of this document.
     *
     * <p>This is a variant-level method.
     *
     * <p>See {@link #setDocumentTypeChecksEnabled} to disable checks against the document type.
     *
     * @param name name of the field type of the field to set.
     * @param value the value of the field, the type of the object must be in correspondence with the ValueType of the field type.
     *              For multi-value fields, the value should be an array (i.e. Object[] or a more specifically typed array).
     *              For link field types, the value is a VariantKey object. The branch and language fields of the VariantKey
     *              object can be -1 to indicate they are the same as the containing document (recommended for most uses).
     *              For hierarchical fields, the value is a HierarchyPath containing values according to the field type.
     *              For multi-value hierarchical fields, the value is an array of HierarchyPath objects.
     */
    void setField(String name, Object value) throws DocumentTypeInconsistencyException;

    /**
     * Same as {@link #setField(String, Object)} but using the id of the field type.
     *
     * <p>This is a variant-level method.
     */
    void setField(long fieldTypeId, Object value) throws DocumentTypeInconsistencyException;

    /**
     * Removes the specified field. Silently ignored if the document doesn't have that field.
     *
     * <p>This is a variant-level method.
     *
     * @param name name of the field type
     */
    void deleteField(String name);

    /**
     * Removes the specified field. Silently ignored if the document doesn't have that field.
     *
     * <p>This is a variant-level method.
     */
    void deleteField(long fieldTypeId);

    /**
     * Saves a document.
     *
     * <p>This is the same as calling {@link #save(boolean)} with argument true.
     */
    void save() throws RepositoryException;

    /**
     * Saves the document and document variant.
     *
     * <p>If only changes have been done to document-level properties,
     * only the document will be saved. If only changes have been done
     * to variant-level properties, only the variant will be saved.
     *
     * <p>If this is a new document, it will cause an id to be assigned
     * to this document object.
     *
     * <p>Saving the document variant might or might not cause the creation of a new
     * version, depending on whether any versioned data has changed. Versioned
     * data includes the parts, fields, links and the document's name. So if
     * for example only the retired flag changed, customfields were changed,
     * or collection membership changed, no new version will be created.
     * After saving, you can get the new version id from the method
     * {@link #getLastVersionId()}. The state of the new version can be
     * influenced by using {@link #setNewVersionState(org.outerj.daisy.repository.VersionState)}
     * before calling save().
     *
     *
     * <p>If someone else holds a pessimistic lock on the document variant, saving it
     * will fail. Likewise, if another person saved the document since you retrieved
     * this document object (ie a concurrent modification), saving will also fail.
     * Note that locks apply only to document variants, so don't protect from concurrent
     * changes to document-level properties.
     *
     * <p>Saving a document will cause the server to send out an asynchronous
     * event, to which other processes can listen, for example the full
     * text indexer. (There are separate events for updates to the document and
     * the variant)
     *
     * <p>Using the argument <tt>validate</tt> you can specify whether the document variant
     * should be validated against its document type. Usually you will always
     * provide true here. As an example use case for not using validation,
     * it might be that you just want to mark a
     * document as retired without bothering that its content doesn't correspond
     * to the schema anymore.
     *
     * @throws DocumentTypeInconsistencyException in case validation against the schema fails.
     */
    void save(boolean validate) throws RepositoryException;

    /**
     * Validates that this document confirms to its document type.
     */
    void validate() throws DocumentTypeInconsistencyException;

    /**
     * Sets the VersionState that should be used if a new version is created
     * when saving the document.
     *
     * <p>By default this is VersionState.PUBLISH.
     */
    void setNewVersionState(VersionState versionState);

    /**
     * See {@link #setNewVersionState(VersionState)}.
     */
    VersionState getNewVersionState();

    /**
     * Takes a lock on the document variant. If the lock type is "pessimistic", this will prevent
     * others from saving the document variant while you're working on it. If the lock
     * is "warn", the lock only serves for informational purposes and will
     * not enforce anything. So called "optimistic locking" (ie checking against
     * concurrent modifications) happens always, and doesn't require to take a lock.
     *
     * <p>Changing a lock doesn't need a {@link #save()} call afterwards,
     * calling this method has immediate effect.
     *
     * <p>The lock can be removed with {@link #releaseLock()}.
     *
     * <p>This is a variant-level method.
     *
     * @param duration indication of how long the lock should remain (in ms).
     *                  Use -1 for a never-expiring lock.
     * @return false if someone else already has a lock on this document, in
     *         which case you can call {@link #getLockInfo(boolean)} with false
     *         as parameter to know who is holding the lock.
     */
    boolean lock(long duration, LockType lockType) throws RepositoryException;

    /**
     * Releases the lock on the document variant. This can only be done by the person
     * holding this lock, or by an Administrator.
     *
     * <p>This is a variant-level method.
     *
     * @return true if the lock is removed (or if there was no lock). If a lock
     *         remains on the document, false will be returned, in which case
     *         you can call {@link #getLockInfo(boolean)} with false
     *         as parameter to know who is holding the lock.
     */
    boolean releaseLock() throws RepositoryException;

    /**
     * Returns information about the current lock on the document variant.
     *
     * <p>This is a variant-level method.
     *
     * @param fresh if true, the lock information will be refetched. Otherwise
     *              the existing information stored in this Document object will
     *              be returned (which may be out of date).
     */
    LockInfo getLockInfo(boolean fresh) throws RepositoryException;

    /**
     * Sets a custom field. A custom field is an arbitrary name/value pair.
     * Custom fields are not versioned.
     *
     * <p>This is a variant-level method.
     */
    void setCustomField(String name, String value);

    /**
     * Removes the specified custom field. Passes silently if there is no
     * custom field with the given name.
     *
     * <p>This is a variant-level method.
     */
    void deleteCustomField(String name);

    /**
     * Gets the value of the specified custom field, or null if there is no
     * custom field with that name.
     *
     * <p>This is a variant-level method.
     */
    String getCustomField(String name);

    /**
     * Returns true if there is a custom field with the specified name.
     *
     * <p>This is a variant-level method.
     */
    boolean hasCustomField(String name);

    /**
     * Removes all custom fields.
     *
     * <p>This is a variant-level method.
     */
    void clearCustomFields();

    /**
     * Returns a map containing the fields, with the field type name being the key.
     * Making changes to this map will not be reflected in the document.
     *
     * <p>This is a variant-level method.
     */
    Map<String, String> getCustomFields();

    /**
     * Sets a part.
     *
     * <p>This is a variant-level method.
     */
    void setPart(String partTypeName, String mimeType, byte[] data) throws DocumentTypeInconsistencyException;

    /**
     * Sets a part.
     *
     * <p>This is a variant-level method.
     */
    void setPart(long partTypeId, String mimeType, byte[] data) throws DocumentTypeInconsistencyException;

    /**
     * Sets a part.
     *
     * <p>This is a variant-level method.
     */
    void setPart(String partTypeName, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException;

    /**
     * Sets a part.
     *
     * <p>This is a variant-level method.
     */
    void setPart(long partTypeId, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException;

    /**
     * Update the file name of an already existing part. Throws an exception if the document doesn't
     * have the indicated part.
     *
     * <p>This is a variant-level method.
     *
     * @param fileName allowed to be null (to remove the filename information)
     */
    void setPartFileName(String partTypeName, String fileName);

    /**
     * Update the file name of an already existing part. Throws an exception if the document doesn't
     * have the indicated part.
     *
     * <p>This is a variant-level method.
     * 
     * @param fileName allowed to be null (to remove the filename information)
     */
    void setPartFileName(long partTypeId, String fileName);

    /**
     * Update the mime-type of an already existing part. Throws an exception if the document doesn't
     * have the indicated part.
     *
     * <p>This is a variant-level method.
     */
    void setPartMimeType(String partTypeName, String mimeType);

    /**
     * Update the mime-type of an already existing part. Throws an exception if the document doesn't
     * have the indicated part.
     *
     * <p>This is a variant-level method.
     */
    void setPartMimeType(long partTypeId, String mimeType);

    /**
     * Removes a part, passes silently if there is no part in the document with the given id.
     *
     * <p>This is a variant-level method.
     */
    void deletePart(long partTypeId);

    /**
     * Removes a part, passes silently if there is no part in the document with the given name.
     *
     * <p>This is a variant-level method.
     */
    void deletePart(String name);

    /**
     * Adds an out-of-line link (at the end of the list).
     *
     * <p>This is a variant-level method.
     */
    void addLink(String title, String target);

    /**
     * Removes an out-of-line link.
     *
     * <p>This is a variant-level method.
     */
    void deleteLink(int index);

    /**
     * Removes all out-of-line links.
     *
     * <p>This is a variant-level method.
     */
    void clearLinks();
    
    /** 
     * Adds the document variant to a supplied Collection.
     *
     * @param collection the collection to add the current document to
     */
    void addToCollection(DocumentCollection collection);

    /**
     * Removes the document variant from a collection
     * 
     * @param collection the collection from which the document needs to be removed
     */
    void removeFromCollection(DocumentCollection collection);
    
    /**
     * Returns the collections the document variant belongs to, null if the document variant
     * belongs to no Collections.
     */
    DocumentCollections getCollections();

    /**
     * Checks if the document variant belongs to the specified collection.
     */
    boolean inCollection(DocumentCollection collection);

    /**
     * Checks if the document variant belongs to the specified collection.
     */
    boolean inCollection(long collectionId);

    DocumentDocument getXml() throws RepositoryException;

    DocumentDocument getXmlWithoutVariant() throws RepositoryException;

    DocumentDocument getXmlWithoutVersionedData() throws RepositoryException;

    /**
     * Gets the XML of the document but include the data from the
     * specified version, instead of the current data. This only
     * applies to the data of the document that is actually versionable, of course.
     * For the rest of the data, you'll get what's currently in the document
     * object, whether that data has already been saved or not.
     */
    DocumentDocument getXml(long versionId) throws RepositoryException;

    /**
     * Removes the document variant from all the collections it belongs to.
     */
    void clearCollections();

    /**
     * <p>Returns a summary text for the document variant. The summary is only created
     * when the document is saved. Returns an empty string if there's no summary.</p>
     * 
     * <p>If there are no unsaved changes and there is at least one version, this is the same as calling getLastVersion().getSummary()</p>
     */
    String getSummary();

    /**
     * If the variant currently loaded in this document object is created from an existing
     * branch, this method will return the id of that branch, otherwise it will return -1.
     * Note that the branch could possibly no longer exist.
     */
    long getVariantCreatedFromBranchId();

    /**
     * Similar to {@link #getVariantCreatedFromBranchId()}.
     */
    long getVariantCreatedFromLanguageId();

    /**
     * Similar to {@link #getVariantCreatedFromBranchId()}.
     */
    long getVariantCreatedFromVersionId();

    /**
     * Allows to disable some document type related checks. When disabled,
     * it is possible to set fields and parts on this document that are
     * not allowed by the document type. For parts, the mime-type
     * checking won't be performed either.
     */
    void setDocumentTypeChecksEnabled(boolean documentTypeChecksEnabled);

    long getUpdateCount();

    long getVariantUpdateCount();
    
    /**
     * The current reference language for this document.
     * Returns -1 if the reference language is not set.
     *
     * <p>Setting the reference language is also used as an indication that the document should be considered to be under "translation management".
     * 
     * <p>this is a document level method</p>
     */
    long getReferenceLanguageId();
    
    void setReferenceLanguageId(long referenceLanguageId);

    void setReferenceLanguage(String referenceLanguageName) throws RepositoryException;

    /**
     * Gets the last version for which the change type was major.
     *
     * <p>Returns -1 if there is no such version.
     *
     * <p>This is an automatically calculated field. This information is fetched
     * when this document object is retrieved and is not updated afterwards.
     *
     * <p>This is a variant level method.
     *
     * @deprecated Because of the point-in-time feature introduced in Daisy 2.4 this property is no longer useful. 
     */
    long getLastMajorChangeVersionId();

    /**
     * Gets the last version, up to the current live version, for which the change type was major.
     *
     * <p>Returns -1 if there is no such version.
     * 
     * <p>This is an automatically calculated field. This information is fetched
     * when this document object is retrieved and is not updated afterwards.
     *
     * <p>This is a variant level method.
     *
     * @deprecated Because of the point-in-time feature introduced in Daisy 2.4 this property is no longer useful. 
     */
    long getLiveMajorChangeVersionId();

    /**
     * @return <languageId>:<versionId> to indicate which variant/version the document was synced with
     *   or null if the document is not synced 
     */
    VersionKey getNewSyncedWith();
    
    /**
     * Sets the 'synced with' value that should be used if a new version is created
     * as part of saving this document.
     *
     * <p>The 'synced with' indicates the version is synced with another language:version.
     *
     * <p>Use setSyncedWithVersion(-1,-1) to clear any previous set  value.
     */
    void setNewSyncedWith(long languageId, long versionId) throws RepositoryException ;

    /**
     * Alternative to {@link #setNewSyncedWith(long, long)}.
     *
     * <p>Use setSyncedWith(null, -1) to indicate the document is not synced anymore.
     */
    void setNewSyncedWith(String languageName, long versionId) throws RepositoryException;

    /**
     * Sets the 'synced with' pointer by specifying a VersionKey object.
     * Only the language ID and version ID fields of the VersionKey object
     * are significant. The syncedWith argument can be null.
     *
     * <p>See also {@link #setNewSyncedWith(long, long)}.
     */
    void setNewSyncedWith(VersionKey syncedWith) throws RepositoryException;

    /**
     * Sets the ChangeType that should be used if a new version is created
     * when saving the document.
     *
     * <p>By default this is ChangeType.MAJOR
     */
    void setNewChangeType(ChangeType changeType);
    
    ChangeType getNewChangeType();
    
    /**
     * Sets the change comment that should be used if a new version is created
     * when saving the document.
     *
     * <p>By default this is null
     */
    void setNewChangeComment(String changeComment);
    
    String getNewChangeComment();
    
    /**
     * Returns the contextualised version of a {@link VariantKey}, i.e. if the branch or language of the
     * given VariantKey is equal to -1, replace it by this document's branch/language
     * 
     * @param variantKey the {@link VariantKey} to contextualise.
     */
    public VariantKey contextualiseVariantKey(VariantKey variantKey);
    
    public Timeline getTimeline();
    
    /**
     * This property can be used to request a change in the current live version. 
     * 0: leave onchanged - if a new version is created, the live version may change according to the value 'newLiveStrategy'. 
     * -1: make the current last version live
     * -2: don't make any version live.
     * > 0: make a specific version live
     * 
     * (Note: this is only works if the user has publish rights) 
     * 
     * @param requestedLiveVersionId
     * @return 
     */
    public long getRequestedLiveVersionId();

    public void setRequestedLiveVersionId(long liveVersionId);
    
    /**
     * If a new version is created on save, this parameter determines if the new version is made live.
     * See LiveStrategy for explanation of possible values.  Default: LiveStrategy.DEFAULT
     */
    public LiveStrategy getNewLiveStrategy();
    
    public void setNewLiveStrategy(LiveStrategy liveStrategy);
    
    /**
     * @param versionMode
     * @return the version corresponding with the given version mode or null if there is no corresponding version 
     * @throws RepositoryException
     */
    public Version getVersion(VersionMode versionMode) throws RepositoryException;

    /**
     * @param versionMode
     * @return the version id corresponding with the given version mode or -1 if there is no corresponding version 
     */
    public long getVersionId(VersionMode versionMode) throws RepositoryException;

}
