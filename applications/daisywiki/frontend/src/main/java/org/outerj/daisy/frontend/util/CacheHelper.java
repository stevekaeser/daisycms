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
package org.outerj.daisy.frontend.util;

import org.apache.excalibur.store.Store;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceValidity;

import java.io.IOException;

public class CacheHelper {
    public static Object getFromCache(Store store, Source source, String prefix) {
        String key = prefix + source.getURI();
        SourceValidity newValidity = source.getValidity();

        // If source is not valid then remove object from cache and return null
        if (newValidity == null) {
            store.remove(key);
            return null;
        }

        // If object is not in cache then return null
        Object[] objectAndValidity = (Object[]) store.get(key);
        if (objectAndValidity == null) {
            return null;
        }

        // Check stored validity against current source validity
        SourceValidity storedValidity = (SourceValidity) objectAndValidity[1];
        int valid = storedValidity.isValid();
        boolean isValid;
        if (valid == SourceValidity.UNKNOWN) {
            valid = storedValidity.isValid(newValidity);
            isValid = (valid == SourceValidity.VALID);
        } else {
            isValid = (valid == SourceValidity.VALID);
        }

        // If stored object is not valid then remove object from cache and return null
        if (!isValid) {
            store.remove(key);
            return null;
        }

        // If valid then return cached object
        return objectAndValidity[0];
    }

    public static void setInCache(Store store, Object object, Source source, String prefix) throws IOException {
        String key = prefix + source.getURI();
        SourceValidity validity = source.getValidity();
        if (validity != null) {
            Object[] objectAndValidity = {object,  validity};
            store.store(key, objectAndValidity);
        }
    }
}
