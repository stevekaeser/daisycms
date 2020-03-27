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
package org.outerj.daisy.query.model.functions;

import junit.framework.TestCase;

import java.util.GregorianCalendar;
import java.util.Calendar;

public class RelativeDateTimeFunctionTest extends TestCase {
    //
    // This tests the special cases where rolling the week or month accross year boundaries (for first/last week/month)
    //

    public void testFirstWeekRollover() {
        GregorianCalendar calendar = new GregorianCalendar(2008, 0, 1);
        assertEquals(1, calendar.get(Calendar.WEEK_OF_YEAR));
        Calendar result = RelativeDateTimeFunction.calcDate(calendar, true, -1, Calendar.WEEK_OF_YEAR, Calendar.DAY_OF_WEEK);
        assertEquals(2007, result.get(Calendar.YEAR));
        assertEquals(52, result.get(Calendar.WEEK_OF_YEAR));
    }

    public void testLastWeekRollover() {
        GregorianCalendar calendar = new GregorianCalendar(2007, 0, 360);
        assertEquals(52, calendar.get(Calendar.WEEK_OF_YEAR));
        Calendar result = RelativeDateTimeFunction.calcDate(calendar, false, 1, Calendar.WEEK_OF_YEAR, Calendar.DAY_OF_WEEK);
        assertEquals(2008, result.get(Calendar.YEAR));
        assertEquals(1, result.get(Calendar.WEEK_OF_YEAR));
    }

    public void testFirstMonthRollover() {
        GregorianCalendar calendar = new GregorianCalendar(2008, 0, 1);
        assertEquals(0, calendar.get(Calendar.MONTH));
        Calendar result = RelativeDateTimeFunction.calcDate(calendar, true, -1, Calendar.MONTH, Calendar.DAY_OF_MONTH);
        assertEquals(2007, result.get(Calendar.YEAR));
        assertEquals(11, result.get(Calendar.MONTH));
    }

    public void testLastMonthRollover() {
        GregorianCalendar calendar = new GregorianCalendar(2008, 11, 1);
        assertEquals(11, calendar.get(Calendar.MONTH));
        Calendar result = RelativeDateTimeFunction.calcDate(calendar, true, 1, Calendar.MONTH, Calendar.DAY_OF_MONTH);
        assertEquals(2009, result.get(Calendar.YEAR));
        assertEquals(0, result.get(Calendar.MONTH));
    }
}
