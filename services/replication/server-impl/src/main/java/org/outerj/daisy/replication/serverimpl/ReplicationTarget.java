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

import org.outerj.daisy.repository.Credentials;

public class ReplicationTarget {
    
    private String name;
    private String role;
    private String url;
    private Credentials credentials;
    private boolean suspended = false;
    
    public ReplicationTarget(String name, String url, Credentials credentials, String role) {
        this.name = name;
        this.url = url;
        this.credentials = credentials;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public String getUrl() {
        return url;
    }

    public Credentials getCredentials() {
        return credentials;
    }
    
    public boolean isSuspended() {
        return suspended;
    }
    
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }
}
