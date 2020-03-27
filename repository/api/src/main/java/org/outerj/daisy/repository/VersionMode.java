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

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;


public class VersionMode {

    private static DateTimeFormatter iso8601 = ISODateTimeFormat.dateOptionalTimeParser();
    private static DateTimeFormatter iso8601printer = ISODateTimeFormat.dateTimeNoMillis();
    public static final VersionMode LIVE = new VersionMode("live");
    public static final VersionMode LAST = new VersionMode("last");
    
    private String mode;
    private Date pointInTime;
    
    private VersionMode(String mode) {
        if (mode.equals("live") || mode.equals("-1")) {
           this.mode = "live"; 
        } else if (mode.equals("last") || mode.equals("-2")) {
            this.mode = "last";
        } else {
            try {
                pointInTime = iso8601.parseDateTime(mode).toDate();
            } catch (IllegalArgumentException iae) {
                throw new IllegalArgumentException("Bad version mode string ", iae);
            }
            this.mode = mode;
        }
    }

    private VersionMode(Date date) {
        pointInTime = date;
        mode = iso8601printer.print(pointInTime.getTime());
    }
    
    public static VersionMode get(String mode) {
        if (mode == null) {
            throw new NullPointerException("name should not be null");
        }
        if (mode.equals("live")) {
            return LIVE;
        }
        if (mode.equals("last")) {
            return LAST;
        }
        return new VersionMode(mode);
    }
    
    public static VersionMode get(Date date) {
        if (date == null) {
            throw new NullPointerException("date null");
        }
        return new VersionMode(date);
    }

    public boolean equals(VersionMode that) {
        if (that == null) {
            return false;
        }
        return that.mode.equals(mode);
    }
    
    public int hashCode() {
        return mode.hashCode();
    }
    
    public String toString() {
        return mode;
    }

    public boolean isLast() {
        return this.equals(LAST);
    }

    public boolean isLive() {
        return this.equals(LIVE);
    }
    
    public Date getDate() {
        if (this.equals(LAST) || this.equals(LIVE)) {
            return null;
        }
        return pointInTime;
    }
}
