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
package org.outerj.daisy.publisher;

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Using the Publisher it is possible to retrieve a variety of information from the repository
 * using one (remote) call, the response it returned as XML. The Publisher also has performs
 * additional processing and annotation on the content to prepare it for publishing.
 *
 * <p>This component helps a lot to create publishing frontends to the repository server,
 * of which the Daisy Wiki is one example. The Daisy documentation contains more extensive
 * documentation on the possibilities of the Publisher.
 *
 * <p>This is an optional repository extension component.
 *
 * <p>The Publisher is obtained from the {@link org.outerj.daisy.repository.Repository Repository} as
 * follows:
 *
 * <pre>
 * Publisher publisher = (Publisher)repository.getExtension("Publisher");
 * </pre>
 *
 * <p>In the remote repository API, the Publisher extension can be registered as follows:
 *
 * <pre>
 * RemoteRepositoryManager repositoryManager = ...;
 * repositoryManager.registerExtension("Publisher",
 *     new Packages.org.outerj.daisy.publisher.clientimpl.RemotePublisherProvider());
 * </pre>
 */
public interface Publisher {
    /**
     * <p>Retrieves information about a document part without having to go through
     * the normal repository API. In the normal API, this would require multiple API
     * calls, which is a performance disadvantage when using the remote implementation of the API
     * (for the in-VM API, it doesn't make any difference in performance).
     *
     * <b>Important: after usage, you must call the dispose method of the BlobInfo object.</b>
     *
     * @param versionSpec a version number or a version mode string.
     * @param partType a part type id or part type name
     */
    BlobInfo getBlobInfo(VariantKey variantKey, String versionSpec, String partType) throws RepositoryException;

    /**
     * Processes a publisher request. See the Daisy documentation for more information on
     * publisher requests.
     */
    void processRequest(PublisherRequestDocument publisherRequestDocument, ContentHandler contentHandler)
            throws SAXException, RepositoryException, PublisherException;
}
