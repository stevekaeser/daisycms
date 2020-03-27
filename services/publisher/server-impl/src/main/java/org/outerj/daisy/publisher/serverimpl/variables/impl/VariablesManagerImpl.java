/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.publisher.serverimpl.variables.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.publisher.serverimpl.variables.Variables;
import org.outerj.daisy.publisher.serverimpl.variables.VariablesManager;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryListener;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionedData;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;

public class VariablesManagerImpl implements VariablesManager, RepositoryListener {
    private Repository repository;
    // TODO this cache has currently no eviction (only grows, never shrinks)
    private ConcurrentHashMap<VersionKey, Future<Variables>> cache = new ConcurrentHashMap<VersionKey, Future<Variables>>();
    private Log log = LogFactory.getLog(getClass());

    public VariablesManagerImpl(Repository repository) {
        this.repository = repository;
        repository.addListener(this);
    }

    public void destroy() {
        repository.removeListener(this);
    }

    public Variables getVariables(VariantKey[] variableDocs, Repository userRepository, VersionMode versionMode) {

        Set<VariantKey> loadedVariantKeys = new HashSet<VariantKey>();

        List<Variables> variablesList = new ArrayList<Variables>(variableDocs.length);

        for (VariantKey key : variableDocs) {
            Variables variables = getVariables(key, versionMode, loadedVariantKeys);
            if (variables != null && canRead(key, userRepository, versionMode)) {
                variablesList.add(variables);
            }
        }

        return new MultiVariables(variablesList.toArray(new Variables[0]));
    }

    private boolean canRead(VariantKey key, Repository userRepository, VersionMode versionMode) {
        try {
            AclResultInfo aclResultInfo = repository.getAccessManager().getAclInfoOnLive(userRepository.getUserId(), userRepository.getActiveRoleIds(), key);
            boolean allowed;
            if (versionMode.isLast()) {
                allowed = aclResultInfo.isNonLiveAllowed(AclPermission.READ);
            } else if (versionMode.isLive()) {
                allowed = aclResultInfo.isAllowed(AclPermission.READ);
            } else {
                allowed = aclResultInfo.isAllowed(AclPermission.READ);
            }
            return allowed;
        } catch (Throwable e) {
            log.error("Failed to check access rights on document " + key + " for user " + userRepository.getUserLogin(), e);
            return false;
        }
    }


    private Variables getVariables(final VariantKey inputKey, final VersionMode versionMode, Set<VariantKey> loadedVariantKeys) {
        // normalize the variant key
        String docId = repository.normalizeDocumentId(inputKey.getDocumentId());
        final VariantKey key = new VariantKey(docId, inputKey.getBranchId(), inputKey.getLanguageId());

        // avoid loading same document twice
        if (loadedVariantKeys.contains(key))
            return null;
        else
            loadedVariantKeys.add(key);

        VersionKey cacheKey;
        try {
            long versionId = repository.getDocument(key, false).getVersionId(versionMode);
            cacheKey = new VersionKey(inputKey.getDocumentId(), inputKey.getBranchId(), inputKey.getLanguageId(), versionId);
        } catch (RepositoryException re) {
            throw new RuntimeException("Failed to create cache key for caching variables.");
        }
        Future<Variables> future = cache.get(cacheKey);
        if (future == null) {
            Callable<Variables> eval = new Callable<Variables>() {
                public Variables call() throws Exception {
                    return loadVariables(key, versionMode);
                }
            };
            FutureTask<Variables> futureTask = new FutureTask<Variables>(eval);
            future = cache.putIfAbsent(cacheKey, futureTask);
            if (future == null) {
                future = futureTask;
                futureTask.run();
            }
        }
        try {
            return future.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Variables loadVariables(VariantKey key, VersionMode versionMode) {
        try {
            Document document = repository.getDocument(key, false);

            VersionedData data = document.getVersion(versionMode);

            if (data != null)
                return VariablesParser.parseVariables(data);
        } catch (Throwable e) {
            log.error("Error reading variables from document " + key, e);
        }
        return null;
    }

    public void repositoryEvent(RepositoryEventType eventType, Object id, long updateCount) {
        if (eventType.isVariantEvent()) {
            VariantKey key = (VariantKey)id;
            for (VersionKey cacheKey: cache.keySet()) {
                if (cacheKey.getDocumentId().equals(key.getDocumentId()) &&
                        cacheKey.getBranchId() == key.getLanguageId() &&
                        cacheKey.getLanguageId() == key.getLanguageId()) {
                    cache.remove(cacheKey);
                }
            }
        }
    }
}
