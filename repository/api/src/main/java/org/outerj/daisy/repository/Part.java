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

import org.outerx.daisy.x10.PartDocument;

import java.io.InputStream;

/**
 * Parts belong to documents (or versions of documents), and are the things
 * that contain the actual data of the document.
 *
 * <p>A part is always associated with a {@link org.outerj.daisy.repository.schema.PartType}.
 *
 * <p>The repository itself does not really place any restrictions
 * on the kind of data that can be contained in a part. However,
 * parts whose part type's {@link org.outerj.daisy.repository.schema.PartType#isDaisyHtml()}
 * method returns true should contain XML-well-formed HTML (not namespaced XHTML).
 *
 * <p>Note that a part has no setters methods, modifications can only
 * be done through the containing {@link Document}. This is because
 * parts can also be obtained from {@link Version}s, which are not
 * modifiable.
 */
public interface Part {
    /**
     * The id of the part type of this part. More information on the part type
     * can then be retrieved from the {@link org.outerj.daisy.repository.schema.RepositorySchema}.
     */
    long getTypeId();

    /**
     * Get the name of the part type.
     */
    String getTypeName();

    /**
     * Get the mime-type of the data currently stored in this part.
     */
    String getMimeType();

    /**
     * Get the file name for this part, can be null. The file name is a user-selected
     * string used to store a proposed file name that can be used when downloading the
     * data stored in this part into a file. It is not the name of the file that the
     * repository uses to store the data (if this would be file-based, which isn't
     * defined by this API).
     */
    String getFileName();

    /**
     * Get the actual data stored in this part. The data is only retrieved
     * when this method is called, and is not stored inside this object after
     * retrieval.
     */
    byte[] getData() throws RepositoryException;

    /**
     * Get the data stored in this part. The caller is, of course, responsible
     * for closing the input stream in order to avoid resource leakage (thus
     * always do this in a try-finally block).
     */
    InputStream getDataStream() throws RepositoryException;

    /**
     * Get the size of the data. For new or modified parts, this will return -1
     * until the document to which the part belongs is saved.
     */
    long getSize();

    /**
     * Gets the ID of the last version in which the data of this part was changed.
     * Note that this only applies to the binary data, not to the filename or mime type.
     * If a part has been updated, but not yet saved, this method returns -1. For parts
     * retrieved via a version, this will never be the case.
     *
     * @since Daisy 2.0.
     */
    long getDataChangedInVersion();

    /**
     * Get an XML document describing this part.
     */
    PartDocument getXml();
}
