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
package org.outerj.daisy.tools.importexport.model.meta;

import java.util.HashMap;

public class ImpExpMeta extends HashMap<String, String> {
    private static String DAISY_SERVER_VERSION = "daisy-server-version";
    private static String EXPORT_TIME = "export-time";
    private static String EXPORT_FORMAT = "export-format";

    public String getDaisyServerVersion() {
        return get(DAISY_SERVER_VERSION);
    }

    public void setDaisyServerVersion(String value) {
        put(DAISY_SERVER_VERSION, value);
    }

    public String getExportTime() {
        return get(EXPORT_TIME);
    }

    public void setExportTime(String value) {
        put(EXPORT_TIME, value);
    }

    public void setExportFormat(String value) {
        put(EXPORT_FORMAT, value);
    }

    public String getExportFormat() {
        return get(EXPORT_FORMAT);
    }
}
