/*
 * Copyright 2008 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.runtime.repository;

import java.net.URL;
import java.util.List;

public class ResolvedArtifact {
    private URL url;
    private List<String> searchedLocations;
    private boolean exists;

    public ResolvedArtifact(URL url, List<String> searchedLocations, boolean exists) {
        this.url = url;
        this.searchedLocations = searchedLocations;
        this.exists = exists;
    }

    public URL getUrl() {
        return url;
    }

    public List<String> getSearchedLocations() {
        return searchedLocations;
    }

    public boolean exists() {
        return exists;
    }
}
