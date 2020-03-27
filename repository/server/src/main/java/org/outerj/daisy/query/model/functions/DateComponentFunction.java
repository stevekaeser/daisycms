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

import java.util.Locale;
import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class DateComponentFunction extends AbstractFunction {
    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);
        if (params.size() != 1)
            throw new QueryException(getFunctionName() + " takes exactly 1 parameter.");
        ValueExpr param = getParam(0);
        if ((param.getValueType() != QValueType.DATE && param.getValueType() != QValueType.DATETIME
                && param.getValueType() != QValueType.UNDEFINED)
                || !ValueExprUtil.isPrimitiveValue(param))
            throw new QueryException("Invalid argument for " + getFunctionName() + " function: " + param.getExpression());
    }

    protected abstract int getCalendarField();

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        Date date = (Date)getParam(0).evaluate(QValueType.DATE, data, evaluationInfo);
        return evaluate(date);
    }

    private Object evaluate(Date date) {
        if (date == null) {
            return null;
        }
        Calendar calendar = new GregorianCalendar(Locale.getDefault());
        calendar.setTime(date);
        calendar.setMinimalDaysInFirstWeek(4); // Important for week function. Currently '4' is choosen to match MySQL Week function with argument 6 (see JdbcHelper)
        return new Long(calendar.get(getCalendarField()) + getAdjustment());
    }

    protected abstract String[] getSqlFunction(SqlGenerationContext context);

    protected int getAdjustment() {
        return 0;
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        String[] sqlFunction = getSqlFunction(context);
        sql.append(' ').append(sqlFunction[0]);
        super.generateSqlValueExpr(sql, context);
        sql.append(sqlFunction[1]).append(' ');
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        return super.bindValueExpr(stmt, bindPos, QValueType.DATE, evaluationInfo);
    }

    public QValueType getValueType() {
        return QValueType.LONG;
    }

    public String getTitle(Locale locale) {
        return getExpression();
    }

    public QValueType getOutputValueType() {
        return QValueType.LONG;
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        return evaluate(null, data, evaluationInfo);
    }
}
