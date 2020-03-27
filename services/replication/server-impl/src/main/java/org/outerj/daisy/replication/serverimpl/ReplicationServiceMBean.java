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
package org.outerj.daisy.replication.serverimpl;

import org.outerj.daisy.repository.RepositoryException;

public interface ReplicationServiceMBean {
    
    public void scheduleReplication(String query) throws RepositoryException;
    
    public boolean isSuspended(String targetName);
    
    public void resumeTarget(String targetName);
    
    public void suspendTarget(String targetName);
    
    public void restartFailedReplications() throws RepositoryException;
    
    public void clearFailedReplications() throws RepositoryException;
    
    public void triggerReplication();
    
    public int getReplicationQueueSize();
    
    public int getReplicationErrors();
    
    public String[] getTargetNames();
    
    public String[] getFailedTargetNames();
}
