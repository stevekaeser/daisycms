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
package org.outerj.daisy.launcher.repository;

import org.outerj.daisy.launcher.LauncherClasspathHelper;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;
import java.util.Set;

public class RuntimeLauncher {
    private String repositoryLocation;
    private String runtimeConfigLocation;
    private String componentConfigLocation;
    private Set<String> disabledContainerIds;

    public static RuntimeHandle launch(String runtimeConfigLocation, String repositoryLocation,
            String componentConfigLocation, Set<String> disabledContainerIds) {
        return new RuntimeLauncher(runtimeConfigLocation, repositoryLocation, componentConfigLocation, disabledContainerIds).run();
    }

    private RuntimeLauncher(String runtimeConfigLocation, String repositoryLocation, String componentConfigLocation,
            Set<String> disabledContainerIds) {
        this.runtimeConfigLocation = runtimeConfigLocation;
        this.repositoryLocation = repositoryLocation;
        this.componentConfigLocation = componentConfigLocation;
        this.disabledContainerIds = disabledContainerIds;
    }

    public RuntimeHandle run() {
        ClassLoader classLoader = LauncherClasspathHelper.getClassLoader("org/outerj/daisy/launcher/repository/embedded-classloader.xml", new File(repositoryLocation));

        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        Object runtime;
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            Class runtimeHelperClass = classLoader.loadClass("org.outerj.daisy.runtime.DaisyRuntimeHelper");
            Method createMethod = runtimeHelperClass.getMethod("createRuntime", String.class, Properties.class, String.class, Set.class);
            Properties configProps = new Properties();
            configProps.put("daisy.configLocation", componentConfigLocation);
            runtime = createMethod.invoke(null, runtimeConfigLocation, configProps, repositoryLocation, disabledContainerIds);
            return new RuntimeHandle(runtime);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error loading Daisy runtime", e.getTargetException());
        } catch (Exception e) {
            throw new RuntimeException("Error loading Daisy runtime", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        }
    }
}
