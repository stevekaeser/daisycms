/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.workflow;

import org.outerj.daisy.repository.RepositoryException;

import java.util.List;

/**
 * Management of workflow pools, which are basically sets of users.
 *
 * <p>Pools can be created/updated only by Administrators.
 *
 * <p>Accessing information on pools (which pools are available,
 * who is in which pools, ...) can be done by all users.
 */
public interface WfPoolManager {
    /**
     * Creates a new pool. This pool will not be immediately created
     * persistently, you need to call the save method on the returned
     * Pool object to do this.
     */
    WfPool createPool(String name);

    List<WfPool> getPools() throws RepositoryException;

    WfPool getPool(long id) throws RepositoryException;

    WfPool getPoolByName(String name) throws RepositoryException;

    List<Long> getUsersForPool(long poolId) throws RepositoryException;

    List<WfPool> getPoolsForUser(long userId) throws RepositoryException;

    void addUsersToPool(long poolId, List<Long> userIds) throws RepositoryException;

    void removeUsersFromPool(long poolId, List<Long> userIds) throws RepositoryException;

    /**
     * Removes all users from this pool.
     */
    void clearPool(long poolId) throws RepositoryException;

    void deletePool(long poolId) throws RepositoryException;
}
