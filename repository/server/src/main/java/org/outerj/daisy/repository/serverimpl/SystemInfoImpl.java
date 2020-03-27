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
package org.outerj.daisy.repository.serverimpl;

import java.util.Date;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.annotation.PreDestroy;

import org.outerj.daisy.repository.RepositoryManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SystemInfoImpl implements SystemInfoImplMBean {
    private final long systemStartTime;
    private String serverVersion;
    private MBeanServer mbeanServer;
    private ObjectName mbeanName = new ObjectName("Daisy:name=SystemInfo");
    private final Log log = LogFactory.getLog(getClass());

    public SystemInfoImpl(RepositoryManager repositoryManager, MBeanServer mbeanServer) throws Exception {
        systemStartTime = System.currentTimeMillis();
        this.serverVersion = repositoryManager.getRepositoryServerVersion();
        this.mbeanServer = mbeanServer;
        this.initialize();
    }

    private void initialize() throws Exception {
        mbeanServer.registerMBean(this, mbeanName);
    }

    @PreDestroy
    public void destroy() {
        try {
            mbeanServer.unregisterMBean(mbeanName);
        } catch (Exception e) {
            log.error("Error unregistering MBean", e);
        }
    }

    public long getFreeVmMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public String getUpTime() {
        // Runtime.getRuntime().
        long timeDifference = System.currentTimeMillis() - systemStartTime;

        long days = timeDifference / (1000 * 3600 * 24);
        timeDifference %= 1000 * 3600 * 24;
        long hours = timeDifference / (1000 * 3600);
        timeDifference %= 1000 * 3600;
        long minutes = timeDifference / (1000 * 60);
        timeDifference %= 1000 * 60;
        long seconds = timeDifference / 1000;

        return "The system is up for " + days + " days, " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds.";
    }

    public void triggerGarbageCollector() {
        System.gc();
    }

    public Date getStartTime() {
        return new Date(systemStartTime);
    }

    public long getMaxVmMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public long getTotalVmMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public int getAvailableProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    public String getVmMemoryFormatted() {
        return formatSizeInMb(getFreeVmMemory()) + " of " + formatSizeInMb(getTotalVmMemory()) + " (max " + formatSizeInMb(getMaxVmMemory()) + ")";
    }

    private String formatSizeInMb(long sizeInKb) {
        long sizeInMb = sizeInKb / (1024 * 1024);
        return sizeInMb + "M";
    }

    public String getFileEncoding() {
        return System.getProperty("file.encoding");
    }

    public String getServerVersion() {
        return serverVersion;
    }
}
