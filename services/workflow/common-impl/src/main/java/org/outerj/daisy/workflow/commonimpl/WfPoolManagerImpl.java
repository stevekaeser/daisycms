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

import org.outerj.daisy.workflow.WfPoolManager;
import org.outerj.daisy.workflow.WfPool;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;

import java.util.List;

public class WfPoolManagerImpl implements WfPoolManager {
    private Repository repository;
    private WfPoolStrategy strategy;

    public WfPoolManagerImpl(Repository repository, WfPoolStrategy strategy) {
        this.repository = repository;
        this.strategy = strategy;
    }

    public WfPool createPool(String name) {
        return new WfPoolImpl(name, strategy, repository);
    }

    public WfPool getPool(long id) throws RepositoryException {
        return strategy.getPool(id, repository);
    }

    public WfPool getPoolByName(String name) throws RepositoryException {
        return strategy.getPoolByName(name, repository);
    }

    public List<Long> getUsersForPool(long poolId) throws RepositoryException {
        return strategy.getUsersForPool(poolId, repository);
    }

    public List<WfPool> getPoolsForUser(long userId) throws RepositoryException {
        return strategy.getPoolsForUser(userId, repository);
    }

    public void addUsersToPool(long poolId, List<Long> userIds) throws RepositoryException {
        strategy.addUsersToPool(poolId, userIds, repository);
    }

    public void removeUsersFromPool(long poolId, List<Long> userIds) throws RepositoryException {
        strategy.removeUsersFromPool(poolId, userIds, repository);
    }

    public void clearPool(long poolId) throws RepositoryException {
        strategy.clearPool(poolId, repository);
    }

    public List<WfPool> getPools() throws RepositoryException {
        return strategy.getPools(repository);
    }

    public void deletePool(long poolId) throws RepositoryException {
        strategy.deletePool(poolId, repository);
    }
}
