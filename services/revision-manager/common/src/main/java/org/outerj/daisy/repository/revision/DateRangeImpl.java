/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository.revision;

import java.util.Date;

/**
 * //javadoc copied from DateRange interface
 * Representation of a date range. Start date is inclusive, end date is exclusive.
 * A null value indicates an open-ended range.
 */
public class DateRangeImpl implements DateRange {
    private Date start;
    private Date end;
    
    public static DateRangeImpl startingFrom(Date start) {
        return new DateRangeImpl(start, null);
    }
    
    public static DateRangeImpl upTo(Date end) {
        return new DateRangeImpl(null, end);
    }
    
    public DateRangeImpl(Date start, Date end) {
        this.start = start;
        this.end = end;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }
    
    /**
     * @param date
     * @return true if this range contains the date (start inclusive, end exclusive)
     */
    public boolean includes(Date date) {
        if (date == null) {
            throw new NullPointerException("date should not be null");
        }

        if (start != null && start.after(date))
            return false;
        if (end != null && !date.before(end))
            return false;
        
        return true;
    }
    
    /**
     * @param other
     * @return true if the ranges are overlapping.
     */
    public boolean overlaps(DateRange other) {
        return (other.getStart() != null && includes(other.getStart()))
                || (other.getEnd() != null && includes(other.getEnd()))
                || (other.getStart() == null && other.getEnd() == null);
    }
    
}
