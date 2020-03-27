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
package org.outerj.daisy.cache.impl;

import org.outerj.daisy.cache.DocumentCache;
import org.outerj.daisy.repository.commonimpl.DocumentImpl;
import org.outerj.daisy.repository.AvailableVariants;
import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.collections.map.MultiKeyMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.annotation.PreDestroy;

/**
 * Implementation of {@link DocumentCache} that uses a LRUMap with a configurable
 * limit. JMX manageable.
 *
 */
public class DocumentCacheImpl implements DocumentCache, DocumentCacheImplMBean {
    private MultiKeyMap documentCache;
    private LRUMap documentCacheBacker;
    private LRUMap availableVariantsCache;
    private int documentCacheMaxSize;
    private int availableVariantsCacheMaxSize;
    private static final int DEFAULT_MAX_SIZE = 10000;
    private MBeanServer mbeanServer;
    private ObjectName mbeanName = new ObjectName("Daisy:name=DocumentCache");
    private final Log log = LogFactory.getLog(getClass());

    public DocumentCacheImpl(Configuration configuration, MBeanServer mbeanServer) throws Exception {
        this.mbeanServer = mbeanServer;
        this.configure(configuration);
        this.initialize();
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        documentCacheMaxSize = configuration.getChild("documentCacheMaxSize").getValueAsInteger(DEFAULT_MAX_SIZE);
        availableVariantsCacheMaxSize = configuration.getChild("availableVariantsCacheMaxSize").getValueAsInteger(DEFAULT_MAX_SIZE);
    }

    private void initialize() throws Exception {
        documentCacheBacker = new LRUMap(documentCacheMaxSize);
        documentCache = MultiKeyMap.decorate(documentCacheBacker);
        availableVariantsCache = new LRUMap(availableVariantsCacheMaxSize);
        mbeanServer.registerMBean(this, mbeanName);
    }

    @PreDestroy
    public void destroy() {
        try {
            mbeanServer.unregisterMBean(mbeanName);
        } catch (Exception e) {
            log.error("Error unregistering MBean", e);
        }
    }

    public void clear() {
        documentCache.clear();
        availableVariantsCache.clear();
    }

    public synchronized void put(String documentId, long branchId, long languageId, DocumentImpl document) {
        documentCache.put(documentId, new Long(branchId), new Long(languageId), document);
    }

    public synchronized DocumentImpl get(String documentId, long branchId, long languageId) {
        return (DocumentImpl)documentCache.get(documentId, new Long(branchId), new Long(languageId));
    }

    public synchronized void remove(String documentId) {
        documentCache.removeAll(documentId);
    }

    public synchronized void remove(String documentId, long branchId, long languageId) {
        documentCache.remove(documentId, new Long(branchId), new Long(languageId));
    }

    public synchronized void put(String documentId, AvailableVariants availableVariants) {
        availableVariantsCache.put(documentId, availableVariants);
    }

    public synchronized AvailableVariants getAvailableVariants(String documentId) {
        return (AvailableVariants)availableVariantsCache.get(documentId);
    }

    public synchronized void removeAvailableVariants(String documentId) {
        availableVariantsCache.remove(documentId);
    }

    public synchronized int getDocumentCacheMaxSize() {
        return documentCacheBacker.maxSize();
    }

    public synchronized int getDocumentCacheCurrentSize() {
        return documentCache.size();
    }

    public int getAvailableVariantCacheMaxSize() {
        return availableVariantsCache.maxSize();
    }

    public int getAvailableVariantCacheCurrentSize() {
        return availableVariantsCache.size();
    }

    public synchronized void clearCache() {
        documentCache.clear();
    }
}
