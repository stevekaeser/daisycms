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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Timeline;
import org.outerj.daisy.util.DateUtil;
import org.outerx.daisy.x10.TimelineDocument;

public class TimelineImpl implements Timeline {
    
    private static String READ_ONLY_MESSAGE = "This Timeline object is read-only.";

    // internal stuff
    private DocumentVariantImpl ownerVariant;
    private DocumentStrategy documentStrategy;
    private IntimateAccess intimateAccess = new IntimateAccess();
    
    // data
    private List<LiveHistoryEntry> liveHistory = new ArrayList<LiveHistoryEntry>();

    // state
    private long variantUpdateCount;
    private boolean hasChanges = false;

    
    public TimelineImpl(DocumentVariantImpl ownerVariant, DocumentStrategy documentStrategy) {
        this.ownerVariant = ownerVariant;
        this.documentStrategy = documentStrategy;
        
    }
    
    public TimelineImpl.IntimateAccess getIntimateAccess(DocumentStrategy strategy) {
        if (this.documentStrategy == strategy)
            return intimateAccess;
        return null;
    }

    /**
     * Important note: millisecond information is removed
     * @param beginDate
     * @param endDate
     * @param versionId
     */
    public LiveHistoryEntry addLiveHistoryEntry(Date beginDate, Date endDate, long versionId) {
        if (ownerVariant.getIntimateAccess(documentStrategy).getDocument().isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);
        
        return addLiveHistoryEntry(beginDate, endDate, versionId, true);
    }

    private LiveHistoryEntry addLiveHistoryEntry(Date beginDate, Date endDate, long versionId, boolean checkVersionId) {
        if (beginDate == null)
            throw new NullPointerException("Begindate can't be null");
        beginDate = DateUtil.getNormalizedDate(beginDate, true);
        endDate = DateUtil.getNormalizedDate(endDate, true);
        if (endDate != null && !endDate.after(beginDate)) {
            throw new IllegalArgumentException("End date should be after the begin date");
        }
        if (checkVersionId) {
            if (versionId <= 0 || versionId > ownerVariant.getLastVersionId()) {
                throw new IllegalArgumentException("Version id points to a non-existing version");
            }
        }
        int pos = getLiveHistoryPosition(beginDate);
        if (pos > 0) {
            LiveHistoryEntry previous = liveHistory.get(pos - 1);
            if (previous.getEndDate() == null || previous.getEndDate().after(beginDate)) {
                throw new IllegalArgumentException("Live history entry overlaps with entry beginning at " + previous.getBeginDate());
            }
        }
        if (pos < liveHistory.size()) {
            LiveHistoryEntry next = liveHistory.get(pos);
            if (endDate == null || next.getBeginDate().before(endDate)) {
                throw new IllegalArgumentException("Live history entry overlaps with entry beginning at " + next.getBeginDate());
            }
        }
        hasChanges = true;
        LiveHistoryEntryImpl newEntry = new LiveHistoryEntryImpl(beginDate, endDate, versionId, ownerVariant.getIntimateAccess(documentStrategy).getCurrentUser().getId());
        liveHistory.add(pos, newEntry);
        return newEntry;
    }
    
    public void deleteLiveHistoryEntry(LiveHistoryEntry lhe) {
        if (ownerVariant.getIntimateAccess(documentStrategy).getDocument().isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);
        
        hasChanges = true;
        liveHistory.remove(lhe);
    }
    
    /**
     * important note: millisecond information is ignored.
     * @param when
     * @return
     */
    public LiveHistoryEntry getLiveHistoryEntryAt(Date when) {
        if (when == null) {
            throw new NullPointerException("The date must not be null");
        }
        when = DateUtil.getNormalizedDate(when, true); 
        
        if (liveHistory.size() == 0) {
            return null;
        }
        int result = getLiveHistoryPosition(when);

        LiveHistoryEntry previous = null;
        LiveHistoryEntry current = null;
        if (result > 0)
            previous = liveHistory.get(result - 1);
        if (result < liveHistory.size())
            current = liveHistory.get(result);
        
        if (previous != null && !when.before(previous.getBeginDate()) && (previous.getEndDate() == null || previous.getEndDate().after(when))) {
            return previous;
        }
        if (current != null && !when.before(current.getBeginDate()) && (current.getEndDate() == null || current.getEndDate().after(when))) {
            return current;
        }

        return null;
    }
    
    /**
     * Calculates the correct position to insert a live history entry beginning at the given date.  This means
     * that you still must verify the corresponding live history entry if you are looking for an entry matching the given date.
     * @param when
     * @return the position in the liveHistory list where an entry beginning at the given date would be inserted.   
     */
    private int getLiveHistoryPosition(Date when) {
        when = DateUtil.getNormalizedDate(when, true);
        int first = 0;
        int last = liveHistory.size();

        while (first < last) {
            int mid = (first + last) / 2;
            if (when.after(liveHistory.get(mid).getBeginDate())) {
                first = mid + 1;
            } else {
                last = mid;
            }
        }
        
        return first;
    }
    
    public boolean hasLiveHistoryEntry(long versionId) {
        for (LiveHistoryEntry lhe: liveHistory) {
            if (lhe.getVersionId() == versionId) {
                return true;
            }
        }
        return false;
    }

    public LiveHistoryEntry[]  getLiveHistory() {
        return (LiveHistoryEntry[]) liveHistory.toArray(new LiveHistoryEntry[liveHistory.size()]);
    }

    public class IntimateAccess {

        private IntimateAccess() {
        }
        
        public void invalidate() {
            TimelineImpl.this.liveHistory = null;
        }
        
        public void setLiveHistory(LiveHistoryEntry[] liveHistory, long variantUpdateCount) {
            TimelineImpl.this.liveHistory = new ArrayList<LiveHistoryEntry>(Arrays.asList(liveHistory));
            TimelineImpl.this.variantUpdateCount = variantUpdateCount;
            TimelineImpl.this.hasChanges = false;
        }

        public LiveHistoryEntry addLiveHistoryEntry(Date beginDate, Date endDate, long versionId) {
            return TimelineImpl.this.addLiveHistoryEntry(beginDate, endDate, versionId, false);
        }

        public boolean hasChanges() {
            return TimelineImpl.this.hasChanges;
        }
    }
    
    public TimelineDocument getXml() {
        TimelineDocument timelineDoc = TimelineDocument.Factory.newInstance();
        TimelineDocument.Timeline timelineXml = timelineDoc.addNewTimeline();
        for (LiveHistoryEntry entry: liveHistory) {
            entry.toXml(timelineXml.addNewLiveHistoryEntry());
        }
        
        timelineXml.setVariantUpdateCount(variantUpdateCount);
        
        return timelineDoc;
    }

    public void save() throws RepositoryException {
        if (hasChanges) {
            documentStrategy.storeTimeline(ownerVariant.getIntimateAccess(documentStrategy).getVariant(), this);
        }
    }
    
    public long getVersionId(Date when) {
        LiveHistoryEntry lhe = getLiveHistoryEntryAt(when);
        if (lhe == null) {
            return -1;
        }
        return lhe.getVersionId();
    }
}
