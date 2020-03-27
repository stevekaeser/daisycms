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
package org.outerj.daisy.query.model;

public class NotEq  extends UnaryPredicateExpr {

    public NotEq(ValueExpr valueExpr1, ValueExpr valueExpr2) {
        super(valueExpr1, valueExpr2);
    }

    protected boolean evaluate(Object value1, Object value2) {
        return !value1.equals(value2);        
    }

    protected final String getOperatorSqlSymbol() {
        return " != ";
    }

    protected boolean makesSenseForNonOrderedValues() {
        return true;
    }
}