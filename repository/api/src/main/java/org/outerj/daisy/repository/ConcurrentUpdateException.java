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

import java.util.*;
import java.text.MessageFormat;

/**
 * A concurrent update happened to some repository-managed entity.
 */
public class ConcurrentUpdateException extends RepositoryException implements LocalizedException {
    private String objectType;
    private String objectId;

    private static final String OBJECT_TYPE = "objectType";
    private static final String OBJECT_ID = "objectId";

    public ConcurrentUpdateException(String objectType, String objectId) {
        this.objectType = objectType;
        this.objectId = objectId;
    }

    public ConcurrentUpdateException(Map state) {
        this.objectType = (String)state.get(OBJECT_TYPE);
        this.objectId = (String)state.get(OBJECT_ID);
    }

    /**
     * Returns the type of object to which this exception applies.
     * This is usually the class name (but can be something else).
     */
    public String getObjectType() {
        return objectType;
    }

    public String getObjectId() {
        return objectId;
    }

    public String getMessage() {
        return getMessage(Locale.getDefault());
    }

    public String getMessage(Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/repository/messages", locale);
        String message = bundle.getString("concurrent-update-exception");
        String typeName;
        try {
            typeName = bundle.getString(objectType + ".shortName");
        } catch (MissingResourceException e) {
            typeName = objectType;
        }
        return MessageFormat.format(message, typeName, objectId);
    }

    public Map<String, String> getState() {
        Map<String, String> state = new HashMap<String, String>();
        state.put(OBJECT_TYPE, objectType);
        state.put(OBJECT_ID, objectId);
        return state;
    }
}
