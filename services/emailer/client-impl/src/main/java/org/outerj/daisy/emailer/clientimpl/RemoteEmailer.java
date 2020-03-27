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
package org.outerj.daisy.emailer.clientimpl;

import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.emailer.Emailer;
import org.apache.commons.httpclient.methods.PostMethod;

public class RemoteEmailer implements Emailer {
    private RemoteRepositoryImpl repository;

    public RemoteEmailer(RemoteRepositoryImpl repository) {
        this.repository = repository;
    }

    public void send(String to, String subject, String messageText) throws RepositoryException {
        if (to == null || to.equals(""))
            throw new IllegalArgumentException("Null or empty value for 'to' parameter");
        if (subject == null)
            throw new IllegalArgumentException("Null value for 'subject' parameter");
        if (messageText == null)
            throw new IllegalArgumentException("Null value for 'messageText' parameter");

        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/emailer/mail");
        method.addParameter("to", to);
        method.addParameter("subject", subject);
        method.addParameter("messageText", messageText);

        httpClient.executeMethod(method, null, true);
    }
}
