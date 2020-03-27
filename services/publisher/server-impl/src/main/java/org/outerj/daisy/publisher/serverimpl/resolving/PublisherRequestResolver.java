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
package org.outerj.daisy.publisher.serverimpl.resolving;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.publisher.serverimpl.CommonPublisher;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherRequest;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContext;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.apache.xmlbeans.XmlOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.xml.sax.SAXException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

/**
 * Decides which publisher request to use for a certain document.
 */
public class PublisherRequestResolver {
    private File pubRequestsRoot;
    private Repository repository;
    private CommonPublisher commonPublisher;
    private Log log = LogFactory.getLog(getClass());
    private Map<String, CacheEntry> cache = new ConcurrentHashMap<String, CacheEntry>(16, .75f, 2);
    private final String resolveDefinitionMutex = "ResolveDefinitionMutex";
    private final String publisherRequestMutex = "PublisherRequestMutex";
    private ResolveDefinition defaultResolveDefinition;
    private Map<String, PublisherRequest> defaultPubReqs = new ConcurrentHashMap<String, PublisherRequest>(16, .75f, 2);

    public PublisherRequestResolver(File pubRequestsRoot, Repository repository, CommonPublisher commonPublisher) {
        this.pubRequestsRoot = pubRequestsRoot;
        this.repository = repository;
        this.commonPublisher = commonPublisher;
    }

    /**
     * Caller is responsible for closing the returned input stream!
     */
    public PublisherRequest lookupPublisherRequest(String pubReqSetName, Document document, Version version, PublisherContext publisherContext)
            throws PublisherException, RepositoryException, SAXException {
        ResolveDefinition resolveDefinition = getResolveDefinition(pubReqSetName);
        String pubReqName = resolveDefinition.resolve(document, version, publisherContext);
        if (pubReqName == null)
            throw new PublisherException("No publisher request found in set \"" + pubReqSetName + "\" for document " + document.getVariantKey());

        if (pubReqSetName.equals("default")) {
            return getDefaultPubReq(pubReqName);
        } else {
            File pubReqFile = new File(new File(pubRequestsRoot, pubReqSetName), pubReqName);
            return getPublisherRequest(pubReqFile);
        }
    }

    private ResolveDefinition getResolveDefinition(String pubReqSetName) throws PublisherException {
        if (pubReqSetName.equals("default"))
            return getDefaultResolveDefinition();

        ResolveDefinition resolveDefinition = (ResolveDefinition)getFromCache(pubReqSetName);
        if (resolveDefinition == null) {
            synchronized(resolveDefinitionMutex) {
                resolveDefinition = (ResolveDefinition)getFromCache(pubReqSetName);
                if (resolveDefinition == null) {
                    ResolveDefinitionBuilder builder = new ResolveDefinitionBuilder(repository, log);
                    File mappingFile = new File(new File(pubRequestsRoot, pubReqSetName), "mapping.xml");
                    long timestamp = mappingFile.lastModified();
                    resolveDefinition = builder.build(mappingFile, pubReqSetName);
                    putInCache(pubReqSetName, timestamp, mappingFile, resolveDefinition);
                }
            }
        }
        return resolveDefinition;
    }

    private PublisherRequest getPublisherRequest(File pubReqFile) throws PublisherException, RepositoryException, SAXException {
        String cacheKey = pubReqFile.getAbsolutePath();
        PublisherRequest publisherRequest = (PublisherRequest)getFromCache(cacheKey);
        if (publisherRequest == null) {
            synchronized(publisherRequestMutex) {
                publisherRequest = (PublisherRequest)getFromCache(cacheKey);
                if (publisherRequest == null) {
                    long timestamp = pubReqFile.lastModified();
                    PublisherRequestDocument publisherRequestDocument;
                    InputStream is = null;
                    try {
                        XmlOptions xmlOptions = new XmlOptions();
                        xmlOptions.setLoadLineNumbers();
                        xmlOptions.setDocumentSourceName(pubReqFile.getParentFile().getName() + "/" + pubReqFile.getName());
                        is = new BufferedInputStream(new FileInputStream(pubReqFile));
                        publisherRequestDocument = PublisherRequestDocument.Factory.parse(is, xmlOptions);
                    } catch (Exception e) {
                        throw new PublisherException("Error parsing publisher request from " + pubReqFile.getAbsolutePath(), e);
                    } finally {
                        if (is != null) {
                            try {
                                is.close();
                            } catch (IOException e) {
                                log.error("Error closing publisher request input stream for " + pubReqFile.getAbsolutePath(), e);
                            }
                        }
                    }

                    publisherRequest = commonPublisher.buildPublisherRequest(publisherRequestDocument);
                    putInCache(cacheKey, timestamp, pubReqFile, publisherRequest);
                }
            }
        }
        return publisherRequest;
    }

    private synchronized Object getFromCache(String cacheKey) {
        CacheEntry cacheEntry = cache.get(cacheKey);
        if (cacheEntry == null) {
            return null;
        } else if (cacheEntry.timestamp != cacheEntry.file.lastModified()) {
            cache.remove(cacheKey);
            return null;
        } else {
            return cacheEntry.object;
        }
    }

    private synchronized void putInCache(String cacheKey, long timestamp, File file, Object object) {
        CacheEntry cacheEntry = new CacheEntry();
        cacheEntry.timestamp = timestamp;
        cacheEntry.file = file;
        cacheEntry.object = object;
        cache.put(cacheKey, cacheEntry);
    }

    private static class CacheEntry {
        long timestamp;
        File file;
        Object object;
    }

    private ResolveDefinition getDefaultResolveDefinition() throws PublisherException {
        if (this.defaultResolveDefinition != null)
            return this.defaultResolveDefinition;

        synchronized(this) {
            if (this.defaultResolveDefinition != null)
                return this.defaultResolveDefinition;

            InputStream is = null;
            try {
                is = this.getClass().getClassLoader().getResourceAsStream("org/outerj/daisy/publisher/serverimpl/resolving/default/mapping.xml");
                ResolveDefinitionBuilder builder = new ResolveDefinitionBuilder(repository, log);
                this.defaultResolveDefinition = builder.build(is, "default");
                return this.defaultResolveDefinition;
            } finally {
                if (is != null)
                    try { is.close(); } catch (Exception e) { log.error("Error closing InputStream in finally.", e); }
            }
        }
    }

    private PublisherRequest getDefaultPubReq(String name) throws PublisherException, RepositoryException, SAXException {
        PublisherRequest publisherRequest = defaultPubReqs.get(name);
        if (publisherRequest != null)
            return publisherRequest;

        synchronized (this) {
            publisherRequest = defaultPubReqs.get(name);
            if (publisherRequest != null)
                return publisherRequest;

            InputStream is = null;
            try {
                String path = "org/outerj/daisy/publisher/serverimpl/resolving/default/" + name;
                is = this.getClass().getClassLoader().getResourceAsStream(path);

                PublisherRequestDocument publisherRequestDocument;
                try {
                    XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                    publisherRequestDocument = PublisherRequestDocument.Factory.parse(is, xmlOptions);
                } catch (Exception e) {
                    throw new PublisherException("Error parsing publisher request from resource " + path, e);
                }

                publisherRequest = commonPublisher.buildPublisherRequest(publisherRequestDocument);
                defaultPubReqs.put(name, publisherRequest);
                return publisherRequest;
            } finally {
                if (is != null)
                    try { is.close(); } catch (Exception e) { log.error("Error closing InputStream in finally.", e); }
            }
        }
    }

}
