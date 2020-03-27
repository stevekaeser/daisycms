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

import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;

import java.util.ArrayList;
import java.util.Iterator;

public final class ValueExprList {
    private final ArrayList valueExprs = new ArrayList();

    public void add(ValueExpr valueExpr) {
        valueExprs.add(valueExpr);
    }

    public void add(int index, ValueExpr valueExpr) {
        valueExprs.add(index, valueExpr);
    }
    
    void prepare(QueryContext context) throws QueryException {
        Iterator valueExprIt = valueExprs.iterator();
        while (valueExprIt.hasNext()) {
            ValueExpr valueExpr = (ValueExpr)valueExprIt.next();
            valueExpr.prepare(context);
        }
    }

    public ValueExpr[] getArray() {
        return (ValueExpr[])valueExprs.toArray(new ValueExpr[valueExprs.size()]);
    }
}
