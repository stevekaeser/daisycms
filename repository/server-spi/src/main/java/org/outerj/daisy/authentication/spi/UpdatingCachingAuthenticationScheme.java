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

import java.util.Collections;

import org.apache.commons.collections.map.LRUMap;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;

public class UpdatingCachingAuthenticationScheme extends CachingAuthenticationScheme implements UpdatingAuthenticationScheme {
    private UpdatingAuthenticationScheme delegate;
    
    public UpdatingCachingAuthenticationScheme(UpdatingAuthenticationScheme delegate, long maxCacheDuration, int maxCacheSize) {
        super(delegate, maxCacheDuration, maxCacheSize);
        this.delegate = delegate;        
    }

    public void update(User user, UserManager userManager) throws RepositoryException {
       this.delegate.update(user,userManager);
    }

}
