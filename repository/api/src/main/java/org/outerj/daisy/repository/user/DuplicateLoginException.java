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
package org.outerj.daisy.repository.user;

import org.outerj.daisy.repository.LocalizedException;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Map;
import java.util.HashMap;
import java.text.MessageFormat;

public class DuplicateLoginException extends UserManagementException implements LocalizedException {
    private String login;

    public DuplicateLoginException(String login) {
        this.login = login;
    }

    public DuplicateLoginException(Map state) {
        login = (String)state.get("login");
    }

    public Map<String, String> getState() {
        Map<String, String> state = new HashMap<String, String>();
        state.put("login", login);
        return state;
    }

    public String getMessage() {
        return getMessage(Locale.getDefault());
    }

    public String getMessage(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/repository/messages", locale);
        return MessageFormat.format(bundle.getString("duplicate-login"), login);
    }
}
