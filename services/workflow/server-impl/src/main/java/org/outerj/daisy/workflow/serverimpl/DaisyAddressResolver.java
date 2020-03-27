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
package org.outerj.daisy.workflow.serverimpl;

import org.jbpm.mail.AddressResolver;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.UserNotFoundException;

/**
 * A jBPM address resolver. It relies on a repository object being made
 * available through a thread local variable.
 */
public class DaisyAddressResolver implements AddressResolver {
    public static ThreadLocal<Repository> repository = new ThreadLocal<Repository>();

    public Object resolveAddress(String actorId) {
        long userId;
        try {
            userId = Long.parseLong(actorId);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid actor ID passed to resolveAddress: " + actorId);
        }

        try {
            return repository.get().getUserManager().getUser(userId, false).getEmail();
        } catch (UserNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            throw new RuntimeException("Error getting the workflow actor's email address from Daisy.", e);
        }
    }
}
