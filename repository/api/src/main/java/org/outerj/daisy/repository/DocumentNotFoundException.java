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
package org.outerj.daisy.repository;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.text.MessageFormat;

public class DocumentNotFoundException extends RepositoryException implements LocalizedException {
    private String id;

    private static final String ID_KEY = "id";

    public DocumentNotFoundException(String id) {
        this.id = id;
    }

    public DocumentNotFoundException(String id, Throwable cause) {
        super(cause);
        this.id = id;
    }

    public DocumentNotFoundException(Map params) {
        this.id = (String)params.get(ID_KEY);
    }

    public Map<String, String> getState() {
        Map<String, String> map = new HashMap<String, String>(1);
        map.put(ID_KEY, id);
        return map;
    }

    public String getMessage() {
        return getMessage(Locale.getDefault());
    }

    public String getMessage(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/repository/messages", locale);
        String message = bundle.getString("document-not-found-exception");
        return MessageFormat.format(message, id);
    }
}
