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
package org.outerj.daisy.frontend.admin;

import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.impl.FileSource;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.excalibur.monitor.ActiveMonitor;
import org.apache.avalon.excalibur.monitor.FileResource;
import org.outerj.daisy.repository.LocaleHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

public class AdminLocalesImpl extends AbstractLogEnabled implements AdminLocales, ThreadSafe, Configurable, Serviceable, Initializable, Disposable {
    private Locales locales;
    private ServiceManager serviceManager;
    private ActiveMonitor monitor;
    private String localesURI;
    private File localesFile;
    private FileResource monitoredResource;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        monitor = (ActiveMonitor)serviceManager.lookup(ActiveMonitor.ROLE);
    }

    public void configure(Configuration configuration) throws ConfigurationException {
        localesURI = configuration.getChild("localesSource").getValue();
    }

    public void initialize() throws Exception {
        SourceResolver sourceResolver = null;
        Source localesSource = null;
        try {
            sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);
            localesSource = sourceResolver.resolveURI(localesURI);
            if (localesSource instanceof FileSource) {
                localesFile = ((FileSource)localesSource).getFile();
                monitoredResource = new FileResource(localesFile);
                monitoredResource.addPropertyChangeListener(new ResourceListener());
                monitor.addResource(monitoredResource);
            } else {
                throw new Exception("Source for the list of admin locales must be a file, not: " + localesURI);
            }
        } finally {
            if (localesSource != null)
                sourceResolver.release(localesSource);
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
        }
        reloadLocales();
    }

    private synchronized void reloadLocales() {
        List<String> locales = new ArrayList<String>();
        InputStream is = null;

        try {
            is = new FileInputStream(localesFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            while (line != null) {
                if (line.equals("empty")) {
                    locales.add("");
                } else if (line.length() > 0 && line.charAt(0) != '#') {
                    locales.add(line.trim());
                }
                line = reader.readLine();
            }
        } catch (Exception e) {
            getLogger().error("Error reading locales.", e);
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (Exception e) {
                getLogger().error("Error closing input stream of locales file.", e);
            }
        }

        String[] localeStrings = locales.toArray(new String[locales.size()]);
        Locale[] localeObjects = new Locale[localeStrings.length];
        for (int i = 0; i < localeStrings.length; i++) {
            localeObjects[i] = LocaleHelper.parseLocale(localeStrings[i]);
        }
        this.locales = new LocalesImpl(localeStrings, localeObjects);
    }

    public Locales getLocales() {
        return locales;
    }

    public void dispose() {
        monitor.removeResource(monitoredResource);
        serviceManager.release(monitor);
    }

    class ResourceListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("last-modified")) {
                reloadLocales();
            }
        }
    }

    static class LocalesImpl implements Locales {
        private String[] localeStrings;
        private Locale[] localeObjects;

        public LocalesImpl(String[] localeStrings, Locale[] localeObjects) {
            this.localeStrings = localeStrings;
            this.localeObjects = localeObjects;
        }

        public String[] getAsStrings() {
            return localeStrings;
        }

        public Locale[] getAsObjects() {
            return localeObjects;
        }
    }
}
