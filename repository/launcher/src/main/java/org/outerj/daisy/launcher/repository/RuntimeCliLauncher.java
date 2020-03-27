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

public class RuntimeCliLauncher {
    private File repositoryLocation;

    public static void main(String[] args) throws Throwable {
        String launcherRepo = System.getProperty("daisy.launcher.repository");
        if (launcherRepo == null) {
            System.err.println();
            System.err.println("You need to define a Java system property (using -D) called");
            System.err.println("daisy.launcher.repository, pointing to the artifact repository.");
            System.err.println();
            System.exit(1);
        }

        launch(launcherRepo, args);
    }

    public static void launch(String repositoryLocation, String[] args) throws Throwable {
        new RuntimeCliLauncher(repositoryLocation).run(args);
    }

    private RuntimeCliLauncher(String repositoryLocation) {
        this.repositoryLocation = new File(repositoryLocation);
    }

    public void run(String[] args) throws Throwable {

        ClassLoader classLoader = LauncherClasspathHelper.getClassLoader("org/outerj/daisy/launcher/repository/cli-classloader.xml", repositoryLocation);

        Method mainMethod;
        try {
            Class runtimeClass = classLoader.loadClass("org.outerj.daisy.runtime.cli.DaisyRuntimeCli");
            mainMethod = runtimeClass.getMethod("main", String[].class);
        } catch (Exception e) {
            throw new RuntimeException("Error loading Daisy runtime", e);
        }

        try {
            mainMethod.invoke(null, (Object)args);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error launching Daisy runtime", e);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

}
