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
package org.outerj.daisy.frontend;

import org.apache.avalon.framework.service.ServiceManager;
import org.apache.excalibur.source.Source;

public class WikiStylesheetProvider implements DocumentTypeSpecificStyler.StylesheetProvider {
    private String publishType;
    private ServiceManager serviceManager;
    private String stylesheetBasePath;

    public WikiStylesheetProvider(String publishType, ServiceManager serviceManager) {
        this.publishType = publishType;
        this.serviceManager = serviceManager;
        this.stylesheetBasePath = "daisyskin:document-styling/";
    }

    public String getStylesheet(String documentTypeName) throws Exception {
        return determineStylesheet(stylesheetBasePath + publishType + "/" + documentTypeName + ".xsl");
    }

    public String getStylesheetByHint(String styleHint) throws Exception {
        return determineStylesheet(stylesheetBasePath + publishType + "/" + styleHint);
    }

    private String determineStylesheet(String path) throws Exception {
        org.apache.excalibur.source.SourceResolver sourceResolver = (org.apache.excalibur.source.SourceResolver)serviceManager.lookup(org.apache.excalibur.source.SourceResolver.ROLE);
        Source source = null;
        try {
            // first try the specified path otherwise return default
            source = sourceResolver.resolveURI(path);
            if (source.exists()) {
                return source.getURI();
            } else {
                return getDefaultStylesheet();
            }
        } finally {
            if (source != null)
                sourceResolver.release(source);
            serviceManager.release(sourceResolver);
        }
    }

    public String getDefaultStylesheet() throws Exception {
        return "daisyskin:xslt/document-to-" + publishType + ".xsl";
    }
}
