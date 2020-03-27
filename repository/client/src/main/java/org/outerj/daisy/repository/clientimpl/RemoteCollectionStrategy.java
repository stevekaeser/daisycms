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
package org.outerj.daisy.repository.clientimpl;

import org.outerj.daisy.repository.commonimpl.DocumentCollectionImpl;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.CollectionStrategy;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.repository.clientimpl.infrastructure.AbstractRemoteStrategy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.HttpMethod;
import org.outerx.daisy.x10.CollectionDocument;
import org.outerx.daisy.x10.CollectionsDocument;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

public class RemoteCollectionStrategy extends AbstractRemoteStrategy implements CollectionStrategy {

    public RemoteCollectionStrategy(RemoteRepositoryManager.Context context) {
        super(context);
    }

    public void store(DocumentCollectionImpl collection) throws RepositoryException {
        if (collection==null) throw new RuntimeException("DocumentCollectionImpl expected - instead received null!");
        DocumentCollectionImpl.IntimateAccess collInt = collection.getIntimateAccess(this);

        DaisyHttpClient httpClient = getClient(collInt.getCurrentUser());

        String url = "/repository";
        boolean isNew = collection.getId() == -1;
        if (isNew)
            url += "/collection";
        else
            url += "/collection/" + collection.getId();

        PostMethod method = new PostMethod(url);

        CollectionDocument collectionDocument = collection.getXml();
        method.setRequestEntity(new InputStreamRequestEntity(collectionDocument.newInputStream()));

        CollectionDocument responseCollectionDocument = (CollectionDocument)httpClient.executeMethod(method, CollectionDocument.class, true);
        CollectionDocument.Collection collectionXml = responseCollectionDocument.getCollection();
        DocumentCollectionImpl.IntimateAccess collectionInt = collection.getIntimateAccess(this);
        collectionInt.saved(collectionXml.getId(), collectionXml.getName(), collectionXml.getLastModified().getTime(), collectionXml.getLastModifier(), collectionXml.getUpdatecount());

        if (isNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.COLLECTION_CREATED, new Long(collection.getId()), collection.getUpdateCount());
        else
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.COLLECTION_UPDATED, new Long(collection.getId()), collection.getUpdateCount());
    }

    public DocumentCollectionImpl loadCollection(long collectionId, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/collection/" + collectionId);

        CollectionDocument collectionDocument = (CollectionDocument)httpClient.executeMethod(method, CollectionDocument.class, true);
        CollectionDocument.Collection documentXml = collectionDocument.getCollection();
        DocumentCollectionImpl document = instantiateCollectionFromXml(documentXml, user);
        return document;
    }

    public DocumentCollectionImpl loadCollectionByName(String name, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        String encodedName = encodeNameForUseInPath("collection", name);
        HttpMethod method = new GetMethod("/repository/collectionByName/" + encodedName);

        CollectionDocument collectionDocument = (CollectionDocument)httpClient.executeMethod(method, CollectionDocument.class, true);
        CollectionDocument.Collection documentXml = collectionDocument.getCollection();
        DocumentCollectionImpl document = instantiateCollectionFromXml(documentXml, user);
        return document;
    }

    private DocumentCollectionImpl instantiateCollectionFromXml(CollectionDocument.Collection collectionXml, AuthenticatedUser user) {
        DocumentCollectionImpl docColl = new DocumentCollectionImpl(this, collectionXml.getName(), user);

        DocumentCollectionImpl.IntimateAccess collInt = docColl.getIntimateAccess(this);
        collInt.setId(collectionXml.getId());
        collInt.setLastModified(collectionXml.getLastModified().getTime());
        collInt.setLastModifier(collectionXml.getLastModifier());
        collInt.setUpdateCount(collectionXml.getUpdatecount());

        return docColl;
    }

    public Collection<DocumentCollectionImpl> loadCollections(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/collection");

        CollectionsDocument documentCollectionsDocument = (CollectionsDocument)httpClient.executeMethod(method, CollectionsDocument.class, true);
        CollectionsDocument.Collections collections = documentCollectionsDocument.getCollections();
        List<CollectionDocument.Collection> collectionsXml = collections.getCollectionList();
        List<DocumentCollectionImpl> documentCollections = new ArrayList<DocumentCollectionImpl>(collectionsXml.size());
        for (CollectionDocument.Collection collectionXml : collectionsXml) {
            DocumentCollectionImpl documentCollection = instantiateCollectionFromXml(collectionXml, user);
            documentCollections.add(documentCollection);
        }
        return documentCollections;
    }

    public void deleteCollection(long collectionId, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        DeleteMethod method = new DeleteMethod("/repository/collection/" + collectionId);
        httpClient.executeMethod(method, null, true);
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.COLLECTION_DELETED, new Long(collectionId), -1);
    }
}
