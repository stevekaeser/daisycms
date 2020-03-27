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

import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.query.model.*;

import java.util.Locale;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class CurrentDateFunction extends AbstractFunction {
    public static final String NAME = "CurrentDate";

    public String getFunctionName() {
        return NAME;
    }

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);
        if (params.size() > 1)
            throw new QueryException(getFunctionName() + " takes at most 1 (optional) parameter.");

        if (params.size() == 1) {
            ValueExpr param = getParam(0);
            if (!ValueExprUtil.isPrimitiveValue(QValueType.STRING, param))
                throw new QueryException("Invalid argument for " + NAME + " function: " + param.getExpression());
        }
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" ? ");
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        stmt.setDate(bindPos, new java.sql.Date(((Date)evaluate(null, null, evaluationInfo)).getTime()));
        return ++bindPos;
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        if (params.size() == 1)
            return getDate((String)getParam(0).evaluate(QValueType.STRING, data, evaluationInfo));
        else
            return getDate(null);
    }

    public QValueType getValueType() {
        return QValueType.DATE;
    }

    public String getTitle(Locale locale) {
        return getExpression();
    }

    public QValueType getOutputValueType() {
        return QValueType.DATE;
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return evaluate(null, data, evaluationInfo);
    }

    protected Date getDate(String expression) throws QueryException {
        Calendar calendar = new GregorianCalendar();
        calendar.setLenient(false);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (expression != null) {
            int[] valueAndField = parseExpression(expression);
            calendar.add(valueAndField[1], valueAndField[0]);
        }
        return calendar.getTime();
    }

    protected int[] parseExpression(String expression) throws QueryException {
        Pattern pattern = Pattern.compile("([+-])\\s*([0-9]+)\\s*(days|weeks|months|years)");
        Matcher matcher = pattern.matcher(expression);
        if (!matcher.matches())
            throw new QueryException("Invalid relative date expression: \"" + expression + "\"");
        int value = Integer.parseInt(matcher.group(2));
        if (matcher.group(1).equals("-"))
            value = value * -1;

        String fieldName = matcher.group(3);
        int field;
        if (fieldName.equals("days"))
            field = Calendar.DATE;
        else if (fieldName.equals("weeks"))
            field = Calendar.WEEK_OF_YEAR;
        else if (fieldName.equals("months"))
            field = Calendar.MONTH;
        else if (fieldName.equals("years"))
            field = Calendar.YEAR;
        else
            throw new RuntimeException("Unexpected value for date field name in " + NAME + " function: " + fieldName);

        return new int[] {value, field};
    }

    public ValueExpr clone() {
        CurrentDateFunction clone = new CurrentDateFunction();
        super.fillInClone(clone);
        return clone;
    }
}
