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

import org.outerj.daisy.repository.*;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerx.daisy.x10.CollectionDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class CollectionHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/collection/*";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        long id = HttpUtil.parseId("collection", (String)matchMap.get("1"));
        CollectionManager collMan = repository.getCollectionManager();

        if (request.getMethod().equals(HttpConstants.GET)) {
            // we receive a GET, so the client wishes to retrieve the collection
            DocumentCollection docColl = collMan.getCollection(id, true);
            CollectionDocument docCollXml = docColl.getXml();
            docCollXml.save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            CollectionDocument collectionDocument = CollectionDocument.Factory.parse(request.getInputStream(), xmlOptions);
            CollectionDocument.Collection collectionXml = collectionDocument.getCollection();

            DocumentCollection collection = collMan.getCollection(id, true);

            // check for concurrent modifications
            if (collection.getUpdateCount() != collectionXml.getUpdatecount())
                throw new ConcurrentUpdateException(DocumentCollection.class.getName(), String.valueOf(collection.getId()));

            // update object
            collection.setName(collectionXml.getName());
            collection.save();

            collection.getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.DELETE)) {
            collMan.deleteCollection(id);
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
