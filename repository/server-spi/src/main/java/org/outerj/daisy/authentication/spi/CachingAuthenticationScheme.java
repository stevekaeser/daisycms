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
package org.outerj.daisy.authentication.spi;

import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.apache.commons.collections.map.LRUMap;

import java.util.Collections;
import java.util.Map;

/**
 * A wrapper around an AuthenticationScheme that performs credential caching.
 */
public class CachingAuthenticationScheme implements AuthenticationScheme {
    private AuthenticationScheme delegate;
    private long maxCacheDuration;
    private Map cache;

    /**
     *
     * @param maxCacheDuration max time an entry can stay in the cache before becoming invalid, in millis
     * @param maxCacheSize maximum size of the cache (should be large enough to handle max expected concurrent users
     *                     for optimal performance)
     */
    public CachingAuthenticationScheme(AuthenticationScheme delegate, long maxCacheDuration, int maxCacheSize) {
        this.delegate = delegate;
        this.maxCacheDuration = maxCacheDuration;
        this.cache = Collections.synchronizedMap(new LRUMap(maxCacheSize));
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public void clearCaches() {
        cache.clear();
    }

    public boolean check(Credentials credentials) throws AuthenticationException {
        String password = getFromCache(credentials.getLogin());

        if (password != null && password.equals(credentials.getPassword()))
            return true;

        boolean valid = delegate.check(credentials);
        if (valid) {
            putInCache(credentials);
            return true;
        }

        return false;
    }

    public User createUser(Credentials crendentials, UserManager userManager) throws AuthenticationException {
        return delegate.createUser(crendentials, userManager);
    }

    private String getFromCache(String login) {
        CacheEntry cacheEntry = (CacheEntry)cache.get(login);
        if (cacheEntry != null) {
            if ((System.currentTimeMillis() - cacheEntry.created) > maxCacheDuration) {
                cache.remove(login);
                return null;
            }
            return cacheEntry.password;
        }
        return null;
    }

    private void putInCache(Credentials credentials) {
        cache.put(credentials.getLogin(), new CacheEntry(credentials.getPassword()));
    }

    private static class CacheEntry {
        long created = System.currentTimeMillis();
        String password;

        CacheEntry(String password) {
            this.password = password;
        }
    }
}
