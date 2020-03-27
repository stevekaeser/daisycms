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

import org.apache.cocoon.Constants;
import org.apache.cocoon.environment.Context;
import org.apache.avalon.framework.context.ContextException;

import java.util.Properties;

public class WikiPropertiesHelper {
    private static Properties resolveProperties;
    private static final String DAISYWIKI_DATA_PROPNAME = "daisywiki.data";
    private static final String DAISYWIKI_LOCALE_PROPNAME = "daisywiki.locale";

    static {
        resolveProperties = new Properties(System.getProperties());
    }

    public static String getWikiDataDir(org.apache.avalon.framework.context.Context context) {
        String value = getProperty(DAISYWIKI_DATA_PROPNAME, context); 
        if (value != null) {
            return value;
        }
        
        throw new RuntimeException("The property that specifies the location of the Daisy Wiki data directory (daisywiki.data) is neither specified as Java system property, nor as Servlet context initialization parameter.");
    }
    
    public static String getDefaultLocale(org.apache.avalon.framework.context.Context context) {
        return getProperty(DAISYWIKI_LOCALE_PROPNAME, context); 
    }

    public static Properties getResolveProperties(org.apache.avalon.framework.context.Context context) {
        String wikiData = getProperty(DAISYWIKI_DATA_PROPNAME, context);
        String defaultLocale = getProperty(DAISYWIKI_LOCALE_PROPNAME, context);
        
        Properties result = new Properties(resolveProperties);
        if (wikiData != null) {
            result.setProperty(DAISYWIKI_DATA_PROPNAME, wikiData);
        }
        if (defaultLocale != null) {
            result.setProperty(DAISYWIKI_LOCALE_PROPNAME, defaultLocale);
        }
        return result;
    }
    
    private static String getProperty(String key, org.apache.avalon.framework.context.Context context) {
        return getProperty(key, context, null);
    }

    private static String getProperty(String key, org.apache.avalon.framework.context.Context context, String defaultValue) {
        Context servletContext;
        try {
            servletContext = (Context)context.get(Constants.CONTEXT_ENVIRONMENT_CONTEXT);
        } catch (ContextException e) {
            throw new RuntimeException(e);
        }
        
        String value = servletContext.getInitParameter(key);
        if (value != null) {
            return value;
        }
            
        value = resolveProperties.getProperty(key);
        if (value != null) {
            return value;
        }
        
        return defaultValue;
    }
}
