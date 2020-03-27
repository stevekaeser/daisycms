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
package org.outerj.daisy.query.model.functions;

import org.outerj.daisy.query.model.QValueType;
import org.outerj.daisy.query.model.ValueExpr;

import java.util.Calendar;

public class RelativeDateFunction extends RelativeDateTimeFunction {
    public static final String NAME = "RelativeDate";

    public String getFunctionName() {
        return NAME;
    }

    public QValueType getValueType() {
        return QValueType.DATE;
    }

    public QValueType getOutputValueType() {
        return QValueType.DATE;
    }

    protected Calendar calcDate(boolean start, int shift, int shiftUnit, int dayInShiftUnit) {
        Calendar calendar = super.calcDate(start, shift, shiftUnit, dayInShiftUnit);

        // for the Date function the time fields should always be null
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        return calendar;
    }

    public ValueExpr clone() {
        RelativeDateFunction clone = new RelativeDateFunction();
        super.fillInClone(clone);
        return clone;
    }
}
