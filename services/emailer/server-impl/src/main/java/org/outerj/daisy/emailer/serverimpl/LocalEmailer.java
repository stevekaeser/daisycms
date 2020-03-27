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
package org.outerj.daisy.emailer.serverimpl;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.emailer.Emailer;

public class LocalEmailer implements Emailer {
    private Repository repository;
    private CommonEmailer commonEmailer;

    public LocalEmailer(Repository repository, CommonEmailer commonEmailer) {
        this.repository = repository;
        this.commonEmailer = commonEmailer;
    }

    public void send(String to, String subject, String messageText) throws RepositoryException {
        if (!repository.isInRole(Role.ADMINISTRATOR))
            throw new RepositoryException("Only administrator users can send emails.");

        commonEmailer.send(to, subject, messageText);
    }
}
