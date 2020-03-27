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

import org.outerj.daisy.runtime.classloading.ClassLoadingConfig;
import org.outerj.daisy.runtime.DaisyRTException;
import org.outerj.daisy.runtime.DaisyRuntime;
import org.outerj.daisy.runtime.ContainerEntry;
import org.outerj.daisy.runtime.cli.Logging;
import org.outerj.daisy.runtime.repository.ArtifactNotFoundException;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.core.io.InputStreamResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;

public class ContainerConfigImpl implements ContainerConfig {
    private ClassLoadingConfig classLoadingConfig;
    private ContainerEntry containerEntry;
    private Properties springConfigProperties;
    private List<SpringEntry> entries = new ArrayList<SpringEntry>();
    private DaisyRuntime runtime;
    protected final Log infolog = LogFactory.getLog(Logging.INFO_LOG_CATEGORY);

    // These ThreadLocal's serve as communication mechanism for DaisyRuntimeNamespaceHandler
    protected static ThreadLocal<DaisyRuntime> BUILD_CONTEXT_RUNTIME = new ThreadLocal<DaisyRuntime>();
    protected static ThreadLocal<Map<Class, String>> BUILD_CONTEXT_EXPORTS = new ThreadLocal<Map<Class, String>>();

    public ContainerConfigImpl(ContainerEntry containerEntry, ClassLoadingConfig classLoadingConfig, Properties springConfigProperties, DaisyRuntime runtime) {
        this.containerEntry = containerEntry;
        this.classLoadingConfig = classLoadingConfig;
        this.springConfigProperties = springConfigProperties;
        this.runtime = runtime;
    }

    public ClassLoadingConfig getClassLoadingConfig() {
        return classLoadingConfig;
    }

    public Container build() throws ArtifactNotFoundException, MalformedURLException {
        infolog.info("Starting container " + getId() + " - " + getLocation());
        ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
        Map<Class, String> beansToExport = new HashMap<Class, String>();
        BUILD_CONTEXT_RUNTIME.set(runtime);
        BUILD_CONTEXT_EXPORTS.set(beansToExport);
        try {
            ClassLoader classLoader = classLoadingConfig.getClassLoader(runtime.getClassLoader());

            Thread.currentThread().setContextClassLoader(classLoader);

            GenericApplicationContext applicationContext = new GenericApplicationContext();
            applicationContext.setDisplayName(containerEntry.getId());
            applicationContext.setClassLoader(classLoader);

            PropertyPlaceholderConfigurer propertyConfigurer = new PropertyPlaceholderConfigurer();
            propertyConfigurer.setProperties(springConfigProperties);
            propertyConfigurer.setIgnoreUnresolvablePlaceholders(true);
            applicationContext.addBeanFactoryPostProcessor(propertyConfigurer);

            XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(applicationContext);
            xmlReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
            xmlReader.setBeanClassLoader(classLoader);

            for (SpringEntry entry : entries) {
                xmlReader.loadBeanDefinitions(new InputStreamResource(new ByteArrayInputStream(entry.getData()), entry.getPath() + " in " + containerEntry.getFile().getAbsolutePath()));
            }
            applicationContext.refresh();

            ContainerImpl componentContainer = new ContainerImpl(applicationContext, classLoader);

            // Handle the service exports
            for (Map.Entry<Class, String> entry : beansToExport.entrySet()) {
                Class serviceType = entry.getKey();
                if (!serviceType.isInterface())
                    throw new DaisyRTException("Exported service is not an interface: " + serviceType.getName());

                String beanName = entry.getValue();
                Object component;
                try {
                    component = applicationContext.getBean(beanName);
                } catch (NoSuchBeanDefinitionException e) {
                    throw new DaisyRTException("Bean not found for service to export, service type " + serviceType.getName() + ", bean name " + beanName, e);
                }

                if (!serviceType.isAssignableFrom(component.getClass()))
                    throw new DaisyRTException("Exported service does not implemented specified type interface. Bean = " + beanName + ", interface = " + serviceType.getName());
                
                infolog.debug(" exporting bean " + beanName + " for service " + serviceType.getName());
                runtime.provideService(serviceType, wrapAsComponent(serviceType, component, componentContainer, classLoader));
            }

            return componentContainer;
        } catch (Throwable e) {
            throw new DaisyRTException("Error constructing component container defined at " + containerEntry.getFile().getAbsolutePath(), e);
        } finally {
            Thread.currentThread().setContextClassLoader(previousContextClassLoader);
            BUILD_CONTEXT_RUNTIME.set(null);
            BUILD_CONTEXT_EXPORTS.set(null);
        }
    }

    /**
     * Wraps the bean to assure only that only methods of the published service
     * can be accessed.
     */
    private Object wrapAsComponent(Class serviceInterface, Object bean, Container container, ClassLoader classLoader) {
        ComponentInvocationHandler handler = new ComponentInvocationHandler(bean, container, serviceInterface, classLoader);
        return Proxy.newProxyInstance(classLoader, new Class[] { serviceInterface }, handler);
    }

    protected void addSpringConfig(String path, byte[] data) {
        entries.add(new SpringEntry(path, data));
    }

    public String getLocation() {
        return containerEntry.getFile().getAbsolutePath();
    }

    public String getId() {
        return containerEntry.getId();
    }

    private class SpringEntry {
        private final String path;
        private final byte[] data;

        public SpringEntry(String path, byte[] data) {
            this.path = path;
            this.data = data;
        }

        public String getPath() {
            return path;
        }

        public byte[] getData() {
            return data;
        }
    }
}
