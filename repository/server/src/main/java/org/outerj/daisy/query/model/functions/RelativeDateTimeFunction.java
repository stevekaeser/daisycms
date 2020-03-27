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

import org.outerj.daisy.query.model.*;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.sql.PreparedStatement;
import java.sql.SQLException;


public class RelativeDateTimeFunction extends AbstractFunction {
    public static final String NAME = "RelativeDateTime";

    public String getFunctionName() {
        return NAME;
    }

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);
        if (params.size() != 1)
            throw new QueryException(getFunctionName() + " takes exactly 1 parameter.");

        ValueExpr param = getParam(0);
        if (!ValueExprUtil.isPrimitiveValue(QValueType.STRING, param))
            throw new QueryException("Invalid argument for " + getFunctionName() + " function: " + param.getExpression());
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" ? ");
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        java.sql.Date value = new java.sql.Date(((Date)evaluate(null, null, evaluationInfo)).getTime());
        stmt.setDate(bindPos, value);
        return ++bindPos;
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return getDate((String)getParam(0).evaluate(QValueType.STRING, data, evaluationInfo));
    }

    public QValueType getValueType() {
        return QValueType.DATETIME;
    }

    public String getTitle(Locale locale) {
        return getExpression();
    }

    public QValueType getOutputValueType() {
        return QValueType.DATETIME;
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return evaluate(null, data, evaluationInfo);
    }


    private Date getDate(String spec) throws QueryException {
        Pattern pattern = Pattern.compile("^([a-z]+)\\s([a-z]+)\\s([a-z]+)$");
        Matcher matcher = pattern.matcher(spec);
        if (matcher.matches()) {
            // parse start or end field
            String startOrEnd = matcher.group(1);
            boolean start;
            if (startOrEnd.equals("start"))
                start = true;
            else if (startOrEnd.equals("end"))
                start = false;
            else
                throw new QueryException(getFunctionName() + " function: first part of expression should be 'start' or 'end', but got " + startOrEnd);

            // parse this/next/last
            String shiftParam = matcher.group(2);
            int shift;
            if (shiftParam.equals("this"))
                shift = 0;
            else if (shiftParam.equals("next"))
                shift = 1;
            else if (shiftParam.equals("last"))
                shift = -1;
            else
                throw new QueryException(getFunctionName() + " function: second part of expression should be 'this', 'next' or 'last', but got " + shiftParam);

            // parse week/month/year
            String unitParam = matcher.group(3);
            int shiftUnit;
            int dayInShiftUnit;
            if (unitParam.equals("week")) {
                shiftUnit = Calendar.WEEK_OF_YEAR;
                dayInShiftUnit = Calendar.DAY_OF_WEEK;
            } else if (unitParam.equals("month")) {
                shiftUnit = Calendar.MONTH;
                dayInShiftUnit = Calendar.DAY_OF_MONTH;
            } else if (unitParam.equals("year")) {
                shiftUnit = Calendar.YEAR;
                dayInShiftUnit = Calendar.DAY_OF_YEAR;
            } else {
                throw new QueryException(getFunctionName() + " function: third part of expression should be 'week', 'month' or 'calendar', but got " + unitParam);
            }

            return calcDate(start, shift, shiftUnit, dayInShiftUnit).getTime();
        } else {
            throw new QueryException("Invalid specification for " + getFunctionName() + " function: " + spec);
        }
    }

    protected Calendar calcDate(boolean start, int shift, int shiftUnit, int dayInShiftUnit) {
        GregorianCalendar calendar = new GregorianCalendar();
        return calcDate(calendar, start, shift, shiftUnit, dayInShiftUnit);
    }

    public static Calendar calcDate(GregorianCalendar calendar, boolean start, int shift, int shiftUnit, int dayInShiftUnit) {
        if (shift != 0) {
            // Note: calendar.roll doesn't roll larger fields, so we need to do this ourselves
            int currentValue = calendar.get(shiftUnit);
            int minValue = calendar.getActualMinimum(shiftUnit);
            int maxValue = calendar.getActualMaximum(shiftUnit);

            if (shift < 0 && currentValue == minValue) {
                calendar.roll(Calendar.YEAR, -1);
                calendar.set(shiftUnit, calendar.getActualMaximum(shiftUnit));
            } else if (shift > 0 && currentValue == maxValue) {
                calendar.roll(Calendar.YEAR, 1);
                calendar.set(shiftUnit, calendar.getActualMinimum(shiftUnit));
            } else {
                calendar.roll(shiftUnit, shift);                
            }
        }

        if (start)
            calendar.set(dayInShiftUnit, calendar.getActualMinimum(dayInShiftUnit));
        else
            calendar.set(dayInShiftUnit, calendar.getActualMaximum(dayInShiftUnit));

        if (start) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        } else {
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
        }
        // we only work up to second precision
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar;
    }

    public ValueExpr clone() {
        RelativeDateTimeFunction clone = new RelativeDateTimeFunction();
        super.fillInClone(clone);
        return clone;
    }
}
