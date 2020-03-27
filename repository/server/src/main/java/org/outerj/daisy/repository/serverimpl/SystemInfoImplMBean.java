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
package org.outerj.daisy.repository.serverimpl;

import java.util.Date;

/**
 * MBean for providing some general system information.
 */
public interface SystemInfoImplMBean {
    /**
     * Shows the current amount of free memory in the VM, measured in bytes.
     */
    long getFreeVmMemory();

    String getVmMemoryFormatted();

    /**
     * Shows the maximum amount of memory that the VM will attempt to use, measured in bytes.
     */
    long getMaxVmMemory();

    /**
     * Shows the total amount of free memory in the VM, measured in bytes.
     */
    long getTotalVmMemory();

    /**
     * Shows the number of available processors for the VM
     */
    int getAvailableProcessors();

    /**
     * Shows the (approx.) time when the system was started.
     */
    Date getStartTime();

    /**
     * Shows the (approx.) current system uptime
     */
    String getUpTime();

    /**
     * Suggests the VM to run the Garbage Collector
     */
    void triggerGarbageCollector();

    /**
     * Shows file encoding.
     */
    String getFileEncoding();
    
    /**
     * 
     */
    String getServerVersion();
}
