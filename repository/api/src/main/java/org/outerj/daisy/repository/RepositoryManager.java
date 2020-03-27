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
package org.outerj.daisy.repository;

import org.outerj.daisy.repository.user.User;

/**
 * A RepositoryManager is the place to get {@link Repository} instances from.
 *
 * <p>A RepositoryManager represents one 'physical' repository (one database).
 * All Repository instances retrieved from one RepositoryManager thus represent
 * the same repository, the difference between the instances is they are
 * authenticated for different users.
 *
 * <p>To communicate remotely with the repository (in other words, to use the remote
 * repository API implementation), simply instantiate a "RemoteRepositoryManager", as follows:
 *
 * <pre>
 * import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
 * ...
 * RepositoryManager repositoryManager = new RemoteRepositoryManager(
 *            "http://localhost:9263", new Credentials("guest", "guest"));
 * </pre>
 *
 * <p>More extensive getting-started documentation on this is included in the Daisy
 * documentation.
 */
public interface RepositoryManager {
    /**
     *
     * @throws AuthenticationFailedException if login failed because of incorrect credentials
     */
    Repository getRepository(Credentials credentials) throws RepositoryException;
    
    /**
     * @return a repository without checking credentials.  Since no credentials are checked, there should not be a remote implementations 
     */
    Repository getRepositoryAsUser(User user) throws RepositoryException;
    
    String getRepositoryServerVersion();
}
