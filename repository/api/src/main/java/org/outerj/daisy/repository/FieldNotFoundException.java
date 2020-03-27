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

public class FieldNotFoundException extends RuntimeException {
    private long fieldTypeId;
    private String fieldTypeName;

    public FieldNotFoundException(long fieldTypeId) {
        this.fieldTypeId = fieldTypeId;
    }

    public FieldNotFoundException(String fieldTypeName) {
        this.fieldTypeName = fieldTypeName;
    }

    public String getMessage() {
        if (fieldTypeName != null)
            return "No field named \"" + fieldTypeName + "\" available.";
        else
            return "No field with id " + fieldTypeId + " available.";
    }
}
