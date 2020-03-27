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
package org.outerj.daisy.xmlutil;

import java.util.Set;
import java.util.HashSet;

public class XmlMimeTypeHelper {
    private static Set xmlMimeTypes;
    static {
        xmlMimeTypes = new HashSet();
        xmlMimeTypes.add("text/xml");
        xmlMimeTypes.add("application/xml");
    }

    /**
     * Returns true if the mime type is recognized as the mime type of some XML format.
     */
    public static boolean isXmlMimeType(String mimeType) {
        return xmlMimeTypes.contains(mimeType) || mimeType.endsWith("+xml");
    }
}
