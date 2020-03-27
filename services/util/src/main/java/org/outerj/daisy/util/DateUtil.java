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
package org.outerj.daisy.util;

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DateUtil {
    public static Date getNormalizedDate(Date date, boolean keepTime) {
        if (date == null)
            return null;
        
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        if (!keepTime) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        }
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static Date getDate(Calendar cal) {
        if (cal == null) return null;
        return cal.getTime();
    }
}
