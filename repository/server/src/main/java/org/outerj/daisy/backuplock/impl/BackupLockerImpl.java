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
package org.outerj.daisy.backuplock.impl;

import org.outerj.daisy.backuplock.spi.SuspendableProcess;
import org.outerj.daisy.jms.JmsClient;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.plugin.PluginUser;
import org.outerj.daisy.plugin.PluginHandle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

/**
 * The purpose of the BackupLocker is to lock all operations that involve
 * persistent modifications outside of the main SQL database. This way,
 * a consistent backup can be taken of all these persistent stores.
 *
 */
public class BackupLockerImpl implements BackupLockerImplMBean {
    private JmsClient jmsClient;
    private MBeanServer mbeanServer;
    private ObjectName mbeanName = new ObjectName("Daisy:name=BackupLocker");
    private List<ProcessInfo> suspendableProcesses = new ArrayList<ProcessInfo>();
    private boolean locked = false;
    private ExecutorService executorService;
    private PluginRegistry pluginRegistry;
    private MyPluginUser pluginUser = new MyPluginUser();
    private SuspendableProcess jmsClientProcess;
    private Log log = LogFactory.getLog(getClass());

    public BackupLockerImpl(JmsClient jmsClient, MBeanServer mbeanServer, PluginRegistry pluginRegistry) throws Exception {
        this.jmsClient = jmsClient;
        this.mbeanServer = mbeanServer;
        this.pluginRegistry = pluginRegistry;
        this.initialize();
    }

    private void initialize() throws Exception {
        mbeanServer.registerMBean(this, mbeanName);

        // Since the JMS client is not specific to the repository server, we register
        // it ourselves instead of the other way around
        jmsClientProcess = new SuspendableProcess() {
            public boolean suspendExecution(long msecs) throws InterruptedException {
                return jmsClient.suspend(msecs);
            }

            public void resumeExecution() {
                jmsClient.resume();
            }
        };
        pluginRegistry.addPlugin(SuspendableProcess.class, "JMS client", jmsClientProcess);

        pluginRegistry.setPluginUser(SuspendableProcess.class, pluginUser);

        this.executorService = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public synchronized void destroy() {
        unlockInCurrentThread();
        suspendableProcesses.clear();
        pluginRegistry.removePlugin(SuspendableProcess.class, "JMS client", jmsClientProcess);
        pluginRegistry.unsetPluginUser(SuspendableProcess.class, pluginUser);
        try {
            mbeanServer.unregisterMBean(mbeanName);
        } catch (Exception e) {
            log.error("Error unregistering MBean", e);
        }
    }

    private class MyPluginUser implements PluginUser<SuspendableProcess> {
        public void pluginAdded(PluginHandle<SuspendableProcess> pluginHandle) {
            if (pluginHandle.getName().equals("Blobstore")) {
                suspendableProcesses.add(new ProcessInfo(pluginHandle));
            } else {
                // Note: add at begin of list, so that it gets suspended before blobstore
                suspendableProcesses.add(0, new ProcessInfo(pluginHandle));
            }
        }

        public void pluginRemoved(PluginHandle<SuspendableProcess> pluginHandle) {
            Iterator<ProcessInfo> it = suspendableProcesses.iterator();
            while (it.hasNext()) {
                ProcessInfo processInfo = it.next();
                if (processInfo.pluginHandle == pluginHandle) {
                    if (processInfo.isSuspended) {
                        processInfo.process.resumeExecution();
                    }
                    it.remove();
                    break;
                }
            }
        }
    }

    static class ProcessInfo {
        String name;
        SuspendableProcess process;
        boolean isSuspended;
        PluginHandle<SuspendableProcess> pluginHandle;

        public ProcessInfo(PluginHandle<SuspendableProcess> pluginHandle) {
            this.name = pluginHandle.getName();
            this.process = pluginHandle.getPlugin();
            this.pluginHandle = pluginHandle;
        }
    }

    public synchronized void lock(final long msecs) throws Exception {
        if (locked)
            throw new Exception("Already locked for backup.");

        // Since the suspend/resumeExecution is usually implemented using a
        // reentrant lock, we have to make sure the suspend and resume are
        // executed on the same thread. Therefore we go via a single-threaded
        // executor service.

        Future<Object> future = executorService.submit(new Callable<Object>() {
            public Object call() throws Exception {
                for (ProcessInfo processInfo : suspendableProcesses) {
                    boolean success = false;
                    try {
                        success = processInfo.process.suspendExecution(msecs);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    if (!success) {
                        // call unlock to make sure everything is unlocked again.
                        unlockInCurrentThread();
                        return "Failed to suspend execution of " + processInfo.name + " within the given time out of " + msecs + " msecs.";
                    } else {
                        processInfo.isSuspended = true;
                    }
                }
                locked = true;
                return null;
            }
        });
        String errorMessage = (String)future.get();
        if (errorMessage != null)
            throw new Exception(errorMessage);
    }

    public synchronized void unlock() throws Exception {
        if (!locked)
            throw new Exception("Cannot unlock, because currently not locked.");

        // See comment in lock method for why we go through the executor service.

        Future<Object> future = executorService.submit(new Callable<Object>() {
            public Object call() throws Exception {
                unlockInCurrentThread();
                return null;
            }
        });
        future.get();
    }

    private void unlockInCurrentThread() {
        for (ProcessInfo processInfo : suspendableProcesses) {
            if (processInfo.isSuspended) {
                processInfo.process.resumeExecution();
                processInfo.isSuspended = false;
            }
        }
        locked = false;
    }

    public synchronized boolean isLocked() {
        return locked;
    }
}
