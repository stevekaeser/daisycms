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

public class DocumentWriteDeniedException extends AccessException implements LocalizedException {
    private String documentId;
    private String branch;
    private String language;
    private String user;

    protected DocumentWriteDeniedException() {
    }

    public DocumentWriteDeniedException(String documentId, String branch, String language, String user) {
        this.documentId = documentId;
        this.branch = branch;
        this.language = language;
        this.user = user;
    }

    public DocumentWriteDeniedException(Map<String, String> params) {
        this.documentId = params.get("documentId");
        this.branch = params.get("branch");
        this.language = params.get("language");
        this.user = params.get("user");
    }

    @Override
    public Map<String, String> getState() {
        Map<String, String> state = new HashMap<String, String>();
        state.put("documentId", documentId);
        state.put("branch", branch);
        state.put("language", language);
        state.put("user", user);
        return state;
    }

    public String getMessage(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/repository/messages", locale);
        String message = bundle.getString("document-write-denied-exception");
        return MessageFormat.format(message, documentId, branch, language, user);
    }

    public String getMessage() {
        return getMessage(Locale.getDefault());
    }
}
