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

public class ModFunction extends AbstractBinaryMathFunction {
    public static final String NAME = "Mod";

    public String getFunctionName() {
        return NAME;
    }

    protected Object performCalculation(BigDecimal value1, BigDecimal value2) {
        // TODO once we move to Java 1.5, use the build-in remainder function
        // this is just a quick hack to have something
        value1 = value1.abs();
        value2 = value2.abs();
        BigDecimal quotient = value1.divide(value2, BigDecimal.ROUND_HALF_DOWN);
        quotient = quotient.setScale(0, BigDecimal.ROUND_DOWN);
        return value1.subtract(quotient.multiply(value2));
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        sql.append(" MOD( ");
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
        ModFunction clone = new ModFunction();
        super.fillInClone(clone);
        return clone;
    }
}