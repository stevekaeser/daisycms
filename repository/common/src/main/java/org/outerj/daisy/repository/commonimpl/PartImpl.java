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
package org.outerj.daisy.repository.commonimpl;

import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.PartHelper;
import org.outerj.daisy.repository.PartDataSource;
import org.outerj.daisy.repository.schema.PartType;
import org.outerx.daisy.x10.PartDocument;

import java.io.InputStream;
import java.io.IOException;

public class PartImpl implements Part {
    private long partTypeId;
    private String blobKey;
    private long size = 0;
    private String mimeType;
    private String fileName;
    private long dataChangedInVersion;
    private PartDataSource source;
    private DocumentVariantImpl.IntimateAccess ownerVariantInt;
    private boolean newOrUpdated = false;
    private boolean dataUpdated = false;

    /**
     * The ID of the version to which this part belongs, or in case the part belongs
     * directly to the document variant object, the last version ID (which could be -1 for a
     * new document variant).
     * */
    private long versionId;
    private IntimateAccess intimateAccess = new IntimateAccess();

    public PartImpl(DocumentVariantImpl.IntimateAccess ownerVariantInt, long partTypeId, long versionId, long dataChangedInVersion) {
        this.ownerVariantInt = ownerVariantInt;
        this.partTypeId = partTypeId;
        this.versionId = versionId;
        this.dataChangedInVersion = dataChangedInVersion;
        if (dataChangedInVersion > versionId)
            throw new RuntimeException("Assertion error: dataChangedInVersion > versionId");
    }

    public IntimateAccess getIntimateAccess(DocumentStrategy documentStrategy) {
        if (ownerVariantInt.getDocumentStrategy() == documentStrategy)
            return intimateAccess;
        else
            return null;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileName() {
        return fileName;
    }

    public long getSize() {
        return size;
    }

    public byte[] getData() throws RepositoryException {
        if (source != null) {
            try {
                return PartHelper.streamToByteArrayAndClose(source.createInputStream(), (int)source.getSize());
            } catch (IOException e) {
                throw new RepositoryException("Error creating part input stream", e);
            }
        } else {
            if (versionId == -1 && blobKey == null) {
                throw new RuntimeException("Strange situation: versionId is -1 and blobKey is null in PartImpl.getData(). This should never be the case.");
            }
            try {
                if (blobKey != null) {
                    // if we know the blobkey (yes in local impl, no in remote impl),
                    // directly retrieve the blob using the blobkey, which is faster
                    return PartHelper.streamToByteArrayAndClose(ownerVariantInt.getDocumentStrategy().getBlob(blobKey), (int)getSize());
                } else {
                    DocumentVariantImpl variant = ownerVariantInt.getVariant();
                    return PartHelper.streamToByteArrayAndClose(ownerVariantInt.getDocumentStrategy().getBlob(
                            ownerVariantInt.getDocId(), variant.getBranchId(), variant.getLanguageId(),
                            versionId, partTypeId, ownerVariantInt.getCurrentUser()), (int)getSize());
                }
            } catch (IOException e) {
                throw new RepositoryException("Error reading input stream", e);
            }
        }
    }

    public InputStream getDataStream() throws RepositoryException {
        if (source != null) {
            try {
                return source.createInputStream();
            } catch (IOException e) {
                throw new RepositoryException("Error creating part input stream", e);
            }
        } else {
            if (versionId == -1 && blobKey == null) {
                throw new RuntimeException("Strange situation: versionId is -1 and blobKey is null in PartImpl.getDataStream(). This should never be the case.");
            }
            if (blobKey != null) {
                // if we know the blobkey (yes in local impl, no in remote impl),
                // directly retrieve the blob using the blobkey, which is faster
                return ownerVariantInt.getDocumentStrategy().getBlob(blobKey);
            } else {
                DocumentVariantImpl variant = ownerVariantInt.getVariant();
                return ownerVariantInt.getDocumentStrategy().getBlob(ownerVariantInt.getDocId(), variant.getBranchId(),
                        variant.getLanguageId(), versionId, partTypeId, ownerVariantInt.getCurrentUser());
            }
        }
    }

    public String getTypeName() {
        PartType partType;
        try {
            partType = ownerVariantInt.getRepositorySchema().getPartTypeById(partTypeId, false, ownerVariantInt.getCurrentUser());
        } catch (RepositoryException e) {
            throw new RuntimeException(DocumentImpl.ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        return partType.getName();
    }

    public long getTypeId() {
        return partTypeId;
    }

    public long getDataChangedInVersion() {
        return dataChangedInVersion;
    }

    public PartDocument getXml() {
        PartDocument partDocument = PartDocument.Factory.newInstance();
        PartDocument.Part partXml = partDocument.addNewPart();
        partXml.setTypeId(getTypeId());
        partXml.setSize(getSize());
        partXml.setMimeType(getMimeType());
        partXml.setDataChangedInVersion(getDataChangedInVersion());
        if (fileName != null)
            partXml.setFileName(fileName);
        return partDocument;
    }

    public class IntimateAccess {
        private IntimateAccess() {
        }

        public PartDataSource getPartDataSource() {
            return PartImpl.this.source;
        }

        public void setMimeType(String mimeType) {
            PartImpl.this.mimeType = mimeType;
        }

        public void setFileName(String fileName) {
            PartImpl.this.fileName = fileName;
        }

        public void setData(PartDataSource partDataSource) {
            if (partDataSource != null) {
                PartImpl.this.source = partDataSource;
                PartImpl.this.size = partDataSource.getSize();
            } else {
                PartImpl.this.source = null;
            }
        }

        public void setSize(long size) {
            PartImpl.this.size = size;
        }

        public void setBlobKey(String blobKey) {
            PartImpl.this.blobKey = blobKey;
        }

        public String getBlobKey() {
            return blobKey;
        }

        public boolean isNewOrUpdated() {
            return newOrUpdated;
        }

        public boolean isDataUpdated() {
            return dataUpdated;
        }

        public void setVersionId(long versionId) {
            PartImpl.this.versionId = versionId;
        }

        public void setDataChangedInVersion(long dataChangedInVersion) {
            if (dataChangedInVersion > PartImpl.this.versionId)
                throw new RuntimeException("Assertion error: dataChangedInVersion > versionId");
            PartImpl.this.dataChangedInVersion = dataChangedInVersion;
        }

        public void setNewOrUpdated(boolean newOrUpdated, boolean dataUpdated) {
            if (dataUpdated && !newOrUpdated)
                throw new IllegalArgumentException("If dataUpdated is true, newOrUpdated should also be true.");

            PartImpl.this.newOrUpdated = newOrUpdated;
            PartImpl.this.dataUpdated = dataUpdated;
        }

        public PartImpl internalDuplicate(DocumentVariantImpl.IntimateAccess newOwnerVariantInt) {
            PartImpl partImpl = new PartImpl(newOwnerVariantInt, partTypeId, -1, -1);
            partImpl.blobKey = blobKey;
            partImpl.size = size;
            partImpl.partTypeId = partTypeId;
            partImpl.mimeType = mimeType;
            partImpl.newOrUpdated = false;
            partImpl.dataUpdated = false;
            return partImpl;
        }
    }
}
