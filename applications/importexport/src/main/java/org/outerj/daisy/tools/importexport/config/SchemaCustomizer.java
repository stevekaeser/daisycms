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
package org.outerj.daisy.tools.importexport.config;

import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchema;

/**
 * The purpose of a SchemaCustomizer is to alter the Daisy repository
 * schema definitions upon import or export.
 */
public interface SchemaCustomizer {
    void customize(ImpExpSchema impExpSchema);

    /**
     * Returns the configuration XML snippet to store in the options file.
     * Return an empty string if you don't need this.
     */
    String getXml();
}
