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

import org.outerx.daisy.x10.VersionDocument;

import java.util.Date;

/**
 * A version of a {@link Document}.
 *
 * <p>This object provides access to the all versioned information of a document,
 * for a specific version of the document. Versions cannot be modified (except
 * their state, see below),
 * they are read-only. See {@link Document#save()} for when a version is created.
 *
 * <p>A version can have a state of either 'publish' or 'draft'. This state
 * can be changed at any time.
 */
public interface Version extends VersionedData {
    /**
     * The id of the version, which is a sequence number, the first version
     * being 1, then 2, and so on.
     */
    long getId();

    /**
     * Returns the date when this version was created.
     */
    Date getCreated();

    /**
     * Returns the id of the user that created this version. You can
     * retrieve full information on the user via the {@link org.outerj.daisy.repository.user.UserManager}.
     */
    long getCreator();

    /**
     * Get an XML document containing information about this version, but without
     * the actual versioned content, thus no fields, parts, links etc. This is
     * useful when retrieving an overview of all versions of a document.
     */
    VersionDocument getShallowXml();

    /**
     * Get an XML document describing the version.
     */
    VersionDocument getXml() throws RepositoryException;

    /**
     * Changes the state of this version.
     *
     * <p>Sinced daisy 2.2, this method no longer has immediate effect, you need to call version.save() to make the change permanent.</p>
     */
    void setState(VersionState state);

    /**
     * Returns the current state of this version. This is the state as it
     * was when this version object was loaded, it may have changed in the
     * meantime.
     */
    VersionState getState();
    
    /**
     * Changes the 'synced with' field for this version. The purpose of this field is for
     * translation management, it indicates that the data in this version is translated
     * content which is up to date with a certain version of some other (reference) language.
     * 
     * <p>This method does not have immediate effect.  You need to call version.save() to make the change permanent.</p>
     *
     * <p>To clear the 'synced with' field, specify -1 for languageId and versionId.
     * 
     * @throws IllegalArgumentException if exactly one of languageId and versionId is -1 or versionId < 0
     * @throws RepositoryException if the languageId!=-1 and the language does not exist
     * @since daisy 2.2
     */
    void setSyncedWith(long languageId, long versionId) throws RepositoryException;

    /**
     * Sets the 'synced with' pointer by specifying a VersionKey object.
     * Only the language ID and version ID fields of the VersionKey object
     * are significant. The syncedWith argument can be null.
     */
    void setSyncedWith(VersionKey syncedWith) throws RepositoryException;

    /**
     * Returns a VersionKey that identifies the version that this version was synced to.
     * Returns null if not set.
     */
    VersionKey getSyncedWith();

    /**
     * Changes the change type for this version.
     * 
     * <p>This method does not have immediate effect. You need to call version.save() to make the change permanent.</p>
     *
     * <p>The change type indicates if the version contains major or minor changes.
     *
     * <p>When using translation management, the change type is used to indicate that
     * a version contains changes which render translations invalid. So if a user
     * makes changes to a version, even though these might seem minor, but which
     * should be replicated in translations, than the change type should be set
     * to major (when using translation management, otherwise you can assign
     * your own meaning to this field).
     *
     * @since daisy 2.2
     */
    void setChangeType(ChangeType changeType) throws RepositoryException;

    /**
     * Returns the change type for this version (never null).
     */
    ChangeType getChangeType();
    
    /**
     * Changes the change comment for this version.
     * 
     * <p>This method does not have immediate effect.  You need to call version.save() to make the change permanent.</p>
     *
     * @param changeComment the comment text, can be null to clear the change comment
     */
    void setChangeComment(String changeComment) throws RepositoryException;
    
    /**
     * Returns the change comment for this version (can be null).
     */
    String getChangeComment();
    
    /**
     * Saves changes made to the metadata of this version (versionState, syncedWith, changeType and changeComment).
     */
    void save() throws RepositoryException;
    
    /**
     * Get the id of the user that last changed the state of this version.
     */
    long getLastModifier();

    /**
     * Get the time at which the state of this version was last changed.
     */
    Date getLastModified();

    /**
     * Get the sum of the size of the parts in this version.
     */
    long getTotalSizeOfParts();

    /*
     * <p>Returns a summary text for the document variant. The summary is only created
     * when the document is saved. Returns an empty string if there's no summary.</p>
     */
    String getSummary();
}
