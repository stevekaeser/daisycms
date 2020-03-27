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
package org.outerj.daisy.emailer;

import org.outerj.daisy.repository.RepositoryException;

/**
 * An emailing service. This component is currently mostly meant for Daisy-internal
 * purposes, such as for the notification mails or the new-user registration mails.
 *
 * <p>This is an optional repository extension component.
 *
 * <p>The Emailer is obtained from the {@link org.outerj.daisy.repository.Repository Repository} as
 * follows:
 *
 * <pre>
 * Emailer emailer = (Emailer)repository.getExtension("Emailer");
 * </pre>
 *
 * <p>In the remote repository API, the Emailer extension can be registered as follows:
 *
 * <pre>
 * RemoteRepositoryManager repositoryManager = ...;
 * repositoryManager.registerExtension("Emailer",
 *     new Packages.org.outerj.daisy.emailer.clientimpl.RemoteEmailerProvider());
 * </pre>
 */
public interface Emailer {
    /**
     * Sends an email. In case for some reason sending the email fails,
     * then the implementation of this service should take care of that
     * (ie retry after certain time, log it, notify an admin, ...), rather
     * then throwing an exception.
     */
    public void send(String to, String subject, String messageText) throws RepositoryException;
}
