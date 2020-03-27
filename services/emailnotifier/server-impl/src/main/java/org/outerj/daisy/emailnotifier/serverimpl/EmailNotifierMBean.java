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
package org.outerj.daisy.emailnotifier.serverimpl;

public interface EmailNotifierMBean {
    /**
     * Adds a user whose modifications will be ignored for sending emails.
     * In other words, if this user performs some update (create a document,
     * update the ACL, whathever), then no e-mail notification will be sent
     * out (to anyone).
     */
    void addIgnoreUser(String login);

    void removeIgnoreUser(String login);

    int getIgnoredUserCount();

    String[] getIgnoredUsers();

    boolean getEnabled();

    void enable();

    /**
     * Ignore received change events (do not send out e-mail notifications).
     */
    void disable();
}
