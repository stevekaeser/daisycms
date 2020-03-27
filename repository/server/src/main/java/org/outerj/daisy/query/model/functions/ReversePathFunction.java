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
package org.outerj.daisy.query.model.functions;

import org.outerj.daisy.query.model.QValueType;
import org.outerj.daisy.query.model.ValueExpr;
import org.outerj.daisy.query.model.ExprDocData;
import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.HierarchyPath;

import java.util.Locale;

/**
 * Reverses the order of the elements in a hierarchical path.
 */
public class ReversePathFunction extends AbstractNonBindableFunction {
    public static final String NAME = "ReversePath";

    public String getFunctionName() {
        return NAME;
    }

    public void prepare(QueryContext context) throws QueryException {
        super.prepare(context);
        if (params.size() != 1) {
            throw new QueryException("Function " + getFunctionName() + " takes exactly one argument, at " + getLocation());
        }

        if (!getParam(0).isHierarchical()) {
            throw new QueryException("The argument of " + getFunctionName() + " should be a hierarchical expression.");
        }
    }

    public Object evaluate(QValueType valueType, ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        HierarchyPath hierarchyPath = (HierarchyPath)getParam(0).evaluate(valueType, data, evaluationInfo);
        return hierarchyPath == null ? null : reverse(hierarchyPath);
    }

    private HierarchyPath reverse(HierarchyPath path) {
        Object[] original = path.getElements();
        Object[] dest = new Object[original.length];
        for (int i = 0; i < original.length; i++) {
            dest[i] = original[original.length - i - 1];
        }
        return new HierarchyPath(dest);
    }

    public QValueType getValueType() {
        return getParam(0).getValueType();
    }

    public String getTitle(Locale locale) {
        return getExpression();
    }

    public QValueType getOutputValueType() {
        return getParam(0).getOutputValueType();
    }

    public boolean isHierarchical() {
        return true;
    }

    public Object getOutputValue(ExprDocData data, EvaluationInfo evaluationInfo) throws QueryException {
        HierarchyPath hierarchyPath = (HierarchyPath)getParam(0).getOutputValue(data, evaluationInfo);
        return hierarchyPath == null ? null : reverse(hierarchyPath);
    }

    public ValueExpr clone() {
        ReversePathFunction clone = new ReversePathFunction();
        super.fillInClone(clone);
        return clone;
    }
}