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

import org.outerj.daisy.query.EvaluationInfo;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractFunction extends AbstractExpression implements Function {
    protected List<ValueExpr> params = new ArrayList<ValueExpr>(4);
    private String cachedExpression;

    public void addParam(ValueExpr param) {
        params.add(param);
    }

    protected ValueExpr getParam(int index) {
        return params.get(index);
    }

    public void prepare(QueryContext context) throws QueryException {
        if (params.size() == 0)
            return;
        for (ValueExpr param : params) {
            param.prepare(context);
        }
    }

    public String getSqlPreConditions(SqlGenerationContext context) throws QueryException {
        if (params.size() == 0)
            return null;
        StringBuilder preConds = new StringBuilder();
        for (ValueExpr param : params) {
            String preCond = param.getSqlPreConditions(context);
            if (preCond != null) {
                if (preConds.length() > 0)
                    preConds.append(" and ");
                preConds.append(preCond);
            }
        }
        return preConds.length() > 0 ? preConds.toString() : null;
    }

    public int bindPreConditions(PreparedStatement stmt, int bindPos, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        if (params.size() == 0)
            return bindPos;
        for (ValueExpr param : params) {
            bindPos = param.bindPreConditions(stmt, bindPos, evaluationInfo);
        }
        return bindPos;
    }

    public boolean isSymbolicIdentifier() {
        return false;
    }

    public boolean isMultiValue() {
        return false;
    }

    public boolean isHierarchical() {
        return false;
    }

    public boolean isOutputOnly() {
        for (ValueExpr param : params) {
            if (param.isOutputOnly())
                return true;
        }
        return false;
    }

    public AclConditionViolation isAclAllowed() {
        for (ValueExpr param : params) {
            AclConditionViolation violation = param.isAclAllowed();
            if (violation != null)
                return violation;
        }
        return null;
    }

    public Object translateSymbolic(ValueExpr valueExpr, EvaluationInfo evaluationInfo) throws QueryException {
        throw new QueryException("translateSymbolic should not be called if isSymbolic returns false");
    }

    public void generateSqlValueExpr(StringBuilder sql, SqlGenerationContext context) throws QueryException {
        Iterator paramsIt = params.iterator();
        boolean first = true;
        while (paramsIt.hasNext()) {
            if (first)
                first = false;
            else
                sql.append(", ");
            ValueExpr param = (ValueExpr)paramsIt.next();
            param.generateSqlValueExpr(sql, context);
        }
    }

    public int bindValueExpr(PreparedStatement stmt, int bindPos, QValueType valueType, EvaluationInfo evaluationInfo) throws SQLException, QueryException {
        for (ValueExpr param : params) {
            bindPos = param.bindValueExpr(stmt, bindPos, valueType, evaluationInfo);
        }
        return bindPos;
    }

    public final String getExpression() {
        if (cachedExpression == null)
            cachedExpression = buildExpression();
        return cachedExpression;
    }

    protected String buildExpression() {
        StringBuilder expression = new StringBuilder();
        expression.append(getFunctionName()).append("(");
        boolean first = true;
        for (ValueExpr param : params) {
            if (first)
                first = false;
            else
                expression.append(", ");
            expression.append(param.getExpression());
        }
        expression.append(")");
        return expression.toString();
    }

    public boolean canTestAppliesTo() {
        for (ValueExpr param : params) {
            if (!param.canTestAppliesTo())
                return false;
        }
        return true;
    }

    public void collectAccessRestrictions(AccessRestrictions restrictions) {
        for (ValueExpr param : params) {
            param.collectAccessRestrictions(restrictions);
        }
    }

    public void doInContext(SqlGenerationContext context, ContextualizedRunnable runnable) throws QueryException {
        runnable.run(context);
    }

    protected void fillInClone(AbstractFunction clone) {
        clone.params = new ArrayList<ValueExpr>(params.size());
        for (ValueExpr param : params)
            clone.params.add(param.clone());
        clone.cachedExpression = cachedExpression;
    }

    public abstract ValueExpr clone();
}
