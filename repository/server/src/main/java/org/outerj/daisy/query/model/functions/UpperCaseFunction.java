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

public class UpperCaseFunction extends AbstractCaseFunction {
    public static final String NAME = "UpperCase";

    public String getFunctionName() {
        return NAME;
    }

    protected String getSqlFunctionName(SqlGenerationContext context) {
        return context.getJdbcHelper().getUpperCaseFunction();
    }

    protected String applyCase(String value) {
        return value.toUpperCase();
    }

    public ValueExpr clone() {
        UpperCaseFunction clone = new UpperCaseFunction();
        super.fillInClone(clone);
        return clone;
    }
}

