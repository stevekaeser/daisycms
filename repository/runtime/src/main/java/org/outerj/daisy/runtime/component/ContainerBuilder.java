/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.runtime.component;

import org.outerj.daisy.runtime.DaisyRuntime;
import org.outerj.daisy.runtime.DaisyRTException;
import org.outerj.daisy.runtime.ContainerEntry;
import org.outerj.daisy.runtime.classloading.XmlClassLoaderBuilder;
import org.outerj.daisy.runtime.classloading.ClassLoadingConfig;
import org.outerj.daisy.runtime.classloading.ClassLoadingConfigImpl;
import org.outerj.daisy.runtime.classloading.ClassPathEntry;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.w3c.dom.Document;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.URL;

public class ContainerBuilder {
    private ContainerEntry containerEntry;
    private Properties springConfigProperties;
    private DaisyRuntime runtime;

    private static final Pattern SPRING_CONF_PATTERN = Pattern.compile("DAISY-INF/spring/.*\\.xml");
    private static final String DAISY_CLASSLOADER_CONF = "DAISY-INF/classloader.xml";

    private final Log log = LogFactory.getLog(getClass());

    public static ContainerConfig build(ContainerEntry containerEntry, Properties springConfigProperties, DaisyRuntime runtime) throws DaisyRTException {
        return new ContainerBuilder(containerEntry, springConfigProperties, runtime).build();
    }

    private ContainerBuilder(ContainerEntry containerEntry, Properties springConfigProperties, DaisyRuntime runtime) {
        this.containerEntry = containerEntry;
        this.springConfigProperties = springConfigProperties;
        this.runtime = runtime;
    }

    private ContainerConfig build() {
        List<ZipEntry> springConfigEntries = new ArrayList<ZipEntry>();
        ZipEntry classLoaderConfig = null;

        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(containerEntry.getFile());

            // Find the relevant entries in the zip file
            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            while (zipEntries.hasMoreElements()) {
                ZipEntry zipEntry = zipEntries.nextElement();
                if (!zipEntry.isDirectory()) {
                    String name = zipEntry.getName();
                    if (name.equals(DAISY_CLASSLOADER_CONF)) {
                        classLoaderConfig = zipEntry;
                    } else {
                        Matcher matcher = SPRING_CONF_PATTERN.matcher(name);
                        if (matcher.matches()) {
                            springConfigEntries.add(zipEntry);
                        }
                    }
                }
            }

            // build classpath
            List<URL> initialEntries = Collections.singletonList(containerEntry.getFile().toURL()); // container jar itself is always part of the classpath
            ClassLoadingConfig classLoadingConfig;
            if (classLoaderConfig != null) {
                InputStream is = zipFile.getInputStream(classLoaderConfig);
                try {
                    Document document = DocumentHelper.parse(is);
                    classLoadingConfig = XmlClassLoaderBuilder.build(document.getDocumentElement(), initialEntries, runtime.getArtifactRepository());
                } finally {
                    is.close();
                }
            } else {
                classLoadingConfig = new ClassLoadingConfigImpl(new ArrayList<ClassPathEntry>(), initialEntries, runtime.getArtifactRepository());
            }

            // build spring container
            if (springConfigEntries.size() == 0)
                throw new DaisyRTException("Component jar does not contain any spring container configurations: " + containerEntry.getFile().getAbsolutePath());

            ContainerConfigImpl componentContainerConfig = new ContainerConfigImpl(containerEntry, classLoadingConfig, springConfigProperties, runtime);

            for (ZipEntry zipEntry : springConfigEntries) {
                InputStream is = zipFile.getInputStream(zipEntry);
                try {
                    byte[] springConfigData = readStream(is);
                    componentContainerConfig.addSpringConfig(zipEntry.getName(), springConfigData);
                } finally {
                    is.close();
                }
            }

            return componentContainerConfig;

        } catch (Throwable e) {
            throw new DaisyRTException("Error reading component container config from " + containerEntry.getFile().getAbsolutePath(), e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    log.error("Error closing zip file " + zipFile.getName(), e);
                }
            }
        }
    }


    private byte[] readStream(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);

        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = is.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }

        return bos.toByteArray();
    }

}
