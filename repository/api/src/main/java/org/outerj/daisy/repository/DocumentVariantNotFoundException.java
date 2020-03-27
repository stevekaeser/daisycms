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

public class DocumentVariantNotFoundException  extends RepositoryException implements LocalizedException {
    private String documentId;
    private String branchName;
    private long branchId;
    private String languageName;
    private long languageId;

    private static final String DOC_ID_KEY = "docId";
    private static final String BRANCH_KEY = "branchName";
    private static final String LANG_KEY = "langName";
    private static final String BRANCH_ID_KEY = "branchId";
    private static final String LANG_ID_KEY = "langId";

    public DocumentVariantNotFoundException(String documentId, String branchName, long branchId, String languageName, long languageId) {
        this.documentId = documentId;
        this.branchName = branchName;
        this.branchId = branchId;
        this.languageName = languageName;
        this.languageId = languageId;
    }

    public DocumentVariantNotFoundException(Map params) {
        this.documentId = (String)params.get(DOC_ID_KEY);
        this.branchName = (String)params.get(BRANCH_KEY);
        this.branchId = Long.parseLong((String)params.get(BRANCH_ID_KEY));
        this.languageName = (String)params.get(LANG_KEY);
        this.languageId = Long.parseLong((String)params.get(LANG_ID_KEY));
    }

    public Map<String, String> getState() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(DOC_ID_KEY, documentId);
        map.put(BRANCH_ID_KEY, String.valueOf(branchId));
        map.put(LANG_ID_KEY, String.valueOf(languageId));
        if (branchName != null)
            map.put(BRANCH_KEY, branchName);
        if (languageName != null)
            map.put(LANG_KEY, languageName);
        return map;
    }

    public String getMessage() {
        return getMessage(Locale.getDefault());
    }

    public String getMessage(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/repository/messages", locale);
        String message = bundle.getString("documentvariant-not-found-exception");
        String branch = branchName != null ? branchName + " (ID " + branchId + ")" : String.valueOf(branchId);
        String language = languageName != null ? languageName + " (ID " + languageId + ")" : String.valueOf(languageId);
        return MessageFormat.format(message, documentId, branch, language);
    }

    public String getDocumentId() {
        return documentId;
    }

    public long getBranchId() {
        return branchId;
    }

    public long getLanguageId() {
        return languageId;
    }
}