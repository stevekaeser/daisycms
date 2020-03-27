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
package org.outerj.daisy.frontend.components.skinsource;

import org.apache.excalibur.source.*;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.avalon.excalibur.monitor.ActiveMonitor;
import org.apache.avalon.excalibur.monitor.Resource;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.environment.Request;
import org.outerj.daisy.frontend.util.AltFileResource;
import org.outerj.daisy.frontend.util.WikiPropertiesHelper;
import org.outerj.daisy.frontend.WikiHelper;
import org.outerj.daisy.frontend.FrontEndContext;

import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * Source Factory for "daisyskin:" sources.
 *
 * <p>For documentation on this source, see the Daisy documentation, at the time of this
 * writing at http://cocoondev.org/daisydocs-1_5/192.html</p>
 * <p>(update: it is now at http://daisycms.org/daisydocs-1_5/192.html</p>
 */
public class SkinSourceFactory extends AbstractLogEnabled implements SourceFactory, Contextualizable, Serviceable,
        Disposable, ThreadSafe, Initializable {
    private Context context;
    /**
     * Map key = java.util.File object, map value = a {@link CachedEntry} object.
     */
    private Map<File, CachedEntry> baseSkinCache = new ConcurrentHashMap<File, CachedEntry>(8, .75f, 2);
    /**
     * A lock to synchronize updates to the baseSkinCache.
     */
    private Lock updateLock = new ReentrantLock();
    private ActiveMonitor monitor;
    private ServiceManager serviceManager;
    private static final String BASE_SKIN_FILENAME = "baseskin.txt";
    private File skinsDir;
    private File skinsFallbackDir;

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        monitor = (ActiveMonitor)serviceManager.lookup(ActiveMonitor.ROLE);
    }

    public void initialize() throws Exception {
        this.skinsDir = new File(new File(WikiPropertiesHelper.getWikiDataDir(context)), "resources" + File.separator + "skins").getAbsoluteFile();
    }

    public void dispose() {
        serviceManager.release(monitor);
    }

    public Source getSource(String location, Map parameters) throws IOException {
        Request request = ContextHelper.getRequest(context);
        initFallbackDir();

        String[] parsedLocation = parseLocation(location);

        String skin;
        if (parsedLocation[0] != null) {
            skin = parsedLocation[0];
        } else {
            skin = FrontEndContext.get(request).getSkin();
        }

        boolean forceFallback = parsedLocation[1] != null;

        String path = parsedLocation[2];

        File skinFallbackDir = new File(skinsFallbackDir, skin);
        File skinFallbackResource = new File(skinFallbackDir, path);
        File skinDir = new File(skinsDir, skin);
        File skinResource = forceFallback ? skinFallbackResource : new File(skinDir, path);
        File skinOriginalResource = skinResource;
        File skinSelectedSource;

        Set<String> searchedParentSkins = null;

        while ((skinSelectedSource = getExistingFile(skinResource, skinFallbackResource)) == null) {
            String parentSkin = getBaseSkin(skinDir);
            parentSkin = parentSkin == null ? getBaseSkin(skinFallbackDir) : parentSkin;
            if (parentSkin != null) {
                if (searchedParentSkins != null && searchedParentSkins.contains(parentSkin))
                    throw new IOException("Recursive skin dependency found for skin \"" + skin + "\" and base skin \"" + parentSkin + "\".");

                skinFallbackDir = new File(skinsFallbackDir, parentSkin);
                skinFallbackResource = new File(skinFallbackDir, path);
                skinDir = new File(skinsDir, parentSkin);
                skinResource = forceFallback ? skinFallbackResource : new File(skinDir, path);

                if (searchedParentSkins == null)
                    searchedParentSkins = new HashSet<String>();
                searchedParentSkins.add(parentSkin);
            } else {
                // just pretend we found it at the original location
                skinSelectedSource = skinOriginalResource;
                break;
            }
        }

        // construct absolute URL
        StringBuilder absoluteURL = new StringBuilder(150);
        absoluteURL.append("daisyskin:/(").append(skin).append(')');
        if (forceFallback)
            absoluteURL.append("(webapp)");
        absoluteURL.append(path);

        return new SkinSource(skinSelectedSource, absoluteURL.toString());
    }

    private File getExistingFile(File firstChoice, File secondChoice) {
        if (firstChoice.exists())
            return firstChoice;
        else if (secondChoice.exists())
            return secondChoice;
        else
            return null;
    }

    private String[] parseLocation(String location) throws MalformedURLException {
        if (!location.startsWith("daisyskin:"))
            throw new MalformedURLException("The URL does not use the daisyskin sheme, it cannot be handled by this source implementation.");

        String schemeSpecificPart = location.substring("daisyskin:".length());

        String skinName;
        String locationName; // wikidata or webapp
        String path;

        if (schemeSpecificPart.startsWith("/(")) {
            int closeSkinNameParenPos = schemeSpecificPart.indexOf(')');
            if (closeSkinNameParenPos == -1)
                throw new MalformedURLException("Missing closing parenthesis in: " + location);
            skinName = schemeSpecificPart.substring(2, closeSkinNameParenPos);
            if (skinName.trim().equals("")) {
                // it is allowed to have an empty skin specification (e.g. in case you
                // only want to specify the skins dir)
                skinName = null;
            }
            int pathStartPos = closeSkinNameParenPos + 1;
            if (schemeSpecificPart.length() > closeSkinNameParenPos && schemeSpecificPart.charAt(closeSkinNameParenPos + 1) == '(') {
                int locationNameParenPos = schemeSpecificPart.indexOf(')', closeSkinNameParenPos + 1);
                if (locationNameParenPos == -1)
                    throw new MalformedURLException("Missing closing parenthesis in: " + location);
                locationName = schemeSpecificPart.substring(closeSkinNameParenPos + 2, locationNameParenPos);
                if (locationName.trim().equals("")) {
                    locationName = null;
                } else if (!locationName.equals("webapp")) {
                    throw new MalformedURLException("The location name in the URL, when specified, should be 'webapp'.");
                }
                pathStartPos = locationNameParenPos + 1;
            } else {
                locationName = null;
            }
            path = schemeSpecificPart.substring(pathStartPos);
        } else {
            skinName = null;
            locationName = null;
            path = schemeSpecificPart;
        }

        return new String[] {skinName, locationName, path};
    }

    private String getBaseSkin(File skinDir) throws IOException {
        CachedEntry cachedEntry = baseSkinCache.get(skinDir);
        if (cachedEntry != null)
            return cachedEntry.baseSkin;

        if (!skinDir.exists())
            return null;

        try {
            updateLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new IOException("Error acquiring updateLock: " + e.toString());
        }
        try {
            cachedEntry = baseSkinCache.get(skinDir);
            if (cachedEntry != null)
                return cachedEntry.baseSkin;

            File baseSkinFile = new File(skinDir, BASE_SKIN_FILENAME);
            String baseSkin = loadBaseSkin(baseSkinFile);

            Resource fileResource;
            try {
                fileResource = new AltFileResource(baseSkinFile);
            } catch (Exception e) {
                throw new IOException("Error constructing FileResource object for " + baseSkinFile.getAbsolutePath() + ": " + e.toString());
            }
            fileResource.addPropertyChangeListener(new BaseSkinListener(skinDir));
            cachedEntry = new CachedEntry(baseSkin, fileResource);
            baseSkinCache.put(skinDir, cachedEntry);
            monitor.addResource(fileResource);

            return baseSkin;
        } finally {
            updateLock.unlock();
        }
    }

    static class CachedEntry {
        public String baseSkin;
        public Resource fileResource;

        public CachedEntry(String baseSkin, Resource fileResource) {
            this.baseSkin = baseSkin;
            this.fileResource = fileResource;
        }
    }

    class BaseSkinListener implements PropertyChangeListener {
        private File skinDir;

        public BaseSkinListener(File skinDir) {
            this.skinDir = skinDir;
        }

        public void propertyChange(PropertyChangeEvent evt) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Received change event for " + ((Resource)evt.getSource()).getResourceKey());
            }

            try {
                updateLock.lockInterruptibly();
                try {
                    Resource fileResource = (Resource)evt.getSource();

                    if (!skinDir.exists()) {
                        baseSkinCache.remove(skinDir);
                        monitor.removeResource(fileResource);
                    } else {
                        CachedEntry cachedEntry = baseSkinCache.get(skinDir);
                        if (cachedEntry != null) {
                            File baseSkinFile = new File(skinDir, BASE_SKIN_FILENAME);
                            cachedEntry.baseSkin = loadBaseSkin(baseSkinFile);
                        } else {
                            // this situation should normally not occur
                            monitor.removeResource(fileResource);
                        }
                    }
                } finally {
                    updateLock.unlock();
                }
            } catch (Exception e) {
                getLogger().error("Error processing baseskin.txt change event for " + skinDir.getAbsolutePath(), e);
            }
        }
    }

    private String loadBaseSkin(File baseSkinFile) throws IOException {
        if (baseSkinFile.exists()) {
            String baseskin = null;
            FileReader fileReader = null;
            try {
                fileReader = new FileReader(baseSkinFile);
                BufferedReader reader = new BufferedReader(fileReader);
                baseskin = reader.readLine();
            } catch (Exception e) {
                throw new IOException("Error reading baseskin.txt file at " + baseSkinFile.getAbsolutePath());
            } finally {
                if (fileReader != null)
                    fileReader.close();
            }

            if (baseskin == null) {
                return null;
            } else {
                baseskin = baseskin.trim();
                if (baseskin.length() == 0)
                    return null;
                else
                    return baseskin;
            }
        }
        return null;
    }

    public void release(Source source) {
    }

    private void initFallbackDir() throws MalformedURLException {
        if (skinsFallbackDir == null) {
            synchronized(this) {
                if (skinsFallbackDir == null) {
                    Request request = ContextHelper.getRequest(context);
                    File contextDir = new File(new URL(WikiHelper.getDaisyContextPath(request)).getPath());
                    skinsFallbackDir = new File(contextDir, "resources" + File.separator + "skins").getAbsoluteFile();
                }
            }
        }
    }
}
