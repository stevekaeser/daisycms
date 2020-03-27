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
package org.outerj.daisy.repository;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.text.MessageFormat;

public class VersionNotFoundException  extends RepositoryException implements LocalizedException {
    private String id;
    private String documentId;
    private String branch;
    private String language;

    private static final String ID_KEY = "id";
    private static final String DOCID_KEY = "documentId";
    private static final String BRANCH_KEY = "branch";
    private static final String LANG_KEY = "language";

    public VersionNotFoundException(String id, String documentId, String branch, String language) {
        this.id = id;
        this.documentId = documentId;
        this.branch = branch;
        this.language = language;
    }

    public VersionNotFoundException(String id, Throwable cause) {
        super(cause);
        this.id = id;
    }

    public VersionNotFoundException(Map params) {
        this.id = (String)params.get(ID_KEY);
        this.documentId = (String)params.get(DOCID_KEY);
        this.branch = (String)params.get(BRANCH_KEY);
        this.language = (String)params.get(LANG_KEY);
    }

    public Map<String, String> getState() {
        Map<String, String> map = new HashMap<String, String>(1);
        map.put(ID_KEY, id);
        map.put(DOCID_KEY, documentId);
        map.put(BRANCH_KEY, branch);
        map.put(LANG_KEY, language);
        return map;
    }

    public String getMessage() {
        return getMessage(Locale.getDefault());
    }

    public String getMessage(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/repository/messages", locale);
        String message = bundle.getString("version-not-found-exception");
        return MessageFormat.format(message, id, documentId, branch, language);
    }
}

