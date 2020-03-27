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

import org.outerj.daisy.query.model.SqlGenerationContext;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.repository.query.QueryException;

import java.math.BigDecimal;

public class RoundFunction extends AbstractBinaryMathFunction {
    public static final String NAME = "Round";

    public String getFunctionName() {
        return NAME;
    }

    protected Object performCalculation(BigDecimal value1, BigDecimal value2) {
        int scale = value2.intValue();
        if (scale < 0)
            scale = 0;
        if (value1.scale() > scale)
            return value1.setScale(scale, BigDecimal.ROUND_HALF_DOWN);
        else
            return value1;
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" ROUND( ");
        getParam(0).generateSqlValueExpr(sql, context);
        sql.append(", ");
        getParam(1).generateSqlValueExpr(sql, context);
        sql.append(") ");
    }

    public String buildExpression() {
        return getFunctionName() + "(" + getParam(0).getExpression() + ", " + getParam(1).getExpression() + ")";
    }

    protected String getMathSymbol() {
        throw new RuntimeException("This method should not be called.");
    }

    public ValueExpr clone() {
        RoundFunction clone = new RoundFunction();
        super.fillInClone(clone);
        return clone;
    }
}
