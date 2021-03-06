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

import java.math.BigDecimal;

public class CeilingFunction extends AbstractUnaryMathFunction {
    public static final String NAME = "Ceiling";

    public String getFunctionName() {
        return NAME;
    }

    protected Object performCalculation(BigDecimal value) {
        return value.setScale(0, BigDecimal.ROUND_CEILING);
    }

    protected String[] getSqlFunction(SqlGenerationContext context) {
        return new String[] { "CEILING(", ")" };
    }

    public ValueExpr clone() {
        CeilingFunction clone = new CeilingFunction();
        super.fillInClone(clone);
        return clone;
    }
}