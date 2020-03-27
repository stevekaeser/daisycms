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

import org.outerj.daisy.query.model.ValueExpr;

import java.math.BigDecimal;

public class DivideFunction  extends AbstractBinaryMathFunction {
    static final String NAME = "/";

    public String getFunctionName() {
        return NAME;
    }

    protected Object performCalculation(BigDecimal value1, BigDecimal value2) {
        return value1.divide(value2, 8, BigDecimal.ROUND_HALF_DOWN);
    }

    protected String getMathSymbol() {
        return " / ";
    }

    public ValueExpr clone() {
        DivideFunction clone = new DivideFunction();
        super.fillInClone(clone);
        return clone;
    }
}
