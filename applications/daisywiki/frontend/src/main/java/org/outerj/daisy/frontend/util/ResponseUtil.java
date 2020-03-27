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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.http.HttpResponse;

public class ResponseUtil {


    public static void addContentDispositionHeader(Response response,
            String disposition, String fileName) {
        if (fileName == null) {
            fileName = "data";
        }
        if (!"inline".equals(disposition) && !"attachment".equals(disposition)) {
            throw new IllegalArgumentException("disposition should be 'inline' or 'attachment'");
        }
        response.setHeader("Content-Disposition" , disposition + "; filename=\"" + fileName + "\"");
        
    }
    
    /**
     * Redirects to the given location, but give a head-up when the location is a complete URL. (i.e. only relative and absolute paths, which let the user stay on the same host are accepted)
     */
    public static void safeRedirect(AppleRequest request, AppleResponse response, String location) throws IllegalArgumentException {
        if (location == null) {
            throw new NullPointerException("location should not be null");
        }
        try {
            new URL(location);
            throw new IllegalArgumentException("Attention! Somebody may be trying to trick you into going to " + location + ".  If you are sure that is not the case, copy the url to the address bar and proceed"); 
        } catch (MalformedURLException me) {
            response.redirectTo(location);
        }
    }

}
