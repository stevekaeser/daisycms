/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.frontend.components.config.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.excalibur.source.Source;
import org.apache.excalibur.source.SourceResolver;
import org.apache.excalibur.source.impl.FileSource;
import org.outerj.daisy.configutil.ConfigurationWrapper;
import org.outerj.daisy.frontend.components.config.ConfigurationManager;
import org.outerj.daisy.frontend.util.WikiPropertiesHelper;

public class ConfigurationManagerImpl implements ConfigurationManager, ThreadSafe, Contextualizable,Initializable, Serviceable, LogEnabled, Disposable {
    private Context context;
    private ServiceManager serviceManager;
    private Logger logger;

    private File webappConfDir;
    private File datadirConfDir;
    private File sitesDir;

    // The cached are read by many threads, but only updated by one thread
    private Map<String, CachedConfig> webappConfs = new ConcurrentHashMap<String, CachedConfig>(INITIAL_CACHE_SIZE, CACHE_LOAD_FACTOR, CACHE_CONCURRENCY_LEVEL);
    private Map<String, CachedConfig> datadirConfs = new ConcurrentHashMap<String, CachedConfig>(INITIAL_CACHE_SIZE, CACHE_LOAD_FACTOR, CACHE_CONCURRENCY_LEVEL);
    private Map<String, Map<String, CachedConfig>> siteConfs = new ConcurrentHashMap<String, Map<String, CachedConfig>>(INITIAL_CACHE_SIZE, CACHE_LOAD_FACTOR, CACHE_CONCURRENCY_LEVEL);

    private static int INITIAL_CACHE_SIZE = 16;
    private static float CACHE_LOAD_FACTOR = .75f;
    private static int CACHE_CONCURRENCY_LEVEL = 1;

    private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
    private int delay = 10;

    private static final String CONFIG_FILE_EXT = ".xml";

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public void initialize() throws Exception {
        // Determine locations of conf directories

        // webapp conf
        webappConfDir = resolve("context:/daisy/conf");


        // datadir conf
        String wikiDataDir = WikiPropertiesHelper.getWikiDataDir(context);
        this.datadirConfDir = new File(wikiDataDir, "conf");
        if (!datadirConfDir.exists())
            logger.info("wiki data dir conf dir does not exist.");

        // sites dir
        sitesDir = new File(wikiDataDir, "sites");

        // do initial read of configuration
        scanConfiguration();

        // schedule thread for regular up-to-date check of configuration
        executor.scheduleWithFixedDelay(new ConfigurationRefresher(), delay, delay, TimeUnit.SECONDS);
    }

    public void dispose() {
        executor.shutdownNow();
    }


    public Configuration getConfiguration(String path, boolean fallback) {
        CachedConfig cached = datadirConfs.get(path);
        if (cached == null && fallback)
            cached = webappConfs.get(path);
        return cached != null ? cached.configuration : null;
    }

    public Configuration getConfiguration(String site, String path, boolean fallback) {
        Map<String, CachedConfig> siteConf = siteConfs.get(site);
        CachedConfig cached = null;

        if (siteConf != null) {
            cached = siteConf.get(path);
        }

        if (cached == null && fallback) {
            return getConfiguration(path, true);
        }

        return cached != null ? cached.configuration : null;
    }

    public Configuration getConfiguration(String site, String path) {
        return getConfiguration(site, path, true);
    }

    public Configuration getConfiguration(String path) {
        return getConfiguration(path, true);
    }

    private File resolve(String location) throws ServiceException, IOException {
        SourceResolver sourceResolver = null;
        try {
            sourceResolver = (SourceResolver)serviceManager.lookup(SourceResolver.ROLE);
            Source source = sourceResolver.resolveURI(location);
            if (source instanceof FileSource) {
                return ((FileSource)source).getFile();
            } else {
                throw new RuntimeException("Unexpected error: resolved location isn't a file. Location = " + location);
            }
        } finally {
            if (sourceResolver != null)
                serviceManager.release(sourceResolver);
        }
    }

    private static class CachedConfig {
        long lastModified;
        Configuration configuration;
        ConfigState state;
    }

