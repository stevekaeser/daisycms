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
package org.outerj.daisy.tools.importexport.import_.fs;

import java.util.ArrayList;
import java.util.List;

public class FsUtil {
    public static String[] parsePath(String path) {
        String[] parts = path.split("[/\\\\]");
        List<String> list = new ArrayList<String>(parts.length);
        for (String part : parts) {
            if (part.trim().length() > 0)
                list.add(part);
        }
        return list.toArray(new String[0]);
    }

    public static String getPath(ImportFileEntry entry) {
        StringBuilder path = new StringBuilder(50);
        ImportFileEntry parent = entry;
        while (parent != null) {
            path.insert(0, path.length() > 0 ? parent.getName() + "/" : parent.getName());
            parent = parent.getParent();
        }
        return path.toString();
    }
}
