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

/**
 * This exception is thrown when trying to save a document that is locked.
 */
public class DocumentLockedException extends RepositoryException implements LocalizedException {
    private String documentId;
    private long lockOwner;
    private String lockOwnerName;

    private static final String DOCID_KEY = "docId";
    private static final String LOCKOWNER_KEY = "lockOwnerId";
    private static final String LOCKOWNER_NAME_KEY = "lockOwnerName";

    public DocumentLockedException(String documentId, long lockOwner, String lockOwnerName) {
        this.documentId = documentId;
        this.lockOwner = lockOwner;
        this.lockOwnerName = lockOwnerName;
    }

    public DocumentLockedException(Map params) {
        this.documentId = (String)params.get(DOCID_KEY);
        this.lockOwner = Long.parseLong((String)params.get(LOCKOWNER_KEY));
        this.lockOwnerName = (String)params.get(LOCKOWNER_NAME_KEY);
    }

    public Map<String, String> getState() {
        Map<String, String> map = new HashMap<String, String>(3);
        map.put(DOCID_KEY, documentId);
        map.put(LOCKOWNER_KEY, String.valueOf(lockOwner));
        map.put(LOCKOWNER_NAME_KEY, lockOwnerName);
        return map;
    }

    public String getMessage() {
        return getMessage(Locale.getDefault());
    }

    public String getMessage(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/repository/messages", locale);
        String message = bundle.getString("document-locked-exception");
        return MessageFormat.format(message, documentId, String.valueOf(lockOwner), lockOwnerName);
    }
}

