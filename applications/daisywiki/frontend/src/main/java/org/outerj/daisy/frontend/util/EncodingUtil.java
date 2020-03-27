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

import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.httpclient.URIException;

public class EncodingUtil {
    public static String encodePath(String path) {
        try {
            return URIUtil.encodePath(path, "UTF-8");
        } catch (URIException e) {
            throw new RuntimeException("Problem encoding path " + path, e);
        }
    }

    public static String encodePathQuery(String pathQuery) {
        try {
            return URIUtil.encodePathQuery(pathQuery, "UTF-8");
        } catch (URIException e) {
            throw new RuntimeException("Problem encoding path+query " + pathQuery, e);
        }
    }
}
