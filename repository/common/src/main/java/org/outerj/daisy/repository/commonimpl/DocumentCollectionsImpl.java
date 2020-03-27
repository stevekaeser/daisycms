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

import java.util.ArrayList;

import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.DocumentCollections;
import org.outerx.daisy.x10.CollectionsDocument;
import org.outerx.daisy.x10.CollectionDocument;

/**
 * implementation class of Collections
 */
public class DocumentCollectionsImpl implements DocumentCollections{
    private final DocumentCollection[] collections;
    
    public DocumentCollectionsImpl (DocumentCollection[] collections) {
        this.collections = collections;
    }

    public DocumentCollection[] getArray() {
        return collections;
    }

    public CollectionsDocument getXml() {
        CollectionsDocument collectionsDocument = CollectionsDocument.Factory.newInstance();
        CollectionsDocument.Collections collectionsXml = collectionsDocument.addNewCollections();
        
        ArrayList collectionList = new ArrayList();
        for (int i = 0; i < collections.length; i++) {
            collectionList.add(collections[i].getXml().getCollection());
        }
        
        collectionsXml.setCollectionArray((CollectionDocument.Collection[])collectionList.toArray(new CollectionDocument.Collection[collectionList.size()]));
    
        return collectionsDocument;
    }
}
