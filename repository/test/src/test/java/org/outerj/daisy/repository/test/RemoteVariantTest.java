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
package org.outerj.daisy.repository.test;

import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;

public class RemoteVariantTest extends AbstractVariantTest {
    protected RepositoryManager getRepositoryManager() throws Exception {
        return getRemoteRepositoryManager();
    }

    protected void moreTests() throws Exception {
        checkRemoteCacheInvalidation();
    }

    private void checkRemoteCacheInvalidation() throws Exception {
        RepositoryManager localRepositoryManager = getLocalRepositoryManager();
        Repository localRepository = localRepositoryManager.getRepository(new Credentials("testuser", "testuser"));
        localRepository.switchRole(Role.ADMINISTRATOR);

        RepositoryManager remoteRepositoryManager = getRemoteRepositoryManager();
        Repository remoteRepository = remoteRepositoryManager.getRepository(new Credentials("testuser", "testuser"));
        remoteRepository.switchRole(Role.ADMINISTRATOR);

        VariantManager localVariantManager = localRepository.getVariantManager();
        VariantManager remoteVariantManager = remoteRepository.getVariantManager();

        {
            Branch localBranch = localVariantManager.createBranch("branch99");
            localBranch.save();

            Branch remoteBranch = remoteVariantManager.getBranch(localBranch.getId(), false);

            localBranch.setName("branch100");
            localBranch.save();

            // give some time to receive JMS event
            Thread.sleep(5000);

            remoteBranch = remoteVariantManager.getBranch(localBranch.getId(), false);
            assertEquals("branch100", remoteBranch.getName());
        }

        {
            Language localLanguage = localVariantManager.createLanguage("language99");
            localLanguage.save();

            // make sure the variant cache is loaded
            Language remoteLanguage = remoteVariantManager.getLanguage(localLanguage.getId(), false);

            //update the object locally
            localLanguage.setName("language100");
            localLanguage.save();

            // give some time to receive JMS event
            Thread.sleep(5000);

            // check that the remote cache has been updated
            remoteLanguage = remoteVariantManager.getLanguage(localLanguage.getId(), false);
            assertEquals("language100", remoteLanguage.getName());
        }
    }
}
