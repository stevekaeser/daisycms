/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class AbstractMultiArgPredicate extends AbstractPredicateExpr {
    protected ValueExpr valueExpr;
    protected QValueType valueType;
    protected String name;
    protected List<ValueExpr> args;
    protected boolean isHierarchical;

    public AbstractMultiArgPredicate(MultiArgPredicate.MultiArgPredicateContext multiArgContext) {
        this.valueExpr = multiArgContext.getValueExpr();
        this.valueType = multiArgContext.getValueType();
        this.name = multiArgContext.getName();
        this.args = multiArgContext.getArgs();
        this.isHierarchical = multiArgContext.isHierarcical();
    }

    public void prepare(QueryContext context) throws QueryException {
    }

    /**
     * Gets the list of all argument values. Arguments which are multi value
     * are expanded, that is their 'multiple values' become part of the returned
     * array. The list of returned values may thus be larger then the number of arguments.
     * Arguments might evaluate to null, so the returned array might
     * contain null entries.
     */
    protected Object[] getExpandedArgumentList(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        List<Object> resultValues = new ArrayList<Object>();
        for (ValueExpr arg : args) {
            Object value;
            if (valueExpr.isSymbolicIdentifier())
                value = valueExpr.translateSymbolic(arg, evaluationInfo);
            else
                value = arg.evaluate(valueType, data, evaluationInfo);

            if (arg.isMultiValue())
                resultValues.addAll(Arrays.asList((Object[])value));
            else
                resultValues.add(value);
        }
        return resultValues.toArray();
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        // Note: for now, since the implementations of AbstractMultiArgPredicate are used as
        // delegates of the MultiArgPredicate class, this method does not actually get called,
        // it is the MultiArgPredicate itself which does these things
        valueExpr.collectAccessRestrictions(restrictions);
        for (Expression arg : args) {
            arg.collectAccessRestrictions(restrictions);
        }
    }
}
