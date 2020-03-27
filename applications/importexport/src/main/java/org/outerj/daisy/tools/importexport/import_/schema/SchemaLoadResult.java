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
package org.outerj.daisy.tools.importexport.import_.schema;

public enum SchemaLoadResult {
    UPDATE_SKIPPED("update-skipped"),
    UPDATED("updated"),
    CREATED("created"),
    NO_UPDATE_NEEDED("no-update-needed");

    private String name;

    private SchemaLoadResult(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
