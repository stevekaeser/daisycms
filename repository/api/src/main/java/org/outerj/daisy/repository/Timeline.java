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
package org.outerj.daisy.repository;

import java.util.Date;

import org.outerx.daisy.x10.TimelineDocument;

public interface Timeline {

    public LiveHistoryEntry addLiveHistoryEntry(Date beginDate, Date endDate, long versionId) ;
    
    public void deleteLiveHistoryEntry(LiveHistoryEntry liveHistoryEntry);

    public LiveHistoryEntry getLiveHistoryEntryAt(Date date);

    /**
     * This method is useful for checking whether the user will have read access to the given version
     * if he does not have READ_NON_LIVE permissions 
     * 
     * @param versionId
     * @return true if there is a live history entry referring to the given version
     */
    public boolean hasLiveHistoryEntry(long versionId);
    
    public LiveHistoryEntry[] getLiveHistory();

    TimelineDocument getXml();
    
    public long getVersionId(Date when);

    public void save() throws RepositoryException;
}
