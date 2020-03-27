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
package org.outerj.daisy.workflow.commonimpl;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.workflow.WfPool;

import java.util.List;

public interface WfPoolStrategy {
    void store(WfPoolImpl pool, Repository repository) throws RepositoryException;

    WfPool getPool(long id, Repository repository) throws RepositoryException;

    WfPool getPoolByName(String name, Repository repository) throws RepositoryException;

    List<WfPool> getPools(Repository repository) throws RepositoryException;

    List<WfPool> getPoolsForUser(long userId, Repository repository) throws RepositoryException;

    void addUsersToPool(long poolId, List<Long> userIds, Repository repository) throws RepositoryException;

    void removeUsersFromPool(long poolId, List<Long> userIds, Repository repository) throws RepositoryException;

    void clearPool(long poolId, Repository repository) throws RepositoryException;

    List<Long> getUsersForPool(long poolId, Repository repository) throws RepositoryException;

    void deletePool(long poolId, Repository repository) throws RepositoryException;
}
