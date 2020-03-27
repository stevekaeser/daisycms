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
package org.outerj.daisy.repository.commonimpl;

import java.util.Date;

import org.apache.xmlbeans.GDateBuilder;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerx.daisy.x10.LiveHistoryEntryDocument;

public class LiveHistoryEntryImpl implements LiveHistoryEntry {
    
    private long id = -1;
    private Date beginDate;
    private Date endDate;
    private long versionId;
    private long creator;
    
    public LiveHistoryEntryImpl(Date beginDate, Date endDate, long versionId, long creator) {
        this.beginDate = beginDate;
        this.endDate = endDate;
        this.versionId = versionId;
        this.creator = creator;
    }
            
    public LiveHistoryEntryImpl(long id, Date beginDate, Date endDate, long versionId, long creator) {
        this.id = id;
        this.beginDate = beginDate;
        this.endDate = endDate;
        this.versionId = versionId;
        this.creator = creator;
    }

    public long getId() {
        return id;
    }

    public Date getBeginDate() {
        return beginDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public long getVersionId() {
        return versionId;
    }
    
    public long getCreator() {
        return creator;
    }

    public void toXml(LiveHistoryEntryDocument.LiveHistoryEntry entryXml) {
        if (id != -1)
            entryXml.setId(id);
        entryXml.setVersionId(versionId);
        
        GDateBuilder gdb = new GDateBuilder(beginDate);
        gdb.normalizeToTimeZone(0);
        entryXml.setBeginDate(gdb.getCalendar());
        
        if (endDate != null) {
            gdb = new GDateBuilder(endDate);
            gdb.normalizeToTimeZone(0);
            entryXml.setEndDate(gdb.getCalendar());
        }
        
        entryXml.setCreator(creator);
    }
    
    public static LiveHistoryEntry fromXml(LiveHistoryEntryDocument.LiveHistoryEntry entryXml) {
        Date beginDate = entryXml.getBeginDate().getTime();
        Date endDate = null;
        if (entryXml.isSetEndDate()) {
            endDate = entryXml.getEndDate().getTime();
        }
        if (entryXml.isSetId())
            return new LiveHistoryEntryImpl(entryXml.getId(), beginDate, endDate, entryXml.getVersionId(), entryXml.getCreator());
        
        return new LiveHistoryEntryImpl(beginDate, endDate, entryXml.getVersionId(), entryXml.getCreator());
    }

}
