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

import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.LockType;
import org.outerx.daisy.x10.LockInfoDocument;

import java.util.Date;
import java.util.GregorianCalendar;

public class LockInfoImpl implements LockInfo {
    private boolean hasLock;
    private long userId;
    private Date timeAcquired;
    private long duration;
    private LockType lockType;

    public LockInfoImpl() {
        this.hasLock = false;
    }

    public LockInfoImpl(long userId, Date timeAcquired, long duration, LockType lockType) {
        this.hasLock = true;
        this.userId = userId;
        this.timeAcquired = timeAcquired;
        this.duration = duration;
        this.lockType = lockType;
    }

    public long getUserId() {
        return userId;
    }

    public Date getTimeAcquired() {
        return timeAcquired;
    }

    public long getDuration() {
        return duration;
    }

    public LockType getType() {
        return lockType;
    }

    public boolean hasLock() {
        return hasLock;
    }

    public LockInfoDocument getXml() {
        LockInfoDocument lockInfoDocument = LockInfoDocument.Factory.newInstance();
        LockInfoDocument.LockInfo lockInfoXml = lockInfoDocument.addNewLockInfo();

        if (!hasLock) {
            lockInfoXml.setHasLock(false);
        } else {
            lockInfoXml.setHasLock(true);
            lockInfoXml.setUserId(userId);
            GregorianCalendar timeAcquiredCalendar = new GregorianCalendar();
            timeAcquiredCalendar.setTime(timeAcquired);
            lockInfoXml.setTimeAcquired(timeAcquiredCalendar);
            lockInfoXml.setDuration(duration);
            lockInfoXml.setType(LockInfoDocument.LockInfo.Type.Enum.forString(lockType.toString()));
        }

        return lockInfoDocument;
    }
}
