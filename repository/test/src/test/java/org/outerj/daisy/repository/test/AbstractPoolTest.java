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
package org.outerj.daisy.repository.test;

import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.WfPoolManager;
import org.outerj.daisy.workflow.WfPool;
import org.outerj.daisy.workflow.WorkflowException;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Collections;

public abstract class AbstractPoolTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected Set<String> getDisabledContainerIds() {
        return Collections.emptySet();
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testPoolManagement() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();

        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);

        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        WfPoolManager poolManager = workflowManager.getPoolManager();

        WfPool pool = poolManager.createPool("My first pool");
        pool.save();
        assertNotNull(pool.getLastModified());
        assertEquals(repository.getUserId(), pool.getLastModifier());

        WfPool refetchedPool = poolManager.getPool(pool.getId());
        assertNotNull(refetchedPool);
        refetchedPool.setDescription("This is my first pool.");
        refetchedPool.save();

        pool = poolManager.getPoolByName("My first pool");
        assertEquals("My first pool", pool.getName());
        assertEquals("This is my first pool.", pool.getDescription());

        WfPool pool2 = poolManager.createPool("My second pool");
        pool2.save();

        assertEquals(2, poolManager.getPools().size());

        poolManager.addUsersToPool(pool.getId(), idList(1, 2, 3));
        poolManager.addUsersToPool(pool2.getId(), idList(2));
        // adding user again to same pool should not give an exception
        poolManager.addUsersToPool(pool2.getId(), idList(2));
        poolManager.addUsersToPool(pool2.getId(), idList());

        try {
            poolManager.addUsersToPool(23232, idList(1, 2, 3));
            fail("Adding users to non-existing pool should give an exception.");
        } catch (WorkflowException e) {}

        List<WfPool> userPools = poolManager.getPoolsForUser(2);
        assertEquals(2, userPools.size());

        assertEquals(3, poolManager.getUsersForPool(pool.getId()).size());
        assertEquals(1, poolManager.getUsersForPool(pool2.getId()).size());

        poolManager.clearPool(pool2.getId());
        assertEquals(0, poolManager.getUsersForPool(pool2.getId()).size());

        poolManager.removeUsersFromPool(pool.getId(), idList(3, 4, 5));
        assertEquals(2, poolManager.getUsersForPool(pool.getId()).size());

        // TODO test nonadmin users cannot update/create pools

    }

    private List<Long> idList(long... ids) {
        List<Long> list = new ArrayList<Long>(ids.length);
        for (long id : ids) {
            list.add(id);
        }
        return list;
    }
}
