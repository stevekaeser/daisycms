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

public class RightFunction extends LeftFunction {
    public static final String NAME = "Right";

    public String getFunctionName() {
        return NAME;
    }

    protected String getFunctionName(SqlGenerationContext context) {
        return context.getJdbcHelper().getStringRightFunction();
    }

    protected Object evaluate(String value, int length) throws QueryException {
        if (value == null)
            return null;
        else if (length < 0)
            throw new QueryException("Length parameter of " + NAME + " function cannot be negative.");
        else if (length > value.length())
            return value;
        else
            return value.substring(value.length() - length, value.length());
    }

    public ValueExpr clone() {
        RightFunction clone = new RightFunction();
        super.fillInClone(clone);
        return clone;
    }
}
