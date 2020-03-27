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
package org.outerj.daisy.httpconnector.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.outerj.daisy.httpconnector.spi.BadRequestException;
import org.outerj.daisy.httpconnector.spi.UploadItem;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.CollectionNotFoundException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Field;
import org.outerj.daisy.repository.FieldHelper;
import org.outerj.daisy.repository.Link;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.LiveStrategy;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.PartDataSource;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Timeline;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerx.daisy.x10.DocumentDocument;
import org.outerx.daisy.x10.FieldDocument;
import org.outerx.daisy.x10.LinksDocument;
import org.outerx.daisy.x10.LiveHistoryEntryDocument;
import org.outerx.daisy.x10.PartDocument;
import org.outerx.daisy.x10.TimelineDocument;

public abstract class AbstractDocumentHandler extends AbstractRepositoryRequestHandler {
    protected Log requestErrorLogger;

    public AbstractDocumentHandler(Log requestErrorLogger) {
        this.requestErrorLogger = requestErrorLogger;
    }

    /**
     * Updates a Document object based on XML data and other uploaded data.
     */
    protected void updateDocument(Document document, DocumentDocument.Document documentXml, List<UploadItem> uploadedItems,
                                   HttpServletResponse response, Repository repository) throws RepositoryException, IOException, BadRequestException {

        // Change document type, if needed, before updating fields and parts,
        // as those will belong to the new document type
        if (document.getDocumentTypeId() != documentXml.getTypeId())
            document.changeDocumentType(documentXml.getTypeId());

        //
        // Handle the Parts
        //

        List<PartDocument.Part> partsXml = documentXml.getParts().getPartList();
        Part parts[] = document.getParts().getArray();

        // search for deleted parts and delete them
        for (Part part : parts) {
            boolean found = false;
            for (PartDocument.Part partXml : partsXml) {
                if (partXml.getTypeId() == part.getTypeId())
                    found = true;
            }
            if (!found)
                document.deletePart(part.getTypeId());
        }

        // update other parts if needed
        for (PartDocument.Part partXml : partsXml) {
            if (partXml.getDataRef() != null) {
                final UploadItem item = getItemByName(uploadedItems, partXml.getDataRef());
                if (item == null) {
                    throw new BadRequestException("Referred data item not present in uploaded data: " + partXml.getDataRef());
                }
                PartDataSource partDataSource = new PartDataSource() {
                    public InputStream createInputStream() throws IOException {
                        return item.getInputStream();
                    }

                    public long getSize() {
                        return item.getSize();
                    }
                };
                document.setPart(partXml.getTypeId(), partXml.getMimeType(), partDataSource);
                if (partXml.isSetFileName())
                    document.setPartFileName(partXml.getTypeId(), partXml.getFileName());
            } else {
                Part part = document.getPart(partXml.getTypeId());
                if (!partXml.getMimeType().equals(part.getMimeType()))
                    document.setPartMimeType(partXml.getTypeId(), partXml.getMimeType());
                if ((!partXml.isSetFileName() && part.getFileName() != null) || (partXml.isSetFileName() && !partXml.getFileName().equals(part.getFileName())))
                    document.setPartFileName(part.getTypeId(), partXml.getFileName());
            }
        }

        //
        // Handle the Fields
        //

        List<FieldDocument.Field> fieldsXml = documentXml.getFields().getFieldList();
        Field[] fields = document.getFields().getArray();

        // search for deleted fields and delete them
        for (Field field : fields) {
            boolean found = false;
            for (FieldDocument.Field fieldXml : fieldsXml) {
                if (fieldXml.getTypeId() == field.getTypeId())
                    found = true;
            }
            if (!found)
                document.deleteField(field.getTypeId());
        }

        // update the other fields if needed
        RepositorySchema repositorySchema = repository.getRepositorySchema();
        for (FieldDocument.Field fieldXml : fieldsXml) {
            FieldType fieldType = repositorySchema.getFieldTypeById(fieldXml.getTypeId(), false);
            ValueType valueType = fieldType.getValueType();
            Object value = FieldHelper.getFieldValueFromXml(valueType, fieldType.isMultiValue(), fieldType.isHierarchical(), fieldXml);

            if (document.hasField(fieldXml.getTypeId())) {
                Field field = document.getField(fieldXml.getTypeId());
                // only update if value has changed
                if (!(fieldType.isMultiValue() ? Arrays.equals((Object[])field.getValue(), (Object[])value) : field.getValue().equals(value)))
                    document.setField(fieldXml.getTypeId(), value);
            } else {
                document.setField(fieldXml.getTypeId(), value);
            }
        }

        //
        // Handle the links
        //
        List<LinksDocument.Links.Link> linksXml = documentXml.getLinks().getLinkList();
        Link[] links = document.getLinks().getArray();
        boolean linksNeedUpdating = false;
        if (linksXml.size() != links.length) {
            linksNeedUpdating = true;
        } else {
            for (int i = 0; i < linksXml.size(); i++) {
                if (!linksXml.get(i).getTarget().equals(links[i].getTarget()) || !linksXml.get(i).getTitle().equals(links[i].getTitle())) {
                    linksNeedUpdating = true;
                    break;
                }
            }
        }

        if (linksNeedUpdating) {
            document.clearLinks();
            for (LinksDocument.Links.Link linkXml : linksXml) {
                document.addLink(linkXml.getTitle(), linkXml.getTarget());
            }
        }

        //
        // Handle the custom fields
        //
        List<DocumentDocument.Document.CustomFields.CustomField> customFieldsXml = documentXml.getCustomFields().getCustomFieldList();
        Map currentCustomFields = document.getCustomFields();
        boolean customFieldsNeedUpdating = false;
        if (currentCustomFields.size() != customFieldsXml.size()) {
            customFieldsNeedUpdating = true;
        } else {
            for (DocumentDocument.Document.CustomFields.CustomField customFieldXml : customFieldsXml) {
                String value = (String)currentCustomFields.get(customFieldXml.getName());
                if (value == null || !value.equals(customFieldXml.getValue())) {
                    customFieldsNeedUpdating = true;
                    break;
                }
            }
        }
        if (customFieldsNeedUpdating) {
            document.clearCustomFields();
            for (DocumentDocument.Document.CustomFields.CustomField customFieldXml : customFieldsXml) {
                document.setCustomField(customFieldXml.getName(), customFieldXml.getValue());
            }
        }

        //
        // handle the collections
        //

        List<Long> collectionIds = documentXml.getCollectionIds().getCollectionIdList();
        boolean collectionsNeedUpdating = false;
        if (document.getCollections().getArray().length != collectionIds.size()) {
            collectionsNeedUpdating = true;
        } else {
            for (long collectionId : collectionIds) {
                if (!document.inCollection(collectionId)) {
                    collectionsNeedUpdating = true;
                    break;
                }
            }
        }
        if (collectionsNeedUpdating) {
            CollectionManager collectionManager = repository.getCollectionManager();
            document.clearCollections();
            for (long collectionId : collectionIds) {
                try {
                    document.addToCollection(collectionManager.getCollection(collectionId, false));
                } catch (CollectionNotFoundException e) {
                    if (requestErrorLogger.isInfoEnabled())
                        requestErrorLogger.info("A remote request to update a document referenced the non-existing collection " + collectionId + ", silently skipping this collection.");
                }
            }
        }
        
        //
        // Timeline
        //
        Timeline timeline = document.getTimeline();
        TimelineDocument.Timeline timelineXml = documentXml.getTimeline();
        List<Long> newIds = new ArrayList<Long>();
        Map<Long, LiveHistoryEntry> lheById = new HashMap<Long, LiveHistoryEntry>();
        
        for (LiveHistoryEntry entry: timeline.getLiveHistory()) {
            lheById.put(entry.getId(), entry);
        }
        
        for (LiveHistoryEntryDocument.LiveHistoryEntry entry: timelineXml.getLiveHistoryEntryList()) {
            if (entry.isSetId())
                newIds.add(entry.getId());
        }

        for (LiveHistoryEntry entry: timeline.getLiveHistory()) {
            if (!newIds.contains(entry.getId())) {
                timeline.deleteLiveHistoryEntry(lheById.get(entry.getId()));
            }
        }
        
        for (LiveHistoryEntryDocument.LiveHistoryEntry entry: timelineXml.getLiveHistoryEntryList()) {
            if (!entry.isSetId()) {
                Date beginDate = entry.getBeginDate().getTime();
                Date endDate = null;
                if (entry.isSetEndDate()) {
                    endDate = entry.getEndDate().getTime();
                }
                timeline.addLiveHistoryEntry(beginDate, endDate, entry.getVersionId());
            }
        }

        
        //
        // Other document properties
        //
        if (!document.getName().equals(documentXml.getName()))
            document.setName(documentXml.getName());
        if (document.isPrivate() != documentXml.getPrivate())
            document.setPrivate(documentXml.getPrivate());
        if (document.isRetired() != documentXml.getRetired())
            document.setRetired(documentXml.getRetired());
        if (document.getOwner() != documentXml.getOwner())
            document.setOwner(documentXml.getOwner());
        document.setReferenceLanguageId(documentXml.isSetReferenceLanguageId() ? documentXml.getReferenceLanguageId() : -1);

        if (documentXml.isSetNewVersionState())
            document.setNewVersionState(VersionState.fromString(documentXml.getNewVersionState().toString()));
        if (documentXml.isSetNewSyncedWithLanguageId() && documentXml.isSetNewSyncedWithVersionId())
            document.setNewSyncedWith(documentXml.getNewSyncedWithLanguageId(), documentXml.getNewSyncedWithVersionId());
        if (documentXml.isSetNewChangeType())
            document.setNewChangeType(ChangeType.fromString(documentXml.getNewChangeType().toString()));
        if (documentXml.isSetNewChangeType())
            document.setNewChangeComment(documentXml.getNewChangeComment());
        if (documentXml.isSetNewLiveStrategy())
            document.setNewLiveStrategy(LiveStrategy.fromString(documentXml.getNewLiveStrategy().toString()));
        if (documentXml.isSetRequestedLiveVersionId())
            document.setRequestedLiveVersionId(documentXml.getRequestedLiveVersionId());
        
    }

    protected UploadItem getItemByName(List<UploadItem> items, String name) {
        for (UploadItem item : items) {
            if (item.getFieldName().equals(name))
                return item;
        }
        return null;
    }
}