    private synchronized void scanConfiguration() {
        logger.debug("Scanning configuration");
        updateCache(webappConfDir, webappConfs);
        updateCache(datadirConfDir, datadirConfs);

        List<Site> sites = findSitesWithConfs();

        // Delete sites from cache which do no longer exist
        Iterator<String> currentSitesIt = siteConfs.keySet().iterator();
        while (currentSitesIt.hasNext()) {
            String siteName = currentSitesIt.next();
            boolean found = false;
            for (Site site : sites) {
                if (site.siteName.equals(siteName)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                currentSitesIt.remove();
                if (logger.isDebugEnabled())
                    logger.debug("Configuration: detected removed site: " + siteName);
            }
        }

        //
        for (Site site : sites) {
            Map<String, CachedConfig> cache = siteConfs.get(site.siteName);
            if (cache == null) {
                cache = new ConcurrentHashMap<String, CachedConfig>(INITIAL_CACHE_SIZE, CACHE_LOAD_FACTOR, CACHE_CONCURRENCY_LEVEL);
                siteConfs.put(site.siteName, cache);
            }
            updateCache(site.confDir, cache);
        }

        logger.debug("Scanning configuration done.");
    }

    private void updateCache(File directory, Map<String, CachedConfig> cache) {
        // Search all config files on disk
        List<ConfigPath> configPaths = new ArrayList<ConfigPath>();
        collectConfigFiles(directory, configPaths, directory);

        // Delete configs from cache which don't exist on disk anymore
        Iterator<String> currentEntriesIt = cache.keySet().iterator();
        while (currentEntriesIt.hasNext()) {
            String path = currentEntriesIt.next();
            boolean found = false;
            for (ConfigPath configPath : configPaths) {
                if (configPath.path.equals(path)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                currentEntriesIt.remove();
                if (logger.isDebugEnabled())
                    logger.debug("Configuration: detected removed config " + path + " in " + directory.getAbsolutePath());
            }
        }

        // Add/update configs
        for (ConfigPath configPath : configPaths) {
            CachedConfig cachedConfig = cache.get(configPath.path);
            if (cachedConfig == null || cachedConfig.lastModified != configPath.file.lastModified()) {
                if (logger.isDebugEnabled())
                    logger.debug("Configuration: detected updated or added config " + configPath.path + " in " + directory.getAbsolutePath());
                long lastModified = configPath.file.lastModified();
                Configuration configuration = parseConfiguration(configPath.file);
                cachedConfig = new CachedConfig();
                cachedConfig.lastModified = lastModified;
                cachedConfig.configuration = configuration;
                cachedConfig.state = configuration == null ? ConfigState.ERROR : ConfigState.OK;
                cache.put(configPath.path, cachedConfig);
            }
        }
    }

    private void collectConfigFiles(File dir, List<ConfigPath> configPaths, File rootDir) {
        File[] files = dir.listFiles(CONFIG_FILE_FILTER);
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    collectConfigFiles(file, configPaths, rootDir);
                } else {
                    String path = getConfigPathForFile(file, rootDir);
                    if (path.endsWith(CONFIG_FILE_EXT)) { // should be the case
                        path = path.substring(0, path.length() - CONFIG_FILE_EXT.length());
                    }
                    configPaths.add(new ConfigPath(path, file));
                }
            }
        }
    }

    private String getConfigPathForFile(File file, File reference) {
        File parent = file.getParentFile();
        if (parent != null && !parent.equals(reference)) {
            return getConfigPathForFile(parent, reference) + "/" + file.getName();
        } else if (parent != null) {
            return file.getName();
        } else {
            return "";
        }
    }

    private static class ConfigPath {
        String path;
        File file;

        public ConfigPath(String path, File file) {
            this.path = path;
            this.file = file;
        }
    }

    private static final FileFilter CONFIG_FILE_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            if (pathname.isDirectory()) {
                String name = pathname.getName();
                return !name.startsWith(".") && !name.equals(".svn") && !name.equals("CVS");
            } else {
                return pathname.getName().endsWith(CONFIG_FILE_EXT);
            }
        }
    };

    private static final FileFilter DIR_FILE_FILTER = new FileFilter() {
        public boolean accept(File pathname) {
            return pathname.isDirectory();
        }
    };

    private Configuration parseConfiguration(File file) {
        Configuration config = null;
        try {
            config = new ConfigurationWrapper(new DefaultConfigurationBuilder().buildFromFile(file));
        } catch (Throwable e) {
            logger.error("Error reading configuration file " + file.getAbsolutePath(), e);
        }
        return config;
    }

    enum ConfigState { OK, ERROR }

    private static class Site {
        String siteName;
        File confDir;

        public Site(String siteName, File confDir) {
            this.siteName = siteName;
            this.confDir = confDir;
        }
    }

    private List<Site> findSitesWithConfs() {
        List<Site> sites = new ArrayList<Site>();
        File[] siteDirs = sitesDir.listFiles(DIR_FILE_FILTER);
        if (siteDirs != null) {
            for (File siteDir : siteDirs) {
                // if it is a site-defining directory
                if (new File(siteDir, "siteconf.xml").exists()) {
                    File confDir = new File(siteDir, "conf");
                    if (confDir.exists()) {
                        sites.add(new Site(siteDir.getName(), confDir));
                    }
                }
            }
        }
        return sites;
    }

    private class ConfigurationRefresher implements Runnable {
        public void run() {
            scanConfiguration();
        }
    }
}
