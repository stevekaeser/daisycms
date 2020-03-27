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
package org.outerj.daisy.launcher.remoteclient;

import org.outerj.daisy.launcher.LauncherClasspathHelper;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class RemoteClientLauncher {
    public static Object getRepositoryManager(String url, Object credentials, Object jmsClient, String jmsTopic, File repositoryLocation) {
        try {
            ClassLoader classLoader = LauncherClasspathHelper.getClassLoader("org/outerj/daisy/launcher/remoteclient/classloader.xml", repositoryLocation);

            Class repoMgrClass = classLoader.loadClass("org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager");
            Class credentialsClass = classLoader.loadClass("org.outerj.daisy.repository.Credentials");
            Class jmsClass = classLoader.loadClass("org.outerj.daisy.jms.JmsClient");


            Constructor constructor = repoMgrClass.getConstructor(String.class, credentialsClass, jmsClass, String.class);
            Object repoMgr = constructor.newInstance(url, credentials, jmsClient, jmsTopic);

            Class extProviderClass = classLoader.loadClass("org.outerj.daisy.repository.spi.ExtensionProvider");
            Class emailSubMgrProviderClass = classLoader.loadClass("org.outerj.daisy.emailnotifier.clientimpl.RemoteEmailSubscriptionManagerProvider");
            Class navMgrProviderClass = classLoader.loadClass("org.outerj.daisy.navigation.clientimpl.RemoteNavigationManagerProvider");
            Class docTaskMgrProviderClass = classLoader.loadClass("org.outerj.daisy.doctaskrunner.clientimpl.RemoteDocumentTaskManagerProvider");
            Class wfMgrProviderClass = classLoader.loadClass("org.outerj.daisy.workflow.clientimpl.RemoteWorkflowManagerProvider");

            Method regExtMethod = repoMgrClass.getMethod("registerExtension", String.class, extProviderClass);
            regExtMethod.invoke(repoMgr, "EmailSubscriptionManager", emailSubMgrProviderClass.newInstance());
            regExtMethod.invoke(repoMgr, "NavigationManager", navMgrProviderClass.newInstance());
            regExtMethod.invoke(repoMgr, "DocumentTaskManager", docTaskMgrProviderClass.newInstance());
            regExtMethod.invoke(repoMgr, "WorkflowManager", wfMgrProviderClass.newInstance());

            return repoMgr;
        } catch (Throwable e) {
            throw new RuntimeException("Error creating remote Daisy repository client.", e);
        }
    }
}
