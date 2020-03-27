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
package org.outerj.daisy.repository.schema;

import org.outerx.daisy.x10.PartTypeUseDocument;

/**
 * Describes the usage of the part type by a document type.
 */
public interface PartTypeUse {
    PartType getPartType();

    boolean isRequired();

    void setRequired(boolean required);

    /**
     * Indicates if the part may be edited. This is only for presentation
     * purposes only and does no validation.
     */
    boolean isEditable();

    /**
     * Sets if the part should be shown as editable or not. By default
     * parts are editable.
     */
    void setEditable(boolean editable);

    PartTypeUseDocument getXml();

    PartTypeUseDocument getExtendedXml();
}
