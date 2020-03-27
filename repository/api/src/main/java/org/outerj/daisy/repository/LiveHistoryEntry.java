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

import org.outerx.daisy.x10.LiveHistoryEntryDocument;

/**
 * <p>A live history entry contains information about a timeperiod during which a specific version of a daisy document was (or is or will be) live.
 * The document model does not allow overlapping entries.</p>
 */
public interface LiveHistoryEntry {
    long getId();

    /**
     * The beginning of the timeperiod (inclusive)
     * @return
     */
    public Date getBeginDate();
    
    /**
     * The end of the timeperiod (exclusive)
     * @return
     */
    public Date getEndDate();
    
    /**
     * The version number that was live at the given period of time
     * @return
     */
    public long getVersionId();
    
    /**
     * The user who created this live entry
     * @return
     */
    public long getCreator();

    void toXml(LiveHistoryEntryDocument.LiveHistoryEntry addNewLiveHistoryEntry);

}
