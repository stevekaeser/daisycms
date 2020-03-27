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
package org.outerj.daisy.linkextraction;

public interface LinkCollector {
    /**
     * Adds a link.
     *
     * @param targetDocId may or may not include a namespace
     * @param targetBranch branch either as name or ID
     * @param targetLanguage language either as name or ID
     * @param version -1 for live, -2 for last, or specific number. If link specifies no specific version, use -1 (live).
     */
    void addLink(LinkType linkType, String targetDocId, String targetBranch, String targetLanguage, long version);

    void addLink(LinkType linkType, String daisyLink);
}
