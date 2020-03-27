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
package org.outerj.daisy.frontend.components.userregistrar;

import java.util.Locale;

public interface UserRegistrar {
    static final String ROLE = UserRegistrar.class.getName();

    void registerNewUser(String login, String password, String email, String firstName, String lastName,
            String server, String mountPoint, Locale locale) throws Exception;

    void confirmUserRegistration(long userId, String confirmKey) throws Exception;

    void sendPasswordReminder(String login, String server, String mountPoint, Locale locale) throws Exception;

    String assignNewPassword(long userId, String confirmKey) throws Exception;

    void sendLoginsReminder(String email, String server, String mountPoint, Locale locale) throws Exception;
}
