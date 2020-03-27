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

import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.source.Source;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceManager;
import org.outerj.daisy.repository.LocaleHelper;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Helper class that reads available locales from a file.
 */
public class AvailableLocales {
    private ServiceManager serviceManager;
    private Logger logger;

    public AvailableLocales(ServiceManager serviceManager, Logger logger) {
        this.serviceManager = serviceManager;
        this.logger = logger;
    }

    public List<Locale> getLocales() throws Exception {
        SourceResolver sourceResolver = null;
        Source source = null;
        InputStream is = null;
        try {
            sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);
            source = sourceResolver.resolveURI("resources/conf/locales.txt");
            is = source.getInputStream();
            return readLocales(is);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Throwable e) {
                    logger.error("Error closing locales.txt input stream.", e);
                }
            }
            if (source != null)
                sourceResolver.release(source);
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
        }
    }

    private List<Locale> readLocales(InputStream inputStream) throws Exception {
        List<Locale> locales = new ArrayList<Locale>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = reader.readLine();
        while (line != null) {
            if (line.length() > 0) {
                if (line.charAt(0) != '#') {
                    line = line.trim();
                    Locale locale = LocaleHelper.parseLocale(line);
                    locales.add(locale);
                }
            }
            line = reader.readLine();
        }
        return locales;
    }
}
